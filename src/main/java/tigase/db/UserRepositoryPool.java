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

import tigase.util.SimpleCache;

import tigase.xmpp.BareJID;

//~--- JDK imports ------------------------------------------------------------

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

/**
 * Created: Jan 28, 2009 8:46:53 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class UserRepositoryPool implements UserRepository {
	private static final Logger log = Logger.getLogger(UserRepositoryPool.class.getName());

	//~--- fields ---------------------------------------------------------------

	private Map<String, Object> cache = null;
	private LinkedBlockingQueue<UserRepository> repoPool =
		new LinkedBlockingQueue<UserRepository>();

	//~--- methods --------------------------------------------------------------

	@Override
	public void addDataList(BareJID user, String subnode, String key, String[] list)
			throws UserNotFoundException, TigaseDBException {
		UserRepository repo = takeRepo();

		if (repo != null) {
			try {
				repo.addDataList(user, subnode, key, list);
			} finally {
				addRepo(repo);
			}
		} else {
			log.log(Level.WARNING, "repo is NULL, pool empty? - {0}", repoPool.size());
		}
	}

	/**
	 * Method description
	 *
	 *
	 * @param repo
	 */
	public void addRepo(UserRepository repo) {
		repoPool.offer(repo);
	}

	@Override
	public void addUser(BareJID user) throws UserExistsException, TigaseDBException {
		UserRepository repo = takeRepo();

		if (repo != null) {
			try {
				repo.addUser(user);
			} finally {
				addRepo(repo);
			}
		} else {
			log.log(Level.WARNING, "repo is NULL, pool empty? - {0}", repoPool.size());
		}
	}

	//~--- get methods ----------------------------------------------------------

	@Override
	public String getData(BareJID user, String subnode, String key, String def)
			throws UserNotFoundException, TigaseDBException {
		String data = (String) cache.get(user + "/" + subnode + "/" + key);

		if (data != null) {
			return data;
		}

		UserRepository repo = takeRepo();

		if (repo != null) {
			try {
				return repo.getData(user, subnode, key, def);
			} finally {
				addRepo(repo);
			}
		} else {
			log.log(Level.WARNING, "repo is NULL, pool empty? - {0}", repoPool.size());
		}

		return null;
	}

	@Override
	public String getData(BareJID user, String subnode, String key)
			throws UserNotFoundException, TigaseDBException {
		String data = (String) cache.get(user + "/" + subnode + "/" + key);

		if (data != null) {
			return data;
		}

		UserRepository repo = takeRepo();

		if (repo != null) {
			try {
				return repo.getData(user, subnode, key);
			} finally {
				addRepo(repo);
			}
		} else {
			log.log(Level.WARNING, "repo is NULL, pool empty? - {0}", repoPool.size());
		}

		return null;
	}

	@Override
	public String getData(BareJID user, String key)
			throws UserNotFoundException, TigaseDBException {
		String data = (String) cache.get(user + "/" + key);

		if (data != null) {
			return data;
		}

		UserRepository repo = takeRepo();

		if (repo != null) {
			try {
				return repo.getData(user, key);
			} finally {
				addRepo(repo);
			}
		} else {
			log.log(Level.WARNING, "repo is NULL, pool empty? - {0}", repoPool.size());
		}

		return null;
	}

	@Override
	public String[] getDataList(BareJID user, String subnode, String key)
			throws UserNotFoundException, TigaseDBException {
		UserRepository repo = takeRepo();

		if (repo != null) {
			try {
				return repo.getDataList(user, subnode, key);
			} finally {
				addRepo(repo);
			}
		} else {
			log.log(Level.WARNING, "repo is NULL, pool empty? - {0}", repoPool.size());
		}

		return null;
	}

	@Override
	public String[] getKeys(BareJID user, String subnode)
			throws UserNotFoundException, TigaseDBException {
		UserRepository repo = takeRepo();

		if (repo != null) {
			try {
				return repo.getKeys(user, subnode);
			} finally {
				addRepo(repo);
			}
		} else {
			log.log(Level.WARNING, "repo is NULL, pool empty? - {0}", repoPool.size());
		}

		return null;
	}

	@Override
	public String[] getKeys(BareJID user) throws UserNotFoundException, TigaseDBException {
		UserRepository repo = takeRepo();

		if (repo != null) {
			try {
				return repo.getKeys(user);
			} finally {
				addRepo(repo);
			}
		} else {
			log.log(Level.WARNING, "repo is NULL, pool empty? - {0}", repoPool.size());
		}

		return null;
	}

	@Override
	public String getResourceUri() {
		return null;
	}

	@Override
	public String[] getSubnodes(BareJID user, String subnode)
			throws UserNotFoundException, TigaseDBException {
		UserRepository repo = takeRepo();

		if (repo != null) {
			try {
				return repo.getSubnodes(user, subnode);
			} finally {
				addRepo(repo);
			}
		} else {
			log.log(Level.WARNING, "repo is NULL, pool empty? - {0}", repoPool.size());
		}

		return null;
	}

	@Override
	public String[] getSubnodes(BareJID user) throws UserNotFoundException, TigaseDBException {
		UserRepository repo = takeRepo();

		if (repo != null) {
			try {
				return repo.getSubnodes(user);
			} finally {
				addRepo(repo);
			}
		} else {
			log.log(Level.WARNING, "repo is NULL, pool empty? - {0}", repoPool.size());
		}

		return null;
	}

	@Override
	public long getUserUID(BareJID user) throws TigaseDBException {
		UserRepository repo = takeRepo();

		if (repo != null) {
			try {
				return repo.getUserUID(user);
			} finally {
				addRepo(repo);
			}
		} else {
			log.log(Level.WARNING, "repo is NULL, pool empty? - {0}", repoPool.size());
		}

		return -1;
	}

	@Override
	public List<BareJID> getUsers() throws TigaseDBException {
		UserRepository repo = takeRepo();

		if (repo != null) {
			try {
				return repo.getUsers();
			} finally {
				addRepo(repo);
			}
		} else {
			log.log(Level.WARNING, "repo is NULL, pool empty? - {0}", repoPool.size());
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
			log.log(Level.WARNING, "repo is NULL, pool empty? - {0}", repoPool.size());
		}

		return 0;
	}

	@Override
	public long getUsersCount(String domain) {
		UserRepository repo = takeRepo();

		if (repo != null) {
			try {
				return repo.getUsersCount(domain);
			} finally {
				addRepo(repo);
			}
		} else {
			log.log(Level.WARNING, "repo is NULL, pool empty? - {0}", repoPool.size());
		}

		return 0;
	}

	//~--- methods --------------------------------------------------------------

	@Override
	public void initRepository(String resource_uri, Map<String, String> params)
			throws DBInitException {
		if (resource_uri.contains("cacheRepo=off")) {
			log.fine("Disabling cache.");
			cache = Collections.synchronizedMap(new RepoCache(0, -1000));
		} else {
			cache = Collections.synchronizedMap(new RepoCache(10000, 60 * 1000));
		}
	}

	@Override
	public void removeData(BareJID user, String subnode, String key)
			throws UserNotFoundException, TigaseDBException {
		cache.remove(user + "/" + subnode + "/" + key);

		UserRepository repo = takeRepo();

		if (repo != null) {
			try {
				repo.removeData(user, subnode, key);
			} finally {
				addRepo(repo);
			}
		} else {
			log.log(Level.WARNING, "repo is NULL, pool empty? - {0}", repoPool.size());
		}
	}

	@Override
	public void removeData(BareJID user, String key)
			throws UserNotFoundException, TigaseDBException {
		cache.remove(user + "/" + key);

		UserRepository repo = takeRepo();

		if (repo != null) {
			try {
				repo.removeData(user, key);
			} finally {
				addRepo(repo);
			}
		} else {
			log.log(Level.WARNING, "repo is NULL, pool empty? - {0}", repoPool.size());
		}
	}

	@Override
	public void removeSubnode(BareJID user, String subnode)
			throws UserNotFoundException, TigaseDBException {
		cache.remove(user + "/" + subnode);

		UserRepository repo = takeRepo();

		if (repo != null) {
			try {
				repo.removeSubnode(user, subnode);
			} finally {
				addRepo(repo);
			}
		} else {
			log.log(Level.WARNING, "repo is NULL, pool empty? - {0}", repoPool.size());
		}
	}

	@Override
	public void removeUser(BareJID user) throws UserNotFoundException, TigaseDBException {
		UserRepository repo = takeRepo();

		if (repo != null) {
			try {
				repo.removeUser(user);
			} finally {
				addRepo(repo);
			}
		} else {
			log.log(Level.WARNING, "repo is NULL, pool empty? - {0}", repoPool.size());
		}
	}

	//~--- set methods ----------------------------------------------------------

	@Override
	public void setData(BareJID user, String subnode, String key, String value)
			throws UserNotFoundException, TigaseDBException {
		UserRepository repo = takeRepo();

		if (repo != null) {
			try {
				repo.setData(user, subnode, key, value);
			} finally {
				addRepo(repo);
			}
		} else {
			log.log(Level.WARNING, "repo is NULL, pool empty? - {0}", repoPool.size());
		}

		cache.put(user + "/" + subnode + "/" + key, value);
	}

	@Override
	public void setData(BareJID user, String key, String value)
			throws UserNotFoundException, TigaseDBException {
		UserRepository repo = takeRepo();

		if (repo != null) {
			try {
				repo.setData(user, key, value);
			} finally {
				addRepo(repo);
			}
		} else {
			log.log(Level.WARNING, "repo is NULL, pool empty? - {0}", repoPool.size());
		}

		cache.put(user + "/" + key, value);
	}

	@Override
	public void setDataList(BareJID user, String subnode, String key, String[] list)
			throws UserNotFoundException, TigaseDBException {
		UserRepository repo = takeRepo();

		if (repo != null) {
			try {
				repo.setDataList(user, subnode, key, list);
			} finally {
				addRepo(repo);
			}
		} else {
			log.log(Level.WARNING, "repo is NULL, pool empty? - {0}", repoPool.size());
		}
	}

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * 
	 */
	public UserRepository takeRepo() {
		try {
			return repoPool.take();
		} catch (InterruptedException ex) {
			log.log(Level.WARNING, "Couldn't obtain user repository from the pool", ex);
		}

		return null;
	}

	@Override
	public boolean userExists(BareJID user) {
		UserRepository repo = takeRepo();

		if (repo != null) {
			try {
				return repo.userExists(user);
			} finally {
				addRepo(repo);
			}
		} else {
			log.log(Level.WARNING, "repo is NULL, pool empty? - {0}", repoPool.size());
		}

		return false;
	}

	//~--- inner classes --------------------------------------------------------

	private class RepoCache extends SimpleCache<String, Object> {

		/**
		 * Constructs ...
		 *
		 *
		 * @param maxsize
		 * @param cache_time
		 */
		public RepoCache(int maxsize, long cache_time) {
			super(maxsize, cache_time);
		}

		//~--- methods ------------------------------------------------------------

		@Override
		public Object remove(Object key) {
			if (cache_off) {
				return null;
			}

			Object val = super.remove(key);
			String strk = key.toString();
			Iterator<String> ks = keySet().iterator();

			while (ks.hasNext()) {
				String k = ks.next().toString();

				if (k.startsWith(strk)) {
					ks.remove();
				}    // end of if (k.startsWith(strk))
			}      // end of while (ks.hasNext())

			return val;
		}
	}
}


//~ Formatted in Sun Code Convention


//~ Formatted by Jindent --- http://www.jindent.com
