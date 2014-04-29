/*
 * C2SDeliveryErrorProcessor.java
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2014 "Tigase, Inc." <office@tigase.com>
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

import java.util.Map;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;
import tigase.db.NonAuthUserRepository;
import tigase.server.Packet;
import tigase.xml.Element;
import tigase.xmpp.JID;
import tigase.xmpp.NotAuthorizedException;
import tigase.xmpp.XMPPResourceConnection;

/**
 * Class implements static methods used to create packets to resend messages undelivered 
 * to client by C2S and methods used later to distinguish if packet was resent for redelivery 
 * 
 * @author andrzej
*/
public class C2SDeliveryErrorProcessor {
	
	private static final Logger log = Logger.getLogger(C2SDeliveryErrorProcessor.class.getCanonicalName());
	
	public static final String ELEM_NAME = "delivery-error";
	public static final String XMLNS = "http://tigase.org/delivery-error";
	
	/**
	 * Filters packets created by processors to remove delivery-error payload
	 * 
	 * @param packet
	 * @param session
	 * @param repo
	 * @param results
	 * @param toIgnore 
	 */
	public static void filter(Packet packet, XMPPResourceConnection session, NonAuthUserRepository repo, Queue<Packet> results, JID toIgnore) {
		for (Packet p : results) {
			if (p.getElemName() != tigase.server.Message.ELEM_NAME)
				continue;
			
			Element elem = p.getElement();
			Element error = elem.getChildStaticStr(ELEM_NAME);
			if (error != null && error.getXMLNS() == XMLNS) {
				// We are removing delivery-error payload for outgoing messages
				// to other components than with jid toIgnore
				if (toIgnore == null || !toIgnore.equals(packet.getPacketTo())) {
					elem.removeChild(error);
				}
			}
		}
	}
	
	/**
	 * Filters packets before they are processed by processors to filter out delivery-error 
	 * packets sent to bare jid if other connection is available
	 * 
	 * @param packet
	 * @param session
	 * @param repo
	 * @param results
	 * @param settings
	 * @return 
	 */
	public static boolean preProcess(Packet packet, XMPPResourceConnection session, NonAuthUserRepository repo, 
			Queue<Packet> results, Map<String, Object> settings) {
		if (packet.getElemName() != tigase.server.Message.ELEM_NAME)
			return false;

		if (!isDeliveryError(packet))
			return false;
		
		try {
			// We should ignore messages sent to bare jid if message contains delivery-error 
			// payload and other connection is currently active - this is needed to reduce
			// issues related to duplication of messages
			return (packet.getStanzaTo() != null && packet.getStanzaTo().getResource() == null 
					&& session != null && !session.getActiveSessions().isEmpty());
		} catch (NotAuthorizedException ex) {
			if (log.isLoggable(Level.FINEST)) {
				log.finest("NotAuthorizedException while processing undelivered message from "
					+ "C2S, packet = " + packet);
			}
		}

		return false;
	}
	
	/**
	 * Checks if packet is delivery-error packet
	 * 
	 * @param packet
	 * @return true - if packet is delivery-error
	 */
	public static boolean isDeliveryError(Packet packet) {
		Element elem = packet.getElement();
		Element error = elem.getChildStaticStr(ELEM_NAME);
		return error != null && error.getXMLNS() == XMLNS;
	}

	/**
	 * Creates delivery-error packets to send to session manager to reprocess
	 * undelivered packets
	 * 
	 * @param packet
	 * @return 
	 */
	public static Packet makeDeliveryError(Packet packet)  {
		Packet result = packet.copyElementOnly();
		result.setPacketFrom(packet.getPacketTo());
		result.getElement().addChild(new Element(ELEM_NAME, new String[] { "xmlns" }, new String[] { XMLNS }));
		return result;
	}
}
