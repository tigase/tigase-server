/*
 * Message.java
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



package tigase.server;

//~--- non-JDK imports --------------------------------------------------------

import tigase.util.TigaseStringprepException;

import tigase.xml.Element;

import tigase.xmpp.JID;
import tigase.xmpp.StanzaType;

/**
 * Created: Dec 31, 2009 8:38:38 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class Message
				extends Packet {
	/** Field description */
	public static final String ELEM_NAME = "message";

	/** Field description */
	public static final String[] MESSAGE_BODY_PATH = { ELEM_NAME, "body" };

	/** Field description */
	public static final String[] MESSAGE_SUBJECT_PATH = { ELEM_NAME, "subject" };

	/** Field description */
	public static final String[] MESSAGE_ERROR_PATH = { ELEM_NAME, "error" };

	/** Field description */
	public static final String[] MESSAGE_DELAY_PATH = { ELEM_NAME, "delay" };

	//~--- constructors ---------------------------------------------------------

	/**
	 * Constructs ...
	 *
	 *
	 * @param elem
	 *
	 * @throws TigaseStringprepException
	 */
	public Message(Element elem) throws TigaseStringprepException {
		super(elem);
	}

	/**
	 * Constructs ...
	 *
	 *
	 * @param elem
	 * @param stanzaFrom
	 * @param stanzaTo
	 */
	public Message(Element elem, JID stanzaFrom, JID stanzaTo) {
		super(elem, stanzaFrom, stanzaTo);
	}

	//~--- get methods ----------------------------------------------------------

	/**
	 * Creates a packet with message stanza.
	 *
	 *
	 * @param from
	 *          is a <code>JID</code> instance with message source address.
	 * @param to
	 *          is a <code>JID</code> instance with message destination address.
	 * @param type
	 *          is a <code>StanzaType</code> object with the message type.
	 * @param body
	 *          is a <code>String</code> object with message body content.
	 * @param subject
	 *          is a <code>String</code> object with message subject.
	 * @param thread
	 *          is a <code>String</code> object with message thread.
	 * @param id
	 *          is a <code>String</code> object with packet id value. Normally we
	 *          do not set packet IDs for messages but in some cases this might be
	 *          useful.
	 *
	 * @return a new <code>Packet</code> instance (more specifically
	 *         <code>Message</code> instance) with the message stanza.
	 */
	public static Packet getMessage(JID from, JID to, StanzaType type, String body,
			String subject, String thread, String id) {
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

	@Override
	protected String[] getElNameErrorPath() {
		return MESSAGE_ERROR_PATH;
	}
}

