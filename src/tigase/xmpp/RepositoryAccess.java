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

import java.util.logging.Level;
import java.util.logging.Logger;
import tigase.util.JID;
import tigase.db.UserRepository;
import tigase.db.UserExistsException;
import tigase.db.UserNotFoundException;

import static tigase.db.NonAuthUserRepository.*;

/**
 * Describe class RepositoryAccess here.
 *
 *
 * Created: Tue Oct 24 10:38:41 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public abstract class RepositoryAccess {

  /**
   * Private logger for class instancess.
   */
  private static final Logger log =
		Logger.getLogger("tigase.xmpp.RepositoryAccess");

  protected static final String NOT_AUTHORIZED_MSG =
    "Session has not been yet authorised.";
  protected static final String NO_ACCESS_TO_REP_MSG =
    "Can not access user repository.";

	/**
   * Handle to user repository - permanent data base for storing user data.
   */
  private UserRepository repo = null;

	/**
	 * Creates a new <code>RepositoryAccess</code> instance.
	 *
	 */
	public RepositoryAccess(UserRepository rep) {
		repo = rep;
	}

	public abstract String getUserId() throws NotAuthorizedException;

	public abstract String getUserName() throws NotAuthorizedException;

	public abstract String getDomain();

	public abstract boolean isAuthorized();

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
    if (getUserName().equals(user_name)) {
			try {
				repo.getData(JID.getNodeID(user_name, getDomain()), "password");
        repo.removeUser(JID.getNodeID(user_name, getDomain()));
				return Authorization.AUTHORIZED;
			} catch (UserNotFoundException e) {
				return Authorization.REGISTRATION_REQUIRED;
			} // end of try-catch
    } else {
      return Authorization.FORBIDDEN;
    }
	}

	public Authorization register(final String name_param,
		final String pass_param, final String email_param)
		throws NotAuthorizedException {
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
      repo.addUser(JID.getNodeID(user_name, getDomain()));
      setRegistration(user_name, pass_param, email_param);
      return Authorization.AUTHORIZED;
    } catch (UserExistsException e) {
      return Authorization.CONFLICT;
    } // end of try-catch
	}

  private Authorization changeRegistration(final String name_param,
    final String pass_param, final String email_param)
		throws NotAuthorizedException {

    if (name_param == null || name_param.equals("")
      || pass_param == null || pass_param.equals("")) {
      return Authorization.BAD_REQUEST;
    }

    if (getUserName().equals(name_param)) {
      setRegistration(name_param, pass_param, email_param);
      return Authorization.AUTHORIZED;
    } else {
      return Authorization.NOT_AUTHORIZED;
    }
  }

  private void setRegistration(final String name_param,
    final String pass_param, final String email_param) {
    try {
      repo.setData(JID.getNodeID(name_param, getDomain()),
        "password", pass_param);
      if (email_param != null && !email_param.equals("")) {
        repo.setData(JID.getNodeID(name_param, getDomain()),
          "email", email_param);
      }
    } catch (UserNotFoundException e) {
      log.log(Level.WARNING, "Problem accessing reposiotry: ", e);
    } // end of try-catch
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
    try { return repo.getDataList(getUserId(), subnode, key);
    } catch (UserNotFoundException e) {
      log.log(Level.WARNING, "Problem accessing reposiotry: ", e);
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
    try { return repo.getData(getUserId(), subnode, key, def);
    } catch (UserNotFoundException e) {
      log.log(Level.WARNING, "Problem accessing reposiotry: ", e);
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
    try { repo.setData(getUserId(), subnode, key, value);
    } catch (UserNotFoundException e) {
      log.log(Level.WARNING, "Problem accessing reposiotry: ", e);
      throw new NotAuthorizedException(NO_ACCESS_TO_REP_MSG, e);
    } // end of try-catch
  }

  public final void removeData(final String subnode, final String key)
		throws NotAuthorizedException {
    try { repo.removeData(getUserId(), subnode, key);
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
    try { return repo.getSubnodes(getUserId(), subnode);
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
    try { return repo.getKeys(getUserId(), subnode);
    } catch (UserNotFoundException e) {
      log.log(Level.WARNING, "Problem accessing reposiotry: ", e);
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
    try { repo.removeSubnode(getUserId(), subnode);
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
    try { repo.setDataList(getUserId(), subnode, key, list);
    } catch (UserNotFoundException e) {
      log.log(Level.WARNING, "Problem accessing reposiotry: ", e);
      throw new NotAuthorizedException(NO_ACCESS_TO_REP_MSG, e);
    } // end of try-catch
  }

  public final void addDataList(final String subnode, final String key,
    final String[] list) throws NotAuthorizedException {
    try { repo.addDataList(getUserId(), subnode, key, list);
    } catch (UserNotFoundException e) {
      log.log(Level.WARNING, "Problem accessing reposiotry: ", e);
      throw new NotAuthorizedException(NO_ACCESS_TO_REP_MSG, e);
    } // end of try-catch
  }

	private String calcNode(String base, String subnode) {
		if (subnode == null) {
			return base;
		} // end of if (subnode == null)
		return base + "/" + subnode;
	}

  public void setPublicDataList(String subnode, String key,	String[] list)
		throws NotAuthorizedException {
		setDataList(calcNode(PUBLIC_DATA_NODE, subnode), key, list);
	}

	public void addPublicDataList(String subnode, String key,	String[] list)
		throws NotAuthorizedException {
		addDataList(calcNode(PUBLIC_DATA_NODE, subnode), key, list);
	}

  public String[] getPublicDataList(String subnode, String key)
    throws NotAuthorizedException {
		return getDataList(calcNode(PUBLIC_DATA_NODE, subnode), key);
	}

  public void setPublicData(String subnode, String key,	String value)
		throws NotAuthorizedException {
		setData(calcNode(PUBLIC_DATA_NODE, subnode), key, value);
	}

  public String getPublicData(String subnode, String key,	String def)
		throws NotAuthorizedException {
		return getData(calcNode(PUBLIC_DATA_NODE, subnode), key, def);
	}

  public void removePublicData(String subnode, String key)
    throws NotAuthorizedException {
		removeData(calcNode(PUBLIC_DATA_NODE, subnode), key);
	}

  public void removePublicDataGroup(String subnode)
    throws NotAuthorizedException {
		removeDataGroup(calcNode(PUBLIC_DATA_NODE, subnode));
	}

  public void setOfflineDataList(String subnode, String key,	String[] list)
		throws NotAuthorizedException {
		setDataList(calcNode(OFFLINE_DATA_NODE, subnode), key, list);
	}

	public void addOfflineDataList(String subnode, String key,	String[] list)
		throws NotAuthorizedException {
		addDataList(calcNode(OFFLINE_DATA_NODE, subnode), key, list);
	}

  public String[] getOfflineDataList(String subnode, String key)
    throws NotAuthorizedException {
		return getDataList(calcNode(OFFLINE_DATA_NODE, subnode), key);
	}

  public void setOfflineData(String subnode, String key,	String value)
		throws NotAuthorizedException {
		setData(calcNode(OFFLINE_DATA_NODE, subnode), key, value);
	}

  public String getOfflineData(String subnode, String key,	String def)
		throws NotAuthorizedException {
		return getData(calcNode(OFFLINE_DATA_NODE, subnode), key, def);
	}

  public void removeOfflineData(String subnode, String key)
    throws NotAuthorizedException {
		removeData(calcNode(OFFLINE_DATA_NODE, subnode), key);
	}

  public void removeOfflineDataGroup(String subnode)
    throws NotAuthorizedException {
		removeDataGroup(calcNode(OFFLINE_DATA_NODE, subnode));
	}

} // RepositoryAccess
