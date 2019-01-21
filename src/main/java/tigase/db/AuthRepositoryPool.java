/**
 * Tigase XMPP Server - The instant messaging server
 * Copyright (C) 2004 Tigase, Inc. (office@tigase.com)
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
 */
package tigase.db;

import tigase.auth.credentials.Credentials;
import tigase.xmpp.jid.BareJID;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Pool for authentication repositories. * <br> This pool should be used if connection to authentication storage is
 * blocking or synchronized, ie. implemented using single connection.* <br> If implementation of
 * <code>AuthRepository</code> uses connection pool or non blocking, concurrent access to authentication storage (ie.
 * <code>DataSourcePool</code>), then this pool is not need.
 * <br>
 * Created: Mar 27, 2010 11:31:17 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class AuthRepositoryPool
		implements AuthRepository, RepositoryPool<AuthRepository> {

	private static final Logger log = Logger.getLogger(AuthRepositoryPool.class.getName());

	private LinkedBlockingQueue<AuthRepository> repoPool = new LinkedBlockingQueue<AuthRepository>();

	public void addRepo(AuthRepository repo) {
		repoPool.offer(repo);
	}

	@Override
	public void addUser(BareJID user, String password) throws TigaseDBException {
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
	public AccountStatus getAccountStatus(BareJID user) throws TigaseDBException {
		AuthRepository repo = takeRepo();

		if (repo != null) {
			try {
				return repo.getAccountStatus(user);
			} finally {
				addRepo(repo);
			}
		} else {
			log.warning("repo is NULL, pool empty? - " + repoPool.size());
			return null;
		}
	}

	@Override
	public Credentials getCredentials(BareJID user, String username) throws TigaseDBException {
		AuthRepository repo = takeRepo();

		if (repo != null) {
			try {
				return repo.getCredentials(user, username);
			} finally {
				addRepo(repo);
			}
		} else {
			log.warning("repo is NULL, pool empty? - " + repoPool.size());
		}
		return null;
	}

	@Override
	public String getPassword(BareJID user) throws TigaseDBException {
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
	public Collection<String> getUsernames(BareJID user) throws TigaseDBException {
		AuthRepository repo = takeRepo();

		if (repo != null) {
			try {
				return repo.getUsernames(user);
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

	@Override
	@Deprecated
	public void initRepository(String resource_uri, Map<String, String> params) throws DBInitException {
	}

	@Override
	public boolean isMechanismSupported(String domain, String mechanism) {
		AuthRepository repo = takeRepo();

		if (repo != null) {
			try {
				return repo.isMechanismSupported(domain, mechanism);
			} finally {
				addRepo(repo);
			}
		} else {
			log.warning("repo is NULL, pool empty? - " + repoPool.size());
			return false;
		}
	}

	@Override
	public boolean isUserDisabled(BareJID user) throws TigaseDBException {
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
	public void loggedIn(BareJID user) throws TigaseDBException {
		AuthRepository repo = takeRepo();

		if (repo != null) {
			try {
				repo.loggedIn(user);
			} finally {
				addRepo(repo);
			}
		} else {
			log.warning("repo is NULL, pool empty? - " + repoPool.size());
		}
	}

	@Override
	public void logout(BareJID user) throws TigaseDBException {
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
			throws TigaseDBException, AuthorizationException {
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
	public void removeCredential(BareJID user, String username) throws TigaseDBException {
		AuthRepository repo = takeRepo();

		if (repo != null) {
			try {
				repo.removeCredential(user, username);
			} finally {
				addRepo(repo);
			}
		} else {
			log.warning("repo is NULL, pool empty? - " + repoPool.size());
		}
	}

	@Override
	public void removeUser(BareJID user) throws TigaseDBException {
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

	@Override
	public void setAccountStatus(BareJID user, AccountStatus status) throws TigaseDBException {
		AuthRepository repo = takeRepo();

		if (repo != null) {
			try {
				repo.setAccountStatus(user, status);
			} finally {
				addRepo(repo);
			}
		} else {
			log.warning("repo is NULL, pool empty? - " + repoPool.size());
		}
	}

	@Override
	public void setUserDisabled(BareJID user, Boolean value) throws TigaseDBException {
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

	public AuthRepository takeRepo() {
		try {
			return repoPool.take();
		} catch (InterruptedException ex) {
			log.log(Level.WARNING, "Couldn't obtain user auth repository from the pool", ex);
		}

		return null;
	}

	@Override
	public void updateCredential(BareJID user, String username, String password) throws TigaseDBException {
		AuthRepository repo = takeRepo();

		if (repo != null) {
			try {
				repo.updateCredential(user, username, password);
			} finally {
				addRepo(repo);
			}
		} else {
			log.warning("repo is NULL, pool empty? - " + repoPool.size());
		}
	}

	@Override
	public void updatePassword(BareJID user, String password) throws TigaseDBException {
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
}
