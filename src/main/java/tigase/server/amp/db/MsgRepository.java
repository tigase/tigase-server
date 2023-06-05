/*
 * Tigase XMPP Server - The instant messaging server
 * Copyright (C) 2004 Tigase, Inc. (office@tigase.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 */
package tigase.server.amp.db;

import tigase.annotations.TigaseDeprecated;
import tigase.db.*;
import tigase.db.beans.MDRepositoryBeanWithStatistics;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.config.ConfigField;
import tigase.kernel.beans.selector.ConfigType;
import tigase.kernel.beans.selector.ConfigTypeEnum;
import tigase.kernel.core.Kernel;
import tigase.osgi.ModulesManagerImpl;
import tigase.server.BasicComponent;
import tigase.server.xmppsession.SessionManager;
import tigase.xml.Element;
import tigase.xml.SimpleParser;
import tigase.xml.SingletonFactory;
import tigase.xmpp.NotAuthorizedException;
import tigase.xmpp.XMPPResourceConnection;
import tigase.xmpp.jid.BareJID;
import tigase.xmpp.jid.JID;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author andrzej
 */
public abstract class MsgRepository<T, S extends DataSource>
		implements MsgRepositoryIfc<S> {

	public static final String OFFLINE_MSGS_KEY = "offline-msgs";
	public static final String MSGS_STORE_LIMIT_KEY = "store-limit";
	protected static final int MAX_QUEUE_SIZE = 1000;
	private static final long MSGS_STORE_LIMIT_VAL = 100;
	private static final String MSGS_USER_STORE_LIMIT_ENABLE_KEY = "user-store-limit-enable";
	private static final String NULL_STR = "NULL";
	private static final Map<String, MsgRepositoryIfc> repos = new ConcurrentSkipListMap<String, MsgRepositoryIfc>();
	public enum MSG_TYPES {
		none(0),
		message(1),
		presence(2);

		private final int numVal;

		public static MSG_TYPES getFromInt(int type) {
			switch (type) {
				case 1:
					return message;
				case 2:
					return presence;
				case 0:
				default:
					return none;
			}
		}

		MSG_TYPES(int numVal) {
			this.numVal = numVal;
		}

		public int getNumVal() {
			return numVal;
		}

	}
	protected AtomicInteger awaitingInExpiredQueue = new AtomicInteger(0);
	protected long earliestOffline = Long.MAX_VALUE;
	protected SimpleParser parser = SingletonFactory.getParserInstance();
	protected DelayQueue<MsgDBItem<T>> expiredQueue = new DelayQueue<MsgDBItem<T>>() {
		@Override
		public boolean offer(MsgDBItem<T> tMsgDBItem) {
			boolean result = false;
			if (msgRepositoryIfc != null) {
				result = msgRepositoryIfc.offerExpired(MsgRepository.this, tMsgDBItem.db_id, tMsgDBItem.msg, tMsgDBItem.expired);
			} else {
				result = super.offer(tMsgDBItem);
			}
			if (result) {
				awaitingInExpiredQueue.incrementAndGet();
			}
			return result;
		}
	};
	@ConfigField(desc = "Limit of offline messages", alias = "store-limit")
	private long msgs_store_limit = MSGS_STORE_LIMIT_VAL;
	@ConfigField(desc = "Support limits of offline messages set by users", alias = "user-store-limit-enable")
	private boolean msgs_user_store_limit = false;
	@Inject
	private UserRepository userRepository;
	@Inject(nullAllowed = true)
	private MsgRepositoryPoolBean msgRepositoryIfc;

	public static MsgRepositoryIfc getInstance(String cls, String id_string) throws TigaseDBException {
		try {
			if (cls == null) {
				cls = RepositoryFactory.getRepoClassName(MsgRepositoryIfc.class, id_string);
			}
			String key = cls + "#" + id_string;
			MsgRepositoryIfc result = repos.get(key);

			if (result == null) {
				result = (MsgRepositoryIfc) ModulesManagerImpl.getInstance().forName(cls).newInstance();
				repos.put(key, result);
			}

			return result;
		} catch (Exception ex) {
			throw new TigaseDBException("Could not create instance of " + cls + " for uri " + id_string, ex);
		}
	}

	public abstract Queue<Element> loadMessagesToJID(List<String> db_ids, XMPPResourceConnection session,
													 boolean delete, OfflineMessagesProcessor proc)
			throws UserNotFoundException;

	public abstract int deleteMessagesToJID(List<String> db_ids, XMPPResourceConnection session)
			throws UserNotFoundException;

	@Override
	@Deprecated
	public void initRepository(String conn_str, Map<String, String> map) throws DBInitException {

		if (map != null) {
			String msgs_store_limit_str = map.get(MSGS_STORE_LIMIT_KEY);

			if (msgs_store_limit_str != null) {
				msgs_store_limit = Long.parseLong(msgs_store_limit_str);
			}

			String msgs_user_store_limit_enable = map.get(MSGS_USER_STORE_LIMIT_ENABLE_KEY);
			if (msgs_user_store_limit_enable != null) {
				msgs_user_store_limit = Boolean.parseBoolean(msgs_user_store_limit_enable);
			}
		}
	}

	@Override
	@Deprecated
	public Element getMessageExpired(long time, boolean delete) {
		if (expiredQueue.size() == 0) {

			// If the queue is empty load it with some elements
			loadExpiredQueue(MAX_QUEUE_SIZE);
		} else {

			// If the queue is not empty, check whether recently saved off-line
			// message
			// is due to expire sooner then the head of the queue.
			MsgDBItem item = expiredQueue.peek();

			if ((item != null) && (earliestOffline < item.expired.getTime())) {

				// There is in fact off-line message due to expire sooner then the head
				// of the
				// queue. Load all off-line message due to expire sooner then the first
				// element
				// in the queue.
				loadExpiredQueue(item.expired);
			}
		}

		MsgDBItem<T> item = expiredQueue.poll();

		if (item == null) {
			return null;
		}

		awaitingInExpiredQueue.decrementAndGet();
		if (delete) {
			deleteMessage(item.db_id);
		}

		return item.msg;
	}

	@TigaseDeprecated(since = "8.2.0", removeIn = "9.0.0")
	@Deprecated
	@Override
	public void setCondition(ReentrantLock lock, Condition condition) {
	}

	@TigaseDeprecated(since = "8.2.0", removeIn = "9.0.0", note = "Will be replaced by method in MsgRepositoryIfc returning loaded items")
	@Deprecated
	protected abstract void loadExpiredQueue(int max);

	@TigaseDeprecated(since = "8.2.0", removeIn = "9.0.0", note = "Will be replaced by method in MsgRepositoryIfc returning loaded items")
	@Deprecated
	protected abstract void loadExpiredQueue(Date expired);

	protected abstract void deleteMessage(T db_id);

	protected long getMsgsStoreLimit(BareJID userJid, NonAuthUserRepository userRepo) throws UserNotFoundException {
		if (msgs_user_store_limit) {
			String limitStr = userRepo.getPublicData(userJid, OFFLINE_MSGS_KEY, MSGS_STORE_LIMIT_KEY, NULL_STR);
			if (limitStr == null) {
				throw new UserNotFoundException("User " + userJid + " not found in user repository");
			}
			if (NULL_STR != limitStr) {
				long limit = Long.parseLong(limitStr);
				// in case of 0 we need to disable offline storage - not to save all as in case of store-limit
				if (limit == 0) {
					limit = -1;
				}
				return limit;
			}
		} else if (!userRepository.userExists(userJid)) {
			throw new UserNotFoundException("User " + userJid + " not found in user repository");
		}
		return msgs_store_limit;
	}

	public interface OfflineMessagesProcessor {

		public void stamp(Element msg, String msgID);
	}

	public static class MsgDBItem<T>
			implements Delayed {

		public final T db_id;
		public final Date expired;
		public final Element msg;

		public MsgDBItem(T db_id, Element msg, Date expired) {
			this.db_id = db_id;
			this.msg = msg;
			this.expired = expired;
		}

		@Override
		public int compareTo(Delayed o) {
			return (int) (getDelay(TimeUnit.NANOSECONDS) - o.getDelay(TimeUnit.NANOSECONDS));
		}

		@Override
		public long getDelay(TimeUnit unit) {
			return unit.convert(expired.getTime() - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
		}
	}

	/**
	 * Bean used to provide MsgRepository implementations
	 */
	@Bean(name = "msgRepository", parent = Kernel.class, active = true, exportable = true)
	@ConfigType({ConfigTypeEnum.DefaultMode, ConfigTypeEnum.SessionManagerMode, ConfigTypeEnum.ConnectionManagersMode,
				 ConfigTypeEnum.ComponentMode})
	public static class MsgRepositoryMDBean
			extends MDRepositoryBeanWithStatistics<MsgRepositoryIfc>
			implements MsgRepositoryIfc, MsgRepositoryPoolBean {

		private static final Logger log = Logger.getLogger(MsgRepositoryMDBean.class.getCanonicalName());

		private DelayQueue<RepoAwareMsgDBItem> expiredQueue = new DelayQueue<RepoAwareMsgDBItem>();
		private long earliestOffline = Long.MAX_VALUE;

		@TigaseDeprecated(since = "8.2.0", removeIn = "9.0.0")
		@Deprecated
		private final transient ReentrantLock lock = new ReentrantLock();
		@TigaseDeprecated(since = "8.2.0", removeIn = "9.0.0")
		@Deprecated
		private final Condition expiredMessagesCondition = lock.newCondition();

		public MsgRepositoryMDBean() {
			super(MsgRepositoryIfc.class, OfflineMsgRepositoryIfc.class);
		}

		@Override
		public boolean belongsTo(Class<? extends BasicComponent> component) {
			return SessionManager.class.isAssignableFrom(component);
		}

		@Override
		public Element getMessageExpired(long time, boolean delete) {
			// what if some queue has entries one repo (far in the future)
			// but the other repo loaded only part of his queue and some remained in the database?

			if (expiredQueue.size() == 0) {

				// If the queue is empty load it with some elements
				loadExpiredQueue(MAX_QUEUE_SIZE);
			} else {
				// Check if any repository in the poll has empty expiredQueue and if so, try to load for them..
				for (MsgRepositoryIfc repo : getRepositories().values()) {
					if (repo instanceof MsgRepository && ((MsgRepository) repo).awaitingInExpiredQueue.get() == 0) {
						((MsgRepository) repo).loadExpiredQueue(MAX_QUEUE_SIZE);
					}
				}

				// If the queue is not empty, check whether recently saved off-line
				// message
				// is due to expire sooner then the head of the queue.
				RepoAwareMsgDBItem item = expiredQueue.peek();

				if ((item != null) && (earliestOffline < item.expired.getTime())) {

					// There is in fact off-line message due to expire sooner then the head
					// of the
					// queue. Load all off-line message due to expire sooner then the first
					// element
					// in the queue.
					loadExpiredQueue(item.expired);
				}
			}

			RepoAwareMsgDBItem item = null;
			while (item == null) {
				try {
					item = expiredQueue.take();
				} catch (InterruptedException ex) {
				}
			}

			if (item.getRepo() instanceof MsgRepository) {
				((MsgRepository) item.getRepo()).awaitingInExpiredQueue.decrementAndGet();
				if (delete) {
					((MsgRepository) item.getRepo()).deleteMessage(item.db_id);
				}
			}

			return item.msg;
		}

		@Override
		public boolean offerExpired(MsgRepositoryIfc repo, Object id, Element element, Date expired) {
			return expiredQueue.offer(new RepoAwareMsgDBItem(repo, id, element, expired));
		}

		protected void loadExpiredQueue(int min_elements) {
			int max = Math.max(min_elements / getRepositories().size(), 1);
			for (MsgRepositoryIfc repo : getRepositories().values()) {
				if (repo instanceof MsgRepository) {
					((MsgRepository) repo).loadExpiredQueue(max);
				}
			}
			earliestOffline = Long.MAX_VALUE;
		}

		protected void loadExpiredQueue(Date expired) {
			if (expiredQueue.size() > 100 * MAX_QUEUE_SIZE) {
				expiredQueue.clear();
				for (MsgRepositoryIfc repo : getRepositories().values()) {
					if (repo instanceof MsgRepository) {
						((MsgRepository) repo).awaitingInExpiredQueue.set(0);
					}
				}
			}
			
			for (MsgRepositoryIfc repo : getRepositories().values()) {
				if (repo instanceof MsgRepository) {
					((MsgRepository) repo).loadExpiredQueue(expired);
				}
			}
			earliestOffline = Long.MAX_VALUE;
		}

		@Override
		public Queue<Element> loadMessagesToJID(XMPPResourceConnection session, boolean delete)
				throws UserNotFoundException, TigaseDBException {
			Queue<Element> result = null;
			try {
				MsgRepositoryIfc repo = getRepository(session.getBareJID().getDomain());
				result = repo.loadMessagesToJID(session, delete);
			} catch (NotAuthorizedException ex) {
				log.log(Level.WARNING, "Session not authorized yet!", ex);
			}
			return result;
		}

		@Override
		public boolean storeMessage(JID from, JID to, Date expired, Element msg, NonAuthUserRepository userRepo)
				throws UserNotFoundException, TigaseDBException {
			MsgRepositoryIfc repo = getRepository(to.getDomain());
			boolean result = repo.storeMessage(from, to, expired, msg, userRepo);
			if (result && expired != null) {
				if (expired.getTime() < earliestOffline) {
					earliestOffline = expired.getTime();
				}
			}
			return result;
		}

		@Override
		@Deprecated
		public void initRepository(String resource_uri, Map<String, String> params) throws DBInitException {

		}

		@Override
		public Map<Enum, Long> getMessagesCount(JID to) throws UserNotFoundException, TigaseDBException {
			return getRepository(to.getDomain()).getMessagesCount(to);
		}

		@Override
		public List<Element> getMessagesList(JID to) throws UserNotFoundException, TigaseDBException {
			return getRepository(to.getDomain()).getMessagesList(to);
		}

		@Override
		public void setCondition(ReentrantLock lock, Condition condition) {

		}

		@Override
		public int deleteMessagesToJID(List db_ids, XMPPResourceConnection session) throws UserNotFoundException {
			return getRepository(session.getDomainAsJID().getDomain()).deleteMessagesToJID(db_ids, session);
		}

		@Override
		public Queue<Element> loadMessagesToJID(List db_ids, XMPPResourceConnection session, boolean delete,
												OfflineMessagesProcessor proc) throws UserNotFoundException, TigaseDBException {
			return getRepository(session.getDomainAsJID().getDomain()).loadMessagesToJID(db_ids, session, delete, proc);
		}

		@Override
		public void setDataSource(DataSource dataSource) {

		}

		@Override
		public Class<?> getDefaultBeanClass() {
			return MsgRepositoryConfigBean.class;
		}

		@Override
		protected Class<? extends MsgRepositoryIfc> findClassForDataSource(DataSource dataSource)
				throws DBInitException {
			return DataSourceHelper.getDefaultClass(MsgRepository.class, dataSource.getResourceUri());
		}

		@Override
		protected void initializeRepository(String domain, MsgRepositoryIfc repo) {
			super.initializeRepository(domain, repo);
			repo.setCondition(lock, expiredMessagesCondition);
		}

		protected <T> T getValueForDomain(Map<String, T> map, String domain) {
			T value = map.get(domain);
			if (value == null) {
				value = map.get("default");
			}
			return value;
		}

		public static class MsgRepositoryConfigBean
				extends MDRepositoryConfigBean<MsgRepositoryIfc> {

		}

		public static class RepoAwareMsgDBItem extends MsgDBItem {

			private final MsgRepositoryIfc repo;

			public RepoAwareMsgDBItem(MsgRepositoryIfc repo, Object db_id, Element msg, Date expired) {
				super(db_id, msg, expired);
				this.repo = repo;
			}

			public MsgRepositoryIfc getRepo() {
				return repo;
			}
		}
	}

	@TigaseDeprecated(since = "8.2.0", removeIn = "9.0.0", note = "It is expected to be moved to MsgRepositoryIfc")
	@Deprecated
	public interface MsgRepositoryPoolBean<T> {
		boolean offerExpired(MsgRepositoryIfc repo, T id, Element element, Date expired);
	}

}
