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
package tigase.server.amp;

import tigase.db.MsgRepositoryIfc;
import tigase.db.RepositoryFactory;
import tigase.db.TigaseDBException;
import tigase.db.UserNotFoundException;

import tigase.xmpp.BareJID;
import tigase.xmpp.JID;

import tigase.osgi.ModulesManagerImpl;
import tigase.xml.Element;
import tigase.xml.SimpleParser;
import tigase.xml.SingletonFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import tigase.db.DBInitException;
import tigase.db.NonAuthUserRepository;
import tigase.vhosts.VHostItem;
import tigase.xmpp.XMPPResourceConnection;

/**
 *
 * @author andrzej
 */
public abstract class MsgRepository<T> implements MsgRepositoryIfc {

	public static final String OFFLINE_MSGS_KEY = "offline-msgs";
	private static final long MSGS_STORE_LIMIT_VAL = 100;
	public static final String MSGS_STORE_LIMIT_KEY = "store-limit";
	private static final String MSGS_USER_STORE_LIMIT_ENABLE_KEY = "user-store-limit-enable";
	
	protected static final int MAX_QUEUE_SIZE = 1000;
	
	private static final Map<String, MsgRepositoryIfc> repos =
			new ConcurrentSkipListMap<String, MsgRepositoryIfc>();
	
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
	protected DelayQueue<MsgDBItem> expiredQueue = new DelayQueue<MsgDBItem>();
	protected long broadcastMessagesLastCleanup = 0;
	protected Map<String,BroadcastMsg> broadcastMessages = new ConcurrentHashMap<String,BroadcastMsg>();

	private long msgs_store_limit = MSGS_STORE_LIMIT_VAL;
	private boolean msgs_user_store_limit = false;
	
	protected abstract void loadExpiredQueue(int max);
	protected abstract void loadExpiredQueue(Date expired);
	protected abstract void deleteMessage(T db_id);
	
	public abstract void loadMessagesToBroadcast();
	protected abstract void ensureBroadcastMessageRecipient(String id, BareJID recipient);
	protected abstract void insertBroadcastMessage(String id, Element msg, Date expire, BareJID recipient);

	public abstract Map<Enum,Long> getMessagesCount(JID to)  throws UserNotFoundException;
	public abstract List<Element> getMessagesList(JID to)  throws UserNotFoundException;
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

	public BroadcastMsg getBroadcastMsg(String id) {
		return broadcastMessages.get(id);
	}
	
	public String dumpBroadcastMessageKeys() {
		StringBuilder sb = new StringBuilder();
		for (String key : broadcastMessages.keySet()) {
			if (sb.length() == 0)
				sb.append("[");
			else
				sb.append(",");
			sb.append(key);
		}
		return sb.append("]").toString();
	}
	
	public Collection<BroadcastMsg> getBroadcastMessages() { 
		long now = System.currentTimeMillis();
		if (now - broadcastMessagesLastCleanup > 60 * 1000) {
			broadcastMessagesLastCleanup = now;
			List<String> toRemove = new ArrayList<String>();
			for (Map.Entry<String,BroadcastMsg> e : broadcastMessages.entrySet()) {
				if (e.getValue().getDelay(TimeUnit.MILLISECONDS) < 0) {
					toRemove.add(e.getKey());
				}
			}
			for (String key : toRemove)
				broadcastMessages.remove(key);
		}
		return Collections.unmodifiableCollection(broadcastMessages.values());
	}
	
	public boolean updateBroadcastMessage(String id, Element msg, Date expire, BareJID recipient) {
		boolean isNew = false;
		synchronized (broadcastMessages) {
			MsgRepository.BroadcastMsg bmsg = broadcastMessages.get(id);
			if (bmsg == null) {
				bmsg = new MsgRepository.BroadcastMsg(null, msg, expire);
				broadcastMessages.put(id, bmsg);
				isNew = true;
				insertBroadcastMessage(id, msg, expire, recipient);
			}
			if (bmsg.addRecipient(recipient)) {
				ensureBroadcastMessageRecipient(id, recipient);
			}
			return isNew;
		}
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

		MsgDBItem item = null;

		while (item == null) {
			try {
				item = expiredQueue.take();
			} catch (InterruptedException ex) {
			}
		}

		if (delete) {
			deleteMessage(item.db_id);
		}

		return item.msg;
	}

	public String getStanzaTo() {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}
	
	// ~--- inner classes --------------------------------------------------------

	protected class MsgDBItem implements Delayed {
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
		
	public class BroadcastMsg extends MsgDBItem {
		
		private JidResourceMap<Boolean> recipients = new JidResourceMap<Boolean>();
		
		public BroadcastMsg(T db_id, Element msg, Date expired) {
			super(db_id, msg, expired);
		}
				
		protected boolean addRecipient(BareJID jid) {
			if (recipients.containsKey(jid))
				return false;
			recipients.put(JID.jidInstance(jid), Boolean.TRUE);
			return true;
		}
		
		public boolean needToSend(JID jid) {
			return recipients.containsKey(jid.getBareJID()) 
					&& (jid.getResource() == null || !recipients.containsKey(jid));
		}
		
		public void markAsSent(JID jid) {
			recipients.put(jid, Boolean.TRUE);
		}
		
	}

	public interface OfflineMessagesProcessor {
		public void stamp(Element msg, String msgID);
	}

}
