/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2008 "Artur Hefczyc" <artur.hefczyc@tigase.org>
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

import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created: Jan 28, 2009 8:46:53 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class UserRepositoryPool implements UserRepository {

  private static final Logger log =
    Logger.getLogger(UserRepositoryPool.class.getName());

	private LinkedBlockingQueue<UserRepository> repoPool =
					new LinkedBlockingQueue<UserRepository>();

	public void addRepo(UserRepository repo) {
		repoPool.offer(repo);
	}

	public UserRepository takeRepo() {
		try {
			return repoPool.take();
		} catch (InterruptedException ex) {
			log.log(Level.WARNING, "Couldn't obtain user repository from the pool",
							ex);
		}
		return null;
	}

	@Override
	public void initRepository(String resource_uri,
					Map<String, String> params)
					throws DBInitException {	}

	@Override
	public String getResourceUri() {
		return null;
	}

	@Override
	public List<String> getUsers() throws TigaseDBException {
		UserRepository repo = takeRepo();
		if (repo != null) {
			try {
				return repo.getUsers();
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
		UserRepository repo = takeRepo();
		if (repo != null) {
			try {
				return repo.getUsersCount();
			} finally {
				addRepo(repo);
			}
		} else {
			log.warning("repo is NULL, pool empty? - " + repoPool.size());
		}
		return 0;
	}

	@Override
	public boolean userExists(String user) {
		UserRepository repo = takeRepo();
		if (repo != null) {
			try {
				return repo.userExists(user);
			} finally {
				addRepo(repo);
			}
		} else {
			log.warning("repo is NULL, pool empty? - " + repoPool.size());
		}
		return false;
	}

	@Override
	public void addUser(String user)
					throws UserExistsException, TigaseDBException {
		UserRepository repo = takeRepo();
		if (repo != null) {
			try {
				repo.addUser(user);
			} finally {
				addRepo(repo);
			}
		} else {
			log.warning("repo is NULL, pool empty? - " + repoPool.size());
		}
	}

	@Override
	public void removeUser(String user)
					throws UserNotFoundException, TigaseDBException {
		UserRepository repo = takeRepo();
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
	public String[] getDataList(String user, String subnode, String key)
					throws UserNotFoundException, TigaseDBException {
		UserRepository repo = takeRepo();
		if (repo != null) {
			try {
				return repo.getDataList(user, subnode, key);
			} finally {
				addRepo(repo);
			}
		} else {
			log.warning("repo is NULL, pool empty? - " + repoPool.size());
		}
		return null;
	}

	@Override
	public String getData(String user, String subnode, String key, String def)
					throws UserNotFoundException, TigaseDBException {
		UserRepository repo = takeRepo();
		if (repo != null) {
			try {
				return repo.getData(user, subnode, key, def);
			} finally {
				addRepo(repo);
			}
		} else {
			log.warning("repo is NULL, pool empty? - " + repoPool.size());
		}
		return null;
	}

	@Override
	public String getData(String user, String subnode, String key)
					throws UserNotFoundException, TigaseDBException {
		UserRepository repo = takeRepo();
		if (repo != null) {
			try {
				return repo.getData(user, subnode, key);
			} finally {
				addRepo(repo);
			}
		} else {
			log.warning("repo is NULL, pool empty? - " + repoPool.size());
		}
		return null;
	}

	@Override
	public String getData(String user, String key)
					throws UserNotFoundException, TigaseDBException {
		UserRepository repo = takeRepo();
		if (repo != null) {
			try {
				return repo.getData(user, key);
			} finally {
				addRepo(repo);
			}
		} else {
			log.warning("repo is NULL, pool empty? - " + repoPool.size());
		}
		return null;
	}

	@Override
	public String[] getSubnodes(String user, String subnode)
					throws UserNotFoundException, TigaseDBException {
		UserRepository repo = takeRepo();
		if (repo != null) {
			try {
				return repo.getSubnodes(user, subnode);
			} finally {
				addRepo(repo);
			}
		} else {
			log.warning("repo is NULL, pool empty? - " + repoPool.size());
		}
		return null;
	}

	@Override
	public String[] getSubnodes(String user)
					throws UserNotFoundException, TigaseDBException {
		UserRepository repo = takeRepo();
		if (repo != null) {
			try {
				return repo.getSubnodes(user);
			} finally {
				addRepo(repo);
			}
		} else {
			log.warning("repo is NULL, pool empty? - " + repoPool.size());
		}
		return null;
	}

	@Override
	public String[] getKeys(String user, String subnode)
					throws UserNotFoundException, TigaseDBException {
		UserRepository repo = takeRepo();
		if (repo != null) {
			try {
				return repo.getKeys(user, subnode);
			} finally {
				addRepo(repo);
			}
		} else {
			log.warning("repo is NULL, pool empty? - " + repoPool.size());
		}
		return null;
	}

	@Override
	public String[] getKeys(String user)
					throws UserNotFoundException, TigaseDBException {
		UserRepository repo = takeRepo();
		if (repo != null) {
			try {
				return repo.getKeys(user);
			} finally {
				addRepo(repo);
			}
		} else {
			log.warning("repo is NULL, pool empty? - " + repoPool.size());
		}
		return null;
	}

	@Override
	public void removeData(String user, String subnode, String key)
					throws UserNotFoundException, TigaseDBException {
		UserRepository repo = takeRepo();
		if (repo != null) {
			try {
				repo.removeData(user, subnode, key);
			} finally {
				addRepo(repo);
			}
		} else {
			log.warning("repo is NULL, pool empty? - " + repoPool.size());
		}
	}

	@Override
	public void removeData(String user, String key)
					throws UserNotFoundException, TigaseDBException {
		UserRepository repo = takeRepo();
		if (repo != null) {
			try {
				repo.removeData(user, key);
			} finally {
				addRepo(repo);
			}
		} else {
			log.warning("repo is NULL, pool empty? - " + repoPool.size());
		}
	}

	@Override
	public void removeSubnode(String user, String subnode)
					throws UserNotFoundException, TigaseDBException {
		UserRepository repo = takeRepo();
		if (repo != null) {
			try {
				repo.removeSubnode(user, subnode);
			} finally {
				addRepo(repo);
			}
		} else {
			log.warning("repo is NULL, pool empty? - " + repoPool.size());
		}
	}

	@Override
	public void setData(String user, String subnode, String key, String value)
					throws UserNotFoundException, TigaseDBException {
		UserRepository repo = takeRepo();
		if (repo != null) {
			try {
				repo.setData(user, subnode, key, value);
			} finally {
				addRepo(repo);
			}
		} else {
			log.warning("repo is NULL, pool empty? - " + repoPool.size());
		}
	}

	@Override
	public void setData(String user, String key, String value)
					throws UserNotFoundException, TigaseDBException {
		UserRepository repo = takeRepo();
		if (repo != null) {
			try {
				repo.setData(user, key, value);
			} finally {
				addRepo(repo);
			}
		} else {
			log.warning("repo is NULL, pool empty? - " + repoPool.size());
		}
	}

	@Override
	public void setDataList(String user, String subnode, String key, String[] list)
					throws UserNotFoundException, TigaseDBException {
		UserRepository repo = takeRepo();
		if (repo != null) {
			try {
				repo.setDataList(user, subnode, key, list);
			} finally {
				addRepo(repo);
			}
		} else {
			log.warning("repo is NULL, pool empty? - " + repoPool.size());
		}
	}

	@Override
	public void addDataList(String user, String subnode, String key, String[] list)
					throws UserNotFoundException, TigaseDBException {
		UserRepository repo = takeRepo();
		if (repo != null) {
			try {
				repo.addDataList(user, subnode, key, list);
			} finally {
				addRepo(repo);
			}
		} else {
			log.warning("repo is NULL, pool empty? - " + repoPool.size());
		}
	}

}
