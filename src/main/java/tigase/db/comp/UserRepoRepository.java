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

package tigase.db.comp;

//~--- non-JDK imports --------------------------------------------------------

import tigase.db.RepositoryFactory;
import tigase.db.UserExistsException;
import tigase.db.UserRepository;

import tigase.xml.DomBuilderHandler;
import tigase.xml.Element;
import tigase.xml.SimpleParser;
import tigase.xml.SingletonFactory;

import tigase.xmpp.BareJID;

import static tigase.conf.Configurable.*;

//~--- JDK imports ------------------------------------------------------------

import java.util.Map;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

/**
 * Created: Oct 3, 2009 3:55:27 PM
 *
 * @param <Item>
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public abstract class UserRepoRepository<Item extends RepositoryItem>
		extends ConfigRepository<Item> {
	private static final Logger log = Logger.getLogger(UserRepoRepository.class.getName());

	/** Field description */
	public static final String REPO_CLASS_PROP_KEY = "repo-class";

	/** Field description */
	public static final String REPO_URI_PROP_KEY = "repo-uri";

	//~--- fields ---------------------------------------------------------------

	private String items_list_pkey = "items-lists";
	private UserRepository repo = null;

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	public abstract BareJID getRepoUser();

	/**
	 * Method description
	 *
	 *
	 * @param defs
	 * @param params
	 */
	@Override
	public void getDefaults(Map<String, Object> defs, Map<String, Object> params) {

		// Something to initialize database with, in case it is empty
		// Otherwise the server would not work at all with empty Items database
		super.getDefaults(defs, params);

		// Now the real items data storage:
		String repo_class = DERBY_REPO_CLASS_PROP_VAL;
		String repo_uri = DERBY_REPO_URL_PROP_VAL;

		if (params.get(GEN_USER_DB) != null) {
			repo_class = (String) params.get(GEN_USER_DB);
		}

		if (params.get(GEN_USER_DB_URI) != null) {
			repo_uri = (String) params.get(GEN_USER_DB_URI);
		}

		defs.put(REPO_CLASS_PROP_KEY, repo_class);
		defs.put(REPO_URI_PROP_KEY, repo_uri);
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	public String getItemsListPKey() {
		return items_list_pkey;
	}

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 */
	@Override
	public void reload() {
		super.reload();

		try {

			// It is now time to load all Items' settings from the database:
			String items_list = repo.getData(getRepoUser(), getItemsListPKey());

			if ((items_list != null) &&!items_list.isEmpty()) {
				DomBuilderHandler domHandler = new DomBuilderHandler();
				SimpleParser parser = SingletonFactory.getParserInstance();

				parser.parse(domHandler, items_list.toCharArray(), 0, items_list.length());

				Queue<Element> elems = domHandler.getParsedElements();

				if ((elems != null) && (elems.size() > 0)) {
					for (Element elem : elems) {
						Item item = getItemInstance();

						item.initFromElement(elem);
						items.put(item.getKey(), item);
					}
				}
			}
		} catch (Exception ex) {
			log.log(Level.SEVERE, "Problem with loading items list from the database.", ex);
		}

		log.config("All loaded items: " + items.toString());
	}

	//~--- set methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param properties
	 */
	@Override
	public void setProperties(Map<String, Object> properties) {

		// Let's load items from configuration first. Later we can overwrite
		// them with items settings in the database.
		super.setProperties(properties);
		repo = (UserRepository) properties.get(SHARED_USER_REPO_PROP_KEY);

		if (repo != null) {
			log.config("Using shared repository instance.");
		} else {
			String repo_class = (String) properties.get(REPO_CLASS_PROP_KEY);
			String repo_uri = (String) properties.get(REPO_URI_PROP_KEY);

			try {
				repo = RepositoryFactory.getUserRepository(getRepoUser().getLocalpart(), repo_class,
						repo_uri, null);
			} catch (Exception e) {
				log.log(Level.SEVERE, "Can't initialize Items repository", e);
				repo = null;
			}
		}

		if (repo != null) {

			// If this is the first run of the Items manager the database might not
			// be properly initialized yet....
			try {
				repo.addUser(getRepoUser());
			} catch (UserExistsException e) {

				// This is expected when the Items repository has been already running on
				// this databaseso and can be ignored.
			} catch (Exception e) {

				// This is not expected so let's signal an error:
				log.log(Level.SEVERE,
						"Problem with adding '" + getRepoUser() + "' user to the database", e);
			}

			reload();
		}
	}

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 */
	@Override
	public void store() {
		super.store();

		StringBuilder sb = new StringBuilder();

		for (Item item : items.values()) {
			sb.append(item.toElement().toString());
		}

		try {
			repo.setData(getRepoUser(), getItemsListPKey(), sb.toString());
		} catch (Exception e) {
			log.log(Level.SEVERE, "Error storing items list in the repository", e);
		}
	}
}


//~ Formatted in Sun Code Convention


//~ Formatted by Jindent --- http://www.jindent.com
