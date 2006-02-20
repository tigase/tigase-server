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

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import tigase.db.UserExistsException;
import tigase.db.UserNotFoundException;
import tigase.db.UserRepository;
import tigase.util.JID;

/**
 * Describe class XMPPResourceConnection here.
 *
 *
 * Created: Wed Feb  8 22:30:37 2006
 *
 * @author <a href="mailto:artur.hefczyc@gmail.com">Artur Hefczyc</a>
 * @version $Rev$
 */
public class XMPPResourceConnection {

  /**
   * Private logger for class instancess.
   */
  private static final Logger log =
		Logger.getLogger("tigase.xmpp.XMPPResourceConnection");

	private XMPPSession parentSession = null;

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

	private String connectionId = null;
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
	}

	public void streamClosed() {
		if (parentSession != null) {
			parentSession.streamClosed(this);
		} // end of if (parentSession != null)
		parentSession = null;
		authState = Authorization.NOT_AUTHORIZED;
		resource = null;
		sessionId = null;
		domain = null;
		repository = null;
	}

	public void setParentSession(final XMPPSession parent) {
		this.parentSession = parent;
	}

	public XMPPSession getParentSession() {
		return parentSession;
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
		throws NotAuthorizedException{
		return Authorization.NOT_ALLOWED;
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
      repository.addUser(JID.getNodeID(user_name, getDomain()));
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
      repository.setData(JID.getNodeID(name_param, getDomain()),
        "password", pass_param);
      if (email_param != null && !email_param.equals("")) {
        repository.setData(JID.getNodeID(name_param, getDomain()),
          "email", email_param);
      }
    } catch (UserNotFoundException e) {
      log.log(Level.SEVERE, "Problem accessing reposiotry: ", e);
    } // end of try-catch
  }

  /**
   * <code>authorizePlain</code> method performs authorization with given
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
  public final Authorization authorize(final String user,
		final String password) {

    if (user == null || user.equals("")
      || password == null || password.equals("")) {
      authState = Authorization.NOT_ACCEPTABLE;
    } else {
			// Some clients send plain user name and others send
			// jid as user name. Let's resolve this here.
			String user_name = JID.getNodeNick(user);
			if (user_name == null || user_name.equals("")) {
				user_name = user;
			} // end of if (user_mame == null || user_name.equals(""))
			try {
				String pattern =
					repository.getData(JID.getNodeID(user_name, getDomain()), "password");
				if (password.equals(pattern)) {
					this.resource = resource;
					authState = Authorization.AUTHORIZED;
				} else {
					authState = Authorization.NOT_AUTHORIZED;
				}
			} // end of try
			catch (UserNotFoundException e) {
				return Authorization.NOT_AUTHORIZED;
			} // end of try-catch
    }
    return authState;
  }

} // XMPPResourceConnection
