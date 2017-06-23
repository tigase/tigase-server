
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

import tigase.db.beans.MDPoolBeanWithStatistics;
import tigase.db.beans.UserRepositoryMDPoolBean;
import tigase.eventbus.EventBus;
import tigase.kernel.beans.Inject;
import tigase.xmpp.BareJID;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

//~--- JDK imports ------------------------------------------------------------

//~--- classes ----------------------------------------------------------------

/**
 * Created: Mar 27, 2010 6:43:02 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public abstract class UserRepositoryMDImpl extends MDPoolBeanWithStatistics<UserRepository,UserRepositoryMDPoolBean.UserRepositoryConfigBean>
		implements UserRepository {
	private static final Logger log = Logger.getLogger(UserRepositoryMDImpl.class.getName());

	@Inject
	private EventBus eventBus;

	//~--- fields ---------------------------------------------------------------

	public UserRepositoryMDImpl() {
		super(UserRepository.class);
	}

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

	@Override
	public String getResourceUri() {
		return getDefaultRepository().getResourceUri();
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
		try {
			return repositoriesStream().sequential().flatMap(userRepository -> {
				try {
					return userRepository.getUsers().stream();
				} catch (TigaseDBException e) {
					throw new RuntimeException(e);
				}
			}).collect(Collectors.toList());
		} catch (RuntimeException ex) {
			Throwable cause = ex.getCause();
			if (cause instanceof TigaseDBException) {
				throw new TigaseDBException("Could not retrieve list of users", cause);
			}
			throw ex;
		}
	}

	@Override
	public long getUsersCount() {
		return repositoriesStream().mapToLong(UserRepository::getUsersCount).sum();
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
	@Deprecated
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

			eventBus.fire(new UserRemovedEvent(user));
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
