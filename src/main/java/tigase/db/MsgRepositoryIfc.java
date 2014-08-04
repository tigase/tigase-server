
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
 */
package tigase.db;

//~--- non-JDK imports --------------------------------------------------------
import tigase.xml.Element;

import tigase.xmpp.JID;

//~--- JDK imports ------------------------------------------------------------

import java.util.Date;
import java.util.Map;
import java.util.Queue;

//~--- interfaces -------------------------------------------------------------
/**
 * Interface for storing and restoring offline Elements.
 *
 * Created: May 11, 2010 6:56:14 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 */
public interface MsgRepositoryIfc {

	/**
	 * Loads head of the payloads queue which holds items that would be expired after
	 * stated time with an option to delete them from repository after being
	 * retrieved.
	 *
	 * @param time   time in milliseconds representing time after which given
	 *               message would be considered as expired
	 * @param delete boolean parameter controlling whether messages should be
	 *               removed from repository after they retrieved.
	 *
	 * @return head of the payloads queue which holds items that would be expired after
	 * stated time with an option to delete them from repository after being
	 * retrieved.
	 */
	Element getMessageExpired( long time, boolean delete );
	
	/**
	 * Method to initialize this instance of MsgResitoryIfc including creation
	 * of tables when needed
	 * 
	 * @param conn_str
	 * @param map 
	 * @throws tigase.db.TigaseDBException 
	 */
	void initRepository(String conn_str, Map<String, String> map) throws TigaseDBException;

	//~--- methods --------------------------------------------------------------
	/**
	 * Loads all payloads for the given user's {@link JID} from repository.
	 *
	 * @param to     {@link JID} denotes address of the receiver for which payload
	 *               stored in the repository should be retrieved.
	 * @param delete boolean parameter controlling whether messages should be
	 *               removed from repository after they retrieved.
	 *
	 * @return a {@link Queue} of {@link Element} objects representing stored
	 *         payloads for the given user's {@link JID}
	 *
	 * @throws UserNotFoundException
	 */
	Queue<Element> loadMessagesToJID( JID to, boolean delete ) throws UserNotFoundException;

	/**
	 * Saves the massage to the repository
	 *
	 * @param from    {@link JID} denotes address of the sender
	 * @param to      {@link JID} denotes address of the receiver
	 * @param expired {@link Date} object denoting expiration date of the message
	 * @param msg     {@link Element} payload of the stanza to be saved
	 *
	 * @throws UserNotFoundException
	 */
	void storeMessage( JID from, JID to, Date expired, Element msg ) throws UserNotFoundException;
}
