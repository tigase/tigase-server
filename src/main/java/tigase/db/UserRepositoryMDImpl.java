/*
 * UserRepositoryMDImpl.java
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



package tigase.db;

//~--- non-JDK imports --------------------------------------------------------

import tigase.xmpp.BareJID;

//~--- JDK imports ------------------------------------------------------------

import java.util.ArrayList;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Map;

/**
 * Created: Mar 27, 2010 6:43:02 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class UserRepositoryMDImpl
				implements UserRepository {
	private static final Logger log = Logger.getLogger(UserRepositoryMDImpl.class
			.getName());

	//~--- fields ---------------------------------------------------------------

	private UserRepository                                def = null;
	private ConcurrentSkipListMap<String, UserRepository> repos =
			new ConcurrentSkipListMap<String, UserRepository>();

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param user
	 * @param subnode
	 * @param key
	 * @param list
	 *
	 * @throws TigaseDBException
	 * @throws UserNotFoundException
	 */
	@Override
	public void addDataList(BareJID user, String subnode, String key, String[] list)
					throws UserNotFoundException, TigaseDBException {
		UserRepository repo = getRepo(user.getDomain());

		if (repo != null) {
			repo.addDataList(user, subnode, key, list);
		} else {
			log.log(Level.WARNING, "Couldn't obtain user repository for domain: " + user
					.getDomain() + ", not even default one!");
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

	/**
	 * Method description
	 *
	 *
	 * @param user
	 *
	 * @throws TigaseDBException
	 * @throws UserExistsException
	 */
	@Override
	public void addUser(BareJID user) throws UserExistsException, TigaseDBException {
		UserRepository repo = getRepo(user.getDomain());

		if (repo != null) {
			repo.addUser(user);
		} else {
			log.log(Level.WARNING, "Couldn't obtain user repository for domain: " + user
					.getDomain() + ", not even default one!");
		}
	}

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
					throws DBInitException {
		log.info("Multi-domain repository pool initialized: " + resource_uri + ", params: " +
				params);
	}

	/**
	 * Method description
	 *
	 *
	 * @param user
	 * @param subnode
	 * @param key
	 *
	 * @throws TigaseDBException
	 * @throws UserNotFoundException
	 */
	@Override
	public void removeData(BareJID user, String subnode, String key)
					throws UserNotFoundException, TigaseDBException {
		UserRepository repo = getRepo(user.getDomain());

		if (repo != null) {
			repo.removeData(user, subnode, key);
		} else {
			log.log(Level.WARNING, "Couldn't obtain user repository for domain: " + user
					.getDomain() + ", not even default one!");
		}
	}

	/**
	 * Method description
	 *
	 *
	 * @param user
	 * @param key
	 *
	 * @throws TigaseDBException
	 * @throws UserNotFoundException
	 */
	@Override
	public void removeData(BareJID user, String key)
					throws UserNotFoundException, TigaseDBException {
		UserRepository repo = getRepo(user.getDomain());

		if (repo != null) {
			repo.removeData(user, key);
		} else {
			log.log(Level.WARNING, "Couldn't obtain user repository for domain: " + user
					.getDomain() + ", not even default one!");
		}
	}

	/**
	 * Method description
	 *
	 *
	 * @param domain
	 *
	 *
	 *
	 * @return a value of <code>UserRepository</code>
	 */
	public UserRepository removeRepo(String domain) {
		return repos.remove(domain);
	}

	/**
	 * Method description
	 *
	 *
	 * @param user
	 * @param subnode
	 *
	 * @throws TigaseDBException
	 * @throws UserNotFoundException
	 */
	@Override
	public void removeSubnode(BareJID user, String subnode)
					throws UserNotFoundException, TigaseDBException {
		UserRepository repo = getRepo(user.getDomain());

		if (repo != null) {
			repo.removeSubnode(user, subnode);
		} else {
			log.log(Level.WARNING, "Couldn't obtain user repository for domain: " + user
					.getDomain() + ", not even default one!");
		}
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
		UserRepository repo = getRepo(user.getDomain());

		if (repo != null) {
			repo.removeUser(user);
		} else {
			log.log(Level.WARNING, "Couldn't obtain user repository for domain: " + user
					.getDomain() + ", not even default one!");
		}
	}

	/**
	 * Method description
	 *
	 *
	 * @param user
	 *
	 *
	 *
	 * @return a value of <code>boolean</code>
	 */
	@Override
	public boolean userExists(BareJID user) {
		UserRepository repo = getRepo(user.getDomain());

		if (repo != null) {
			return repo.userExists(user);
		} else {
			log.log(Level.WARNING, "Couldn't obtain user repository for domain: " + user
					.getDomain() + ", not even default one!");
		}

		return false;
	}

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param user
	 * @param subnode
	 * @param key
	 * @param def
	 *
	 *
	 *
	 *
	 * @return a value of <code>String</code>
	 * @throws TigaseDBException
	 * @throws UserNotFoundException
	 */
	@Override
	public String getData(BareJID user, String subnode, String key, String def)
					throws UserNotFoundException, TigaseDBException {
		UserRepository repo = getRepo(user.getDomain());

		if (repo != null) {
			return repo.getData(user, subnode, key, def);
		} else {
			log.log(Level.WARNING, "Couldn't obtain user repository for domain: " + user
					.getDomain() + ", not even default one!");
		}

		return null;
	}

	/**
	 * Method description
	 *
	 *
	 * @param user
	 * @param subnode
	 * @param key
	 *
	 *
	 *
	 *
	 * @return a value of <code>String</code>
	 * @throws TigaseDBException
	 * @throws UserNotFoundException
	 */
	@Override
	public String getData(BareJID user, String subnode, String key)
					throws UserNotFoundException, TigaseDBException {
		UserRepository repo = getRepo(user.getDomain());

		if (repo != null) {
			return repo.getData(user, subnode, key);
		} else {
			log.log(Level.WARNING, "Couldn't obtain user repository for domain: " + user
					.getDomain() + ", not even default one!");
		}

		return null;
	}

	/**
	 * Method description
	 *
	 *
	 * @param user
	 * @param key
	 *
	 *
	 *
	 *
	 * @return a value of <code>String</code>
	 * @throws TigaseDBException
	 * @throws UserNotFoundException
	 */
	@Override
	public String getData(BareJID user, String key)
					throws UserNotFoundException, TigaseDBException {
		UserRepository repo = getRepo(user.getDomain());

		if (repo != null) {
			return repo.getData(user, key);
		} else {
			log.log(Level.WARNING, "Couldn't obtain user repository for domain: " + user
					.getDomain() + ", not even default one!");
		}

		return null;
	}

	/**
	 * Method description
	 *
	 *
	 * @param user
	 * @param subnode
	 * @param key
	 *
	 *
	 *
	 *
	 * @return a value of <code>String[]</code>
	 * @throws TigaseDBException
	 * @throws UserNotFoundException
	 */
	@Override
	public String[] getDataList(BareJID user, String subnode, String key)
					throws UserNotFoundException, TigaseDBException {
		UserRepository repo = getRepo(user.getDomain());

		if (repo != null) {
			return repo.getDataList(user, subnode, key);
		} else {
			log.log(Level.WARNING, "Couldn't obtain user repository for domain: " + user
					.getDomain() + ", not even default one!");
		}

		return null;
	}

	/**
	 * Method description
	 *
	 *
	 * @param user
	 * @param subnode
	 *
	 *
	 *
	 *
	 * @return a value of <code>String[]</code>
	 * @throws TigaseDBException
	 * @throws UserNotFoundException
	 */
	@Override
	public String[] getKeys(BareJID user, String subnode)
					throws UserNotFoundException, TigaseDBException {
		UserRepository repo = getRepo(user.getDomain());

		if (repo != null) {
			return repo.getKeys(user, subnode);
		} else {
			log.log(Level.WARNING, "Couldn't obtain user repository for domain: " + user
					.getDomain() + ", not even default one!");
		}

		return null;
	}

	/**
	 * Method description
	 *
	 *
	 * @param user
	 *
	 *
	 *
	 *
	 * @return a value of <code>String[]</code>
	 * @throws TigaseDBException
	 * @throws UserNotFoundException
	 */
	@Override
	public String[] getKeys(BareJID user) throws UserNotFoundException, TigaseDBException {
		UserRepository repo = getRepo(user.getDomain());

		if (repo != null) {
			return repo.getKeys(user);
		} else {
			log.log(Level.WARNING, "Couldn't obtain user repository for domain: " + user
					.getDomain() + ", not even default one!");
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
	 *
	 * @return a value of <code>UserRepository</code>
	 */
	public UserRepository getRepo(String domain) {
		UserRepository result = repos.get(domain);

		if (result == null) {
			result = def;
		}

		return result;
	}

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of <code>String</code>
	 */
	@Override
	public String getResourceUri() {
		return def.getResourceUri();
	}

	/**
	 * Method description
	 *
	 *
	 * @param user
	 * @param subnode
	 *
	 *
	 *
	 *
	 * @return a value of <code>String[]</code>
	 * @throws TigaseDBException
	 * @throws UserNotFoundException
	 */
	@Override
	public String[] getSubnodes(BareJID user, String subnode)
					throws UserNotFoundException, TigaseDBException {
		UserRepository repo = getRepo(user.getDomain());

		if (repo != null) {
			return repo.getSubnodes(user, subnode);
		} else {
			log.log(Level.WARNING, "Couldn't obtain user repository for domain: " + user
					.getDomain() + ", not even default one!");
		}

		return null;
	}

	/**
	 * Method description
	 *
	 *
	 * @param user
	 *
	 *
	 *
	 *
	 * @return a value of <code>String[]</code>
	 * @throws TigaseDBException
	 * @throws UserNotFoundException
	 */
	@Override
	public String[] getSubnodes(BareJID user)
					throws UserNotFoundException, TigaseDBException {
		UserRepository repo = getRepo(user.getDomain());

		if (repo != null) {
			repo.getSubnodes(user);
		} else {
			log.log(Level.WARNING, "Couldn't obtain user repository for domain: " + user
					.getDomain() + ", not even default one!");
		}

		return null;
	}

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 *
	 * @return a value of <code>List<BareJID></code>
	 * @throws TigaseDBException
	 */
	@Override
	public List<BareJID> getUsers() throws TigaseDBException {
		List<BareJID> result = new ArrayList<BareJID>();

		for (UserRepository repo : repos.values()) {
			result.addAll(repo.getUsers());
		}

		return result;
	}

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of <code>long</code>
	 */
	@Override
	public long getUsersCount() {
		long result = 0;

		for (UserRepository repo : repos.values()) {
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
	 *
	 *
	 * @return a value of <code>long</code>
	 */
	@Override
	public long getUsersCount(String domain) {
		UserRepository repo = getRepo(domain);

		if (repo != null) {
			return repo.getUsersCount(domain);
		} else {
			log.log(Level.WARNING, "Couldn't obtain user repository for domain: " + domain +
					", not even default one!");
		}

		return -1;
	}

	/**
	 * Method description
	 *
	 *
	 * @param user
	 *
	 *
	 *
	 *
	 * @return a value of <code>long</code>
	 * @throws TigaseDBException
	 */
	@Override
	public long getUserUID(BareJID user) throws TigaseDBException {
		UserRepository repo = getRepo(user.getDomain());

		if (repo != null) {
			return repo.getUserUID(user);
		} else {
			log.log(Level.WARNING, "Couldn't obtain user repository for domain: " + user
					.getDomain() + ", not even default one!");
		}

		return -1;
	}

	//~--- set methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param user
	 * @param subnode
	 * @param key
	 * @param value
	 *
	 * @throws TigaseDBException
	 * @throws UserNotFoundException
	 */
	@Override
	public void setData(BareJID user, String subnode, String key, String value)
					throws UserNotFoundException, TigaseDBException {
		UserRepository repo = getRepo(user.getDomain());

		if (repo != null) {
			repo.setData(user, subnode, key, value);
		} else {
			log.log(Level.WARNING, "Couldn't obtain user repository for domain: " + user
					.getDomain() + ", not even default one!");
		}
	}

	/**
	 * Method description
	 *
	 *
	 * @param user
	 * @param key
	 * @param value
	 *
	 * @throws TigaseDBException
	 * @throws UserNotFoundException
	 */
	@Override
	public void setData(BareJID user, String key, String value)
					throws UserNotFoundException, TigaseDBException {
		UserRepository repo = getRepo(user.getDomain());

		if (repo != null) {
			repo.setData(user, key, value);
		} else {
			log.log(Level.WARNING, "Couldn't obtain user repository for domain: " + user
					.getDomain() + ", not even default one!");
		}
	}

	/**
	 * Method description
	 *
	 *
	 * @param user
	 * @param subnode
	 * @param key
	 * @param list
	 *
	 * @throws TigaseDBException
	 * @throws UserNotFoundException
	 */
	@Override
	public void setDataList(BareJID user, String subnode, String key, String[] list)
					throws UserNotFoundException, TigaseDBException {
		UserRepository repo = getRepo(user.getDomain());

		if (repo != null) {
			repo.setDataList(user, subnode, key, list);
		} else {
			log.log(Level.WARNING, "Couldn't obtain user repository for domain: " + user
					.getDomain() + ", not even default one!");
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
}


//~ Formatted in Tigase Code Convention on 13/08/29
