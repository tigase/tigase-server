/*
 * DynamicRosterIfc.java
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2015 "Tigase, Inc." <office@tigase.com>
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



package tigase.xmpp.impl.roster;

//~--- non-JDK imports --------------------------------------------------------

import tigase.xml.Element;

import tigase.xmpp.JID;
import tigase.xmpp.NotAuthorizedException;
import tigase.xmpp.XMPPResourceConnection;

//~--- JDK imports ------------------------------------------------------------

import java.util.List;
import java.util.Map;

/**
 * Interface <code>DynamicRosterIfc</code> is to dynamically generate user roster
 * entries when the user requests the roster content. The dynamic roster
 * feature doesn't replace the normal roster with entries added by the
 * user. It just allows you to inject extra contacts lists to the user roster.
 * <br>
 * There is a very simple example implementing this interface which creates
 * roster entries for anonymous users - <code>tigase.xmpp.impl.AnonymousRoster</code>.
 * You can use it as a starting point for your code.
 * <br>
 * You can have as many implementations of this interface loaded at the same
 * time as you need and all of them are called for each user roster request.
 * <br>
 * To load your implementations you have to specify them in the configuration
 * file. The simplest way is to use <code>init.properties</code> file. Have a look
 * at the example file available in the repository. Following line added to
 * the end of the file tell the server to load the dynamic roster implementation:
 * <pre>sess-man/plugins-conf/dynamic-roster-classes=tigase.xmpp.impl.AnonymousRoster</pre>
 * If you want to load more implementations you just put a comma separated list
 * of classes instead. If your implementation needs to connect to a database
 * or any other resource or just needs extra configuration parameters you can
 * also specify them in the properties file:
 * <pre>sess-man/plugins-conf/dynamic-roster-classes=tigase.xmpp.impl.AnonymousRoster
 * sess-man/plugins-conf/dbinit=jdbc:jtds:mysql://localhost/roster-db;user=user-name;password=db-passwd
 * sess-man/plugins-conf/max-buddies=1000
 * </pre>
 * Basically all parameters starting with string:
 * <code>sess-man/plugins-conf/</code> will be provided at
 * initialization time in the <code>init(....)</code> method.
 * <br>
 * There is also a simplified form for providing configuration parameters. It is
 * used if you want to provide just one parameter to the object (like database
 * connection string):
 * <pre>sess-man/plugins-conf/tigase.xmpp.impl.AnonymousRoster.init=configuration-string</pre>
 * <br>
 * Created: Mon Oct 29 08:52:22 2007
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 */
public interface DynamicRosterIfc {
	/**
	 * <code>init</code> method is used to provide configuration parameters
	 * and initialize the object. Please have a look at the interface description
	 * for more details about configuration parameters. The object is never used
	 * before it's <code>init(...)</code> method is called but it might be
	 * used straight away after the method has finished.
	 *
	 * @param props a {@code Map<String, Object> props} is a configuration
	 * parameters map in the form: key:value exactly as they were specified
	 * in the configuration file.
	 */
	void init(Map<String, Object> props);

	/**
	 * <code>init</code> method is called at the initialization time when simple
	 * form of startup parameters are used:
	 * <pre>sess-man/plugins-conf/class-name.init=configuration-string</pre>
	 * The <code>configuration-string</code> is passed to this <code>init(...)</code>
	 * method in exact form as it was found in the configuration file.
	 *
	 * @param par a <code>String</code> value of the configuration string.
	 */
	void init(String par);

	//~--- get methods ----------------------------------------------------------

	/**
	 * <code>getBuddies</code> method returns <code>String</code> array with
	 * all roster buddies JIDs. Normally they are bare JIDs (without resource part).
	 * This method is normally used by presence plugin to send probe and initial
	 * presence to all contacts in the dynamic roster.
	 *
	 * @param session a <code>XMPPResourceConnection</code> value of the connection
	 * session object.
	 * @return a <code>String[]</code> array of bare JIDs for the dynamic part
	 * of the user roster.
	 * @exception NotAuthorizedException may be thrown if the connection session
	 * is not yet authenticated but authorization is required to access roster data.
	 * @exception RosterRetrievingException may be thrown when an unknown error in the
	 * custom roster retrieving logic occurs. A message from the exception must be sent
	 * back to a user as an error message.
	 * @exception RepositoryAccessException may be thrown when there is an error accessing
	 * the roster data repository, even though the user is correctly authenticated. No
	 * error is sent back to a user, only an empty roster but the repository exception is
	 * logged to the log file.
	 */
	JID[] getBuddies(XMPPResourceConnection session)
					throws NotAuthorizedException, RosterRetrievingException,
							RepositoryAccessException;

	/**
	 * <code>getBuddyItem</code> method returns buddy item element for a given JID
	 * or <code>null</code> if the buddy doesn't exist on the user roster list.
	 *
	 * @param session a <code>XMPPResourceConnection</code> value of the connection
	 * session object.
	 * @param buddy a <code>String</code> value of the buddy JID. It may be bare JID
	 * or full JID.
	 * @return an <code>Element</code> value of the XML element with all the roster
	 * item data - JID, subscription, nick name and groups. Sample code for creating
	 * the buddy element could look like this:
	 * <pre>Element item = new Element("item", new Element[] {
	 *     new Element("group", "Tigase devs")},
	 *  new String[] {"jid", "subscription", "name"},
	 *  new String[] {peer, "both", JIDUtils.getNodeNick(peer)});</pre>
	 * @exception NotAuthorizedException may be thrown if the connection session
	 * is not yet authenticated but authorization is required to access roster data.
	 * @exception RosterRetrievingException may be thrown when an unknown error in the
	 * custom roster retrieving logic occurs. A message from the exception must be sent
	 * back to a user as an error message.
	 * @exception RepositoryAccessException may be thrown when there is an error accessing
	 * the roster data repository, even though the user is correctly authenticated. No
	 * error is sent back to a user, only an empty roster but the repository exception is
	 * logged to the log file.
	 */
	Element getBuddyItem(XMPPResourceConnection session, JID buddy)
					throws NotAuthorizedException, RosterRetrievingException,
							RepositoryAccessException;

	/**
	 * Returns a new roster Item element with additional, non-standard information
	 * for a given item. This is a way to associate custom roster information with
	 * a contact.
	 *
	 *
	 * @param item is a <code>Element</code>
	 *
	 * @return a value of <code>Element</code>
	 */
	Element getItemExtraData(Element item);

	/**
	 * <code>getRosterItems</code> method returns a full list with all buddies
	 * generated by this dynamic roster implementation. The list contains all
	 * contacts for the roster with all contacts details - buddy JID, nick name,
	 * subscription (typically always both) and groups. Please have a look at
	 * <code>getBuddyItem(...)</code> description for details how to create
	 * an Element entry for the roster item.
	 * <br>
	 * In theory you could here
	 * call the <code>getBuddies(...)</code> method and then for each entry from
	 * the array call the <code>getBuddyItem(...)</code>. I strongly advice to
	 * not do it. This is a server with thousands of connected users and possibly
	 * thousands of packets going through the server. Think of a performance and
	 * execute database query once if possible rather then many times.
	 *
	 * @param session a <code>XMPPResourceConnection</code> value of the connection
	 * session object.
	 * @return a {@code List<Element>} value
	 * @exception NotAuthorizedException may be thrown if the connection session
	 * is not yet authenticated but authorization is required to access roster data.
	 * @exception RosterRetrievingException may be thrown when an unknown error in the
	 * custom roster retrieving logic occurs. A message from the exception must be sent
	 * back to a user as an error message.
	 * @exception RepositoryAccessException may be thrown when there is an error accessing
	 * the roster data repository, even though the user is correctly authenticated. No
	 * error is sent back to a user, only an empty roster but the repository exception is
	 * logged to the log file.
	 */
	List<Element> getRosterItems(XMPPResourceConnection session)
					throws NotAuthorizedException, RosterRetrievingException,
							RepositoryAccessException;

	//~--- set methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param item is a <code>Element</code>
	 */
	void setItemExtraData(Element item);
}


//~ Formatted in Tigase Code Convention on 13/11/29
