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
import tigase.db.UserExistsException;
import tigase.db.UserNotFoundException;
import tigase.db.UserRepository;
import tigase.util.JID;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

/**
 * Describe class XMPPResourceConnection here.
 *
 *
 * Created: Wed Feb  8 22:30:37 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class XMPPResourceConnection {

  private static final String NOT_AUTHORIZED_MSG =
    "Session has not been yet authorised.";
  private static final String NO_ACCESS_TO_REP_MSG =
    "Can not access user repository.";

  /**
   * Private logger for class instancess.
   */
  private static final Logger log =
		Logger.getLogger("tigase.xmpp.XMPPResourceConnection");

	private XMPPSession parentSession = null;
	private LoginContext loginContext = null;

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
   * Handle to user repository - permanent data base for storing user data.
   */
  private UserRepository repository = null;

	/**
	 * Creates a new <code>XMPPResourceConnection</code> instance.
	 *
	 */
	public XMPPResourceConnection(String connectionId, UserRepository rep) {
		this.connectionId = connectionId;
		this.repository = rep;
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
   * @see #getData(String)
   * @see #setData(String, String)
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
		//		domain = null;
		repository = null;
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

	public Authorization unregister(final String name_param)
		throws NotAuthorizedException {
    if (!isAuthorized()) {
      return Authorization.FORBIDDEN;
    }
    // Some clients send plain user name and others send
    // jid as user name. Let's resolve this here.
    String user_name = JID.getNodeNick(name_param);
    if (user_name == null || user_name.equals("")) {
      user_name = name_param;
    } // end of if (user_mame == null || user_name.equals(""))
    if (parentSession.getUserName().equals(user_name)) {
			try {
				List<XMPPResourceConnection> res_conn =
					parentSession.getActiveResources();
				for (XMPPResourceConnection res: res_conn) {
					if (res != this) {
						res.logout();
					} // end of if (res != this)
				} // end of for (XMPPResourceConnection res: res_conn)
				repository.getData(JID.getNodeID(user_name, getDomain()), "password");
        repository.removeUser(JID.getNodeID(user_name, getDomain()));
				logout();
				return Authorization.AUTHORIZED;
			} catch (UserNotFoundException e) {
				return Authorization.REGISTRATION_REQUIRED;
			} // end of try-catch
    } else {
      return Authorization.FORBIDDEN;
    }
	}

	public Authorization register(final String name_param,
		final String pass_param, final String email_param) {
    // Some clients send plain user name and others send
    // jid as user name. Let's resolve this here.
    String user_name = JID.getNodeNick(name_param);
    if (user_name == null || user_name.equals("")) {
      user_name = name_param;
    } // end of if (user_mame == null || user_name.equals(""))

    if (isAuthorized()) {
      return changeRegistration(user_name, pass_param, email_param);
    }

    if (user_name == null || user_name.equals("")
      || pass_param == null || pass_param.equals("")) {
      return Authorization.NOT_ACCEPTABLE;
    }

    try {
      repository.addUser(JID.getNodeID(user_name, domain));
      setRegistration(user_name, pass_param, email_param);
      return Authorization.AUTHORIZED;
    } catch (UserExistsException e) {
      return Authorization.CONFLICT;
    } // end of try-catch
	}

  private Authorization changeRegistration(final String name_param,
    final String pass_param, final String email_param) {

    if (name_param == null || name_param.equals("")
      || pass_param == null || pass_param.equals("")) {
      return Authorization.BAD_REQUEST;
    }

    if (parentSession.getUserName().equals(name_param)) {
      setRegistration(name_param, pass_param, email_param);
      return Authorization.AUTHORIZED;
    } else {
      return Authorization.NOT_AUTHORIZED;
    }
  }

  private void setRegistration(final String name_param,
    final String pass_param, final String email_param) {
    try {
      repository.setData(JID.getNodeID(name_param, domain),
        "password", pass_param);
      if (email_param != null && !email_param.equals("")) {
        repository.setData(JID.getNodeID(name_param, domain),
          "email", email_param);
      }
    } catch (UserNotFoundException e) {
      log.log(Level.WARNING, "Problem accessing reposiotry: ", e);
    } // end of try-catch
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
   * @param user a <code>String</code> value of user ID for which authorization
   * is performed.
   * @param password a <code>String</code> value of plain text user password.
   * @param resource a <code>String</code> value of resource to which this
   * session is binded after successful authorization.
   * @return a <code>Authorization</code> value of result code.
   */
  public final Authorization login() throws LoginException {

		loginContext.login();
		authState = Authorization.AUTHORIZED;
    return authState;
  }

	public final void logout() {
		streamClosed();
		try {
			loginContext.logout();
		} catch (LoginException e) {} // end of try-catch
	}

	public void setLoginContext(final LoginContext lc) {
		loginContext = lc;
	}

	/**
   * This method allows to retrieve list of values associated with one key.
   * As it is possible to store many values with one key there are a few methods
   * which provides this functionality. If given key does not exists in given
   * subnode <code>null</code> is returned.
   *
   * @param subnode a <code>String</code> value pointing to specific subnode in
   * user reposiotry where data have to be stored.
   * @param key a <code>String</code> value of data key ID.
   * @return a <code>String[]</code> array containing all values found for
   * given key.
   * @exception NotAuthorizedException is thrown when session
   * has not been authorized yet and there is no access to permanent storage.
   * @see #setData(String, String, String)
   * @see #setData(String, String)
   */
  public final String[] getDataList(final String subnode, final String key)
    throws NotAuthorizedException {
    try { return repository.getDataList(getUserId(), subnode, key);
    } catch (UserNotFoundException e) {
      log.log(Level.WARNING, "Problem accessing to reposiotry: ", e);
      throw new NotAuthorizedException(NO_ACCESS_TO_REP_MSG, e);
    } // end of try-catch
  }

  /**
   * <code>getData</code> method is a twin sister (brother?) of
   * <code>setData(String, String, String)</code> method.
   * It allows you to retrieve data stored with above method. It is data stored
   * in given node with given key identifier. If there are no data associated
   * with given key or given node does not exist given <code>def</code> value
   * is returned.
   *
   * @param subnode a <code>String</code> value is path to node where pair
   * <code>(key, value)</code> are stored.
   * @param key a <code>String</code> value of key ID for data to retrieve.
   * @param def a <code>String</code> value of default returned if there is
   * nothing stored with given key. <code>def</code> can be set to any value
   * you wish to have back as default value or <code>null</code> if you want
   * to have back <code>null</code> if no data was found. If you set
   * <code>def</code> to <code>null</code> it has exactly the
   * same effect as if you use <code>getData(String)</code> method.
   * @return a <code>String</code> value of data found for given key or
   * <code>def</code> if there was no data associated with given key.
   * @exception NotAuthorizedException is thrown when session
   * has not been authorized yet and there is no access to permanent storage.
   * @see #setData(String, String, String)
   */
  public final String getData(final String subnode,
    final String key, final String def) throws NotAuthorizedException {
    try { return repository.getData(getUserId(), subnode, key, def);
    } catch (UserNotFoundException e) {
      log.log(Level.WARNING, "Problem accessing to reposiotry: ", e);
      throw new NotAuthorizedException(NO_ACCESS_TO_REP_MSG, e);
    } // end of try-catch
  }

  /**
   * This method stores given data in permanent storage in given point of
   * hierarchy of data base.
   * This method is similar to <code>setData(String, String)</code> and
   * differs in one additional parameter which point to user data base subnode
   * where data must be stored. It helps to organize user data in more logical
   * hierarchy.<br/>
   * User data is kind of tree where you can store data in each tree node. The
   * most relevant sample might be structure like typical file system or
   * XML like or LDAP data base. The first implementation is actually done as
   * XML file to make it easier test application and deploy simple installation
   * where there is no more users than 1000.<br/>
   * To find out more about user repository refer to <code>UserRepository</code>
   * interface for general info and to <code>XMLRepository</code> for detailed
   * explanation regarding XML implementation of user repository.
   * <p>
   * Thus <code>subnode</code> is kind of path to data node. If you specify
   * <code>null</code> or empty node data will be stored in root user node.
   * This has exactly the same effect as you call
   * <code>setData(String, String)</code>. If you want to store data in
   * different node you must just specify node path like you do it to directory
   * on most file systems:
   * <pre>
   * /roster
   * </pre>
   * Or, if you need access deeper node:
   * <pre>
   * /just/like/path/to/file
   * </pre>
   * </p>
   * If given node does not yet exist it will be automaticaly created with all
   * nodes in given path so there is no need for developer to perform additional
   * action to create node. There is, however method
   * <code>removeDataGroup(String)</code> for deleting specified node as nodes
   * are not automaticaly deleted.
   *
   * @param subnode a <code>String</code> value pointing to specific subnode in
   * user reposiotry where data have to be stored.
   * @param key a <code>String</code> value of data key ID.
   * @param value a <code>String</code> actual data stored in user repository.
   * @exception NotAuthorizedException is thrown when session
   * has not been authorized yet and there is no access to permanent storage.
   * @see #removeDataGroup(String)
   * @see UserRepository
   * @see tigase.xmpp.rep.xml.XMLRepository
   */
  public final void setData(final String subnode,
    final String key, final String value) throws NotAuthorizedException {
    try { repository.setData(getUserId(), subnode, key, value);
    } catch (UserNotFoundException e) {
      log.log(Level.WARNING, "Problem accessing reposiotry: ", e);
      throw new NotAuthorizedException(NO_ACCESS_TO_REP_MSG, e);
    } // end of try-catch
  }

  public final void removeData(final String subnode, final String key)
		throws NotAuthorizedException {
    try { repository.removeData(getUserId(), subnode, key);
    } catch (UserNotFoundException e) {
      log.log(Level.WARNING, "Problem accessing reposiotry: ", e);
      throw new NotAuthorizedException(NO_ACCESS_TO_REP_MSG, e);
    } // end of try-catch
  }

  /**
   * This method retrieves list of all direct subnodes for given node.
   * It works in similar way as <code>ls</code> unix command or <code>dir</code>
   * under DOS/Windows systems.
   *
   * @param subnode a <code>String</code> value of path to node for which we
   * want to retrieve list of direct subnodes.
   * @return a <code>String[]</code> array of direct subnodes names for given
   * node.
   * @exception NotAuthorizedException is thrown when session
   * has not been authorized yet and there is no access to permanent storage.
   * @see #setData(String, String, String)
   */
  public final String[] getDataGroups(final String subnode)
    throws NotAuthorizedException {
    try { return repository.getSubnodes(getUserId(), subnode);
    } catch (UserNotFoundException e) {
      log.log(Level.WARNING, "Problem accessing reposiotry: ", e);
      throw new NotAuthorizedException(NO_ACCESS_TO_REP_MSG, e);
    } // end of try-catch
  }

  /**
   * This method returns all data keys available in permanent storage in given
   * node.
   * There is not though any information what kind of data is stored with this
   * key. This is up to user (developer) to determine what data type is
   * associated with key and what is it's meaning.
   *
   * @param subnode a <code>String</code> value pointing to specific subnode in
   * user reposiotry where data have to be stored.
   * @return a <code>String[]</code> array containing all data keys found in
   * given subnode.
   * @exception NotAuthorizedException is thrown when session
   * has not been authorized yet and there is no access to permanent storage.
   * @see #setData(String, String, String)
   * @see #setData(String, String)
   */
  public final String[] getDataKeys(final String subnode)
    throws NotAuthorizedException {
    try { return repository.getKeys(getUserId(), subnode);
    } catch (UserNotFoundException e) {
      log.log(Level.WARNING, "Problem accessing to reposiotry: ", e);
      throw new NotAuthorizedException(NO_ACCESS_TO_REP_MSG, e);
    } // end of try-catch
  }

  /**
   * Removes the last data node given in subnode path as parameter to this
   * method.
   * All subnodes are moved as well an all data stored as
   * <code>(key, val)</code> are removed as well. Changes are commited to
   * repository immediatelly and there is no way to undo this operation so
   * use it with care.
   *
   * @param subnode a <code>String</code> value of path to node which has
   * to be removed.
   * @exception NotAuthorizedException is thrown when session
   * has not been authorized yet and there is no access to permanent storage.
   * @see #setData(String, String, String)
   */
  public final void removeDataGroup(final String subnode)
    throws NotAuthorizedException {
    try { repository.removeSubnode(getUserId(), subnode);
    } catch (UserNotFoundException e) {
      log.log(Level.WARNING, "Problem accessing reposiotry: ", e);
      throw new NotAuthorizedException(NO_ACCESS_TO_REP_MSG, e);
    } // end of try-catch
  }

  /**
   * This method allows to store list of values under one key ID reference.
   * It is often necessary to keep set of values which can be refered by one
   * key. As an example might be list of groups for specific buddy in roster.
   * There is no actual need to store each group with separate key because
   * we usually need to acces whole list of groups.
   *
   * @param subnode a <code>String</code> value pointing to specific subnode in
   * user reposiotry where data have to be stored.
   * @param key a <code>String</code> value of data key ID.
   * @param list a <code>String[]</code> keeping list of actual data to be
   * stored in user repository.
   * @exception NotAuthorizedException is thrown when session
   * has not been authorized yet and there is no access to permanent storage.
   * @see #setData(String, String, String)
   * @see #setData(String, String)
   */
  public final void setDataList(final String subnode, final String key,
    final String[] list) throws NotAuthorizedException {
    try { repository.setDataList(getUserId(), subnode, key, list);
    } catch (UserNotFoundException e) {
      log.log(Level.WARNING, "Problem accessing to reposiotry: ", e);
      throw new NotAuthorizedException(NO_ACCESS_TO_REP_MSG, e);
    } // end of try-catch
  }

} // XMPPResourceConnection
