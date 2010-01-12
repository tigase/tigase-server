/*  Tigase Jabber/XMPP Server
 *  Copyright (C) 2004-2008 "Artur Hefczyc" <artur.hefczyc@tigase.org>
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
package tigase.xmpp.impl;

import java.util.Map;
import java.util.List;
import tigase.xmpp.XMPPResourceConnection;
import tigase.xml.Element;
import tigase.xmpp.JID;
import tigase.xmpp.NotAuthorizedException;

/**
 * Interface <code>DynamicRosterIfc</code> is to dynamically generate user roster
 * entries when the user requests the roster content. The dynamic roster
 * feature doesn't replace the normal roster with entries added by the
 * user. It just allows you to inject extra contacts lists to the user roster.
 * <p/>
 * There is a very simple example implementing this interface which creates
 * roster entries for anoymous users - <code>tigase.xmpp.impl.AnonymousRoster</code>.
 * You can use it as a starting point for your code.
 * <p/>
 * You can have as many implementations of this interface loaded at the same
 * time as you need and all of them are called for each user roster request.
 * <p/>
 * To load your implementations you have to specify them in the configuration
 * file. The simplest way is to use <code>init.properties</code> file. Have a look
 * at the example file available in the SVN repository. Following line added to
 * the end of the file tell the server to load the dynamic roster implementation:
 * <pre>sess-man/plugins-conf/roster-presence/dynamic-roster-classes=tigase.xmpp.impl.AnonymousRoster</pre>
 * If you want to load more implementations you just put a comma separated list
 * of classes instead. If your implementation needs to connect to a database
 * or any other resource or just needs extra configuration parameters you can
 * also specify them in the properties file:
 * <pre>sess-man/plugins-conf/roster-presence/dynamic-roster-classes=tigase.xmpp.impl.AnonymousRoster
 * sess-man/plugins-conf/roster-presence/dbinit=jdbc:jtds:mysql://localhost/roster-db;user=user-name;password=db-passwd
 * sess-man/plugins-conf/roster-presence/max-buddies=1000
 * </pre>
 * Basically all parameters starting with string:
 * <code>sess-man/plugins-conf/roster-presence/</code> will be provided at
 * initialization time in the <code>init(....)</code> method.
 * <p/>
 * There is also a simplified form for providing configuration parameters. It is
 * used if you want to provide just one parameter to the object (like database
 * connection string):
 * <pre>sess-man/plugins-conf/roster-presence/tigase.xmpp.impl.AnonymousRoster.init=configuration-string</pre>
 * <p/>
 * Created: Mon Oct 29 08:52:22 2007
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public interface DynamicRosterIfc {

	void setItemExtraData(Element item);

	Element getItemExtraData(Element item);

	/**
	 * <code>init</code> method is used to provide configuration parameters
	 * and initialize the object. Please have a look at the interface descriotion
	 * for more details about configuration parameters. The object is never used
	 * before it's <code>init(...)</code> method is called but it might be
	 * used stright away after the method has finished.
	 *
	 * @param props a <code>Map<String, Object> props</code> is a configuration
	 * parameters map in the form: key:value exactly as they were specified
	 * in the configuration file.
	 */
	void init(Map<String, Object> props);

	/**
	 * <code>init</code> method is called at the initialization time when simple
	 * form of startu parameters are used:
   * <pre>sess-man/plugins-conf/roster-presence/class-name.init=configuration-string</pre>
   * The <code>configuration-string</code> is passed to this <code>init(...)</code>
   * method in exact form as it was found in the configuration file.
	 *
	 * @param par a <code>String</code> value of the configuration string.
	 */
	void init(String par);

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
	 */
	JID[] getBuddies(XMPPResourceConnection session)
		throws NotAuthorizedException;

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
	 */
	Element getBuddyItem(XMPPResourceConnection session, JID buddy)
		throws NotAuthorizedException;

	/**
	 * <code>getRosterItems</code> method returns a full list with all buddies
	 * generated by this dynamic roster implementation. The list contains all
	 * contacts for the roster with all contacts details - buddy jid, nick name,
	 * subscription (typically always both) and groups. Please have a look at
	 * <code>getBuddyItem(...)</code> description for details how to create
	 * an Element entry for the roster item.
	 * <p/>
	 * In theory you could here
	 * call the <code>getBuddies(...)</code> method and then for each entry from
	 * the array call the <code>getBuddyItem(...)</code>. I strongly advice to
	 * not do it. This is a server with thousands of connected users and possibly
	 * thousands of packets going through the server. Think of a performance and
	 * excute database query once if possible rather then many times.
	 *
	 * @param session a <code>XMPPResourceConnection</code> value of the connection
	 * session object.
	 * @return a <code>List<Element></code> value
	 * @exception NotAuthorizedException may be thrown if the connection session
	 * is not yet authenticated but authorization is required to access roster data.
	 */
	List<Element> getRosterItems(XMPPResourceConnection session)
		throws NotAuthorizedException;

}
