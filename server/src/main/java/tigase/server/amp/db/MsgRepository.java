/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2014 "Artur Hefczyc" <artur.hefczyc@tigase.org>
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
 *
 * $Rev$
 * Last modified by $Author$
 * $Date$
 */
package tigase.server.amp.db;

import tigase.db.*;
import tigase.db.beans.MDRepositoryBeanWithStatistics;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.config.ConfigField;
import tigase.kernel.core.Kernel;
import tigase.osgi.ModulesManagerImpl;
import tigase.server.BasicComponent;
import tigase.server.xmppsession.SessionManager;
import tigase.xml.Element;
import tigase.xml.SimpleParser;
import tigase.xml.SingletonFactory;
import tigase.xmpp.BareJID;
import tigase.xmpp.JID;
import tigase.xmpp.NotAuthorizedException;
import tigase.xmpp.XMPPResourceConnection;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author andrzej
 */
public abstract class MsgRepository<T,S extends DataSource> implements MsgRepositoryIfc<S> {

	public static final String OFFLINE_MSGS_KEY = "offline-msgs";
	private static final long MSGS_STORE_LIMIT_VAL = 100;
	public static final String MSGS_STORE_LIMIT_KEY = "store-limit";
	private static final String MSGS_USER_STORE_LIMIT_ENABLE_KEY = "user-store-limit-enable";
	
	protected static final int MAX_QUEUE_SIZE = 1000;
	
	private static final Map<String, MsgRepositoryIfc> repos =
			new ConcurrentSkipListMap<String, MsgRepositoryIfc>();
	private ReentrantLock expiredMessagesLock;
	private Condition expiredMessagesCondition;

	public enum MSG_TYPES { none(0), message(1), presence(2);

    private final int numVal;

    MSG_TYPES(int numVal) {
        this.numVal = numVal;
    }

    public int getNumVal() {
        return numVal;
    }

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

	};

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

	protected SimpleParser parser = SingletonFactory.getParserInstance();	
	protected long earliestOffline = Long.MAX_VALUE;
	protected DelayQueue<MsgDBItem<T>> expiredQueue = new DelayQueue<MsgDBItem<T>>() {
		@Override
		public boolean offer(MsgDBItem msgDBItem) {
			expiredMessagesLock.lock();
			boolean result = false;;
			try {
				result =  super.offer(msgDBItem);
				if (result && expiredMessagesCondition != null)
					expiredMessagesCondition.signal();
			} finally {
				expiredMessagesLock.unlock();
			}
			return result;
		}
	};

	@ConfigField(desc = "Limit of offline messages", alias = "store-limit")
	private long msgs_store_limit = MSGS_STORE_LIMIT_VAL;
	@ConfigField(desc = "Support limits of offline messages set by users", alias = "user-store-limit-enable")
	private boolean msgs_user_store_limit = false;
	
	protected abstract void loadExpiredQueue(int max);
	protected abstract void loadExpiredQueue(Date expired);
	protected abstract void deleteMessage(T db_id);

	public abstract	Queue<Element> loadMessagesToJID(List<String> db_ids,  XMPPResourceConnection session, boolean delete,
																		OfflineMessagesProcessor proc ) throws UserNotFoundException;
	public abstract	int deleteMessagesToJID( List<String> db_ids, XMPPResourceConnection session) throws UserNotFoundException;

	@Override
	public void initRepository(String conn_str, Map<String, String> map)
			throws DBInitException {
		
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

	protected long getMsgsStoreLimit(BareJID userJid, NonAuthUserRepository userRepo) {
		if (msgs_user_store_limit) {
			try {
				String limitStr = userRepo.getPublicData(userJid, OFFLINE_MSGS_KEY, MSGS_STORE_LIMIT_KEY, null);
				if (limitStr != null) {
					long limit = Long.parseLong(limitStr);
					// in case of 0 we need to disable offline storage - not to save all as in case of store-limit
					if (limit == 0)
						limit = -1;
					return limit;
				}
			} catch (UserNotFoundException ex) {
				// should not happen
			}
		}
		return msgs_store_limit;
	}

	@Override
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

		if (delete) {
			deleteMessage(item.db_id);
		}

		return item.msg;
	}

	@Override
	public void setCondition(ReentrantLock lock, Condition condition) {
		this.expiredMessagesLock = lock;
		this.expiredMessagesCondition = condition;
	}

	// ~--- inner classes --------------------------------------------------------

	public static class MsgDBItem<T> implements Delayed {
		public final T db_id;
		public final Date expired;
		public final Element msg;

		// ~--- constructors -------------------------------------------------------

		public MsgDBItem(T db_id, Element msg, Date expired) {
			this.db_id = db_id;
			this.msg = msg;
			this.expired = expired;
		}

		// ~--- methods ------------------------------------------------------------

		@Override
		public int compareTo(Delayed o) {
			return (int) (getDelay(TimeUnit.NANOSECONDS) - o.getDelay(TimeUnit.NANOSECONDS));
		}

		// ~--- get methods --------------------------------------------------------

		@Override
		public long getDelay(TimeUnit unit) {
			return unit.convert(expired.getTime() - System.currentTimeMillis(),
					TimeUnit.MILLISECONDS);
		}
	}	

	public interface OfflineMessagesProcessor {
		public void stamp(Element msg, String msgID);
	}

	/**
	 * Bean used to provide MsgRepository implementations
	 */
	@Bean(name = "msgRepository", parent = Kernel.class, active = true, exportable = true)
	public static class MsgRepositoryMDBean extends MDRepositoryBeanWithStatistics<MsgRepositoryIfc>
			implements MsgRepositoryIfc {

		private static final Logger log = Logger.getLogger(MsgRepositoryMDBean.class.getCanonicalName());

		private final transient ReentrantLock lock = new ReentrantLock();
		private final Condition expiredMessagesCondition = lock.newCondition();

		public MsgRepositoryMDBean() {
			super(MsgRepositoryIfc.class,OfflineMsgRepositoryIfc.class);
		}

		@Override
		public boolean belongsTo(Class<? extends BasicComponent> component) {
			return SessionManager.class.isAssignableFrom(component);
		}

		@Override
		protected Class<? extends MsgRepositoryIfc> findClassForDataSource(DataSource dataSource) throws DBInitException {
			return DataSourceHelper.getDefaultClass(MsgRepository.class, dataSource.getResourceUri());
		}

		@Override
		public Element getMessageExpired(long time, boolean delete) {
			lock.lock();
			try {
				for (MsgRepositoryIfc repo : getRepositories().values()) {
					Element el = repo.getMessageExpired(time, delete);
					if (el != null)
						return el;
				}
				expiredMessagesCondition.await();
			} catch (InterruptedException e) {
				log.log(Level.FINER, "awaiting for expired messages interrupted");
			} finally {
				lock.unlock();
			}

			return null;
		}

		@Override
		public Queue<Element> loadMessagesToJID(XMPPResourceConnection session, boolean delete) throws UserNotFoundException {
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
		public boolean storeMessage(JID from, JID to, Date expired, Element msg, NonAuthUserRepository userRepo) throws UserNotFoundException {
			MsgRepositoryIfc repo = getRepository(to.getDomain());
			return repo.storeMessage(from, to, expired, msg, userRepo);
		}

		@Override
		protected void initializeRepository(String domain, MsgRepositoryIfc repo) {
			super.initializeRepository(domain, repo);
			repo.setCondition(lock, expiredMessagesCondition);
		}

		protected <T> T getValueForDomain(Map<String,T> map, String domain) {
			T value = map.get(domain);
			if (value == null)
				value = map.get("default");
			return value;
		}

		@Override
		public void initRepository(String resource_uri, Map<String, String> params) throws DBInitException {

		}

		@Override
		public Map<Enum, Long> getMessagesCount(JID to) throws UserNotFoundException {
			return getRepository(to.getDomain()).getMessagesCount(to);
		}

		@Override
		public List<Element> getMessagesList(JID to) throws UserNotFoundException {
			return getRepository(to.getDomain()).getMessagesList(to);
		}

		@Override
		public void setCondition(ReentrantLock lock, Condition condition) {

		}

		@Override
		public void setDataSource(DataSource dataSource) {

		}

		@Override
		public Class<?> getDefaultBeanClass() {
			return MsgRepositoryConfigBean.class;
		}

		public static class MsgRepositoryConfigBean extends MDRepositoryConfigBean<MsgRepositoryIfc> {

		}
	}

}
