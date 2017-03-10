/*
 * RepositoryAccess.java
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
import tigase.db.UserExistsException;
import tigase.db.UserNotFoundException;
import tigase.db.UserRepository;

import tigase.util.TigaseStringprepException;

import tigase.vhosts.VHostItem;

import static tigase.db.NonAuthUserRepository.*;

//~--- JDK imports ------------------------------------------------------------

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Map;
import java.util.UUID;

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
	/** Field description */
	protected static final String NO_ACCESS_TO_REP_MSG = "Can not access user repository.";

	/** Field description */
	protected static final String NOT_AUTHORIZED_MSG =
			"Session has not been yet authorised.";
	private static final String ANONYMOUS_MECH = "ANONYMOUS";

	/**
	 * Private logger for class instances.
	 */
	private static final Logger log = Logger.getLogger("tigase.xmpp.RepositoryAccess");

	//~--- fields ---------------------------------------------------------------

	// ~--- fields ---------------------------------------------------------------

	/** Field description */
	protected AuthRepository authRepo = null;

	/** Field description */
	protected VHostItem domain = null;

	/**
	 * Handle to user repository - permanent data base for storing user data.
	 */
	private UserRepository repo = null;

	// private boolean anon_allowed = false;

	/** Field description */
	protected boolean is_anonymous = false;

	/**
	 * Current authorization state - initialy session i
	 * <code>NOT_AUTHORIZED</code>. It becomes <code>AUTHORIZED</code>
	 */
	protected Authorization authState = Authorization.NOT_AUTHORIZED;

	//~--- constructors ---------------------------------------------------------

	// ~--- constructors ---------------------------------------------------------

	/**
	 * Creates a new <code>RepositoryAccess</code> instance.
	 *
	 *
	 * @param rep
	 * @param auth
	 */
	public RepositoryAccess(UserRepository rep, AuthRepository auth) {
		repo     = rep;
		authRepo = auth;

		// this.anon_allowed = anon_allowed;
	}

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param subnode
	 * @param key
	 * @param list
	 *
	 * @throws NotAuthorizedException
	 * @throws TigaseDBException
	 */
	public void addDataList(final String subnode, final String key, final String[] list)
					throws NotAuthorizedException, TigaseDBException {
		if (is_anonymous) {
			return;
		}
		if (!isAuthorized()) {
			throw new NotAuthorizedException(NO_ACCESS_TO_REP_MSG);
		}
		try {
			repo.addDataList(getBareJID(), subnode, key, list);
		} catch (UserNotFoundException e) {
			log.log(Level.FINEST, "Problem accessing reposiotry: ", e);

			throw new NotAuthorizedException(NO_ACCESS_TO_REP_MSG, e);

			// } catch (TigaseDBException e) {
			// log.log(Level.SEVERE, "Repository access exception.", e);
		}    // end of try-catch
	}

	/**
	 * Method description
	 *
	 *
	 * @param subnode
	 * @param key
	 * @param list
	 *
	 * @throws NotAuthorizedException
	 * @throws TigaseDBException
	 */
	public void addOfflineDataList(String subnode, String key, String[] list)
					throws NotAuthorizedException, TigaseDBException {
		addDataList(calcNode(OFFLINE_DATA_NODE, subnode), key, list);
	}

	/**
	 * Method description
	 *
	 *
	 * @param subnode
	 * @param key
	 * @param list
	 *
	 * @throws NotAuthorizedException
	 * @throws TigaseDBException
	 */
	public void addPublicDataList(String subnode, String key, String[] list)
					throws NotAuthorizedException, TigaseDBException {
		addDataList(calcNode(PUBLIC_DATA_NODE, subnode), key, list);
	}

	// ~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param userId
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
	 */
	@Deprecated
	public Authorization loginDigest(BareJID userId, String digest, String id, String alg)
					throws NotAuthorizedException, AuthorizationException, TigaseDBException {
		isLoginAllowed();
		try {
			if (authRepo.digestAuth(userId, digest, id, alg)) {
				authState = Authorization.AUTHORIZED;
				login();
			}    // end of if (authRepo.loginPlain())auth.login();

			return authState;
		} catch (UserNotFoundException e) {
			log.log(Level.FINEST, "Problem accessing reposiotry: ", e);

			throw new NotAuthorizedException("Authorization failed", e);

			// } catch (TigaseDBException e) {
			// log.log(Level.SEVERE, "Repository access exception.", e);
			// throw new NotAuthorizedException("Authorization failed", e);
		}      // end of try-catch
	}

	/**
	 * Method description
	 *
	 *
	 * @param props
	 *
	 *
	 *
	 *
	 * @return a value of <code>Authorization</code>
	 * @throws AuthorizationException
	 * @throws NotAuthorizedException
	 * @throws TigaseDBException
	 */
	@Deprecated
	public Authorization loginOther(Map<String, Object> props)
					throws NotAuthorizedException, AuthorizationException, TigaseDBException {
		isLoginAllowed();
		try {
			String mech = (String) props.get(AuthRepository.MACHANISM_KEY);

			if (domain.isAnonymousEnabled() && (mech != null) && mech.equals(ANONYMOUS_MECH)) {
				is_anonymous = true;
				props.put(AuthRepository.USER_ID_KEY, BareJID.bareJIDInstanceNS(UUID.randomUUID()
						.toString(), getDomain().getVhost().getDomain()));
				authState = Authorization.AUTHORIZED;
				login();
			} else {
				if (authRepo.otherAuth(props)) {
					authState = Authorization.AUTHORIZED;
					login();
				}    // end of if (authRepo.loginPlain())auth.login();
			}

			return authState;
		} catch (UserNotFoundException e) {
			log.log(Level.FINEST, "User not found: ", e);

			throw new NotAuthorizedException("Authorization failed", e);

			// } catch (TigaseDBException e) {
			// log.log(Level.SEVERE, "Repository access exception.", e);
			// throw new NotAuthorizedException("Authorization failed", e);
		}        // end of try-catch
	}

	/**
	 * <code>authorize</code> method performs authorization with given password as
	 * plain text. If <code>AUTHORIZED</code> has been returned it means
	 * authorization process is successful and session has been activated,
	 * otherwise session hasn't been authorized and return code gives more
	 * detailed information of fail reason. Please refer to
	 * <code>Authorization</code> documentation for more details.
	 *
	 * @param userId
	 * @param password
	 * @return a <code>Authorization</code> value of result code.
	 * @throws NotAuthorizedException
	 * @throws AuthorizationException
	 * @throws TigaseDBException
	 */
	@Deprecated
	public Authorization loginPlain(BareJID userId, String password)
					throws NotAuthorizedException, AuthorizationException, TigaseDBException {
		isLoginAllowed();
		try {
			if (authRepo.plainAuth(userId, password)) {
				authState = Authorization.AUTHORIZED;
				login();
			}    // end of if (authRepo.loginPlain())auth.login();

			return authState;
		} catch (UserNotFoundException e) {
			log.info("User not found, authorization failed: " + userId);

			throw new NotAuthorizedException("Authorization failed", e);

			// } catch (TigaseDBException e) {
			// log.log(Level.SEVERE, "Repository access exception.", e);
			// throw new NotAuthorizedException("Authorization failed", e);
		}      // end of try-catch
	}

	/**
	 * Method description
	 *
	 *
	 * @param userId
	 * @param xmpp_sessionId
	 * @param token
	 *
	 *
	 *
	 *
	 *
	 * @return a value of <code>Authorization</code>
	 * @throws AuthorizationException
	 * @throws NotAuthorizedException
	 * @throws TigaseDBException
	 */
	@Deprecated
	public Authorization loginToken(BareJID userId, String xmpp_sessionId, String token)
					throws NotAuthorizedException, AuthorizationException, TigaseDBException {
		isLoginAllowed();
		try {
			String db_token = repo.getData(userId, "tokens", xmpp_sessionId);

			if (token.equals(db_token)) {
				authState = Authorization.AUTHORIZED;
				login();
				repo.removeData(userId, "tokens", xmpp_sessionId);
			}
		} catch (UserNotFoundException e) {
			log.log(Level.FINEST, "Problem accessing reposiotry: ", e);

			throw new NotAuthorizedException(NO_ACCESS_TO_REP_MSG, e);

			// } catch (TigaseDBException e) {
			// log.log(Level.SEVERE, "Repository access exception.", e);
		}    // end of try-catch

		return authState;
	}

	/**
	 * Method description
	 *
	 *
	 * @throws NotAuthorizedException
	 */
	public void logout() throws NotAuthorizedException {
		authState = Authorization.NOT_AUTHORIZED;
	}

	/**
	 * Method description
	 *
	 *
	 * @param authProps
	 * @throws TigaseDBException
	 */
	public void queryAuth(Map<String, Object> authProps) throws TigaseDBException {
		if (authRepo == null) {
			log.severe(
					"Authentication repository is not available! Misconfiguration error or " +
					"authentication database is not available. Please check your logs from the " +
					"server startup time.");

			return;
		}
		authProps.put(AuthRepository.SERVER_NAME_KEY, getDomain().getVhost().getDomain());
		authRepo.queryAuth(authProps);
		if (domain.isAnonymousEnabled() && (authProps.get(AuthRepository.PROTOCOL_KEY) ==
				AuthRepository.PROTOCOL_VAL_SASL)) {
			String[] auth_mechs = (String[]) authProps.get(AuthRepository.RESULT_KEY);

			if (auth_mechs == null) {
				throw new TigaseDBException("No euthentication mechanisms found, probably " +
						"DB misconfiguration problem.");
			}
			auth_mechs                        = Arrays.copyOf(auth_mechs, auth_mechs.length +
					1);
			auth_mechs[auth_mechs.length - 1] = ANONYMOUS_MECH;
			authProps.put(AuthRepository.RESULT_KEY, auth_mechs);
		}
	}

	/**
	 * Method description
	 *
	 *
	 * @param name_param
	 * @param pass_param
	 * @param email_param
	 *
	 *
	 *
	 *
	 * @return a value of <code>Authorization</code>
	 * @throws NotAuthorizedException
	 * @throws TigaseDBException
	 * @throws TigaseStringprepException
	 *
	 * @deprecated
	 */
	@Deprecated
	public Authorization register(String name_param, String pass_param, String email_param)
					throws NotAuthorizedException, TigaseDBException, TigaseStringprepException {
		Map<String, String> reg_params = null;

		if ((email_param != null) &&!email_param.trim().isEmpty()) {
			reg_params = new LinkedHashMap<String, String>();
			reg_params.put("email", email_param);
		}

		return register(name_param, pass_param, reg_params);
	}

	/**
	 * Method description
	 *
	 *
	 * @param name_param
	 * @param pass_param
	 * @param reg_params
	 *
	 *
	 *
	 *
	 * @return a value of <code>Authorization</code>
	 * @throws NotAuthorizedException
	 * @throws TigaseDBException
	 * @throws TigaseStringprepException
	 */
	public Authorization register(String name_param, String pass_param, Map<String,
			String> reg_params)
					throws NotAuthorizedException, TigaseDBException, TigaseStringprepException {

		// Some clients send plain user name and others send
		// jid as user name. Let's resolve this here.
		String user_name = BareJID.parseJID(name_param)[0];

		if ((user_name == null) || user_name.trim().isEmpty()) {
			user_name = name_param;
		}    // end of if (user_mame == null || user_name.equals(""))
		if (isAuthorized()) {
			return changeRegistration(user_name, pass_param, reg_params);
		}

		// new user registration, let's check limits...
		if (!domain.isRegisterEnabled()) {
			throw new NotAuthorizedException("Registration is now allowed for this domain");
		}
		if (domain.getMaxUsersNumber() > 0) {
			long domainUsers = authRepo.getUsersCount(domain.getVhost().getDomain());

			if (log.isLoggable(Level.FINEST)) {
				log.finest("Current number of users for domain: " + domain.getVhost()
						.getDomain() + " is: " + domainUsers);
			}
			if (domainUsers >= domain.getMaxUsersNumber()) {
				throw new NotAuthorizedException("Maximum users number for the domain exceeded.");
			}
		}
		if ((user_name == null) || user_name.equals("") || (pass_param == null) || pass_param
				.equals("")) {
			return Authorization.NOT_ACCEPTABLE;
		}
		try {
			authRepo.addUser(BareJID.bareJIDInstance(user_name, getDomain().getVhost()
					.getDomain()), pass_param);
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "User added: {0}, pass: {1}", new Object[]{BareJID.toString(user_name, getDomain().getVhost()
								.getDomain()), pass_param});
			}
			setRegistration(user_name, pass_param, reg_params);
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "Registration data set for: {0}, pass: {1}, reg_params: {2}", new Object[]{BareJID.toString(user_name, getDomain()
								.getVhost().getDomain()), pass_param, reg_params});
			}

			return Authorization.AUTHORIZED;
		} catch (UserExistsException e) {
			return Authorization.CONFLICT;
		} catch (TigaseDBException e) {
			log.log(Level.SEVERE, "Repository access exception.", e);

			return Authorization.INTERNAL_SERVER_ERROR;
		}    // end of try-catch
	}

	/**
	 * Method description
	 *
	 *
	 * @param subnode
	 * @param key
	 *
	 * @throws NotAuthorizedException
	 * @throws TigaseDBException
	 */
	public void removeData(String subnode, String key)
					throws NotAuthorizedException, TigaseDBException {
		try {
			repo.removeData(getBareJID(), subnode, key);
		} catch (UserNotFoundException e) {
			log.log(Level.FINEST, "Problem accessing reposiotry: ", e);

			throw new NotAuthorizedException(NO_ACCESS_TO_REP_MSG, e);

			// } catch (TigaseDBException e) {
			// log.log(Level.SEVERE, "Repository access exception.", e);
		}    // end of try-catch
	}

	/**
	 * Removes the last data node given in subnode path as parameter to this
	 * method. All subnodes are moved as well an all data stored as
	 * <code>(key, val)</code> are removed as well. Changes are commited to
	 * repository immediatelly and there is no way to undo this operation so use
	 * it with care.
	 *
	 * @param subnode
	 *          a <code>String</code> value of path to node which has to be
	 *          removed.
	 * @exception NotAuthorizedException
	 *              is thrown when session has not been authorized yet and there
	 *              is no access to permanent storage.
	 * @throws TigaseDBException
	 * @see #setData(String, String, String)
	 */
	public void removeDataGroup(final String subnode)
					throws NotAuthorizedException, TigaseDBException {
		if (is_anonymous) {
			return;
		}
		if (!isAuthorized()) {
			throw new NotAuthorizedException(NO_ACCESS_TO_REP_MSG);
		}
		try {
			repo.removeSubnode(getBareJID(), subnode);
		} catch (UserNotFoundException e) {
			log.log(Level.FINEST, "Problem accessing reposiotry: ", e);

			throw new NotAuthorizedException(NO_ACCESS_TO_REP_MSG, e);

			// } catch (TigaseDBException e) {
			// log.log(Level.SEVERE, "Repository access exception.", e);
		}    // end of try-catch
	}

	/**
	 * Method description
	 *
	 *
	 * @param subnode
	 * @param key
	 *
	 * @throws NotAuthorizedException
	 * @throws TigaseDBException
	 */
	public void removeOfflineData(String subnode, String key)
					throws NotAuthorizedException, TigaseDBException {
		removeData(calcNode(OFFLINE_DATA_NODE, subnode), key);
	}

	/**
	 * Method description
	 *
	 *
	 * @param subnode
	 *
	 * @throws NotAuthorizedException
	 * @throws TigaseDBException
	 */
	public void removeOfflineDataGroup(String subnode)
					throws NotAuthorizedException, TigaseDBException {
		removeDataGroup(calcNode(OFFLINE_DATA_NODE, subnode));
	}

	/**
	 * Method description
	 *
	 *
	 * @param subnode
	 * @param key
	 *
	 * @throws NotAuthorizedException
	 * @throws TigaseDBException
	 */
	public void removePublicData(String subnode, String key)
					throws NotAuthorizedException, TigaseDBException {
		removeData(calcNode(PUBLIC_DATA_NODE, subnode), key);
	}

	/**
	 * Method description
	 *
	 *
	 * @param subnode
	 *
	 * @throws NotAuthorizedException
	 * @throws TigaseDBException
	 */
	public void removePublicDataGroup(String subnode)
					throws NotAuthorizedException, TigaseDBException {
		removeDataGroup(calcNode(PUBLIC_DATA_NODE, subnode));
	}

	// ~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param name_param
	 *
	 *
	 *
	 *
	 * @return a value of <code>Authorization</code>
	 * @throws NotAuthorizedException
	 * @throws TigaseDBException
	 * @throws TigaseStringprepException
	 */
	public Authorization unregister(String name_param)
					throws NotAuthorizedException, TigaseDBException, TigaseStringprepException {
		if (!isAuthorized()) {
			return Authorization.FORBIDDEN;
		}

		// Some clients send plain user name and others send
		// jid as user name. Let's resolve this here.
		String user_name = BareJID.parseJID(name_param)[0];

		if ((user_name == null) || user_name.trim().isEmpty()) {
			user_name = name_param;
		}    // end of if (user_mame == null || user_name.equals(""))
		if (getUserName().equals(user_name)) {
			try {
				authRepo.removeUser(BareJID.bareJIDInstance(user_name, getDomain().getVhost()
						.getDomain()));
				try {
					repo.removeUser(BareJID.bareJIDInstance(user_name, getDomain().getVhost()
							.getDomain()));
				} catch (UserNotFoundException ex) {

					// We ignore this error here. If auth_repo and user_repo are in fact
					// the same
					// database, then user has been already removed with the
					// auth_repo.removeUser(...)
					// then the second call to user_repo may throw the exception which is
					// fine.
				}

				// We mark the session as no longer authorized to prevent data access through
				// this session.
				logout();

				// Session authorized is returned only to indicate successful operation.
				return Authorization.AUTHORIZED;
			} catch (UserNotFoundException e) {
				return Authorization.REGISTRATION_REQUIRED;
			} catch (TigaseDBException e) {
				log.log(Level.SEVERE, "Repository access exception.", e);

				return Authorization.INTERNAL_SERVER_ERROR;
			}    // end of catch
		} else {
			return Authorization.FORBIDDEN;
		}
	}

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param xmpp_sessionId
	 *
	 *
	 *
	 *
	 * @return a value of <code>String</code>
	 * @throws NotAuthorizedException
	 * @throws TigaseDBException
	 */
	public String getAuthenticationToken(String xmpp_sessionId)
					throws NotAuthorizedException, TigaseDBException {
		UUID token = UUID.randomUUID();

		setData("tokens", xmpp_sessionId, token.toString());

		return token.toString();
	}

	// ~--- get methods ----------------------------------------------------------

	/**
	 * Gets the value of authState
	 *
	 * @return the value of authState
	 */
	public final Authorization getAuthState() {
		return this.authState;
	}

	// ~--- get methods ----------------------------------------------------------

	/**
	 * Returns user JID but without <em>resource</em> part. This is real user ID
	 * not session ID. To retrieve session ID - full JID refer to
	 * <code>getJID()</code> method.<br>
	 * If session has not been authorized yet this method throws
	 * <code>NotAuthorizedException</code>.
	 *
	 * @return a <code>String</code> value of user ID - this is user JID without
	 *         resource part. To obtain full user JID please refer to
	 *         <code>getJID</code> method.
	 *
	 * @exception NotAuthorizedException when this session has not been authorized
	 *                                   yet and some parts of user JID are not
	 *                                   known yet.
	 */
public abstract BareJID getBareJID() throws NotAuthorizedException;

	/**
	 * <code>getData</code> method is a twin sister (brother?) of
	 * <code>setData(String, String, String)</code> method. It allows you to
	 * retrieve data stored with above method. It is data stored in given node
	 * with given key identifier. If there are no data associated with given key
	 * or given node does not exist given <code>def</code> value is returned.
	 *
	 * @param subnode
	 *          a <code>String</code> value is path to node where pair
	 *          <code>(key, value)</code> are stored.
	 * @param key
	 *          a <code>String</code> value of key ID for data to retrieve.
	 * @param def
	 *          a <code>String</code> value of default returned if there is
	 *          nothing stored with given key. <code>def</code> can be set to any
	 *          value you wish to have back as default value or <code>null</code>
	 *          if you want to have back <code>null</code> if no data was found.
	 *          If you set <code>def</code> to <code>null</code> it has exactly
	 *          the same effect as if you use <code>getData(String)</code> method.
	 * @return a <code>String</code> value of data found for given key or
	 *         <code>def</code> if there was no data associated with given key.
	 * @exception NotAuthorizedException
	 *              is thrown when session has not been authorized yet and there
	 *              is no access to permanent storage.
	 * @throws TigaseDBException
	 * @see #setData(String, String, String)
	 */
	public String getData(String subnode, String key, String def)
					throws NotAuthorizedException, TigaseDBException {
		if (is_anonymous) {
			return null;
		}
		if (!isAuthorized()) {
			throw new NotAuthorizedException(NO_ACCESS_TO_REP_MSG);
		}
		try {
			return repo.getData(getBareJID(), subnode, key, def);
		} catch (UserNotFoundException e) {
			log.log(Level.FINEST, "Problem accessing reposiotry: ", e);

			throw new NotAuthorizedException(NO_ACCESS_TO_REP_MSG, e);

			// } catch (TigaseDBException e) {
			// log.log(Level.SEVERE, "Repository access exception.", e);
		}    // end of try-catch

		// return null;
	}

	/**
	 * This method retrieves list of all direct subnodes for given node. It works
	 * in similar way as <code>ls</code> unix command or <code>dir</code> under
	 * DOS/Windows systems.
	 *
	 * @param subnode
	 *          a <code>String</code> value of path to node for which we want to
	 *          retrieve list of direct subnodes.
	 * @return a <code>String[]</code> array of direct subnodes names for given
	 *         node.
	 * @exception NotAuthorizedException
	 *              is thrown when session has not been authorized yet and there
	 *              is no access to permanent storage.
	 * @throws TigaseDBException
	 * @see #setData(String, String, String)
	 */
	public String[] getDataGroups(String subnode)
					throws NotAuthorizedException, TigaseDBException {
		if (is_anonymous) {
			return null;
		}
		if (!isAuthorized()) {
			throw new NotAuthorizedException(NO_ACCESS_TO_REP_MSG);
		}
		try {
			return repo.getSubnodes(getBareJID(), subnode);
		} catch (UserNotFoundException e) {
			log.log(Level.FINEST, "Problem accessing reposiotry: ", e);

			throw new NotAuthorizedException(NO_ACCESS_TO_REP_MSG, e);

			// } catch (TigaseDBException e) {
			// log.log(Level.SEVERE, "Repository access exception.", e);
		}    // end of try-catch

		// return null;
	}

	/**
	 * This method returns all data keys available in permanent storage in given
	 * node. There is not though any information what kind of data is stored with
	 * this key. This is up to user (developer) to determine what data type is
	 * associated with key and what is it's meaning.
	 *
	 * @param subnode
	 *          a <code>String</code> value pointing to specific subnode in user
	 *          reposiotry where data have to be stored.
	 * @return a <code>String[]</code> array containing all data keys found in
	 *         given subnode.
	 * @exception NotAuthorizedException
	 *              is thrown when session has not been authorized yet and there
	 *              is no access to permanent storage.
	 * @throws TigaseDBException
	 * @see #setData(String, String, String)
	 */
	public String[] getDataKeys(final String subnode)
					throws NotAuthorizedException, TigaseDBException {
		if (is_anonymous) {
			return null;
		}
		if (!isAuthorized()) {
			throw new NotAuthorizedException(NO_ACCESS_TO_REP_MSG);
		}
		try {
			return repo.getKeys(getBareJID(), subnode);
		} catch (UserNotFoundException e) {
			log.log(Level.FINEST, "Problem accessing reposiotry: ", e);

			throw new NotAuthorizedException(NO_ACCESS_TO_REP_MSG, e);

			// } catch (TigaseDBException e) {
			// log.log(Level.SEVERE, "Repository access exception.", e);
		}    // end of try-catch

		// return null;
	}

	/**
	 * This method allows to retrieve list of values associated with one key. As
	 * it is possible to store many values with one key there are a few methods
	 * which provides this functionality. If given key does not exists in given
	 * subnode <code>null</code> is returned.
	 *
	 * @param subnode
	 *          a <code>String</code> value pointing to specific subnode in user
	 *          reposiotry where data have to be stored.
	 * @param key
	 *          a <code>String</code> value of data key ID.
	 * @return a <code>String[]</code> array containing all values found for given
	 *         key.
	 * @exception NotAuthorizedException
	 *              is thrown when session has not been authorized yet and there
	 *              is no access to permanent storage.
	 * @throws TigaseDBException
	 * @see #setData(String, String, String)
	 */
	public String[] getDataList(String subnode, String key)
					throws NotAuthorizedException, TigaseDBException {
		if (is_anonymous) {
			return null;
		}
		if (!isAuthorized()) {
			throw new NotAuthorizedException(NO_ACCESS_TO_REP_MSG);
		}
		try {
			return repo.getDataList(getBareJID(), subnode, key);
		} catch (UserNotFoundException e) {
			log.log(Level.FINEST, "Problem accessing reposiotry: ", e);

			throw new NotAuthorizedException(NO_ACCESS_TO_REP_MSG, e);

			// } catch (TigaseDBException e) {
			// log.log(Level.SEVERE, "Repository access exception.", e);
		}    // end of try-catch

		// return null;
	}

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of <code>VHostItem</code>
	 */
	public VHostItem getDomain() {
		return domain;
	}

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of <code>JID</code>
	 */
	public JID getDomainAsJID() {
		return domain.getVhost();
	}

	/**
	 * Method description
	 *
	 *
	 * @param subnode
	 * @param key
	 * @param def
	 *
	 *
	 *
	 *
	 * @return a value of <code>String</code>
	 * @throws NotAuthorizedException
	 * @throws TigaseDBException
	 */
	public String getOfflineData(String subnode, String key, String def)
					throws NotAuthorizedException, TigaseDBException {
		return getData(calcNode(OFFLINE_DATA_NODE, subnode), key, def);
	}

	/**
	 * Method description
	 *
	 *
	 * @param subnode
	 * @param key
	 *
	 *
	 *
	 *
	 * @return a value of <code>String[]</code>
	 * @throws NotAuthorizedException
	 * @throws TigaseDBException
	 */
	public String[] getOfflineDataList(String subnode, String key)
					throws NotAuthorizedException, TigaseDBException {
		return getDataList(calcNode(OFFLINE_DATA_NODE, subnode), key);
	}

	/**
	 * Method description
	 *
	 *
	 * @param subnode
	 * @param key
	 * @param def
	 *
	 *
	 *
	 *
	 * @return a value of <code>String</code>
	 * @throws NotAuthorizedException
	 * @throws TigaseDBException
	 */
	public String getPublicData(String subnode, String key, String def)
					throws NotAuthorizedException, TigaseDBException {
		return getData(calcNode(PUBLIC_DATA_NODE, subnode), key, def);
	}

	/**
	 * Method description
	 *
	 *
	 * @param subnode
	 * @param key
	 *
	 *
	 *
	 *
	 * @return a value of <code>String[]</code>
	 * @throws NotAuthorizedException
	 * @throws TigaseDBException
	 */
	public String[] getPublicDataList(String subnode, String key)
					throws NotAuthorizedException, TigaseDBException {
		return getDataList(calcNode(PUBLIC_DATA_NODE, subnode), key);
	}

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 *
	 * @return a value of <code>String</code>
	 * @throws NotAuthorizedException
	 */
	public abstract String getUserName() throws NotAuthorizedException;

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of <code>boolean</code>
	 */
	public boolean isAnonymous() {
		return is_anonymous;
	}

	// /**
	// * Sets the value of authState
	// *
	// * @param argAuthState Value to assign to this.authState
	// */
	// protected void setAuthState(final Authorization argAuthState) {
	// this.authState = argAuthState;
	// }

	/**
	 * This method allows you test this session if it already has been authorized.
	 * If <code>true</code> is returned as method result it means session has
	 * already been authorized, if <code>false</code> however session is still not
	 * authorized.
	 *
	 * @return a <code>boolean</code> value which informs whether this session has
	 *         been already authorized or not.
	 */
	public boolean isAuthorized() {
		return authState == Authorization.AUTHORIZED;
	}

	//~--- set methods ----------------------------------------------------------

	// ~--- set methods ----------------------------------------------------------

	/**
	 * This method stores given data in permanent storage in given point of
	 * hierarchy of data base. This method is similar to
	 * <code>setData(String, String)</code> and differs in one additional
	 * parameter which point to user data base subnode where data must be stored.
	 * It helps to organize user data in more logical hierarchy.<br>
	 * User data is kind of tree where you can store data in each tree node. The
	 * most relevant sample might be structure like typical file system or XML
	 * like or LDAP data base. The first implementation is actually done as XML
	 * file to make it easier test application and deploy simple installation
	 * where there is no more users than 1000.<br>
	 * To find out more about user repository refer to <code>UserRepository</code>
	 * interface for general info and to <code>XMLRepository</code> for detailed
	 * explanation regarding XML implementation of user repository.
	 * <br>
	 * Thus <code>subnode</code> is kind of path to data node. If you specify
	 * <code>null</code> or empty node data will be stored in root user node. This
	 * has exactly the same effect as you call
	 * <code>setData(String, String)</code>. If you want to store data in
	 * different node you must just specify node path like you do it to directory
	 * on most file systems:
	 *
	 * <pre>
	 * /roster
	 * </pre>
	 *
	 * Or, if you need access deeper node:
	 *
	 * <pre>
	 * /just/like/path/to/file
	 * </pre>
	 *
	 * <br>
	 * If given node does not yet exist it will be automaticaly created with all
	 * nodes in given path so there is no need for developer to perform additional
	 * action to create node. There is, however method
	 * <code>removeDataGroup(String)</code> for deleting specified node as nodes
	 * are not automaticaly deleted.
	 *
	 * @param subnode
	 *          a <code>String</code> value pointing to specific subnode in user
	 *          reposiotry where data have to be stored.
	 * @param key
	 *          a <code>String</code> value of data key ID.
	 * @param value
	 *          a <code>String</code> actual data stored in user repository.
	 * @exception NotAuthorizedException
	 *              is thrown when session has not been authorized yet and there
	 *              is no access to permanent storage.
	 * @throws TigaseDBException
	 * @see #removeDataGroup(String)
	 * @see UserRepository
	 */
	public void setData(String subnode, String key, String value)
					throws NotAuthorizedException, TigaseDBException {
		try {
			repo.setData(getBareJID(), subnode, key, value);
		} catch (UserNotFoundException e) {
			log.log(Level.FINEST, "Problem accessing reposiotry: ", e);

			throw new NotAuthorizedException(NO_ACCESS_TO_REP_MSG, e);

			// } catch (TigaseDBException e) {
			// log.log(Level.SEVERE, "Repository access exception.", e);
		}    // end of try-catch
	}

	/**
	 * This method allows to store list of values under one key ID reference. It
	 * is often necessary to keep set of values which can be refered by one key.
	 * As an example might be list of groups for specific buddy in roster. There
	 * is no actual need to store each group with separate key because we usually
	 * need to acces whole list of groups.
	 *
	 * @param subnode
	 *          a <code>String</code> value pointing to specific subnode in user
	 *          reposiotry where data have to be stored.
	 * @param key
	 *          a <code>String</code> value of data key ID.
	 * @param list
	 *          a <code>String[]</code> keeping list of actual data to be stored
	 *          in user repository.
	 * @exception NotAuthorizedException
	 *              is thrown when session has not been authorized yet and there
	 *              is no access to permanent storage.
	 * @throws TigaseDBException
	 * @see #setData(String, String, String)
	 */
	public void setDataList(final String subnode, final String key, final String[] list)
					throws NotAuthorizedException, TigaseDBException {
		if (is_anonymous) {
			return;
		}
		if (!isAuthorized()) {
			throw new NotAuthorizedException(NO_ACCESS_TO_REP_MSG);
		}
		try {
			repo.setDataList(getBareJID(), subnode, key, list);
		} catch (UserNotFoundException e) {
			log.log(Level.FINEST, "Problem accessing reposiotry: ", e);

			throw new NotAuthorizedException(NO_ACCESS_TO_REP_MSG, e);

			// } catch (TigaseDBException e) {
			// log.log(Level.SEVERE, "Repository access exception.", e);
		}    // end of try-catch
	}

	/**
	 * Method description
	 *
	 *
	 * @param domain
	 *
	 * @throws TigaseStringprepException
	 */
	public void setDomain(final VHostItem domain) throws TigaseStringprepException {
		this.domain = domain;
	}

	/**
	 * Method description
	 *
	 *
	 * @param subnode
	 * @param key
	 * @param value
	 *
	 * @throws NotAuthorizedException
	 * @throws TigaseDBException
	 */
	public void setOfflineData(String subnode, String key, String value)
					throws NotAuthorizedException, TigaseDBException {
		setData(calcNode(OFFLINE_DATA_NODE, subnode), key, value);
	}

	/**
	 * Method description
	 *
	 *
	 * @param subnode
	 * @param key
	 * @param list
	 *
	 * @throws NotAuthorizedException
	 * @throws TigaseDBException
	 */
	public void setOfflineDataList(String subnode, String key, String[] list)
					throws NotAuthorizedException, TigaseDBException {
		setDataList(calcNode(OFFLINE_DATA_NODE, subnode), key, list);
	}

	/**
	 * Method description
	 *
	 *
	 * @param subnode
	 * @param key
	 * @param value
	 *
	 * @throws NotAuthorizedException
	 * @throws TigaseDBException
	 */
	public void setPublicData(String subnode, String key, String value)
					throws NotAuthorizedException, TigaseDBException {
		setData(calcNode(PUBLIC_DATA_NODE, subnode), key, value);
	}

	/**
	 * Method description
	 *
	 *
	 * @param subnode
	 * @param key
	 * @param list
	 *
	 * @throws NotAuthorizedException
	 * @throws TigaseDBException
	 */
	public void setPublicDataList(String subnode, String key, String[] list)
					throws NotAuthorizedException, TigaseDBException {
		setDataList(calcNode(PUBLIC_DATA_NODE, subnode), key, list);
	}

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 */
	protected abstract void login();

	private String calcNode(String base, String subnode) {
		if (subnode == null) {
			return base;
		}    // end of if (subnode == null)

		return base + "/" + subnode;
	}

	private Authorization changeRegistration(final String name_param,
			final String pass_param, final Map<String, String> registr_params)
					throws NotAuthorizedException, TigaseDBException, TigaseStringprepException {
		if ((name_param == null) || name_param.equals("") || (pass_param == null) ||
				pass_param.equals("")) {
			return Authorization.BAD_REQUEST;
		}
		if (getUserName().equals(name_param)) {
			setRegistration(name_param, pass_param, registr_params);

			return Authorization.AUTHORIZED;
		} else {
			return Authorization.NOT_AUTHORIZED;
		}
	}

	//~--- get methods ----------------------------------------------------------

	private boolean isLoginAllowed() throws AuthorizationException {
		if (isAuthorized()) {
			throw new AuthorizationException("User session already authenticated. " +
					"Subsequent login is forbidden. You must loggin on a different connection.");
		}

		return true;
	}

	//~--- set methods ----------------------------------------------------------

	// ~--- set methods ----------------------------------------------------------
	private void setRegistration(final String name_param, final String pass_param,
			final Map<String, String> registr_params)
					throws TigaseDBException, TigaseStringprepException {
		try {
			authRepo.updatePassword(BareJID.bareJIDInstance(name_param, getDomain().getVhost()
					.getDomain()), pass_param);
			if (registr_params != null) {
				for (Map.Entry<String, String> entry : registr_params.entrySet()) {
					repo.setData(BareJID.bareJIDInstance(name_param, getDomain().getVhost()
							.getDomain()), entry.getKey(), entry.getValue());
				}
			}
		} catch (UserNotFoundException e) {
			log.log(Level.WARNING, "Problem accessing reposiotry: ", e);

			// } catch (TigaseDBException e) {
			// log.log(Level.SEVERE, "Repository access exception.", e);
		}    // end of try-catch
	}
}    // RepositoryAccess


//~ Formatted in Tigase Code Convention on 13/11/02
