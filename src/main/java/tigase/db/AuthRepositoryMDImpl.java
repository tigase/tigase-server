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
import tigase.db.beans.AuthRepositoryMDPoolBean;
import tigase.db.beans.MDPoolBeanWithStatistics;
import tigase.eventbus.EventBus;
import tigase.eventbus.EventBusFactory;
import tigase.xmpp.jid.BareJID;

import java.util.Collection;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementation of multi domain pool for authentication repositories. Created: Mar 27, 2010 9:10:21 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public abstract class AuthRepositoryMDImpl
		extends MDPoolBeanWithStatistics<AuthRepository, AuthRepositoryMDPoolBean.AuthRepositoryConfigBean>
		implements AuthRepository {

	private static final Logger log = Logger.getLogger(AuthRepositoryMDImpl.class.getName());

	private EventBus eventBus = EventBusFactory.getInstance();

	public AuthRepositoryMDImpl() {
		super(AuthRepository.class);
	}

	@Override
	public void addUser(BareJID user, String password) throws TigaseDBException {
		AuthRepository repo = getRepo(user.getDomain());

		if (repo != null) {
			repo.addUser(user, password);
		} else {
			log.log(Level.WARNING,
					"Couldn't obtain user repository for domain: " + user.getDomain() + ", not even default one!");
		}
	}

	@Override
	public AccountStatus getAccountStatus(BareJID user) throws TigaseDBException {
		AuthRepository repo = getRepo(user.getDomain());

		if (repo != null) {
			return repo.getAccountStatus(user);
		} else {
			log.log(Level.WARNING,
					"Couldn't obtain user repository for domain: " + user.getDomain() + ", not even default one!");
			return null;
		}
	}

	@Override
	public Credentials getCredentials(BareJID user, String username) throws TigaseDBException {
		AuthRepository repo = getRepo(user.getDomain());

		if (repo != null) {
			return repo.getCredentials(user, username);
		} else {
			log.log(Level.WARNING,
					"Couldn't obtain user repository for domain: " + user.getDomain() + ", not even default one!");
			return null;
		}
	}

	@Override
	public String getPassword(BareJID user) throws TigaseDBException {
		AuthRepository repo = getRepo(user.getDomain());

		if (repo != null) {
			return repo.getPassword(user);
		} else {
			log.log(Level.WARNING,
					"Couldn't obtain user repository for domain: " + user.getDomain() + ", not even default one!");
			return null;
		}
	}

	@Override
	public String getResourceUri() {
		return getDefaultRepository().getResourceUri();
	}

	@Override
	public Collection<String> getUsernames(BareJID user) throws TigaseDBException {
		AuthRepository repo = getRepo(user.getDomain());

		if (repo != null) {
			return repo.getUsernames(user);
		} else {
			log.log(Level.WARNING,
					"Couldn't obtain user repository for domain: " + user.getDomain() + ", not even default one!");
			return null;
		}
	}

	@Override
	public long getUsersCount() {
		return repositoriesStream().mapToLong(AuthRepository::getUsersCount).sum();
	}

	@Override
	public long getUsersCount(String domain) {
		AuthRepository repo = getRepo(domain);

		if (repo != null) {
			return repo.getUsersCount(domain);
		} else {
			log.log(Level.WARNING, "Couldn't obtain user repository for domain: " + domain + ", not even default one!");
		}

		return -1;
	}

	@Override
	public void initRepository(String resource_uri, Map<String, String> params) throws DBInitException {
		log.info("Multi-domain repository pool initialized: " + resource_uri + ", params: " + params);
	}

	@Override
	public boolean isMechanismSupported(String domain, String mechanism) {
		AuthRepository repo = getRepo(domain);
		if (repo != null) {
			return repo.isMechanismSupported(domain, mechanism);
		} else {
			log.log(Level.WARNING, "Couldn't obtain user repository for domain: " + domain + ", not even default one!");
			return false;
		}
	}

	@Override
	public boolean isUserDisabled(BareJID user) throws TigaseDBException {
		AuthRepository repo = getRepo(user.getDomain());

		if (repo != null) {
			return repo.isUserDisabled(user);
		} else {
			log.log(Level.WARNING,
					"Couldn't obtain user repository for domain: " + user.getDomain() + ", not even default one!");
			return false;
		}
	}

	public void loggedIn(BareJID user) throws TigaseDBException {
		AuthRepository repo = getRepo(user.getDomain());

		if (repo != null) {
			repo.loggedIn(user);
		} else {
			log.log(Level.WARNING,
					"Couldn't obtain user repository for domain: " + user.getDomain() + ", not even default one!");
		}
	}

	@Override
	public void logout(BareJID user) throws TigaseDBException {
		AuthRepository repo = getRepo(user.getDomain());

		if (repo != null) {
			repo.logout(user);
		} else {
			log.log(Level.WARNING,
					"Couldn't obtain user repository for domain: " + user.getDomain() + ", not even default one!");
		}
	}

	@Override
	public boolean otherAuth(Map<String, Object> authProps) throws TigaseDBException, AuthorizationException {
		AuthRepository repo = getRepo((String) authProps.get(SERVER_NAME_KEY));

		if (repo != null) {
			return repo.otherAuth(authProps);
		} else {
			log.log(Level.WARNING, "Couldn't obtain user repository for domain: " + authProps.get(SERVER_NAME_KEY) +
					", not even default one!");
		}

		return false;
	}

	// ~--- methods
	// --------------------------------------------------------------

	@Override
	public void queryAuth(Map<String, Object> authProps) {
		AuthRepository repo = getRepo((String) authProps.get(SERVER_NAME_KEY));

		if (repo != null) {
			repo.queryAuth(authProps);
		} else {
			log.log(Level.WARNING, "Couldn't obtain user repository for domain: " + authProps.get(SERVER_NAME_KEY) +
					", not even default one!");
		}
	}

	@Override
	public void removeCredential(BareJID user, String username) throws TigaseDBException {
		AuthRepository repo = getRepo(user.getDomain());

		if (repo != null) {
			repo.removeCredential(user, username);
		} else {
			log.log(Level.WARNING,
					"Couldn't obtain user repository for domain: " + user.getDomain() + ", not even default one!");
		}
	}

	@Override
	public void removeUser(BareJID user) throws TigaseDBException {
		AuthRepository repo = getRepo(user.getDomain());

		if (repo != null) {
			repo.removeUser(user);

			eventBus.fire(new UserRepository.UserRemovedEvent(user));
		} else {
			log.log(Level.WARNING,
					"Couldn't obtain user repository for domain: " + user.getDomain() + ", not even default one!");
		}
	}

	@Override
	public void setAccountStatus(BareJID user, AccountStatus status) throws TigaseDBException {
		AuthRepository repo = getRepo(user.getDomain());

		if (repo != null) {
			repo.setAccountStatus(user, status);
		} else {
			log.log(Level.WARNING,
					"Couldn't obtain user repository for domain: " + user.getDomain() + ", not even default one!");
		}
	}

	@Override
	public void setUserDisabled(BareJID user, Boolean value) throws TigaseDBException {
		AuthRepository repo = getRepo(user.getDomain());

		if (repo != null) {
			repo.setUserDisabled(user, value);
		} else {
			log.log(Level.WARNING,
					"Couldn't obtain user repository for domain: " + user.getDomain() + ", not even default one!");
		}
	}

	@Override
	public void updateCredential(BareJID user, String username, String password) throws TigaseDBException {
		AuthRepository repo = getRepo(user.getDomain());

		if (repo != null) {
			repo.updateCredential(user, username, password);
		} else {
			log.log(Level.WARNING,
					"Couldn't obtain user repository for domain: " + user.getDomain() + ", not even default one!");
		}
	}

	@Override
	public void updatePassword(BareJID user, String password) throws TigaseDBException {
		AuthRepository repo = getRepo(user.getDomain());

		if (repo != null) {
			repo.updatePassword(user, password);
		} else {
			log.log(Level.WARNING,
					"Couldn't obtain user repository for domain: " + user.getDomain() + ", not even default one!");
		}
	}
}
