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

import tigase.db.TigaseDBException;
import tigase.db.UserExistsException;
import tigase.db.UserRepository;
import tigase.kernel.beans.Inject;
import tigase.xml.DomBuilderHandler;
import tigase.xml.Element;
import tigase.xml.SimpleParser;
import tigase.xml.SingletonFactory;
import tigase.xmpp.BareJID;

import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

//~--- JDK imports ------------------------------------------------------------

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
	@Inject
	private UserRepository repo    = null;

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 *
	 */
	public abstract BareJID getRepoUser();

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

	public void setRepo(UserRepository userRepository) {
		this.repo = userRepository;
		try {
			if (!repo.userExists(getRepoUser())) {
				repo.addUser(getRepoUser());
			}
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
