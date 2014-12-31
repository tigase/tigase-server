/*
 * JabberIqIq.java
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



package tigase.xmpp.impl;

//~--- non-JDK imports --------------------------------------------------------

import tigase.db.NonAuthUserRepository;
import tigase.db.TigaseDBException;
import tigase.db.UserNotFoundException;

import tigase.server.Iq;
import tigase.server.Packet;

import tigase.util.Base64;

import tigase.xml.Element;

import tigase.xmpp.Authorization;
import tigase.xmpp.BareJID;
import tigase.xmpp.NotAuthorizedException;
import tigase.xmpp.StanzaType;
import tigase.xmpp.XMPPException;
import tigase.xmpp.XMPPPreprocessorIfc;
import tigase.xmpp.XMPPProcessor;
import tigase.xmpp.XMPPProcessorIfc;
import tigase.xmpp.XMPPResourceConnection;

//~--- JDK imports ------------------------------------------------------------

import java.util.logging.Logger;
import java.util.Map;
import java.util.Queue;

/**
 * Describe class JabberIqIq here.
 *
 *
 * Created: Sun Feb 25 23:37:48 2007
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class JabberIqIq
				extends XMPPProcessor
				implements XMPPProcessorIfc, XMPPPreprocessorIfc {
	private static final String[][] ELEMENTS = {
		Iq.IQ_QUERY_PATH
	};
	private static final String     LEVEL    = "level";

	/**
	 * Private logger for class instances.
	 */
	private static Logger log = Logger.getLogger(JabberIqIq.class.getName());

	/**
	 * I don't want to have any offensive texts in my code so let's
	 * encode them with Base64....
	 */
	private static String[] not_so_smart_words = {
		"ZnVjaw==", "c2hpdA==", "d2hvcmU=", "ZGljaw==", "YXNz", "YW51cw==", "YXJzZQ==",
		"dmFnaW5h", "cG9ybg==", "cGVuaXM=", "cGlzcw==", "c3V4"
	};
	private static final String    XMLNS  = "jabber:iq:iq";
	private static final String    ID     = XMLNS;
	private static final String[]  XMLNSS = { XMLNS };
	private static final Element[] DISCO_FEATURES = { new Element("feature", new String[] {
			"var" }, new String[] { XMLNS }) };

	//~--- methods --------------------------------------------------------------

	/**
	 * IQ range table:
	 * Number Range   Descriptive Label
	 * 140+   genius
	 * 120-139  very superior
	 * 110-119  superior
	 * 90-109   normal
	 * 80-89  dull
	 * 70-79  borderline deficiency
	 * 50-69  moron
	 * 20-49  imbecile
	 * 0-19   idiot
	 *
	 * @param iq_level
	 *
	 * 
	 */
	public static String calculateIQ(String iq_level) {
		double value = 100;

		try {
			value = Double.parseDouble(iq_level);
		} catch (NumberFormatException e) {
			value = 100;
		}
		if (value >= 140) {
			return "genius";
		}
		if ((120 <= value) && (value <= 139)) {
			return "very superior";
		}
		if ((110 <= value) && (value <= 119)) {
			return "superior";
		}
		if ((90 <= value) && (value <= 109)) {
			return "normal";
		}
		if ((80 <= value) && (value <= 89)) {
			return "dull";
		}
		if ((70 <= value) && (value <= 79)) {
			return "borderline deficiency";
		}
		if ((50 <= value) && (value <= 69)) {
			return "moron";
		}
		if ((20 <= value) && (value <= 49)) {
			return "imbecile";
		}
		if ((0 <= value) && (value <= 19)) {
			return "idiot";
		}

		return "out of range";
	}

	@Override
	public String id() {
		return ID;
	}

	@Override
	public boolean preProcess(Packet packet, XMPPResourceConnection session,
			NonAuthUserRepository repo, Queue<Packet> results, Map<String, Object> settings) {
		try {
			if ((session != null) && (packet.getFrom() != null) && packet.getFrom().equals(
					session.getConnectionId()) && (packet.getElemName() == tigase.server.Message
					.ELEM_NAME)) {
				evaluateMessage(session, packet.getElemCDataStaticStr(tigase.server.Message
						.MESSAGE_BODY_PATH));
			}
		} catch (Exception e) {

			// Ignore....
		}

		return false;
	}

	@Override
	public void process(Packet packet, XMPPResourceConnection session,
			NonAuthUserRepository repo, Queue<Packet> results, final Map<String,
			Object> settings)
					throws XMPPException {
		if ((session == null) && (packet.getType() != null) && (packet.getType() == StanzaType
				.get)) {
			try {
				String iq_level = repo.getPublicData(packet.getStanzaTo().getBareJID(), ID,
						LEVEL, null);

				results.offer(getResponsePacket(packet, iq_level));
			} catch (UserNotFoundException e) {

				// Just ignore....
			}    // end of try-catch

			return;
		}      // end of if (session == null)
		if (session == null) {
			log.info("Session null, dropping packet: " + packet.toString());

			return;
		}    // end of if (session == null)
		try {

			// Not needed anymore. Packet filter does it for all stanzas.
//    if (packet.getFrom().equals(session.getConnectionId())) {
//      packet.getElement().setAttribute("from", session.getJID());
//    } // end of if (packet.getFrom().equals(session.getConnectionId()))
			BareJID id = null;

			if (packet.getStanzaTo() != null) {
				id = packet.getStanzaTo().getBareJID();
			}    // end of if (packet.getElemTo() != null)
			if ((id == null) || session.isUserId(id)) {
				StanzaType type = packet.getType();

				switch (type) {
				case get :
					String iq_level = session.getPublicData(ID, LEVEL, null);

					results.offer(getResponsePacket(packet, iq_level));

					break;

				case set :
					if (packet.getFrom().equals(session.getConnectionId())) {
						String curr_iq = changeIq(session, -2);

						results.offer(Authorization.NOT_ALLOWED.getResponseMessage(packet,
								"You are not allowed to set own IQ, your current IQ score: " + curr_iq,
								true));
					} else {
						results.offer(Authorization.NOT_AUTHORIZED.getResponseMessage(packet,
								"You are not authorized to set vcard data.", true));
					}    // end of else

					break;

				case result :
					Packet result = packet.copyElementOnly();

					result.setPacketTo(session.getConnectionId());
					result.setPacketFrom(packet.getTo());
					results.offer(result);

					break;

				default :
					results.offer(Authorization.BAD_REQUEST.getResponseMessage(packet,
							"Request type is incorrect", false));

					break;
				}    // end of switch (type)
			} else {
				Packet result = packet.copyElementOnly();

				results.offer(result);
			}      // end of else
		} catch (NotAuthorizedException e) {
			log.warning("Received privacy request but user session is not authorized yet: " +
					packet.toString());
			results.offer(Authorization.NOT_AUTHORIZED.getResponseMessage(packet,
					"You must authorize session first.", true));
		} catch (TigaseDBException e) {
			log.warning("Database proble, please contact admin: " + e);
			results.offer(Authorization.INTERNAL_SERVER_ERROR.getResponseMessage(packet,
					"Database access problem, please contact administrator.", true));
		}
	}

	@Override
	public Element[] supDiscoFeatures(final XMPPResourceConnection session) {
		return DISCO_FEATURES;
	}

	@Override
	public String[][] supElementNamePaths() {
		return ELEMENTS;
	}

	@Override
	public String[] supNamespaces() {
		return XMLNSS;
	}

	private String changeIq(XMPPResourceConnection session, double val)
					throws NotAuthorizedException, TigaseDBException {
		double value = getIq(session);

		value += val;

		String curr = new Double(value).toString();

		session.setPublicData(ID, LEVEL, curr);

		return curr;
	}

	private void evaluateMessage(XMPPResourceConnection session, String msg)
					throws NotAuthorizedException, TigaseDBException {
		if (msg == null) {
			return;
		}

		// User wrote a message, good + 0.01
		double val     = 0.01;
		int    msg_len = msg.trim().length();

		if ((msg_len > 10) && (msg_len < 100)) {
			val += 0.01;
		}
		if ((msg_len >= 100) && (msg_len < 200)) {
			val += 0.1;
		}
		if ((msg_len >= 200) && (msg_len < 500)) {
			val += 0.01;
		}
		if (msg_len >= 500) {
			val -= 0.1;
		}
		for (String not_smart : not_so_smart_words) {
			if (msg.contains(new String(Base64.decode(not_smart)))) {
				val -= 0.1;
			}
		}

		double iq = getIq(session);

		val = val / iq;
		iq  += val;

		String curr = new Double(iq).toString();

		session.setPublicData(ID, LEVEL, curr);
	}

	//~--- get methods ----------------------------------------------------------

	private double getIq(XMPPResourceConnection session)
					throws NotAuthorizedException, TigaseDBException {
		String iq_level = session.getPublicData(ID, LEVEL, "100");
		double iq       = 100;

		try {
			iq = Double.parseDouble(iq_level);
		} catch (NumberFormatException e) {
			iq = 100;
		}

		return iq;
	}

	private Packet getResponsePacket(Packet packet, String iq_level) {
		if (iq_level == null) {
			iq_level = "100";
		}

		Element query = new Element("query", new Element[] { new Element("num", iq_level),
				new Element("desc", calculateIQ(iq_level)) }, new String[] { "xmlns" },
						new String[] { XMLNS });

		return packet.okResult(query, 0);
	}
}


//~ Formatted in Tigase Code Convention on 13/03/12
