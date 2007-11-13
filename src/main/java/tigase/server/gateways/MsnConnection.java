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
package tigase.server.gateways;



import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.logging.Logger;
import net.sf.jml.Email;
import net.sf.jml.MsnContact;
import net.sf.jml.MsnContact;
import net.sf.jml.MsnContactList;
import net.sf.jml.MsnGroup;
import net.sf.jml.MsnMessenger;
import net.sf.jml.MsnMessenger;
import net.sf.jml.MsnMessenger;
import net.sf.jml.MsnMessenger;
import net.sf.jml.MsnSwitchboard;
import net.sf.jml.MsnUserStatus;
import net.sf.jml.event.MsnContactListListener;
import net.sf.jml.event.MsnMessageListener;
import net.sf.jml.event.MsnMessengerListener;
import net.sf.jml.impl.MsnMessengerFactory;
import net.sf.jml.message.MsnControlMessage;
import net.sf.jml.message.MsnDatacastMessage;
import net.sf.jml.message.MsnInstantMessage;
import net.sf.jml.message.MsnSystemMessage;
import net.sf.jml.message.MsnUnknownMessage;
import tigase.server.Packet;
import tigase.util.JIDUtils;
import tigase.xml.Element;
import tigase.xml.XMLUtils;

/**
 * Describe class MsnConnection here.
 *
 *
 * Created: Mon Nov 12 11:42:01 2007
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class MsnConnection
	implements MsnContactListListener, GatewayConnection, MsnMessengerListener,
						 MsnMessageListener {

  /**
   * Private logger for class instancess.
   */
  private static Logger log =
		Logger.getLogger("tigase.server.gateways.MsnConnection");

	private String username = null;
	private String password = null;
	private MsnMessenger messenger = null;
	private GatewayListener listener = null;
	private Set<String> xmpp_jids = new HashSet<String>();
	private String active_jid = null;
	private String gatewayDomain = null;

	// Implementation of tigase.server.gateways.GatewayConnection

	public void setLogin(String username, String password) {
		this.username = username;
		this.password = password;
		log.finest("Username, password set: (" + username + "," + password + ")");
	}

	public void setGatewayDomain(String domain) {
		this.gatewayDomain = domain;
		log.finest("gatewayDomain set: " + domain);
	}

	public void addJid(String jid) {
		xmpp_jids.add(jid);
		active_jid = jid;
		log.finest("JID added: " + jid);
	}

	public void removeJid(String jid) {
		xmpp_jids.remove(jid);
		log.finest("JID removed: " + jid);
	}

	public void setGatewayListener(GatewayListener listener) {
		this.listener = listener;
	}

	public void init() throws GatewayException {
		messenger = MsnMessengerFactory.createMsnMessenger(username, password);
		messenger.addMessageListener(this);
		messenger.addMessengerListener(this);
		messenger.addContactListListener(this);
	}

	public void login() {
		messenger.login();
	}

	public void logout() {
		MsnSwitchboard[] msbs = messenger.getActiveSwitchboards();
		messenger.logout();
		for (MsnSwitchboard msb: msbs) {
			msb.close();
		}
	}

	public void sendMessage(Packet packet) {
		log.finest("Sending message: " + packet.toString());
		String address =
			JIDUtils.getNodeNick(packet.getElemTo()).replace("%", "@");
		active_jid = packet.getElemFrom();
		if (packet.getElemName().equals("message")) {
			String body = XMLUtils.unescape(packet.getElemCData("/message/body"));
			messenger.sendText(Email.parseStr(address), body);
		}
	}

	// Implementation of net.sf.jml.event.MsnMessageListener

	/**
	 * Describe <code>instantMessageReceived</code> method here.
	 *
	 * @param msnSwitchboard a <code>MsnSwitchboard</code> value
	 * @param msnInstantMessage a <code>MsnInstantMessage</code> value
	 * @param msnContact a <code>MsnContact</code> value
	 */
	public void instantMessageReceived(final MsnSwitchboard msnSwitchboard,
		final MsnInstantMessage msnInstantMessage, final MsnContact msnContact) {
		String to = active_jid;
		String from =
			XMLUtils.escape(msnContact.getEmail().getEmailAddress().replace("@", "%")
				+ "@" + gatewayDomain);
		String content = XMLUtils.escape(msnInstantMessage.getContent());
		Element message = new Element("message",
			new String[] {"from", "to", "type"},
			new String[] {from, to, "chat"});
		Element body = new Element("body", content);
		message.addChild(body);
		Packet packet = new Packet(message);
		log.finest("Received instant message: " + packet.toString());
		listener.packetReceived(packet);
	}

	/**
	 * Describe <code>controlMessageReceived</code> method here.
	 *
	 * @param msnSwitchboard a <code>MsnSwitchboard</code> value
	 * @param msnControlMessage a <code>MsnControlMessage</code> value
	 * @param msnContact a <code>MsnContact</code> value
	 */
	public void controlMessageReceived(final MsnSwitchboard msnSwitchboard,
		final MsnControlMessage msnControlMessage, final MsnContact msnContact) {
		String to = active_jid;
		String from =
			XMLUtils.escape(msnContact.getEmail().getEmailAddress().replace("@", "%")
				+ "@" + gatewayDomain);
		Element message = new Element("message",
			new String[] {"from", "to", "type"},
			new String[] {from, to, "chat"});
		Element composing = new Element("composing");
		composing.setXMLNS("http://jabber.org/protocol/chatstates");
		message.addChild(composing);
		Packet packet = new Packet(message);
		log.finest("Received control message: " + packet.toString());
		listener.packetReceived(packet);
	}

	/**
	 * Describe <code>systemMessageReceived</code> method here.
	 *
	 * @param msnMessenger a <code>MsnMessenger</code> value
	 * @param msnSystemMessage a <code>MsnSystemMessage</code> value
	 */
	public void systemMessageReceived(final MsnMessenger msnMessenger,
		final MsnSystemMessage msnSystemMessage) {
		// Do nothing....
// 		String to = active_jid;
// 		String from = gatewayDomain;
// 		String content = msnSystemMessage.getContent() == null ? ""
// 			: XMLUtils.escape(msnSystemMessage.getContent());
// 		Element message = new Element("message",
// 			new String[] {"from", "to", "type"},
// 			new String[] {from, to, "chat"});
// 		Element body = new Element("body", content);
// 		message.addChild(body);
// 		Packet packet = new Packet(message);
// 		log.finest("Received system message: " + packet.toString());
// 		listener.packetReceived(packet);
	}

	/**
	 * Describe <code>datacastMessageReceived</code> method here.
	 *
	 * @param msnSwitchboard a <code>MsnSwitchboard</code> value
	 * @param msnDatacastMessage a <code>MsnDatacastMessage</code> value
	 * @param msnContact a <code>MsnContact</code> value
	 */
	public void datacastMessageReceived(final MsnSwitchboard msnSwitchboard,
		final MsnDatacastMessage msnDatacastMessage, final MsnContact msnContact) {
		// Ignore for now, I don't know yet how to handle it.
	}

	/**
	 * Describe <code>unknownMessageReceived</code> method here.
	 *
	 * @param msnSwitchboard a <code>MsnSwitchboard</code> value
	 * @param msnUnknownMessage a <code>MsnUnknownMessage</code> value
	 * @param msnContact a <code>MsnContact</code> value
	 */
	public void unknownMessageReceived(final MsnSwitchboard msnSwitchboard,
		final MsnUnknownMessage msnUnknownMessage, final MsnContact msnContact) {
		// Ignore for now, I don't know yet how to handle it.
	}


	// Implementation of net.sf.jml.event.MsnMessengerListener

	/**
	 * Describe <code>logout</code> method here.
	 *
	 * @param msnMessenger a <code>MsnMessenger</code> value
	 */
	public void logout(final MsnMessenger msnMessenger) {
		listener.logout(active_jid);
		log.finest(active_jid + " logout called.");
	}

	/**
	 * Describe <code>loginCompleted</code> method here.
	 *
	 * @param msnMessenger a <code>MsnMessenger</code> value
	 */
	public void loginCompleted(final MsnMessenger msnMessenger) {
		listener.loginCompleted(active_jid);
		log.finest(active_jid + " logout completed.");
	}

	/**
	 * Describe <code>exceptionCaught</code> method here.
	 *
	 * @param msnMessenger a <code>MsnMessenger</code> value
	 * @param throwable a <code>Throwable</code> value
	 */
	public void exceptionCaught(final MsnMessenger msnMessenger,
		final Throwable throwable) {
		listener.gatewayException(active_jid, throwable);
	}


	// Implementation of net.sf.jml.event.MsnContactListListener

	/**
	 * Describe <code>contactListSyncCompleted</code> method here.
	 *
	 * @param msnMessenger a <code>MsnMessenger</code> value
	 */
	public void contactListSyncCompleted(final MsnMessenger msnMessenger) {
		log.finest(active_jid + " contactListSyncCompleted completed.");
// 		MsnContact[] list = msnMessenger.getContactList().getContacts();
// 		if (list != null) {
// 			Queue<Packet> buddy_presences = new LinkedList<Packet>();
// 			String to = active_jid;
// 			String from = gatewayDomain;
// 			Element iq = new Element("iq",
// 				new String[] {"from", "to", "type", "id"},
// 				new String[] {from, to, "set", "1r"});
// 			Element query = new Element("query");
// 			query.setXMLNS("jabber:iq:roster");
// 			iq.addChild(query);
// 			for (MsnContact contact: list) {
// 				Element item = new Element("item");
// 				String jid = XMLUtils.escape(contact.getId().replace("@", "%")
// 					+ "@" + gatewayDomain);
// 				item.setAttribute("jid", jid);
// 				item.setAttribute("subscription", "both");
// 				item.setAttribute("name", XMLUtils.escape(contact.getFriendlyName()));
// 				MsnGroup[] groups = contact.getBelongGroups();
// 				if (groups != null && groups.length > 0) {
// 					for (MsnGroup group: groups) {
// 						Element grel = new Element("group");
// 						grel.setCData(group.getGroupName());
// 						item.addChild(grel);
// 					}
// 				}
// 				query.addChild(item);
// 				Element presence = new Element("presence",
// 					new String[] {"from", "to"}, new String[] {jid, to});
// 				if (contact.getStatus() == MsnUserStatus.OFFLINE ) {
// 					presence.setAttribute("type", "unavailable");
// 				} else {
// 					presence.addChild(new Element("show",
// 							contact.getStatus().getDisplayStatus()));
// 				}
// 				buddy_presences.offer(new Packet(presence));
// 			}
// 			Packet roster = new Packet(iq);
// 			log.finest("Sending out the roster: " + roster.toString());
// 			listener.packetReceived(roster);
// 			Packet pack = null;
// 			while ((pack = buddy_presences.poll()) != null) {
// 				log.finest("Sending out the buddy presence: " + pack.toString());
// 				listener.packetReceived(pack);
// 			}
// 		}
	}

	/**
	 * Describe <code>contactListInitCompleted</code> method here.
	 *
	 * @param msnMessenger a <code>MsnMessenger</code> value
	 */
	public void contactListInitCompleted(final MsnMessenger msnMessenger) {
		log.finest(active_jid + " contactListInitCompleted completed.");
		MsnContact[] list = msnMessenger.getContactList().getContacts();
		if (list != null) {
// 			Queue<Packet> buddy_presences = new LinkedList<Packet>();
// 			String to = active_jid;
// 			String from = gatewayDomain;
// 			Element iq = new Element("iq",
// 				new String[] {"from", "to", "type", "id"},
// 				new String[] {from, to, "set", "1r"});
// 			Element query = new Element("query");
// 			query.setXMLNS("jabber:iq:roster");
// 			iq.addChild(query);
			List<RosterItem> roster = new ArrayList<RosterItem>();
			for (MsnContact contact: list) {
				RosterItem item = new RosterItem(contact.getEmail().getEmailAddress());
				item.setName(contact.getFriendlyName());
				item.setSubscription("both");
				if (contact.getStatus() == MsnUserStatus.OFFLINE ) {
					item.setStatus(new UserStatus("unavailable", null));
				} else {
					item.setStatus(new UserStatus(null,
							contact.getStatus().getDisplayStatus().toLowerCase()));
				}
				MsnGroup[] groups = contact.getBelongGroups();
				if (groups != null && groups.length > 0) {
					List<String> grps = new ArrayList<String>();
					for (MsnGroup group: groups) {
						grps.add(group.getGroupName());
					}
					item.setGroups(grps);
				}
				roster.add(item);
// 				Element item = new Element("item");
// 				String jid = XMLUtils.escape(contact.getId().replace("@", "%")
// 					+ "@" + gatewayDomain);
// 				item.setAttribute("jid", jid);
// 				item.setAttribute("subscription", "both");
// 				item.setAttribute("name", XMLUtils.escape(contact.getFriendlyName()));
// 				query.addChild(item);
// 				Element presence = new Element("presence",
// 					new String[] {"from", "to"}, new String[] {jid, to});
// 				if (contact.getStatus() == MsnUserStatus.OFFLINE ) {
// 					presence.setAttribute("type", "unavailable");
// 				} else {
// 					presence.addChild(new Element("show",
// 							contact.getStatus().getDisplayStatus()));
// 				}
// 				buddy_presences.offer(new Packet(presence));
// 			}
// 			Packet roster = new Packet(iq);
// 			log.finest("Sending out the roster: " + roster.toString());
// 			listener.packetReceived(roster);
// 			Packet pack = null;
// 			while ((pack = buddy_presences.poll()) != null) {
// 				log.finest("Sending out the buddy presence: " + pack.toString());
// 				listener.packetReceived(pack);
 			}
			listener.userRoster(active_jid, roster);
		}
	}

	/**
	 * Describe <code>contactStatusChanged</code> method here.
	 *
	 * @param msnMessenger a <code>MsnMessenger</code> value
	 * @param msnContact a <code>MsnContact</code> value
	 */
	public void contactStatusChanged(final MsnMessenger msnMessenger,
		final MsnContact msnContact) {
		log.finest(active_jid + " contactStatusChanged completed.");
		String to = active_jid;
		String jid =
			XMLUtils.escape(msnContact.getEmail().getEmailAddress().replace("@", "%")
				+ "@" + gatewayDomain);
		Element presence = new Element("presence",
			new String[] {"from", "to"}, new String[] {jid, to});
		if (msnContact.getStatus() == MsnUserStatus.OFFLINE ) {
			presence.setAttribute("type", "unavailable");
		} else {
			presence.addChild(new Element("show",
					msnContact.getStatus().getDisplayStatus().toLowerCase()));
		}
		Packet packet = new Packet(presence);
		log.finest("Sending out buddy presence: " + packet.toString());
		listener.packetReceived(packet);
	}

	/**
	 * Describe <code>ownerStatusChanged</code> method here.
	 *
	 * @param msnMessenger a <code>MsnMessenger</code> value
	 */
	public void ownerStatusChanged(final MsnMessenger msnMessenger) {
		log.finest(active_jid + " ownerStatusChanged completed.");
	}

	/**
	 * Describe <code>contactAddedMe</code> method here.
	 *
	 * @param msnMessenger a <code>MsnMessenger</code> value
	 * @param msnContact a <code>MsnContact</code> value
	 */
	public void contactAddedMe(final MsnMessenger msnMessenger,
		final MsnContact msnContact) {
		log.finest(active_jid + " contactAddedMe completed.");
	}

	/**
	 * Describe <code>contactRemovedMe</code> method here.
	 *
	 * @param msnMessenger a <code>MsnMessenger</code> value
	 * @param msnContact a <code>MsnContact</code> value
	 */
	public void contactRemovedMe(final MsnMessenger msnMessenger,
		final MsnContact msnContact) {
		log.finest(active_jid + " contactRemovedMe completed.");
	}

	/**
	 * Describe <code>contactAddCompleted</code> method here.
	 *
	 * @param msnMessenger a <code>MsnMessenger</code> value
	 * @param msnContact a <code>MsnContact</code> value
	 */
	public void contactAddCompleted(final MsnMessenger msnMessenger,
		final MsnContact msnContact) {
		log.finest(active_jid + " contactAddCompleted completed.");
	}

	/**
	 * Describe <code>contactRemoveCompleted</code> method here.
	 *
	 * @param msnMessenger a <code>MsnMessenger</code> value
	 * @param msnContact a <code>MsnContact</code> value
	 */
	public void contactRemoveCompleted(final MsnMessenger msnMessenger,
		final MsnContact msnContact) {
		log.finest(active_jid + " contactRemoveCompleted completed.");
	}

	/**
	 * Describe <code>groupAddCompleted</code> method here.
	 *
	 * @param msnMessenger a <code>MsnMessenger</code> value
	 * @param msnGroup a <code>MsnGroup</code> value
	 */
	public void groupAddCompleted(final MsnMessenger msnMessenger,
		final MsnGroup msnGroup) {
		log.finest(active_jid + " groupAddCompleted completed.");
	}

	/**
	 * Describe <code>groupRemoveCompleted</code> method here.
	 *
	 * @param msnMessenger a <code>MsnMessenger</code> value
	 * @param msnGroup a <code>MsnGroup</code> value
	 */
	public void groupRemoveCompleted(final MsnMessenger msnMessenger,
		final MsnGroup msnGroup) {
		log.finest(active_jid + " groupRemoveCompleted completed.");
	}

	public String getType() { return "msn"; }

	public String getName() { return "MSN Gateway"; }

}
