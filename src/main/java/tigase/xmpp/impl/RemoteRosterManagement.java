/*
 * RemoteRosterManagement.java
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2012 "Artur Hefczyc" <artur.hefczyc@tigase.org>
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

import tigase.form.*;

import tigase.server.Packet;

import tigase.xml.Element;

import tigase.xmpp.Authorization;
import tigase.xmpp.JID;
import tigase.xmpp.NotAuthorizedException;
import tigase.xmpp.PacketErrorTypeException;
import tigase.xmpp.XMPPProcessorAbstract;
import tigase.xmpp.XMPPResourceConnection;

//~--- JDK imports ------------------------------------------------------------

import java.util.Collections;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 *
 * @author andrzej
 */
public class RemoteRosterManagement
				extends XMPPProcessorAbstract {
	private static final String[][] ELEMENT_PATHS = {{ "iq", "query" }, { "message", "x" }};
	private static final String ID                = "remote-roster-management";
	private static final Logger log               =
		Logger.getLogger("eu.hilow.xtigase.server.xmpp.RemoteRosterManagement");
	private static final String XMLNS             = "http://spectrum.im/protocol/remote-roster";
	private static final String[] XMLNSS          = { XMLNS, "jabber:x:data" };

	//~--- methods --------------------------------------------------------------

	@Override
	public String id() {
		return ID;
	}

	@Override
	public void processFromUserToServerPacket(JID connectionId, Packet packet,
					XMPPResourceConnection session, NonAuthUserRepository repo,
					Queue<Packet> results, Map<String, Object> settings)
					throws PacketErrorTypeException {
		if (packet.getElemName() == "message") {
			try {
				processMessageFormResponse(packet, session, repo, results);
			} catch (NotAuthorizedException ex) {
				results.offer(Authorization.FORBIDDEN.getResponseMessage(packet, null, false));
			} catch (TigaseDBException ex) {
				results.offer(Authorization.INTERNAL_SERVER_ERROR.getResponseMessage(packet,
								null, true));
			}
		} else {
			results.offer(packet);
		}
	}

	@Override
	public void processServerSessionPacket(Packet packet, XMPPResourceConnection session,
					NonAuthUserRepository repo, Queue<Packet> results, Map<String, Object> settings)
					throws PacketErrorTypeException {}

	@Override
	public void processToUserPacket(Packet packet, XMPPResourceConnection session,
																	NonAuthUserRepository repo, Queue<Packet> results,
																	Map<String, Object> settings)
					throws PacketErrorTypeException {
		if (packet.getElemName() == "iq") {
			processIq(packet, session, repo, results, settings);
		} else {
			super.processToUserPacket(packet, session, repo, results, settings);
		}
	}

	private void processIq(Packet packet, XMPPResourceConnection session,
												 NonAuthUserRepository repo, Queue<Packet> results,
												 Map<String, Object> settings)
					throws PacketErrorTypeException {
		Element query = packet.getElement().getChild("query",
											"http://spectrum.im/protocol/remote-roster");

		if (query == null) {
			super.processToUserPacket(packet, session, repo, results, settings);

			// log.warn("processing IQ with bad query = {}", packet.toString());
			return;
		}
		switch (packet.getType()) {
		case set :
			if ("request".equals(query.getAttributeStaticStr(Packet.TYPE_ATT))) {
				JID from    = JID.jidInstanceNS(packet.getStanzaTo().getDomain());
				Element msg = new Element(tigase.server.Message.ELEM_NAME);

				msg.setAttribute(Packet.FROM_ATT, from.toString());
				msg.setAttribute(Packet.TO_ATT, packet.getStanzaTo().toString());

				Form form = new Form(Packet.FROM_ATT, "Roster change permission",
														 packet.getStanzaFrom().getBareJID().toString() +
														 " wants to edit your roster with following reason: " +
														 query.getAttributeStaticStr("reason") +
														 ". Do you want to allow it?");

				form.addField(Field.fieldHidden("FORM_TYPE", XMLNS));
				form.addField(Field.fieldHidden("jid",
																				packet.getStanzaFrom().getBareJID().toString()));
				form.addField(Field.fieldBoolean("answer", true, "Allow roster edit"));
				msg.addChildren(Collections.singletonList(form.getElement()));

				Packet resp = Packet.packetInstance(msg, from, packet.getStanzaTo());

				results.offer(resp);
			} else {
				results.offer(Authorization.BAD_REQUEST.getResponseMessage(packet,
								"Unknown request type", false));
			}

			break;

		default :
			results.offer(Authorization.BAD_REQUEST.getResponseMessage(packet, "Bad request",
							true));
		}
	}

	private void processMessageFormResponse(Packet packet, XMPPResourceConnection session,
					NonAuthUserRepository repo, Queue<Packet> results)
					throws NotAuthorizedException, TigaseDBException {
		Element x = packet.getElement().getChild("x");

		if (x != null) {
			Form form        = new Form(x);
			if (!XMLNS.equals(form.getAsString("FORM_TYPE")) 
					&& !session.isLocalDomain(packet.getStanzaTo().toString(), false)) {				
				results.offer(packet);
			}
			JID jid          = JID.jidInstanceNS(form.get("jid").getValue());
			Boolean answer   = false;
			String answerStr = form.get("answer").getValue();

			if ("true".equals(answerStr) || "1".equals(answerStr)) {
				answer = true;
			}
			synchronized (session) {
				Set<JID> allowed = getAllowed(session);

				// Apply result of form
				if (answer) {
					allowed.add(jid);
				} else {
					allowed.remove(jid);
				}
				setAllowed(session, allowed);
			}

			// Notify remote jid about answer
			Element iq = new Element("iq");

			iq.setAttribute("from", packet.getStanzaFrom().getBareJID().toString());
			iq.setAttribute("to", jid.toString());

			Element query = new Element("query");

			query.setAttribute("xmlns", XMLNS);
			query.setAttribute("type", answer
																 ? "allowed"
																 : "rejected");
			iq.addChildren(Collections.singletonList(query));
			results.offer(Packet.packetInstance(iq,
							packet.getStanzaFrom().copyWithoutResource(), jid));
		} else {
			results.offer(packet);
		}
	}

	@Override
	public String[][] supElementNamePaths() {
		return ELEMENT_PATHS;
	}
	
	@Override
	public String[] supNamespaces() {
		return XMLNSS;
	}

	//~--- get methods ----------------------------------------------------------

	@SuppressWarnings("unchecked")
	private static Set<JID> getAllowed(XMPPResourceConnection session)
					throws NotAuthorizedException, TigaseDBException {

		// Get set of allowed jids
		synchronized (session) {
			Set<JID> allowed = (Set<JID>) session.getCommonSessionData("remote-roster-allowed");

			if (allowed == null) {
				String data = session.getData("remote-roster-management", "allowed", null);

				allowed = new HashSet<JID>();
				if (data != null) {
					if (data.contains("/")) {
						String[] jidsStr = data.split("/");

						for (String jidStr : jidsStr) {
							if (jidStr.length() > 0) {
								allowed.add(JID.jidInstanceNS(jidStr));
							}
						}
					} else {
						allowed.add(JID.jidInstanceNS(data));
					}
				}
				session.putCommonSessionData("remote-roster-allowed", allowed);
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "read list of jids allowed to modify roster = {0}",
									allowed);
				}
			}

			return allowed;
		}
	}

	//~--- set methods ----------------------------------------------------------

	private static void setAllowed(XMPPResourceConnection session, Set<JID> allowed)
					throws NotAuthorizedException, TigaseDBException {

		// Save set of allowed jids
		StringBuilder buf = new StringBuilder(1024);

		session.putCommonSessionData("remote-roster-allowed", allowed);

		boolean first = true;

		for (JID jid : allowed) {
			if (!first) {
				buf.append("/");
			} else {
				first = false;
			}
			buf.append(jid.toString());
		}
		session.setData("remote-roster-management", "allowed", buf.toString());
	}

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param jid
	 * @param session
	 *
	 * 
	 */
	public static boolean isRemoteAllowed(JID jid, XMPPResourceConnection session) {
		try {
			if (session == null) {
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "no session to check if {0} is allowed", jid.toString());
				}

				return false;
			}

			Set<JID> allowed = getAllowed(session);

			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "checking if sender jid = {0} is in allowed set = {1}",
								new Object[] { jid,
															 allowed });
			}

			return allowed.contains(jid);
		} catch (Exception ex) {
			return false;
		}
	}

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param session
	 * @param item
	 * @param update
	 * @param results
	 *
	 * @throws NotAuthorizedException
	 * @throws TigaseDBException
	 */
	public static void updateBuddyChange(XMPPResourceConnection session, Element item,
					Element update, Queue<Packet> results)
					throws NotAuthorizedException, TigaseDBException {
		JID jid = JID.jidInstanceNS(item.getAttributeStaticStr("jid"));

		if (jid.getLocalpart() == null) {
			return;
		}
		jid = JID.jidInstanceNS(jid.getDomain());
		if (isRemoteAllowed(jid, session)) {
			Element iq = update.clone();

			iq.setAttribute("from", session.getBareJID().toString());
			iq.setAttribute("to", jid.getDomain());
			iq.setAttribute("id", "rst" + session.nextStanzaId());
			results.offer(Packet.packetInstance(iq, JID.jidInstance(session.getBareJID()),
							jid));
		}
	}
}


//~ Formatted in Tigase Code Convention on 13/02/20
