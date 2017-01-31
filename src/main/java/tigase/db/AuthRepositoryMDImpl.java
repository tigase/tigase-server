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
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import tigase.disteventbus.EventBus;
import tigase.disteventbus.EventBusFactory;
import tigase.xml.Element;

//~--- classes ----------------------------------------------------------------

/**
 * Created: Mar 27, 2010 9:10:21 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class AuthRepositoryMDImpl implements AuthRepository {
	private static final Logger log = Logger.getLogger(AuthRepositoryMDImpl.class.getName());

	//~--- fields ---------------------------------------------------------------

	private AuthRepository def = null;
	private EventBus eventBus = EventBusFactory.getInstance();
	private ConcurrentSkipListMap<String, AuthRepository> repos =
		new ConcurrentSkipListMap<String, AuthRepository>();

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param domain
	 * @param repo
	 */
	public void addRepo(String domain, AuthRepository repo) {
		repos.put(domain, repo);
	}

	@Override
	public void addUser(BareJID user, String password)
			throws UserExistsException, TigaseDBException {
		AuthRepository repo = getRepo(user.getDomain());

		if (repo != null) {
			repo.addUser(user, password);
		} else {
			log.log(Level.WARNING,
					"Couldn't obtain user repository for domain: " + user.getDomain()
						+ ", not even default one!");
		}
	}

	@Override
	@Deprecated
	public boolean digestAuth(BareJID user, String digest, String id, String alg)
			throws UserNotFoundException, TigaseDBException, AuthorizationException {
		AuthRepository repo = getRepo(user.getDomain());

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
	 * 
	 */
	public AuthRepository getRepo(String domain) {
		AuthRepository result = repos.get(domain);

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
	public long getUsersCount() {
		long result = 0;

		for (AuthRepository repo : repos.values()) {
			result += repo.getUsersCount();
		}

		return result;
	}

	@Override
	public long getUsersCount(String domain) {
		AuthRepository repo = getRepo(domain);

		if (repo != null) {
			return repo.getUsersCount(domain);
		} else {
			log.log(Level.WARNING,
					"Couldn't obtain user repository for domain: " + domain + ", not even default one!");
		}

		return -1;
	}

	@Override
	public void initRepository(String resource_uri, Map<String, String> params)
			throws DBInitException {
		log.info("Multi-domain repository pool initialized: " + resource_uri + ", params: "
				+ params);
	}

	@Override
	public void logout(BareJID user) throws UserNotFoundException, TigaseDBException {
		AuthRepository repo = getRepo(user.getDomain());

		if (repo != null) {
			repo.logout(user);
		} else {
			log.log(Level.WARNING,
					"Couldn't obtain user repository for domain: " + user.getDomain()
						+ ", not even default one!");
		}
	}

	@Override
	public boolean otherAuth(Map<String, Object> authProps)
			throws UserNotFoundException, TigaseDBException, AuthorizationException {
		AuthRepository repo = getRepo((String) authProps.get(SERVER_NAME_KEY));

		if (repo != null) {
			return repo.otherAuth(authProps);
		} else {
			log.log(Level.WARNING,
					"Couldn't obtain user repository for domain: "
						+ (String) authProps.get(SERVER_NAME_KEY) + ", not even default one!");
		}

		return false;
	}

	@Override
	@Deprecated
	public boolean plainAuth(BareJID user, String password)
			throws UserNotFoundException, TigaseDBException, AuthorizationException {
		AuthRepository repo = getRepo(user.getDomain());

		if (repo != null) {
			return repo.plainAuth(user, password);
		} else {
			log.log(Level.WARNING,
					"Couldn't obtain user repository for domain: " + user.getDomain()
						+ ", not even default one!");
		}

		return false;
	}

	@Override
	public void queryAuth(Map<String, Object> authProps) {
		AuthRepository repo = getRepo((String) authProps.get(SERVER_NAME_KEY));

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
	 * 
	 */
	public AuthRepository removeRepo(String domain) {
		return repos.remove(domain);
	}

	@Override
	public void removeUser(BareJID user) throws UserNotFoundException, TigaseDBException {
		AuthRepository repo = getRepo(user.getDomain());

		if (repo != null) {
			repo.removeUser(user);
			
			Element event = new Element("remove", new String[] { "xmlns" }, 
					new String[] { "tigase:user" });
			event.addChild(new Element("jid", user.toString()));
			eventBus.fire(event);
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
	public void setDefault(AuthRepository repo) {
		def = repo;
	}

	//~--- methods --------------------------------------------------------------

	@Override
	public void updatePassword(BareJID user, String password)
			throws UserNotFoundException, TigaseDBException {
		AuthRepository repo = getRepo(user.getDomain());

		if (repo != null) {
			repo.updatePassword(user, password);
		} else {
			log.log(Level.WARNING,
					"Couldn't obtain user repository for domain: " + user.getDomain()
						+ ", not even default one!");
		}
	}

	@Override
	public String getPassword(BareJID user) throws UserNotFoundException, TigaseDBException {
		AuthRepository repo = getRepo(user.getDomain());

		if (repo != null) {
			return repo.getPassword(user);
		} else {
			log.log(Level.WARNING, "Couldn't obtain user repository for domain: " + user.getDomain()
					+ ", not even default one!");
			return null;
		}
	}
	
	@Override
	public boolean isUserDisabled(BareJID user) throws UserNotFoundException, TigaseDBException {
		AuthRepository repo = getRepo(user.getDomain());

		if (repo != null) {
			return repo.isUserDisabled(user);
		} else {
			log.log(Level.WARNING, "Couldn't obtain user repository for domain: " + user.getDomain()
					+ ", not even default one!");
			return false;
		}		
	}
	
	@Override
	public void setUserDisabled(BareJID user, Boolean value) throws UserNotFoundException, TigaseDBException {
		AuthRepository repo = getRepo(user.getDomain());

		if (repo != null) {
			repo.setUserDisabled(user, value);
		} else {
			log.log(Level.WARNING, "Couldn't obtain user repository for domain: " + user.getDomain()
					+ ", not even default one!");
		}		
	}		
}
