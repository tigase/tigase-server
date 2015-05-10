
/*
* Tigase Jabber/XMPP Server
* Copyright (C) 2004-2012 "Artur Hefczyc" <artur.hefczyc@tigase.org>
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
package tigase.db;

//~--- non-JDK imports --------------------------------------------------------

import tigase.xmpp.BareJID;

//~--- JDK imports ------------------------------------------------------------


import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

/**
 * Created: Mar 27, 2010 11:31:17 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class AuthRepositoryPool implements AuthRepository {
	private static final Logger log = Logger.getLogger(AuthRepositoryPool.class.getName());

	//~--- fields ---------------------------------------------------------------

	private LinkedBlockingQueue<AuthRepository> repoPool =
		new LinkedBlockingQueue<AuthRepository>();

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param repo
	 */
	public void addRepo(AuthRepository repo) {
		repoPool.offer(repo);
	}

	@Override
	public void addUser(BareJID user, String password)
			throws UserExistsException, TigaseDBException {
		AuthRepository repo = takeRepo();

		if (repo != null) {
			try {
				repo.addUser(user, password);
			} finally {
				addRepo(repo);
			}
		} else {
			log.warning("repo is NULL, pool empty? - " + repoPool.size());
		}
	}

	@Override
	@Deprecated
	public boolean digestAuth(BareJID user, String digest, String id, String alg)
			throws UserNotFoundException, TigaseDBException, AuthorizationException {
		AuthRepository repo = takeRepo();

		if (repo != null) {
			try {
				return repo.digestAuth(user, digest, id, alg);
			} finally {
				addRepo(repo);
			}
		} else {
			log.warning("repo is NULL, pool empty? - " + repoPool.size());
		}

		return false;
	}

	//~--- get methods ----------------------------------------------------------

	@Override
	public String getResourceUri() {
		AuthRepository repo = takeRepo();

		if (repo != null) {
			try {
				return repo.getResourceUri();
			} finally {
				addRepo(repo);
			}
		} else {
			log.warning("repo is NULL, pool empty? - " + repoPool.size());
		}

		return null;
	}

	@Override
	public long getUsersCount() {
		AuthRepository repo = takeRepo();

		if (repo != null) {
			try {
				return repo.getUsersCount();
			} finally {
				addRepo(repo);
			}
		} else {
			log.warning("repo is NULL, pool empty? - " + repoPool.size());
		}

		return -1;
	}

	@Override
	public long getUsersCount(String domain) {
		AuthRepository repo = takeRepo();

		if (repo != null) {
			try {
				return repo.getUsersCount(domain);
			} finally {
				addRepo(repo);
			}
		} else {
			log.warning("repo is NULL, pool empty? - " + repoPool.size());
		}

		return -1;
	}

	//~--- methods --------------------------------------------------------------

	@Override
	public void initRepository(String resource_uri, Map<String, String> params)
			throws DBInitException {}

	@Override
	public void logout(BareJID user) throws UserNotFoundException, TigaseDBException {
		AuthRepository repo = takeRepo();

		if (repo != null) {
			try {
				repo.logout(user);
			} finally {
				addRepo(repo);
			}
		} else {
			log.warning("repo is NULL, pool empty? - " + repoPool.size());
		}
	}

	@Override
	public boolean otherAuth(Map<String, Object> authProps)
			throws UserNotFoundException, TigaseDBException, AuthorizationException {
		AuthRepository repo = takeRepo();

		if (repo != null) {
			try {
				return repo.otherAuth(authProps);
			} finally {
				addRepo(repo);
			}
		} else {
			log.warning("repo is NULL, pool empty? - " + repoPool.size());
		}

		return false;
	}

	@Override
	@Deprecated
	public boolean plainAuth(BareJID user, String password)
			throws UserNotFoundException, TigaseDBException, AuthorizationException {
		AuthRepository repo = takeRepo();

		if (repo != null) {
			try {
				return repo.plainAuth(user, password);
			} finally {
				addRepo(repo);
			}
		} else {
			log.warning("repo is NULL, pool empty? - " + repoPool.size());
		}

		return false;
	}

	@Override
	public void queryAuth(Map<String, Object> authProps) {
		AuthRepository repo = takeRepo();

		if (repo != null) {
			try {
				repo.queryAuth(authProps);
			} finally {
				addRepo(repo);
			}
		} else {
			log.warning("repo is NULL, pool empty? - " + repoPool.size());
		}
	}

	@Override
	public void removeUser(BareJID user) throws UserNotFoundException, TigaseDBException {
		AuthRepository repo = takeRepo();

		if (repo != null) {
			try {
				repo.removeUser(user);
			} finally {
				addRepo(repo);
			}
		} else {
			log.warning("repo is NULL, pool empty? - " + repoPool.size());
		}
	}

	/**
	 * Method description
	 *
	 *
	 * 
	 */
	public AuthRepository takeRepo() {
		try {
			return repoPool.take();
		} catch (InterruptedException ex) {
			log.log(Level.WARNING, "Couldn't obtain user auth repository from the pool", ex);
		}

		return null;
	}

	@Override
	public void updatePassword(BareJID user, String password)
			throws UserNotFoundException, TigaseDBException {
		AuthRepository repo = takeRepo();

		if (repo != null) {
			try {
				repo.updatePassword(user, password);
			} finally {
				addRepo(repo);
			}
		} else {
			log.warning("repo is NULL, pool empty? - " + repoPool.size());
		}
	}

	@Override
	public String getPassword(BareJID user) throws UserNotFoundException, TigaseDBException {
		AuthRepository repo = takeRepo();

		if (repo != null) {
			try {
				return repo.getPassword(user);
			} finally {
				addRepo(repo);
			}
		} else {
			log.warning("repo is NULL, pool empty? - " + repoPool.size());
			return null;
		}
	}
	
	@Override
	public boolean isUserDisabled(BareJID user) throws UserNotFoundException, TigaseDBException {
		AuthRepository repo = takeRepo();

		if (repo != null) {
			try {
				return repo.isUserDisabled(user);
			} finally {
				addRepo(repo);
			}
		} else {
			log.warning("repo is NULL, pool empty? - " + repoPool.size());
			return false;
		}
	}
	
	@Override
	public void setUserDisabled(BareJID user, Boolean value) throws UserNotFoundException, TigaseDBException {
		AuthRepository repo = takeRepo();

		if (repo != null) {
			try {
				repo.setUserDisabled(user, value);
			} finally {
				addRepo(repo);
			}
		} else {
			log.warning("repo is NULL, pool empty? - " + repoPool.size());
		}
	}		
}
