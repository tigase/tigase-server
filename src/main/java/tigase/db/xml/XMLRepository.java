/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2014 "Tigase, Inc." <office@tigase.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3 of the License,
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
 */

package tigase.db.xml;

//~--- non-JDK imports --------------------------------------------------------

import tigase.db.AuthRepository;
import tigase.db.AuthRepositoryImpl;
import tigase.db.AuthorizationException;
import tigase.db.Repository;
import tigase.db.TigaseDBException;
import tigase.db.UserExistsException;
import tigase.db.UserNotFoundException;
import tigase.db.UserRepository;

import tigase.xmpp.BareJID;

import tigase.xml.db.NodeExistsException;
import tigase.xml.db.NodeNotFoundException;
import tigase.xml.db.XMLDB;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

/**
 * Class <code>XMLRepository</code> is a <em>XML</em> implementation of
 * <code>UserRepository</code>.
 * It uses <code>tigase.xml.db</code> package as repository backend and uses
 * <em>Bridge</em> design pattern to translate <code>XMLDB</code> calls to
 * <code>UserRepository</code> functions.
 *
 * <p>
 * Created: Tue Oct 26 15:27:33 2004
 * </p>
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 */
@Repository.Meta( supportedUris = { "dummy" } )
public class XMLRepository implements AuthRepository, UserRepository {

	/** Log filed */
	private static final String USER_STR = "User: ";

	/** Log filed */
	private static final String NOT_FOUND_STR = " has not been found in repository.";

	/** Logger instance */
	private static final Logger log = Logger.getLogger("tigase.db.xml.XMLRepository");

	/** AuthRepository filed */
	private AuthRepository auth = null;

	/** XML database filed */
	private XMLDB xmldb = null;

	/** autoCreateUser filed */
	private boolean autoCreateUser = false;

	//~--- methods --------------------------------------------------------------

	@Override
	public synchronized void addDataList(BareJID user, final String subnode, final String key,
			final String[] list)
			throws UserNotFoundException, TigaseDBException {
		log.log( Level.FINEST, "Adding data list, user: {0}, subnode: {1}, key: {2}, list: {3}",
						 new Object[] { user, subnode, key, Arrays.asList( list ) } );

		try {
			String[] old_data = getDataList(user, subnode, key);
			String[] all = null;

			if (old_data != null) {
				all = new String[old_data.length + list.length];
				System.arraycopy(old_data, 0, all, 0, old_data.length);
				System.arraycopy(list, 0, all, old_data.length, list.length);
				xmldb.setData(user.toString(), subnode, key, all);
			} else {
				xmldb.setData(user.toString(), subnode, key, list);
			}    // end of else
		} catch (NodeNotFoundException e) {
			throw new UserNotFoundException(USER_STR + user + NOT_FOUND_STR, e);
		}      // end of try-catch
	}

	@Override
	public synchronized void addUser(BareJID user) throws UserExistsException {
		log.log( Level.FINEST, "adding new user, user: {0}",
						 new Object[] { user } );
		try {
			xmldb.addNode1(user.toString());
		} catch (NodeExistsException e) {
			throw new UserExistsException(USER_STR + user + " already exists.", e);
		}    // end of try-catch
	}

	@Override
	public synchronized void addUser(BareJID user, final String password)
			throws UserExistsException, TigaseDBException {
		auth.addUser(user, password);
	}

	@Override
	@Deprecated
	public synchronized boolean digestAuth(BareJID user, final String digest, final String id,
			final String alg)
			throws UserNotFoundException, TigaseDBException, AuthorizationException {
		return auth.digestAuth(user, digest, id, alg);
	}

	@Override
	public synchronized String getData(BareJID user, final String subnode, final String key,
			final String def)
			throws UserNotFoundException, TigaseDBException {
		log.log( Level.FINEST, "Getting data, user: {0}, subnode: {1}, key: {2}",
						 new Object[] { user, subnode, key } );

		try {
			return (String) xmldb.getData(user.toString(), subnode, key, def);
		} catch (NodeNotFoundException e) {
			if (autoCreateUser) {
				try {
					addUser(user);

					return (String) xmldb.getData(user.toString(), subnode, key, def);
				} catch (Exception ex) {
					throw new TigaseDBException("Unknown repository problem: ", ex);
				}
			} else {
				throw new UserNotFoundException(USER_STR + user + NOT_FOUND_STR, e);
			}
		}    // end of try-catch
	}

	@Override
	public String getData(BareJID user, final String subnode, final String key)
			throws UserNotFoundException, TigaseDBException {
		return getData(user, subnode, key, null);
	}

	@Override
	public String getData(BareJID user, final String key)
			throws UserNotFoundException, TigaseDBException {
		return getData(user, null, key, null);
	}

	@Override
	public synchronized String[] getDataList(BareJID user, final String subnode,
			final String key)
			throws UserNotFoundException, TigaseDBException {
		log.log( Level.FINEST, "Getting data list, user: {0}, subnode: {1}, key: {2}",
						 new Object[] { user, subnode, key } );

		try {
			return xmldb.getDataList(user.toString(), subnode, key);
		} catch (NodeNotFoundException e) {
			if (autoCreateUser) {
				try {
					addUser(user);

					return xmldb.getDataList(user.toString(), subnode, key);
				} catch (Exception ex) {
					throw new TigaseDBException("Unknown repository problem: ", ex);
				}
			} else {
				throw new UserNotFoundException(USER_STR + user + NOT_FOUND_STR, e);
			}
		}    // end of try-catch
	}

	@Override
	public synchronized String[] getKeys(BareJID user, final String subnode)
			throws UserNotFoundException, TigaseDBException {
		log.log( Level.FINEST, "Getting keys, user: {0}, subnode: {1}",
						 new Object[] { user, subnode } );

		try {
			return xmldb.getKeys(user.toString(), subnode);
		} catch (NodeNotFoundException e) {
			if (autoCreateUser) {
				try {
					addUser(user);

					return xmldb.getKeys(user.toString(), subnode);
				} catch (Exception ex) {
					throw new TigaseDBException("Unknown repository problem: ", ex);
				}
			} else {
				throw new UserNotFoundException(USER_STR + user + NOT_FOUND_STR, e);
			}
		}    // end of try-catch
	}

	@Override
	public String[] getKeys(BareJID user) throws UserNotFoundException, TigaseDBException {
		return getKeys(user, null);
	}

	@Override
	public String getResourceUri() {
		return xmldb.getDBFileName();
	}

	@Override
	public synchronized String[] getSubnodes(BareJID user, final String subnode)
			throws UserNotFoundException, TigaseDBException {
		log.log( Level.FINEST, "Getting subnodes, user: {0}, subnode: {1}",
						 new Object[] { user, subnode } );

		try {
			return xmldb.getSubnodes(user.toString(), subnode);
		} catch (NodeNotFoundException e) {
			if (autoCreateUser) {
				try {
					addUser(user);

					return xmldb.getSubnodes(user.toString(), subnode);
				} catch (Exception ex) {
					throw new TigaseDBException("Unknown repository problem: ", ex);
				}
			} else {
				throw new UserNotFoundException(USER_STR + user + NOT_FOUND_STR, e);
			}
		}    // end of try-catch
	}

	@Override
	public String[] getSubnodes(BareJID user) throws UserNotFoundException, TigaseDBException {
		return getSubnodes(user, null);
	}

	@Override
	public long getUserUID(BareJID user) throws TigaseDBException {
		return Math.abs(user.hashCode());
	}

	@Override
	public synchronized List<BareJID> getUsers() {
		List<String> users = xmldb.getAllNode1s();
		log.log( Level.FINEST, "Getting users, users: {0}, xmldb: {1}",
						 new Object[] { users, xmldb } );


		List<BareJID> result = new ArrayList<BareJID>();

		for (String usr : users) {
			result.add(BareJID.bareJIDInstanceNS(usr));
		}

		return result;
	}

	@Override
	public synchronized long getUsersCount(String domain) {
		long res = 0;
		List<BareJID> jids = getUsers();

		for (BareJID jid : jids) {
			if (jid.getDomain().equals(domain)) {
				++res;
			}
		}

		return res;
	}

	@Override
	public synchronized long getUsersCount() {
		return xmldb.getAllNode1sCount();
	}

	@Override
	public synchronized void initRepository(String file, Map<String, String> params) {
		String file_name = file;

		log.log( Level.FINEST, "Initializing repository, file: {0}, params: {1}",
						 new Object[] { file, params } );

		try {
			int idx = file.indexOf("?");

			if (idx > 0) {
				file_name = file.substring(0, idx);
			}

			if (file.contains("autoCreateUser=true")) {
				autoCreateUser = true;
			}    // end of if (db_conn.contains())

			auth = new AuthRepositoryImpl(this);
			xmldb = new XMLDB(file_name);
		} catch (Exception e) {
			log.warning("Can not open existing user repository file, creating new one, " + e);
			xmldb = XMLDB.createDB(file_name, "users", "user");
		}      // end of try-catch
	}

	@Override
	public synchronized void logout(BareJID user)
			throws UserNotFoundException, TigaseDBException {
		auth.logout(user);
	}

	@Override
	public synchronized boolean otherAuth(final Map<String, Object> props)
			throws UserNotFoundException, TigaseDBException, AuthorizationException {
		return auth.otherAuth(props);
	}

	// Implementation of tigase.db.AuthRepository

	@Override
	@Deprecated
	public synchronized boolean plainAuth(BareJID user, final String password)
			throws UserNotFoundException, TigaseDBException, AuthorizationException {
		return auth.plainAuth(user, password);
	}

	@Override
	public synchronized void queryAuth(Map<String, Object> authProps) {
		auth.queryAuth(authProps);
	}

	@Override
	public synchronized void removeData(BareJID user, final String subnode, final String key)
			throws UserNotFoundException {
		log.log( Level.FINEST, "Removing data, user: {0}, subnode: {1}, key: {2}",
						 new Object[] { user, subnode, key } );

		try {
			xmldb.removeData(user.toString(), subnode, key);
		} catch (NodeNotFoundException e) {
			if ( !autoCreateUser) {
				throw new UserNotFoundException(USER_STR + user + NOT_FOUND_STR, e);
			}
		}    // end of try-catch
	}

	@Override
	public void removeData(BareJID user, final String key) throws UserNotFoundException {
		removeData(user, null, key);
	}

	@Override
	public synchronized void removeSubnode(BareJID user, final String subnode)
			throws UserNotFoundException {
		log.log( Level.FINEST, "Removing subnode, user: {0}, subnode: {1}",
						 new Object[] { user, subnode } );

		try {
			xmldb.removeSubnode(user.toString(), subnode);
		} catch (NodeNotFoundException e) {
			if ( !autoCreateUser) {
				throw new UserNotFoundException(USER_STR + user + NOT_FOUND_STR, e);
			}
		}    // end of try-catch
	}

	@Override
	public synchronized void removeUser(BareJID user) throws UserNotFoundException {
		try {
		log.log( Level.FINEST, "Removing user: {0}",
						 new Object[] { user } );

			xmldb.removeNode1(user.toString());
		} catch (NodeNotFoundException e) {
			throw new UserNotFoundException(USER_STR + user + NOT_FOUND_STR, e);
		}    // end of try-catch
	}

	//~--- set methods ----------------------------------------------------------

	@Override
	public synchronized void setData( BareJID user, final String subnode, final String key,
																		final String value )
			throws UserNotFoundException, TigaseDBException {
		log.log( Level.FINEST, "Setting data, user: {0}, subnode: {1}, key: {2}, value: {3}",
						 new Object[] { user, subnode, key, value } );
		try {
			xmldb.setData(user.toString(), subnode, key, value);
		} catch (NodeNotFoundException e) {
			if (autoCreateUser) {
				try {
					addUser(user);
					xmldb.setData(user.toString(), subnode, key, value);
				} catch (Exception ex) {
					throw new TigaseDBException("Unknown repository problem: ", ex);
				}
			} else {
				throw new UserNotFoundException(USER_STR + user + NOT_FOUND_STR, e);
			}
		}    // end of try-catch
	}

	@Override
	public void setData(BareJID user, final String key, final String value)
			throws UserNotFoundException, TigaseDBException {
		setData(user, null, key, value);
	}

	@Override
	public synchronized void setDataList(BareJID user, final String subnode, final String key,
			final String[] list)
			throws UserNotFoundException, TigaseDBException {
		log.log( Level.FINEST, "Setting data list, user: {0}, subnode: {1}, key: {2}, value: {3}",
						 new Object[] { user, subnode, key, Arrays.asList( list ) } );
		try {
			xmldb.setData(user.toString(), subnode, key, list);
		} catch (NodeNotFoundException e) {
			if (autoCreateUser) {
				try {
					addUser(user);
					xmldb.setData(user.toString(), subnode, key, list);
				} catch (Exception ex) {
					throw new TigaseDBException("Unknown repository problem: ", ex);
				}
			} else {
				throw new UserNotFoundException(USER_STR + user + NOT_FOUND_STR, e);
			}
		}    // end of try-catch
	}

	@Override
	public synchronized void updatePassword(BareJID user, final String password)
			throws UserExistsException, TigaseDBException {
		auth.updatePassword(user, password);
	}

	@Override
	public synchronized boolean userExists(BareJID user) {
		return xmldb.findNode1(user.toString()) != null;
	}

	@Override
	public String getPassword(BareJID user) throws UserNotFoundException, TigaseDBException {
		return auth.getPassword(user);
	}
	
	@Override
	public boolean isUserDisabled(BareJID user) 
					throws UserNotFoundException, TigaseDBException {
		return false;
	}
	
	@Override
	public void setUserDisabled(BareJID user, Boolean value) 
					throws UserNotFoundException, TigaseDBException {
		throw new TigaseDBException("Feature not supported");
	}	
}
