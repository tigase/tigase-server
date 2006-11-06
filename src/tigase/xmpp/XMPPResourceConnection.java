/*  Package Jabber Server
 *  Copyright (C) 2001, 2002, 2003, 2004, 2005
 *  "Artur Hefczyc" <kobit@users.sourceforge.net>
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software Foundation,
 *  Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 * $Rev$
 * Last modified by $Author$
 * $Date$
 */
package tigase.xmpp;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import tigase.util.JID;
import tigase.db.UserRepository;
import tigase.db.UserAuthRepository;
import tigase.db.TigaseDBException;
import tigase.db.UserNotFoundException;
import tigase.auth.LoginHandler;
import java.security.NoSuchAlgorithmException;

/**
 * Describe class XMPPResourceConnection here.
 *
 *
 * Created: Wed Feb  8 22:30:37 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class XMPPResourceConnection extends RepositoryAccess {

  /**
   * Private logger for class instancess.
   */
  private static final Logger log =
		Logger.getLogger("tigase.xmpp.XMPPResourceConnection");

	private LoginHandler loginHandler = null;
	private XMPPSession parentSession = null;
	private UserAuthRepository authRepo = null;

	//private LoginContext loginContext = null;

	private String sessionId = null;
  /**
   * Session resource - part of user's JID for this session
   */
	private String resource = null;
  /**
   * Value of <code>System.currentTimeMillis()</code> from the time when this
   * session last active from user side.
   */
  private long lastAccessed = 0;

	private String domain = null;

	/**
	 * This variable is to keep relates XMPPIOService ID only.
	 */
	private String connectionId = null;

	private int priority = 0;

  /**
   * Current authorization state - initialy session i <code>NOT_AUTHORIZED</code>.
   * It becomes <code>AUTHORIZED</code>
   */
	private Authorization authState = Authorization.NOT_AUTHORIZED;
	/**
	 * Session temporary data. All data stored in this <code>Map</code> disapear
	 * when session finishes.
	 */
	private Map<String, Object> sessionData = null;

	/**
	 * Creates a new <code>XMPPResourceConnection</code> instance.
	 *
	 */
	public XMPPResourceConnection(String connectionId, UserRepository rep,
		UserAuthRepository authRepo, LoginHandler loginHandler) {
		super(rep);
		this.connectionId = connectionId;
		this.authRepo = authRepo;
		this.loginHandler = loginHandler;
    sessionData = new HashMap<String, Object>();
	}

  /**
   * Saves given session data. Data are saved to temporary storage only and are
   * accessible during this session life only and only from this session
   * instance.<br/>
   * Any <code>Object</code> can be stored and retrieved through
   * <code>getSessionData(...)</code>.<br/>
   * To access permanent storage to keep data between session instances you must
   * use one of <code>get/setData...(...)</code> methods familly. They gives you
   * access to hierachical permanent data base. Permanent data base however can
   * be accessed after successuf authorization while session storage is availble
   * all the time.
   *
   * @param key a <code>String</code> value of stored data key ID.
   * @param value a <code>Object</code> value of data stored in session.
   * @see #getSessionData(String)
   */
  public final void putSessionData(final String key, final Object value) {
    lastAccessed = System.currentTimeMillis();
    sessionData.put(key, value);
  }

  /**
   * Retrieves session data.
   * This method gives access to temporary session data only.
   * You can retrieve earlier saved data giving key ID to receive needed
   * value. Please see <code>putSessionData</code> description for more details.
   *
   * @param key a <code>String</code> value of stored data ID.
   * @return a <code>Object</code> value of data for given key.
   * @see #putSessionData(String, Object)
   */
  public final Object getSessionData(final String key) {
    lastAccessed = System.currentTimeMillis();
    return sessionData.get(key);
  }

	public void setPriority(final int priority) {
		this.priority = priority;
	}

	public int getPriority() {
		return priority;
	}

	public void streamClosed() {
		if (parentSession != null) {
			parentSession.streamClosed(this);
		} // end of if (parentSession != null)
		parentSession = null;
		authState = Authorization.NOT_AUTHORIZED;
		resource = null;
		sessionId = null;
	}

	public void setParentSession(final XMPPSession parent) {
		this.parentSession = parent;
	}

	public XMPPSession getParentSession() {
		return parentSession;
	}

  /**
   * Returns full user JID for this session or throws
   * <code>NotAuthorizedException</code> if session is not authorized yet and
   * therefore user name and resource is not known yet.
   *
   * @return a <code>String</code> value of calculated user full JID for this
   * session including resource name.
   * @exception NotAuthorizedException when this session has not
   * been authorized yet and some parts of user JID are not known yet.
   */
  public final String getJID() throws NotAuthorizedException {
    return getUserId() + "/" + resource;
  }

  /**
   * Returns user JID but without <em>resource</em> part. This is real user ID
   * not session ID.
   * To retrieve session ID - full JID refer to <code>getJID()</code>
   * method.<br/>
   * If session has not been authorized yet this method throws
   * <code>NotAuthorizedException</code>.
   *
   * @return a <code>String</code> value of user ID - this is user JID without
   * resource part. To obtain full user JID please refer to <code>getJID</code>
   * method.
   * @exception NotAuthorizedException when this session has not
   * been authorized yet and some parts of user JID are not known yet.
   * @see #getJID()
   */
  public final String getUserId() throws NotAuthorizedException {
    if (parentSession == null) {
      throw new NotAuthorizedException(NOT_AUTHORIZED_MSG);
    } // end of if (username == null)
    return JID.getNodeID(parentSession.getUserName(), domain);
  }

	public final String getUserName() throws NotAuthorizedException {
    if (parentSession == null) {
      throw new NotAuthorizedException(NOT_AUTHORIZED_MSG);
    } // end of if (username == null)
    return parentSession.getUserName();
	}

	public List<XMPPResourceConnection> getActiveSessions()
		throws NotAuthorizedException {
    if (parentSession == null) {
      throw new NotAuthorizedException(NOT_AUTHORIZED_MSG);
    } // end of if (username == null)
		return parentSession.getActiveResources();
	}

	public String[] getAllResourcesJIDs() throws NotAuthorizedException {
    if (parentSession == null) {
      throw new NotAuthorizedException(NOT_AUTHORIZED_MSG);
    } // end of if (username == null)
		return parentSession.getJIDs();
	}

	public void setDomain(final String domain) {
		this.domain = domain;
	}

	public String getDomain() {
		return domain;
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
	 * Sets the value of sessionId
	 *
	 * @param argSessionId Value to assign to this.sessionId
	 */
	public void setSessionId(final String argSessionId) {
		this.sessionId = argSessionId;
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
	 * Sets the value of resource
	 *
	 * @param argResource Value to assign to this.resource
	 */
	public void setResource(final String argResource) {
		this.resource = argResource;
		parentSession.resourceSet(this);
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
	 * Sets the value of lastAccessed
	 *
	 * @param argLastAccessed Value to assign to this.lastAccessed
	 */
	public void setLastAccessed(final long argLastAccessed) {
		this.lastAccessed = argLastAccessed;
	}

	/**
	 * Gets the value of connectionId
	 *
	 * @return the value of connectionId
	 */
	public String getConnectionId() {
    lastAccessed = System.currentTimeMillis();
		return this.connectionId;
	}

	/**
	 * Gets the value of authState
	 *
	 * @return the value of authState
	 */
	public Authorization getAuthState() {
		return this.authState;
	}

	/**
	 * Sets the value of authState
	 *
	 * @param argAuthState Value to assign to this.authState
	 */
	private void setAuthState(final Authorization argAuthState) {
		this.authState = argAuthState;
	}

  /**
   * This method allows you test this session if it already has been authorized.
   * If <code>true</code> is returned as method result it means session has
   * already been authorized, if <code>false</code> however session is still not
   * authorized.
   *
   * @return a <code>boolean</code> value which informs whether this session has
   * been already authorized or not.
   */
  public final boolean isAuthorized() {
    lastAccessed = System.currentTimeMillis();
    return authState == Authorization.AUTHORIZED;
  }

  /**
   * <code>authorize</code> method performs authorization with given
   * password as plain text.
   * If <code>AUTHORIZED</code> has been returned it means authorization
   * process is successful and session has been activated, otherwise session
   * hasn't been authorized and return code gives more detailed information
   * of fail reason. Please refer to <code>Authorizaion</code> documentation for
   * more details.
   *
   * @return a <code>Authorization</code> value of result code.
   */
  public final Authorization loginPlain(String user, String password)
		throws NotAuthorizedException {
		try {
			if (authRepo.plainAuth(JID.getNodeID(user, getDomain()), password)) {
				authState = Authorization.AUTHORIZED;
				loginHandler.handleLogin(user, this);
			} // end of if (authRepo.loginPlain())auth.login();
			return authState;
    } catch (UserNotFoundException e) {
      log.log(Level.WARNING, "Problem accessing reposiotry: ", e);
      throw new NotAuthorizedException("Authorization failed", e);
    } catch (TigaseDBException e) {
			log.log(Level.SEVERE, "Repository access exception.", e);
      throw new NotAuthorizedException("Authorization failed", e);
		} // end of try-catch
  }

  public final Authorization loginDigest(String user, String digest,
		String id, String alg)
		throws NotAuthorizedException, NoSuchAlgorithmException {
		try {
			if (authRepo.digestAuth(JID.getNodeID(user, getDomain()), digest,
					id, alg)) {
				authState = Authorization.AUTHORIZED;
				loginHandler.handleLogin(user, this);
			} // end of if (authRepo.loginPlain())auth.login();
			return authState;
    } catch (UserNotFoundException e) {
      log.log(Level.WARNING, "Problem accessing reposiotry: ", e);
      throw new NotAuthorizedException("Authorization failed", e);
    } catch (TigaseDBException e) {
			log.log(Level.SEVERE, "Repository access exception.", e);
      throw new NotAuthorizedException("Authorization failed", e);
		} // end of try-catch
  }

	public final void logout()
		throws NotAuthorizedException {
		loginHandler.handleLogout(getUserName(), this);
		streamClosed();
	}

	public Authorization unregister(final String name_param)
		throws NotAuthorizedException {
		Authorization auth_res = super.unregister(name_param);
		if (auth_res == Authorization.AUTHORIZED) {
			List<XMPPResourceConnection> res_conn =
				parentSession.getActiveResources();
			for (XMPPResourceConnection res: res_conn) {
				if (res != this) {
					res.logout();
				} // end of if (res != this)
			} // end of for (XMPPResourceConnection res: res_conn)
			logout();
		} // end of if (res == Authorization.AUTHORIZED)
		return auth_res;
	}

} // XMPPResourceConnection
