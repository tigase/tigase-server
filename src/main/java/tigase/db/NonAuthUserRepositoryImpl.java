
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

import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.logging.Level;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

/**
 * Created: May 3, 2010 1:23:45 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class NonAuthUserRepositoryImpl implements NonAuthUserRepository {

	/**
	 * Variable <code>log</code> is a class logger.
	 */
	private static final Logger log =
		Logger.getLogger(NonAuthUserRepositoryImpl.class.getName());

	//~--- fields ---------------------------------------------------------------

	private BareJID defDomain = null;
	private final Set<BareJID> existing_domains = new ConcurrentSkipListSet<BareJID>();
	private boolean autoCreateOffline = false;
	private final UserRepository rep;

	//~--- constructors ---------------------------------------------------------

	/**
	 * Constructs ...
	 *
	 *
	 * @param userRep
	 * @param defDomain
	 * @param autoCreateOffline
	 */
	public NonAuthUserRepositoryImpl(UserRepository userRep, BareJID defDomain,
			boolean autoCreateOffline) {
		rep = userRep;
		this.defDomain = defDomain;
		this.autoCreateOffline = autoCreateOffline;
	}

	//~--- methods --------------------------------------------------------------

	@Override
	public void addOfflineData(BareJID user, String subnode, String key, String value)
			throws UserNotFoundException, DataOverwriteException {
		String node = calcNode(OFFLINE_DATA_NODE, subnode);

		try {
			String data = rep.getData(user, node, key);

			if (data == null) {
				rep.setData(user, node, key, value);
			} else {
				throw new DataOverwriteException("Not authorized attempt to overwrite data.");
			}    // end of if (data == null) else
		} catch (TigaseDBException e) {
			log.log(Level.SEVERE, "Problem accessing repository data.", e);
		}      // end of try-catch
	}

	@Override
	public void addOfflineDataList(BareJID user, String subnode, String key, String[] list)
			throws UserNotFoundException {
		try {
			if (autoCreateOffline || rep.userExists(user)) {
				rep.addDataList(user, calcNode(OFFLINE_DATA_NODE, subnode), key, list);
			} else {
				throw new UserNotFoundException("User: " + user
						+ " has not been found inthe repository.");
			}
		} catch (UserNotFoundException e) {

			// This is quite normal for anonymous users.
			log.log(Level.INFO, "User not found in repository: {0}", user);
		} catch (TigaseDBException e) {
			log.log(Level.SEVERE, "Problem accessing repository data.", e);
		}    // end of try-catch
	}

	//~--- get methods ----------------------------------------------------------

	@Override
	public String getDomainTempData(BareJID domain, String subnode, String key, String def)
			throws TigaseDBException {
		checkDomain(domain);

		return rep.getData(domain, subnode, key, def);
	}

	@Override
	public String getPublicData(BareJID user, String subnode, String key, String def)
			throws UserNotFoundException {
		try {
			return (rep.userExists(user)
					? rep.getData(user, calcNode(PUBLIC_DATA_NODE, subnode), key, def) : null);
		} catch (TigaseDBException e) {
			log.log(Level.SEVERE, "Problem accessing repository data.", e);

			return null;
		}    // end of try-catch
	}

	@Override
	public String[] getPublicDataList(BareJID user, String subnode, String key)
			throws UserNotFoundException {
		try {
			return (rep.userExists(user)
					? rep.getDataList(user, calcNode(PUBLIC_DATA_NODE, subnode), key) : null);
		} catch (TigaseDBException e) {
			log.log(Level.SEVERE, "Problem accessing repository data.", e);

			return null;
		}    // end of try-catch
	}

	@Override
	public String getTempData(String subnode, String key, String def) throws TigaseDBException {
		checkDomain(defDomain);

		return rep.getData(defDomain, subnode, key, def);
	}

	//~--- methods --------------------------------------------------------------

	@Override
	public void putDomainTempData(BareJID domain, String subnode, String key, String value)
			throws TigaseDBException {
		checkDomain(domain);
		rep.setData(domain, subnode, key, value);
	}

	@Override
	public void putTempData(String subnode, String key, String value) throws TigaseDBException {
		checkDomain(defDomain);
		rep.setData(defDomain, subnode, key, value);
	}

	@Override
	public void removeDomainTempData(BareJID domain, String subnode, String key)
			throws TigaseDBException {
		checkDomain(defDomain);
		rep.removeData(domain, subnode, key);
	}

	@Override
	public void removeTempData(String subnode, String key) throws TigaseDBException {
		checkDomain(defDomain);
		rep.removeData(defDomain, subnode, key);
	}

	private String calcNode(String base, String subnode) {
		if (subnode == null) {
			return base;
		}    // end of if (subnode == null)

		return base + "/" + subnode;
	}

	private void checkDomain(BareJID domain) throws TigaseDBException {
		if ( !existing_domains.contains(domain) &&!rep.userExists(domain)) {
			rep.addUser(domain);
			existing_domains.add(domain);
		}
	}
}
