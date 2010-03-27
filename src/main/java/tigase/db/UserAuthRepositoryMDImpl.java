
/*
* Tigase Jabber/XMPP Server
* Copyright (C) 2004-2010 "Artur Hefczyc" <artur.hefczyc@tigase.org>
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, version 3 of the License.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program. Look for COPYING file in the top folder.
* If not, see http://www.gnu.org/licenses/.
*
* $Rev$
* Last modified by $Author$
* $Date$
 */
package tigase.db;

//~--- non-JDK imports --------------------------------------------------------

import tigase.xmpp.BareJID;

//~--- JDK imports ------------------------------------------------------------

import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.logging.Level;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

/**
 * Created: Mar 27, 2010 9:10:21 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class UserAuthRepositoryMDImpl implements UserAuthRepository {
	private static final Logger log = Logger.getLogger(UserAuthRepositoryMDImpl.class.getName());

	//~--- fields ---------------------------------------------------------------

	private UserAuthRepository def = null;
	private ConcurrentSkipListMap<String, UserAuthRepository> repos =
		new ConcurrentSkipListMap<String, UserAuthRepository>();

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param domain
	 * @param repo
	 */
	public void addRepo(String domain, UserAuthRepository repo) {
		repos.put(domain, repo);
	}

	/**
	 * Method description
	 *
	 *
	 * @param user
	 * @param password
	 *
	 * @throws TigaseDBException
	 * @throws UserExistsException
	 */
	@Override
	public void addUser(BareJID user, String password)
			throws UserExistsException, TigaseDBException {
		UserAuthRepository repo = getRepo(user.getDomain());

		if (repo != null) {
			repo.addUser(user, password);
		} else {
			log.log(Level.WARNING,
					"Couldn't obtain user repository for domain: " + user.getDomain()
						+ ", not even default one!");
		}
	}

	/**
	 * Method description
	 *
	 *
	 * @param user
	 * @param digest
	 * @param id
	 * @param alg
	 *
	 * @return
	 *
	 * @throws AuthorizationException
	 * @throws TigaseDBException
	 * @throws UserNotFoundException
	 */
	@Override
	public boolean digestAuth(BareJID user, String digest, String id, String alg)
			throws UserNotFoundException, TigaseDBException, AuthorizationException {
		UserAuthRepository repo = getRepo(user.getDomain());

		if (repo != null) {
			return repo.digestAuth(user, digest, id, alg);
		} else {
			log.log(Level.WARNING,
					"Couldn't obtain user repository for domain: " + user.getDomain()
						+ ", not even default one!");
		}

		return false;
	}

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param domain
	 *
	 * @return
	 */
	public UserAuthRepository getRepo(String domain) {
		UserAuthRepository result = repos.get(domain);

		if (result == null) {
			result = def;
		}

		return result;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public String getResourceUri() {
		return def.getResourceUri();
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public long getUsersCount() {
		long result = 0;

		for (UserAuthRepository repo : repos.values()) {
			result += repo.getUsersCount();
		}

		return result;
	}

	/**
	 * Method description
	 *
	 *
	 * @param domain
	 *
	 * @return
	 */
	@Override
	public long getUsersCount(String domain) {
		UserAuthRepository repo = getRepo(domain);

		if (repo != null) {
			return repo.getUsersCount();
		} else {
			log.log(Level.WARNING,
					"Couldn't obtain user repository for domain: " + domain + ", not even default one!");
		}

		return -1;
	}

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param resource_uri
	 * @param params
	 *
	 * @throws DBInitException
	 */
	@Override
	public void initRepository(String resource_uri, Map<String, String> params)
			throws DBInitException {}

	/**
	 * Method description
	 *
	 *
	 * @param user
	 *
	 * @throws TigaseDBException
	 * @throws UserNotFoundException
	 */
	@Override
	public void logout(BareJID user) throws UserNotFoundException, TigaseDBException {
		UserAuthRepository repo = getRepo(user.getDomain());

		if (repo != null) {
			repo.logout(user);
		} else {
			log.log(Level.WARNING,
					"Couldn't obtain user repository for domain: " + user.getDomain()
						+ ", not even default one!");
		}
	}

	/**
	 * Method description
	 *
	 *
	 * @param authProps
	 *
	 * @return
	 *
	 * @throws AuthorizationException
	 * @throws TigaseDBException
	 * @throws UserNotFoundException
	 */
	@Override
	public boolean otherAuth(Map<String, Object> authProps)
			throws UserNotFoundException, TigaseDBException, AuthorizationException {
		UserAuthRepository repo = getRepo((String) authProps.get(SERVER_NAME_KEY));

		if (repo != null) {
			return repo.otherAuth(authProps);
		} else {
			log.log(Level.WARNING,
					"Couldn't obtain user repository for domain: "
						+ (String) authProps.get(SERVER_NAME_KEY) + ", not even default one!");
		}

		return false;
	}

	/**
	 * Method description
	 *
	 *
	 * @param user
	 * @param password
	 *
	 * @return
	 *
	 * @throws AuthorizationException
	 * @throws TigaseDBException
	 * @throws UserNotFoundException
	 */
	@Override
	public boolean plainAuth(BareJID user, String password)
			throws UserNotFoundException, TigaseDBException, AuthorizationException {
		UserAuthRepository repo = getRepo(user.getDomain());

		if (repo != null) {
			return repo.plainAuth(user, password);
		} else {
			log.log(Level.WARNING,
					"Couldn't obtain user repository for domain: " + user.getDomain()
						+ ", not even default one!");
		}

		return false;
	}

	/**
	 * Method description
	 *
	 *
	 * @param authProps
	 */
	@Override
	public void queryAuth(Map<String, Object> authProps) {
		UserAuthRepository repo = getRepo((String) authProps.get(SERVER_NAME_KEY));

		if (repo != null) {
			repo.queryAuth(authProps);
		} else {
			log.log(Level.WARNING,
					"Couldn't obtain user repository for domain: "
						+ (String) authProps.get(SERVER_NAME_KEY) + ", not even default one!");
		}
	}

	/**
	 * Method description
	 *
	 *
	 * @param domain
	 *
	 * @return
	 */
	public UserAuthRepository removeRepo(String domain) {
		return repos.remove(domain);
	}

	/**
	 * Method description
	 *
	 *
	 * @param user
	 *
	 * @throws TigaseDBException
	 * @throws UserNotFoundException
	 */
	@Override
	public void removeUser(BareJID user) throws UserNotFoundException, TigaseDBException {
		UserAuthRepository repo = getRepo(user.getDomain());

		if (repo != null) {
			repo.removeUser(user);
		} else {
			log.log(Level.WARNING,
					"Couldn't obtain user repository for domain: " + user.getDomain()
						+ ", not even default one!");
		}
	}

	//~--- set methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param repo
	 */
	public void setDefault(UserAuthRepository repo) {
		def = repo;
	}

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param user
	 * @param password
	 *
	 * @throws TigaseDBException
	 * @throws UserNotFoundException
	 */
	@Override
	public void updatePassword(BareJID user, String password)
			throws UserNotFoundException, TigaseDBException {
		UserAuthRepository repo = getRepo(user.getDomain());

		if (repo != null) {
			repo.updatePassword(user, password);
		} else {
			log.log(Level.WARNING,
					"Couldn't obtain user repository for domain: " + user.getDomain()
						+ ", not even default one!");
		}
	}
}


//~ Formatted in Sun Code Convention


//~ Formatted by Jindent --- http://www.jindent.com
