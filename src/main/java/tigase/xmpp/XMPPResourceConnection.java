/*
 * XMPPResourceConnection.java
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2013 "Tigase, Inc." <office@tigase.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
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
 */



package tigase.xmpp;

//~--- non-JDK imports --------------------------------------------------------

import tigase.db.AuthorizationException;
import tigase.db.AuthRepository;
import tigase.db.TigaseDBException;
import tigase.db.UserRepository;
import tigase.server.Presence;
import tigase.server.XMPPServer;
import tigase.server.xmppsession.SessionManager;
import tigase.server.xmppsession.SessionManagerHandler;
import tigase.util.TigaseStringprepException;
import tigase.vhosts.VHostItem;
import tigase.xml.Element;

//~--- JDK imports ------------------------------------------------------------






import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Map;
import java.util.function.Function;

/**
 * Describe class XMPPResourceConnection here.
 *
 *
 * Created: Wed Feb 8 22:30:37 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class XMPPResourceConnection
				extends RepositoryAccess {
	/** Field description */
	public static final String ALL_RESOURCES_CAPS_KEY = "caps";

	/** Field description */
	public static final String ALL_RESOURCES_KEY = "all-resources";

	/** Field description */
	public static final String ALL_RESOURCES_PRIORITY_KEY = "priority";

	/** Field description */
	public static final String AUTHENTICATION_TIMEOUT_KEY = "authentication-timeout";

	/** Field description */
	public static final String CLOSING_KEY = "closing-conn";

	/** Field description */
	public static final String ERROR_KEY = "error-key";

	/**
	 * Constant <code>PRESENCE_KEY</code> is a key in temporary session data where
	 * the last presence sent by the user to server is stored, either initial
	 * presence or off-line presence before disconnecting.
	 */
	public static final String PRESENCE_KEY = "user-presence";

	/**
	 * Private logger for class instances.
	 */
	private static final Logger log = Logger.getLogger(XMPPResourceConnection.class
			.getName());

	//~--- fields ---------------------------------------------------------------

	private long authenticationTime = 0;

	/**
	 * This variable is to keep relates XMPPIOService ID only.
	 */
	private JID    connectionId    = null;
	private String connectionState = null;
	private long   creationTime    = 0;
	private String defLang         = "en";
	private long   id_counter      = 0;

	/**
	 * Value of <code>System.currentTimeMillis()</code> from the time when this
	 * session last active from user side.
	 */
	private long                  lastAccessed    = 0;
	private SessionManagerHandler loginHandler    = null;
	private long                  packets_counter = 0;
	private XMPPSession           parentSession   = null;
	private int                   priority        = 0;

	/**
	 * Session resource - part of user's JID for this session
	 */
	private String resource = null;

	/**
	 * Session temporary data. All data stored in this <code>Map</code> disappear
	 * when session finishes.
	 */
	private Map<String, Object> sessionData = null;

	/**
	 * <code>sessionId</code> keeps XMPP stream session ID given at connection
	 * initialization time.
	 */
	private String  sessionId  = null;
	private boolean tmpSession = false;
	private JID     userJid    = null;

	//~--- constructors ---------------------------------------------------------

	/**
	 * Creates a new <code>XMPPResourceConnection</code> instance.
	 *
	 *
	 * @param connectionId
	 * @param rep
	 * @param authRepo
	 * @param loginHandler
	 */
	public XMPPResourceConnection(JID connectionId, UserRepository rep,
			AuthRepository authRepo, SessionManagerHandler loginHandler) {
		super(rep, authRepo);

		long currTime = System.currentTimeMillis();

		this.connectionId = connectionId;
		this.loginHandler = loginHandler;
		this.creationTime = currTime;
		this.lastAccessed = currTime;
		sessionData       = new ConcurrentHashMap<String, Object>(4, 0.9f);
	}

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param jid
	 * @param anonymous
	 * @throws TigaseStringprepException 
	 */
	public void authorizeJID(BareJID jid, boolean anonymous) throws TigaseStringprepException {
		authState = Authorization.AUTHORIZED;
		is_anonymous = anonymous;
		if (jid != null && getDomain().getVhost() != null && !jid.getDomain().equals(getDomain().getVhost().getDomain())) {
			loginHandler.handleDomainChange(jid.getDomain(), this);
		}
		loginHandler.handleLogin(jid, this);
//		if (jid != null && getDomain().getVhost() != null && !jid.getDomain().equals(getDomain().getVhost().getDomain())) {
//			if (log.isLoggable(Level.INFO)) {
//				log.log(Level.INFO, "Replacing session VHost: {0} instead of {1}", new Object[] { jid.getDomain(),
//						getDomain().getVhost().getDomain() });
//			}
//			this.userJid = JID.jidInstanceNS(jid.getLocalpart(), jid.getDomain(), this.userJid.getResource());
//			SessionManager sessMan = (SessionManager) XMPPServer.getConfigurator().getComponent("sess-man");
//			VHostItem vHostItem = sessMan.getVHostItem(this.userJid.getDomain());
//			if (vHostItem == null) {
//				if (log.isLoggable(Level.INFO)) {
//					log.log(Level.INFO, "Can't get VHostItem for domain: {0}, using default one instead: {1}", new Object[] {
//							domain, sessMan.getDefHostName() });
//				}
//				vHostItem = new VHostItem(sessMan.getDefHostName().getDomain());
//			}
//			setDomain(vHostItem.getUnmodifiableVHostItem());
//		}
		login();
	}

	// ~--- methods --------------------------------------------------------------

	/**
	 * Method checks if in {@code parentSession} in session data there is value
	 * for passed {@code key} and returns it if exists. If not then it uses
	 * passed {@code valueFactory} to generate value and sets it in
	 * {@code parentSession} in session data under passed {@code key} and
	 * returns newly set value
	 *
	 * @param key
	 * @param valueFactory
	 * @return 
	 */
	public Object computeCommonSessionDataIfAbsent(String key, Function<String,Object> valueFactory) {
		if (parentSession != null) {
			return parentSession.computeCommonSessionDataIfAbsent(key, valueFactory);
		}
		return valueFactory.apply(key);
	}
	
	/**
	 * Method checks if in session data is value for passed {@code key} and
	 * returns it if exists. If not then it uses passed {@code valueFactory} to
	 * generate value and sets it in session data under passed {@code key} and
	 * returns newly set value
	 *
	 * @param key
	 * @param valueFactory
	 * @return
	 */	
	public Object computeSessionDataIfAbsent(String key, Function<String,Object> valueFactory) {
		lastAccessed = System.currentTimeMillis();
		return sessionData.computeIfAbsent(key, valueFactory);
	}
	
	/**
	 * Returns full user JID for this session without throwing the
	 * <code>NotAuthorizedException</code> exception if session is not authorized
	 * yet and therefore user name and resource is not known yet. Please note this
	 * method is for logging using only to avoid excessive use of try/catch for
	 * debugging code. It may return null.
	 *
	 * @return a <code>String</code> value of calculated user full JID for this
	 *         session including resource name.
	 */
	public final JID getjid() {
		return userJid;
	}

	/**
	 * Method description
	 *
	 */
	public void incPacketsCounter() {
		++packets_counter;
	}

	// ~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param user
	 * @param digest
	 * @param id
	 * @param alg
	 *
	 *
	 *
	 *
	 * @return a value of <code>Authorization</code>
	 * @throws AuthorizationException
	 * @throws NotAuthorizedException
	 * @throws TigaseDBException
	 * @throws TigaseStringprepException
	 */
	@Deprecated
	public final Authorization loginDigest(String user, String digest, String id,
			String alg)
					throws NotAuthorizedException, AuthorizationException, TigaseDBException,
							TigaseStringprepException {
		BareJID       userId = BareJID.bareJIDInstance(user, getDomain().getVhost()
				.getDomain());
		Authorization result = super.loginDigest(userId, digest, id, alg);

		if (result == Authorization.AUTHORIZED) {
			loginHandler.handleLogin(userId, this);
		}    // end of if (result == Authorization.AUTHORIZED)

		return result;
	}

	@Override
	@Deprecated
	public final Authorization loginOther(Map<String, Object> props)
					throws NotAuthorizedException, AuthorizationException, TigaseDBException {
		Authorization result = super.loginOther(props);

		if (result == Authorization.AUTHORIZED) {
			BareJID user = (BareJID) props.get(AuthRepository.USER_ID_KEY);

			if (log.isLoggable(Level.FINEST)) {
				log.finest("UserAuthRepository.USER_ID_KEY: " + user);
			}
			loginHandler.handleLogin(user, this);
		}    // end of if (result == Authorization.AUTHORIZED)

		return result;
	}

	/**
	 * Method description
	 *
	 *
	 * @param user
	 * @param password
	 *
	 *
	 *
	 *
	 * @return a value of <code>Authorization</code>
	 * @throws AuthorizationException
	 * @throws NotAuthorizedException
	 * @throws TigaseDBException
	 * @throws TigaseStringprepException
	 */
	@Deprecated
	public final Authorization loginPlain(String user, String password)
					throws NotAuthorizedException, AuthorizationException, TigaseDBException,
							TigaseStringprepException {
		BareJID       userId = BareJID.bareJIDInstance(user, getDomain().getVhost()
				.getDomain());
		Authorization result = super.loginPlain(userId, password);

		if (result == Authorization.AUTHORIZED) {
			loginHandler.handleLogin(userId, this);
		}    // end of if (result == Authorization.AUTHORIZED)

		return result;
	}

	@Override
	public final void logout() throws NotAuthorizedException {
		loginHandler.handleLogout(getBareJID(), this);
		streamClosed();
		super.logout();
	}

	// public boolean isDummy() {
	// return dummy;
	// }
	// public void setDummy(boolean dummy) {
	// this.dummy = dummy;
	// }

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of <code>String</code>
	 */
	public String nextStanzaId() {
		return "tig" + (++id_counter);
	}

	/**
	 * Method sets passed value under passed key in common sessionData kept
	 * in parentSession
	 *
	 * @param key
	 * @param value
	 */
	public void putCommonSessionData(String key, Object value) {
		if (parentSession != null) {
			parentSession.putCommonSessionData(key, value);
		}
	}
	
	/**
	 * Method sets passed value under passed {@code key} in common {@code sessionData} kept
	 * in {@code parentSession} but only if there is no value for this {@code key} already
	 *
	 * @param key
	 * @param value
	 * @return previous value
	 */
	public Object putCommonSessionDataIfAbsent(String key, Object value) {
		if (parentSession != null) {
			return parentSession.putCommonSessionDataIfAbsent(key, value);
		}
		return null;
	}

	// public void setOnHold() {
	// onHold = true;
	// }
	// public boolean isOnHold() {
	// return onHold;
	// }

	/**
	 * Saves given session data. Data are saved to temporary storage only and are
	 * accessible during this session life only and only from this session
	 * instance.<br>
	 * Any <code>Object</code> can be stored and retrieved through
	 * <code>getSessionData(...)</code>.<br>
	 * To access permanent storage to keep data between session instances you must
	 * use one of <code>get/setData...(...)</code> methods familly. They gives you
	 * access to hierachical permanent data base. Permanent data base however can
	 * be accessed after successuf authorization while session storage is availble
	 * all the time.
	 *
	 * @param key
	 *          a <code>String</code> value of stored data key ID.
	 * @param value
	 *          a <code>Object</code> value of data stored in session.
	 * @see #getSessionData(String)
	 */
	public final void putSessionData(final String key, final Object value) {
		lastAccessed = System.currentTimeMillis();
		sessionData.put(key, value);
	}

	/**
	 * Method sets passed value under passed {@code key} in {@code sessionData} 
	 * but only if there is no value for this {@code key} already
	 *
	 * @param key
	 * @param value
	 * @return previous value
	 */
	public Object putSessionDataIfAbsent(String key, Object value) {
		lastAccessed = System.currentTimeMillis();
		return sessionData.putIfAbsent(key, value);
	}	
	
	@Override
	public void queryAuth(Map<String, Object> authProps) throws TigaseDBException {
		super.queryAuth(authProps);
	}

	/**
	 * Method description
	 *
	 *
	 * @param key
	 *
	 *
	 *
	 * @return a value of <code>Object</code>
	 */
	public Object removeCommonSessionData(String key) {
		return (parentSession == null)
				? null
				: parentSession.removeCommonSessionData(key);
	}

	/**
	 * Method description
	 *
	 *
	 * @param parent
	 */
	public void removeParentSession(final XMPPSession parent) {
		synchronized (this) {
			parentSession = null;
		}
	}

	/**
	 * Method description
	 *
	 *
	 * @param key
	 */
	public final void removeSessionData(final String key) {
		lastAccessed = System.currentTimeMillis();
		sessionData.remove(key);
	}

	// ~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 */
	public void streamClosed() {
		synchronized (this) {
			if (parentSession != null) {
				parentSession.streamClosed(this);
				parentSession = null;
			}
		}    // end of if (parentSession != null)
		resource  = null;
		sessionId = null;
	}

	@Override
	public String toString() {
		return "XMPPResourceConnection=[user_jid=" + userJid + ", packets=" + packets_counter + ", connectioId=" +
				connectionId + ", domain=" + domain.getVhost().getDomain() + ", authState=" +
				getAuthState().name() + ", isAnon=" + isAnonymous()
					 + ", isTmp=" + isTmpSession()
					 + ", parentSession hash=" + System.identityHashCode( parentSession )
					 + ", parentSession liveTime=" + (parentSession != null ? parentSession.getLiveTime() : "")
					 + "]";
	}

	@Override
	public Authorization unregister(String name_param)
					throws NotAuthorizedException, TigaseDBException, TigaseStringprepException {
		Authorization auth_res = super.unregister(name_param);

		return auth_res;
	}

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 *
	 *
	 * @return a value of {@code List<XMPPResourceConnection>}
	 * @throws NotAuthorizedException
	 */
	public List<XMPPResourceConnection> getActiveSessions() throws NotAuthorizedException {
		if (!isAuthorized()) {
			throw new NotAuthorizedException(NOT_AUTHORIZED_MSG);
		}    // end of if (username == null)

		return parentSession.getActiveResources();
	}

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 *
	 * @return a value of <code>JID[]</code>
	 */
	public JID[] getAllResourcesJIDs() {
		return (parentSession == null)
				? null
				: parentSession.getJIDs();
	}

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of <code>AuthRepository</code>
	 */
	public AuthRepository getAuthRepository() {
		return authRepo;
	}

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of <code>long</code>
	 */
	public long getAuthTime() {
		return authenticationTime - creationTime;
	}

	@Override
	public final BareJID getBareJID() throws NotAuthorizedException {
		if (!isAuthorized()) {
			throw new NotAuthorizedException(NOT_AUTHORIZED_MSG);
		}    // end of if (username == null)

		return userJid.getBareJID();
	}

	/**
	 *
	 * @param key
	 *
	 *
	 * @return a value of <code>Object</code>
	 */
	public Object getCommonSessionData(String key) {
		return (parentSession == null)
				? null
				: parentSession.getCommonSessionData(key);
	}

	/**
	 * Gets the value of connectionId
	 *
	 * @return the value of connectionId
	 * @throws NoConnectionIdException
	 */
	public JID getConnectionId() throws NoConnectionIdException {
		lastAccessed = System.currentTimeMillis();
		if (this.connectionId == null) {
			throw new NoConnectionIdException("Connection ID not set for this session. " +
					"This is probably the SM session to handle traffic " +
					"addressed to the server itself. Or maybe it's a bug.");
		}

		return this.connectionId;
	}

	/**
	 * Method description
	 *
	 *
	 * @param jid
	 *
	 *
	 *
	 *
	 * @return a value of <code>JID</code>
	 * @throws NoConnectionIdException
	 */
	public JID getConnectionId(JID jid) throws NoConnectionIdException {
		JID result = null;

		if ((jid != null)) {
			if ((jid.getResource() != null) && (parentSession != null)) {
				XMPPResourceConnection conn = parentSession.getResourceForResource(jid
						.getResource());

				if (conn == null) {
					throw new NoConnectionIdException(
							"No connection available for given resource.");
				} else {
					result = conn.getConnectionId();
				}
			} else {
				if ((jid.getResource() == null) || jid.getResource().equals(this.resource)) {
					result = this.connectionId;
				}
			}
		} else {
			result = this.connectionId;
		}

		return result;
	}

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of <code>long</code>
	 */
	public long getCreationTime() {
		return creationTime;
	}

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of <code>String</code>
	 */
	public String getDefLang() {
		return this.defLang;
	}

	/**
	 * Returns full user JID for this session or throws
	 * <code>NotAuthorizedException</code> if session is not authorized yet and
	 * therefore user name and resource is not known yet.
	 *
	 * @return a <code>String</code> value of calculated user full JID for this
	 *         session including resource name.
	 * @throws NotAuthorizedException
	 */
	public final JID getJID() throws NotAuthorizedException {
		if (!isAuthorized()) {
			throw new NotAuthorizedException(NOT_AUTHORIZED_MSG);
		}    // end of if (username == null)

		return userJid;
	}

	/**
	 * Gets the value of lastAccessed
	 *
	 * @return the value of lastAccessed
	 */
	public long getLastAccessed() {
		return this.lastAccessed;
	}

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of <code>long</code>
	 */
	public long getPacketsCounter() {
		return packets_counter;
	}

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of <code>XMPPSession</code>
	 */
	public XMPPSession getParentSession() {
		return parentSession;
	}

	/**
	 * Returns last presence packet with the user presence status or
	 * <code>null</code> if the user has not yet sent an initial presence.
	 *
	 * @return an <code>Element</code> with last presence status received from the
	 *         user.
	 */
	public Element getPresence() {
		return (Element) getSessionData(PRESENCE_KEY);
	}

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of <code>int</code>
	 */
	public int getPriority() {
		return priority;
	}

	/**
	 * Gets the value of resource
	 *
	 * @return the value of resource
	 */
	public String getResource() {
		return this.resource;
	}

	/**
	 * Retrieves session data. This method gives access to temporary session data
	 * only. You can retrieve earlier saved data giving key ID to receive needed
	 * value. Please see <code>putSessionData</code> description for more details.
	 *
	 * @param key
	 *          a <code>String</code> value of stored data ID.
	 * @return a <code>Object</code> value of data for given key.
	 * @see #putSessionData(String, Object)
	 */
	public final Object getSessionData(final String key) {
		lastAccessed = System.currentTimeMillis();

		return sessionData.get(key);
	}

	/**
	 * Gets the value of sessionId
	 *
	 * @return the value of sessionId
	 */
	public String getSessionId() {
		return this.sessionId;
	}

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of <code>JID</code>
	 */
	public JID getSMComponentId() {
		return loginHandler.getComponentId();
	}

	/**
	 * To get the user bare JID please use <code>getBareJID</code> method, to
	 * check the whether the user with given BareJID is owner of the session
	 * please use method <code>isUserId(...)</code>. From now one the user session
	 * may handle more than a single userId, hence getting just userId is not
	 * enough to check whether the user Id belongs to the session.
	 *
	 *
	 *
	 *
	 *
	 * @return a value of <code>BareJID</code>
	 * @throws NotAuthorizedException
	 *
	 * @deprecated
	 */
	@Deprecated
	public BareJID getUserId() throws NotAuthorizedException {
		return this.getBareJID();
	}

	@Override
	public final String getUserName() throws NotAuthorizedException {
		if (!isAuthorized()) {
			throw new NotAuthorizedException(NOT_AUTHORIZED_MSG);
		}    // end of if (username == null)

		return parentSession.getUserName();
	}

	// ~--- get methods ----------------------------------------------------------

	@Override
	public boolean isAuthorized() {
		return super.isAuthorized() && (parentSession != null);
	}

	/**
	 * Method description
	 *
	 *
	 * @param outDomain
	 * @param includeComponents
	 *
	 *
	 *
	 * @return a value of <code>boolean</code>
	 */
	public boolean isLocalDomain(String outDomain, boolean includeComponents) {
		return loginHandler.isLocalDomain(outDomain, includeComponents);
	}

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of <code>boolean</code>
	 */
	public boolean isResourceSet() {
		return this.resource != null;
	}

	/**
	 * Returns information whether this is a server (SessionManager) session or
	 * normal user session. The server session is used to handle packets addressed
	 * to the server itself (local domain name).
	 *
	 * @return a <code>boolean</code> value of <code>true</code> if this is the
	 *         server session and <code>false</code> otherwise.
	 */
	public boolean isServerSession() {
		return false;
	}

	/**
	 * Method description
	 *
	 *
	 * @return a value of <code>boolean</code>
	 */
	public boolean isTmpSession() {
		return tmpSession;
	}

	/**
	 * Method description
	 *
	 *
	 * @param bareJID
	 *
	 *
	 *
	 *
	 * @return a value of <code>boolean</code>
	 * @throws NotAuthorizedException
	 */
	public boolean isUserId(BareJID bareJID) throws NotAuthorizedException {
		if (!isAuthorized()) {
			throw new NotAuthorizedException(NOT_AUTHORIZED_MSG);
		}    // end of if (username == null)

		return userJid.getBareJID().equals(bareJID);
	}

	//~--- set methods ----------------------------------------------------------

	/**
	 * Sets the value of connectionId
	 *
	 * @param connectionId is a <code>JID</code>
	 */
	public void setConnectionId(JID connectionId) {
		this.connectionId = connectionId;
	}

	/**
	 * Method description
	 *
	 *
	 * @param lang
	 */
	public void setDefLang(String lang) {
		this.defLang = lang;
	}

	/**
	 * Sets the value of lastAccessed
	 *
	 * @param argLastAccessed
	 *          Value to assign to this.lastAccessed
	 */
	public void setLastAccessed(final long argLastAccessed) {
		this.lastAccessed = argLastAccessed;
	}

	/**
	 * Method description
	 *
	 *
	 * @param parent
	 *
	 * @throws TigaseStringprepException
	 */
	public void setParentSession(final XMPPSession parent)
					throws TigaseStringprepException {
		synchronized (this) {
			if (parent != null) {
				userJid = JID.jidInstance(parent.getUserName(), domain.getVhost().getDomain(),
						((resource != null)
						? resource
						: sessionId));
			}
			this.parentSession = parent;
		}
	}

	/**
	 * Method description
	 *
	 *
	 * @param packet
	 */
	public void setPresence(Element packet) {
		putSessionData(PRESENCE_KEY, packet);

		// Parse resource priority:
		String pr_str = packet.getCDataStaticStr(Presence.PRESENCE_PRIORITY_PATH);

		if (pr_str != null) {
			int pr = 1;

			try {
				pr = Integer.decode(pr_str);
			} catch (NumberFormatException e) {
				if (log.isLoggable(Level.FINER)) {
					log.finer("Incorrect priority value: " + pr_str + ", setting 1 as default.");
				}
				pr = 1;
			}
			setPriority(pr);
		} else {
			// workaround for case when presence update came before presence was broadcasted due to 
			// loading of roster data in roster processing thread
			if (getPriority() != 0 && !"unavailable".equals(packet.getAttributeStaticStr("type"))) {
				packet.addChild(new Element("priority", String.valueOf(getPriority())));
			}
			putSessionData(PRESENCE_KEY, packet);
		}
		loginHandler.handlePresenceSet(this);
	}

	/**
	 * Method description
	 *
	 *
	 * @param priority
	 */
	public void setPriority(final int priority) {
		this.priority = priority;
	}

	/**
	 * Sets the connection resource
	 *
	 * @param argResource
	 *          Value to assign to this.resource
	 * @throws NotAuthorizedException
	 * @throws TigaseStringprepException
	 */
	public void setResource(final String argResource)
					throws NotAuthorizedException, TigaseStringprepException {
		if (!isAuthorized()) {
			throw new NotAuthorizedException(NOT_AUTHORIZED_MSG);
		}    // end of if (username == null)
		this.resource = argResource;

		// This is really unlikely a parent session would be null here but it may
		// happen when the user disconnects just after sending resource bind.
		// Due to asynchronous nature of packets processing in the Tigase the
		// the authorization might be canceled while resource bind packet still
		// waits in the queue.....
		// This has already happened....
		if (parentSession != null) {
			parentSession.addResourceConnection(this);
		}
		userJid = userJid.copyWithResource((resource == null)
				? sessionId
				: resource);
		loginHandler.handleResourceBind(this);
	}

	/**
	 * Sets the value of sessionId
	 *
	 * @param argSessionId
	 *          Value to assign to this.sessionId
	 */
	public void setSessionId(final String argSessionId) {
		this.sessionId = argSessionId;
	}

	/**
	 * Method description
	 *
	 *
	 * @param tmp is a <code>boolean</code>
	 */
	public void setTmpSession(boolean tmp) {
		tmpSession = tmp;
	}

	//~--- methods --------------------------------------------------------------

	@Override
	protected void login() {
		authenticationTime = System.currentTimeMillis();
	}

	public boolean isEncrypted() {
		String tls = (String) getSessionData("starttls");
		return tls != null && "true".equals(tls);
	}
	
	public boolean isTlsRequired() {
		VHostItem vhost = getDomain();
		try {
			if ( null != getSessionData( "SSL" ) && (boolean) getSessionData( "SSL" ) ){
				return false;
			}
			if ("c2s".equals(getConnectionId().getLocalpart()) )
				return vhost.isTlsRequired();
			else
				return false;
		} catch (NoConnectionIdException e) {
			log.log(Level.WARNING, "Can't check sessionId", e);
			return vhost.isTlsRequired();
		}
	}
	
}    // XMPPResourceConnection


//~ Formatted in Tigase Code Convention on 13/11/02
