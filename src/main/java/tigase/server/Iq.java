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

package tigase.server;

//~--- non-JDK imports --------------------------------------------------------

import tigase.disco.XMPPService;

import tigase.util.TigaseStringprepException;

import tigase.xml.Element;

import tigase.xmpp.JID;
import tigase.xmpp.StanzaType;
import tigase.xmpp.impl.roster.RosterAbstract;

//~--- JDK imports ------------------------------------------------------------

import java.util.List;

//~--- classes ----------------------------------------------------------------

/**
 * Created: Dec 31, 2009 8:43:21 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class Iq extends Packet {

	/** Field description */
	public static final String ELEM_NAME = "iq";

	//~--- fields ---------------------------------------------------------------

	private Command command = null;
	private String iqQueryXMLNS = null;
	private String strCommand = null;
	private boolean serviceDisco = false;
	private boolean cmd = false;

	//~--- constructors ---------------------------------------------------------

	/**
	 * Constructs ...
	 *
	 *
	 * @param elem
	 *
	 * @throws TigaseStringprepException
	 */
	public Iq(Element elem) throws TigaseStringprepException {
		super(elem);
		init();
	}

	/**
	 * Constructs ...
	 *
	 *
	 * @param elem
	 * @param stanzaFrom
	 * @param stanzaTo
	 */
	public Iq(Element elem, JID stanzaFrom, JID stanzaTo) {
		super(elem, stanzaFrom, stanzaTo);
		init();
	}

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param packet
	 *
	 * @return
	 *
	 * @throws TigaseStringprepException
	 */
	public static Packet commandResultForm(Iq packet) throws TigaseStringprepException {
		Packet result = packet.commandResult(Command.DataType.form);

		return result;
	}

	/**
	 * Method description
	 *
	 *
	 * @param packet
	 *
	 * @return
	 *
	 * @throws TigaseStringprepException
	 */
	public static Packet commandResultResult(Iq packet) throws TigaseStringprepException {
		Packet result = packet.commandResult(Command.DataType.result);

		return result;
	}

	/**
	 * Method description
	 * TODO: Remove dependency on RosterAbstract class, possibly move the method again
	 * to more proper location but it needs to be accessible from all parts of the application.
	 *
	 * @param iq_type
	 * @param iq_id
	 * @param from
	 * @param to
	 * @param item_jid
	 * @param item_name
	 * @param item_groups
	 * @param subscription
	 * @param item_type
	 *
	 * @return
	 */
	public static Iq createRosterPacket(String iq_type, String iq_id, JID from, JID to,
			JID item_jid, String item_name, String[] item_groups, String subscription,
				String item_type) {
		Element iq = new Element("iq", new String[] { "type", "id" }, new String[] { iq_type,
				iq_id });

		if (from != null) {
			iq.addAttribute("from", from.toString());
		}

		if (to != null) {
			iq.addAttribute("to", to.toString());
		}

		Element query = new Element("query");

		query.setXMLNS(RosterAbstract.XMLNS);
		iq.addChild(query);

		Element item = new Element("item", new String[] { "jid" },
			new String[] { item_jid.toString() });

		if (item_type != null) {
			item.addAttribute("type", item_type);
		}

		if (item_name != null) {
			item.addAttribute(RosterAbstract.NAME, item_name);
		}

		if (subscription != null) {
			item.addAttribute(RosterAbstract.SUBSCRIPTION, subscription);
		}

		if (item_groups != null) {
			for (String gr : item_groups) {
				Element group = new Element(RosterAbstract.GROUP, gr);

				item.addChild(group);
			}
		}

		query.addChild(item);

		return new Iq(iq, from, to);
	}

	/**
	 * Method description
	 *
	 *
	 * @param cmd_type
	 *
	 * @return
	 */
	public Packet commandResult(Command.DataType cmd_type) {
		Packet result = packetInstance(command.createIqCommand(getStanzaTo(), getStanzaFrom(),
			StanzaType.result, getStanzaId(), strCommand, cmd_type), getStanzaTo(), getStanzaFrom());

		result.setPacketFrom(getTo());
		result.setPacketTo(getFrom());

		return result;
	}

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public Command getCommand() {
		return command;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	public String getIQChildName() {
		List<Element> children = elem.getChildren();

		if ((children != null) && (children.size() > 0)) {
			return children.get(0).getName();
		}

		return null;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	public String getIQXMLNS() {
		if (iqQueryXMLNS == null) {
			iqQueryXMLNS = elem.getXMLNS("/iq/query");
		}

		return iqQueryXMLNS;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	public String getStrCommand() {
		return strCommand;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public boolean isCommand() {
		return cmd;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public boolean isServiceDisco() {
		return serviceDisco;
	}

	//~--- methods --------------------------------------------------------------

	private void init() {
		Element child = elem.getChild("command", Command.XMLNS);

		if (child != null) {
			cmd = true;
			strCommand = child.getAttribute("node");
			command = Command.valueof(strCommand);
		}

		serviceDisco = (isXMLNS("/iq/query", XMPPService.INFO_XMLNS)
				|| isXMLNS("/iq/query", XMPPService.ITEMS_XMLNS));
	}
}


//~ Formatted in Sun Code Convention


//~ Formatted by Jindent --- http://www.jindent.com
