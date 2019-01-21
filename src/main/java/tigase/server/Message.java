/**
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
package tigase.server;

import tigase.util.stringprep.TigaseStringprepException;
import tigase.xml.Element;
import tigase.xmpp.StanzaType;
import tigase.xmpp.jid.JID;

/**
 * Created: Dec 31, 2009 8:38:38 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class Message
		extends Packet {

	public static final String ELEM_NAME = "message";

	public static final String[] MESSAGE_BODY_PATH = {ELEM_NAME, "body"};

	public static final String[] MESSAGE_SUBJECT_PATH = {ELEM_NAME, "subject"};

	public static final String[] MESSAGE_ERROR_PATH = {ELEM_NAME, "error"};

	public static final String[] MESSAGE_DELAY_PATH = {ELEM_NAME, "delay"};

	/**
	 * Creates a packet with message stanza.
	 *
	 * @param from is a <code>JID</code> instance with message source address.
	 * @param to is a <code>JID</code> instance with message destination address.
	 * @param type is a <code>StanzaType</code> object with the message type.
	 * @param body is a <code>String</code> object with message body content.
	 * @param subject is a <code>String</code> object with message subject.
	 * @param thread is a <code>String</code> object with message thread.
	 * @param id is a <code>String</code> object with packet id value. Normally we do not set packet IDs for messages
	 * but in some cases this might be useful.
	 *
	 * @return a new <code>Packet</code> instance (more specifically <code>Message</code> instance) with the message
	 * stanza.
	 */
	public static Packet getMessage(JID from, JID to, StanzaType type, String body, String subject, String thread,
									String id) {
		Element message = new Element("message", null, null);

		message.setXMLNS(CLIENT_XMLNS);
		if (body != null) {
			message.addChild(new Element("body", body));
		}
		if (from != null) {
			message.addAttribute("from", from.toString());
		}
		if (to != null) {
			message.addAttribute("to", to.toString());
		}
		if (type != null) {
			message.addAttribute("type", type.name());
		}
		if (id != null) {
			message.addAttribute("id", id);
		}
		if (subject != null) {
			message.addChild(new Element("subject", subject));
		}
		if (thread != null) {
			message.addChild(new Element("thread", thread));
		}

		return packetInstance(message, from, to);
	}

	public Message(Element elem) throws TigaseStringprepException {
		super(elem);
	}

	public Message(Element elem, JID stanzaFrom, JID stanzaTo) {
		super(elem, stanzaFrom, stanzaTo);
	}

	@Override
	protected String[] getElNameErrorPath() {
		return MESSAGE_ERROR_PATH;
	}
}

