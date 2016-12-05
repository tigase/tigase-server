/*
 * UserRepoRepository.java
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



package tigase.db.comp;

//~--- non-JDK imports --------------------------------------------------------

import tigase.db.RepositoryFactory;
import tigase.db.TigaseDBException;
import tigase.db.UserExistsException;
import tigase.db.UserRepository;

import tigase.xml.DomBuilderHandler;
import tigase.xml.Element;
import tigase.xml.SimpleParser;
import tigase.xml.SingletonFactory;

import tigase.xmpp.BareJID;

//~--- JDK imports ------------------------------------------------------------

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Map;
import java.util.Queue;

/**
 * Created: Oct 3, 2009 3:55:27 PM
 *
 * @param <Item>
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public abstract class UserRepoRepository<Item extends RepositoryItem>
				extends ConfigRepository<Item> {
	/** Field description */
	public static final String REPO_CLASS_PROP_KEY = "repo-class";

	/** Field description */
	public static final String REPO_URI_PROP_KEY = "repo-uri";
	private static final Logger log              =
		Logger.getLogger(UserRepoRepository.class.getName());

	//~--- fields ---------------------------------------------------------------

	private String items_list_pkey = "items-lists";
	private UserRepository repo    = null;

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 *
	 */
	public abstract BareJID getRepoUser();

	@Override
	public void getDefaults(Map<String, Object> defs, Map<String, Object> params) {

		// Something to initialize database with, in case it is empty
		// Otherwise the server would not work at all with empty Items database
		super.getDefaults(defs, params);

		// Do not create defaults for this, let it to use the shared repo pool as
		// a default one. Only if the user sets settings manually it means he wants
		// a different from the shared default.
//  // Now the real items data storage:
//  String repo_class = DERBY_REPO_CLASS_PROP_VAL;
//  String repo_uri = DERBY_REPO_URL_PROP_VAL;
//
//  if (params.get(GEN_USER_DB) != null) {
//    repo_class = (String) params.get(GEN_USER_DB);
//  }
//
//  if (params.get(GEN_USER_DB_URI) != null) {
//    repo_uri = (String) params.get(GEN_USER_DB_URI);
//  }
//  defs.put(REPO_CLASS_PROP_KEY, repo_class);
//  defs.put(REPO_URI_PROP_KEY, repo_uri);
	}

	/**
	 * Method description
	 *
	 *
	 *
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
		int hashCode = 0;
		super.reload();
		try {

			// It is now time to load all Items' settings from the database:
			if (repo == null) {
				log.log(Level.SEVERE, "Repository is not initialised - skipping reload");
				return;
			}
			String items_list = repo.getData(getRepoUser(), getItemsListPKey());

			if ( items_list != null ){
				hashCode = items_list.hashCode();

				if ( !items_list.isEmpty() && hashCode != itemsHash ){
					DomBuilderHandler domHandler = new DomBuilderHandler();
					SimpleParser parser = SingletonFactory.getParserInstance();

					parser.parse( domHandler, items_list.toCharArray(), 0, items_list.length() );

					Queue<Element> elems = domHandler.getParsedElements();

					if ( ( elems != null ) && ( elems.size() > 0 ) ){
						for ( Element elem : elems ) {
							Item item = getItemInstance();

							item.initFromElement( elem );
							addItemNoStore( item );
						}
					}
					log.log( Level.CONFIG, "All loaded items: {0}", items );
					itemsHash = hashCode;
				}
			}
		} catch (TigaseDBException ex) {
			log.log(Level.SEVERE, "Problem with loading items list from the database.", ex);
		}
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

		String repo_class = (String) properties.get(REPO_CLASS_PROP_KEY);
		String repo_uri   = (String) properties.get(REPO_URI_PROP_KEY);

		if (repo_uri != null) {
			log.log(Level.INFO,
							"Initializing custom component repository: {0}, db connection: {1}",
							new Object[] { repo_class,
														 repo_uri });
			try {
				repo = RepositoryFactory.getUserRepository(repo_class, repo_uri, null);
			} catch (Exception e) {
				log.log(Level.SEVERE, "Can't initialize Items repository", e);
				repo = null;
			}
		}
		if (repo == null) {
			repo = (UserRepository) properties.get(RepositoryFactory.SHARED_USER_REPO_PROP_KEY);
			log.config("Using shared repository instance.");
		}
		if (repo != null) {

			// If this is the first run of the Items manager the database might not
			// be properly initialized yet....
			try {
				if (!repo.userExists(getRepoUser())) {
					repo.addUser(getRepoUser());
				}
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
		if (repo != null) {
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
}


//~ Formatted in Tigase Code Convention on 13/03/09
