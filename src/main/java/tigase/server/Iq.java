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

import java.util.List;
import tigase.disco.XMPPService;
import tigase.util.TigaseStringprepException;
import tigase.xml.Element;
import tigase.xmpp.JID;
import tigase.xmpp.StanzaType;

/**
 * Created: Dec 31, 2009 8:43:21 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class Iq extends Packet {

	public static final String ELEM_NAME = "iq";

	private Command command = null;
	private String strCommand = null;
	private boolean cmd = false;
	private boolean serviceDisco = false;
	private String iqQueryXMLNS = null;

	public Iq(Element elem) throws TigaseStringprepException {
		super(elem);
		init();
	}

	public Iq(Element elem, JID stanzaFrom, JID stanzaTo) {
		super(elem, stanzaFrom, stanzaTo);
		init();
	}

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

	@Override
	public Command getCommand() {
		return command;
	}

	public String getStrCommand() {
		return strCommand;
	}

	@Override
	public boolean isCommand() {
		return cmd;
	}

	@Override
	public boolean isServiceDisco() {
		return serviceDisco;
	}

	public String getIQXMLNS() {
		if (iqQueryXMLNS == null) {
			iqQueryXMLNS = elem.getXMLNS("/iq/query");
		}
		return iqQueryXMLNS;
	}

	public String getIQChildName() {
		List<Element> children = elem.getChildren();
		if (children != null && children.size() > 0) {
			return children.get(0).getName();
		}
		return null;
	}

	public Packet commandResult(Command.DataType cmd_type) {
		Packet result = packetInstance(command.createIqCommand(
						getStanzaTo(), getStanzaFrom(),
						StanzaType.result, getStanzaId(), strCommand, cmd_type),
						getStanzaTo(), getStanzaFrom());
		result.setPacketFrom(getTo());
		result.setPacketTo(getFrom());
		return result;
	}

	public static Packet commandResultForm(Iq packet) throws TigaseStringprepException {
		Packet result = packet.commandResult(Command.DataType.form);
		return result;
	}

	public static Packet commandResultResult(Iq packet) throws TigaseStringprepException {
		Packet result = packet.commandResult(Command.DataType.result);
		return result;
	}

}
