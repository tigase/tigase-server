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

package tigase.db;

import tigase.server.Packet;
import tigase.xml.Element;

/**
 * The interface defines a contract for a repository item handled by
 * ComponentRepository implementation.
 * Created: Oct 3, 2009 2:35:58 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public interface RepositoryItem {

	/**
	 * The item can be initialized based on the data loaded from a configuration file.
	 * In such a case the item representation is usually very simplified as a list of
	 * parameters separated by a marker. Please note, usually each item is separated
	 * from another with a comma, therefore do not use a comma in the item property
	 * string. Double collon is commonly used alternative.
	 *
	 * @param propString is a property string to initialize the RepositoryItem.
	 */
	void initFromPropertyString(String propString);

	/**
	 * The item can be initialized based on the data loaded from a configuration file.
	 * In such a case the item representation is usually very simplified as a list of
	 * parameters separated by a marker. Please note, usually each item is separated
	 * from another with a comma, therefore do not use a comma in the item property
	 * string. Double collon is commonly used alternative.
	 *
	 * @return a property string representing the repository item in a simplified form.
	 */
	String toPropertyString();

	/**
	 * The item can be also initialized from a more complex repositories: XML repository
	 * or SQL database. In such a case more complex representation is prefered, possibly
	 * carrying more infomration about the item. The method is called to initialize the
	 * item with a data parsed from an XML representation of the repository.
	 *
	 * @param elem XML Element with all the item initialization data.
	 */
	void initFromElement(Element elem);

	/**
	 * Item data can be stored in a more comlex form than a simple property string.
	 * The XML Element can contain much more detailed information about the element
	 * than the simplified form and is used to store the repository item in more advanced
	 * repositories then just property file. XML repository or SQL database can keep many
	 * records for repository item with as much detailed information as needed.
	 *
	 * @return an XML Element with all the item initialization data.
	 */
	Element toElement();

	/**
	 * Returns a unique key for the item in the repository. All items are stored in
	 * a memory cache which is a Map. And the key returned by this method is the
	 * item identifier in the Map.
	 * @return an Item key.
	 */
	String getKey();

	/**
	 * The method is used for handling ad-hoc commands. The 'empty' ad-hoc command
	 * packet is provided and the Item should fill it with fields for the user.
	 * @param packet with empty ad-hoc command to fill with fields
	 */
	void addCommandFields(Packet packet);

	/**
	 * The method used for handling ad-hoc commands. After a user fills all given field
	 * the ad-hoc command packet is passed back to the item to initialize it with
	 * data. Similar method to initFromElement(), but the data source is different.
	 * @param packet with ad-hoc command filled by the user.
	 */
	void initFromCommand(Packet packet);

}
