/*
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
package tigase.vhosts;

import tigase.server.Packet;
import tigase.xml.Element;

/**
 * Interface required to be implemented by all extensions adding additional configuration options for the virtual
 * host configuration items.
 *
 * WARNING: This is just an interface and you have to use <code>VHostItemExtension</code> class as a base class
 * for your extension.
 *
 * @param <T> - should be provided the same class as we are defining!
 */
public interface VHostItemExtensionIfc<T extends VHostItemExtensionIfc<T>> {

	/**
	 * Unique identifier of the extension. It has to be a valid XML element name!
	 * @return
	 */
	String getId();

	/**
	 * Method initializes instances of a class with values from the element which contains configuration
	 * loaded from the database.
	 *
	 * @see VHostItemExtensionIfc::toElement()
	 *
	 * @param item
	 */
	void initFromElement(Element item);

	/**
	 * Method initializes instance of a class with values provided by the user using ad-hoc command.
	 * 
	 * @param prefix - prefix for data for fields added by this extension
	 * @param packet - stanza with submitted ad-hoc command form
	 * @throws IllegalArgumentException
	 */
	void initFromCommand(String prefix, Packet packet) throws IllegalArgumentException;

	/**
	 * Method serializes data stored by this instance to element which will be then stored in the database.
	 *
	 * Element name should be equal to the extension id.
	 *
	 * @return
	 */
	Element toElement();

	/**
	 * Method adds custom extension fields to the ad-hoc form which will be sent to the user for filling
	 * with data required to create or update the VHost details.
	 *
	 * @param prefix - prefix which should be used by each added field
	 * @param packet - packet which will be sent to the user
	 * @param forDefault - if true, we are preparing form for "default" configuration used by default by
	 * all vhosts.
	 */
	void addCommandFields(String prefix, Packet packet, boolean forDefault);

	/**
	 * Returns a string containing all information about the instance of the extension useful for debugging.
	 * @return
	 */
	String toDebugString();
}
