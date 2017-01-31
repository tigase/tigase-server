
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.logging.Level;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

/**
 * Created: Mar 27, 2010 6:43:02 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class UserRepositoryMDImpl implements UserRepository {
	private static final Logger log = Logger.getLogger(UserRepositoryMDImpl.class.getName());

	//~--- fields ---------------------------------------------------------------

	private UserRepository def = null;
	private ConcurrentSkipListMap<String, UserRepository> repos =
		new ConcurrentSkipListMap<String, UserRepository>();

	//~--- methods --------------------------------------------------------------

	@Override
	public void addDataList(BareJID user, String subnode, String key, String[] list)
			throws UserNotFoundException, TigaseDBException {
		UserRepository repo = getRepo(user.getDomain());

		if (repo != null) {
			repo.addDataList(user, subnode, key, list);
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
	 * @param domain
	 * @param repo
	 */
	public void addRepo(String domain, UserRepository repo) {
		repos.put(domain, repo);
	}

	@Override
	public void addUser(BareJID user) throws UserExistsException, TigaseDBException {
		UserRepository repo = getRepo(user.getDomain());

		if (repo != null) {
			repo.addUser(user);
		} else {
			log.log(Level.WARNING,
					"Couldn't obtain user repository for domain: " + user.getDomain()
						+ ", not even default one!");
		}
	}

	//~--- get methods ----------------------------------------------------------

	@Override
	public String getData(BareJID user, String subnode, String key, String def)
			throws UserNotFoundException, TigaseDBException {
		UserRepository repo = getRepo(user.getDomain());

		if (repo != null) {
			return repo.getData(user, subnode, key, def);
		} else {
			log.log(Level.WARNING,
					"Couldn't obtain user repository for domain: " + user.getDomain()
						+ ", not even default one!");
		}

		return null;
	}

	@Override
	public String getData(BareJID user, String subnode, String key)
			throws UserNotFoundException, TigaseDBException {
		UserRepository repo = getRepo(user.getDomain());

		if (repo != null) {
			return repo.getData(user, subnode, key);
		} else {
			log.log(Level.WARNING,
					"Couldn't obtain user repository for domain: " + user.getDomain()
						+ ", not even default one!");
		}

		return null;
	}

	@Override
	public String getData(BareJID user, String key)
			throws UserNotFoundException, TigaseDBException {
		UserRepository repo = getRepo(user.getDomain());

		if (repo != null) {
			return repo.getData(user, key);
		} else {
			log.log(Level.WARNING,
					"Couldn't obtain user repository for domain: " + user.getDomain()
						+ ", not even default one!");
		}

		return null;
	}

	@Override
	public String[] getDataList(BareJID user, String subnode, String key)
			throws UserNotFoundException, TigaseDBException {
		UserRepository repo = getRepo(user.getDomain());

		if (repo != null) {
			return repo.getDataList(user, subnode, key);
		} else {
			log.log(Level.WARNING,
					"Couldn't obtain user repository for domain: " + user.getDomain()
						+ ", not even default one!");
		}

		return null;
	}

	@Override
	public String[] getKeys(BareJID user, String subnode)
			throws UserNotFoundException, TigaseDBException {
		UserRepository repo = getRepo(user.getDomain());

		if (repo != null) {
			return repo.getKeys(user, subnode);
		} else {
			log.log(Level.WARNING,
					"Couldn't obtain user repository for domain: " + user.getDomain()
						+ ", not even default one!");
		}

		return null;
	}

	@Override
	public String[] getKeys(BareJID user) throws UserNotFoundException, TigaseDBException {
		UserRepository repo = getRepo(user.getDomain());

		if (repo != null) {
			return repo.getKeys(user);
		} else {
			log.log(Level.WARNING,
					"Couldn't obtain user repository for domain: " + user.getDomain()
						+ ", not even default one!");
		}

		return null;
	}

	/**
	 * Method description
	 *
	 *
	 * @param domain
	 *
	 * 
	 */
	public UserRepository getRepo(String domain) {
		if (domain == null ) {
			return def;
		}
		UserRepository result = repos.get(domain);

		if (result == null) {
			result = def;
		}

		return result;
	}

	@Override
	public String getResourceUri() {
		return def.getResourceUri();
	}

	@Override
	public String[] getSubnodes(BareJID user, String subnode)
			throws UserNotFoundException, TigaseDBException {
		UserRepository repo = getRepo(user.getDomain());

		if (repo != null) {
			return repo.getSubnodes(user, subnode);
		} else {
			log.log(Level.WARNING,
					"Couldn't obtain user repository for domain: " + user.getDomain()
						+ ", not even default one!");
		}

		return null;
	}

	@Override
	public String[] getSubnodes(BareJID user) throws UserNotFoundException, TigaseDBException {
		UserRepository repo = getRepo(user.getDomain());

		if (repo != null) {
			repo.getSubnodes(user);
		} else {
			log.log(Level.WARNING,
					"Couldn't obtain user repository for domain: " + user.getDomain()
						+ ", not even default one!");
		}

		return null;
	}

	@Override
	public long getUserUID(BareJID user) throws TigaseDBException {
		UserRepository repo = getRepo(user.getDomain());

		if (repo != null) {
			return repo.getUserUID(user);
		} else {
			log.log(Level.WARNING,
					"Couldn't obtain user repository for domain: " + user.getDomain()
						+ ", not even default one!");
		}

		return -1;
	}

	@Override
	public List<BareJID> getUsers() throws TigaseDBException {
		List<BareJID> result = new ArrayList<BareJID>();

		for (UserRepository repo : repos.values()) {
			result.addAll(repo.getUsers());
		}

		return result;
	}

	@Override
	public long getUsersCount() {
		long result = 0;

		for (UserRepository repo : repos.values()) {
			result += repo.getUsersCount();
		}

		return result;
	}

	@Override
	public long getUsersCount(String domain) {
		UserRepository repo = getRepo(domain);

		if (repo != null) {
			return repo.getUsersCount(domain);
		} else {
			log.log(Level.WARNING,
					"Couldn't obtain user repository for domain: " + domain + ", not even default one!");
		}

		return -1;
	}

	//~--- methods --------------------------------------------------------------

	@Override
	public void initRepository(String resource_uri, Map<String, String> params)
			throws DBInitException {
		log.info("Multi-domain repository pool initialized: " + resource_uri + ", params: "
				+ params);
	}

	@Override
	public void removeData(BareJID user, String subnode, String key)
			throws UserNotFoundException, TigaseDBException {
		UserRepository repo = getRepo(user.getDomain());

		if (repo != null) {
			repo.removeData(user, subnode, key);
		} else {
			log.log(Level.WARNING,
					"Couldn't obtain user repository for domain: " + user.getDomain()
						+ ", not even default one!");
		}
	}

	@Override
	public void removeData(BareJID user, String key)
			throws UserNotFoundException, TigaseDBException {
		UserRepository repo = getRepo(user.getDomain());

		if (repo != null) {
			repo.removeData(user, key);
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
	 * @param domain
	 *
	 * 
	 */
	public UserRepository removeRepo(String domain) {
		return repos.remove(domain);
	}

	@Override
	public void removeSubnode(BareJID user, String subnode)
			throws UserNotFoundException, TigaseDBException {
		UserRepository repo = getRepo(user.getDomain());

		if (repo != null) {
			repo.removeSubnode(user, subnode);
		} else {
			log.log(Level.WARNING,
					"Couldn't obtain user repository for domain: " + user.getDomain()
						+ ", not even default one!");
		}
	}

	@Override
	public void removeUser(BareJID user) throws UserNotFoundException, TigaseDBException {
		UserRepository repo = getRepo(user.getDomain());

		if (repo != null) {
			repo.removeUser(user);
		} else {
			log.log(Level.WARNING,
					"Couldn't obtain user repository for domain: " + user.getDomain()
						+ ", not even default one!");
		}
	}

	//~--- set methods ----------------------------------------------------------

	@Override
	public void setData(BareJID user, String subnode, String key, String value)
			throws UserNotFoundException, TigaseDBException {
		UserRepository repo = getRepo(user.getDomain());

		if (repo != null) {
			repo.setData(user, subnode, key, value);
		} else {
			log.log(Level.WARNING,
					"Couldn't obtain user repository for domain: " + user.getDomain()
						+ ", not even default one!");
		}
	}

	@Override
	public void setData(BareJID user, String key, String value)
			throws UserNotFoundException, TigaseDBException {
		UserRepository repo = getRepo(user.getDomain());

		if (repo != null) {
			repo.setData(user, key, value);
		} else {
			log.log(Level.WARNING,
					"Couldn't obtain user repository for domain: " + user.getDomain()
						+ ", not even default one!");
		}
	}

	@Override
	public void setDataList(BareJID user, String subnode, String key, String[] list)
			throws UserNotFoundException, TigaseDBException {
		UserRepository repo = getRepo(user.getDomain());

		if (repo != null) {
			repo.setDataList(user, subnode, key, list);
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
	 * @param repo
	 */
	public void setDefault(UserRepository repo) {
		def = repo;
	}

	//~--- methods --------------------------------------------------------------

	@Override
	public boolean userExists(BareJID user) {
		UserRepository repo = getRepo(user.getDomain());

		if (repo != null) {
			return repo.userExists(user);
		} else {
			log.log(Level.WARNING,
					"Couldn't obtain user repository for domain: " + user.getDomain()
						+ ", not even default one!");
		}

		return false;
	}
}
