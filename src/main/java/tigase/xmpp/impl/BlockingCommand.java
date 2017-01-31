/*
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
 * Original ource code provided on 2015-03-08 (https://projects.tigase.org/issues/2819) by:
 * - Behnam Hatami
 * - Daniele Ricci
 * under the “Shared code ownership agreement” as described at the address:
 * https://projects.tigase.org/projects/tigase-server/wiki/Submitting_patches_and_other_code
 *
 */
package tigase.xmpp.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

import tigase.db.NonAuthUserRepository;
import tigase.db.TigaseDBException;
import tigase.server.Iq;
import tigase.server.Packet;
import tigase.util.TigaseStringprepException;
import tigase.xml.Element;
import tigase.xmpp.Authorization;
import tigase.xmpp.JID;
import tigase.xmpp.NoConnectionIdException;
import tigase.xmpp.NotAuthorizedException;
import tigase.xmpp.PacketErrorTypeException;
import tigase.xmpp.StanzaType;
import tigase.xmpp.XMPPException;
import tigase.xmpp.XMPPProcessorAbstract;
import tigase.xmpp.XMPPProcessorIfc;
import tigase.xmpp.XMPPResourceConnection;
import tigase.xmpp.XMPPSession;
import tigase.xmpp.impl.annotation.DiscoFeatures;
import tigase.xmpp.impl.annotation.Handle;
import tigase.xmpp.impl.annotation.HandleStanzaTypes;
import tigase.xmpp.impl.annotation.Handles;
import tigase.xmpp.impl.annotation.Id;
import tigase.xmpp.impl.roster.RosterAbstract;
import tigase.xmpp.impl.roster.RosterAbstract.SubscriptionType;
import tigase.xmpp.impl.roster.RosterFactory;

/**
 * XEP-0191: Blocking Command. Based on privacy lists.
 *
 * @author Andrzej Wójcik 
 * 
 * Originally submitted by:
 * @author Daniele Ricci
 * @author Behnam Hatami
 */
@Id(BlockingCommand.ID)
@DiscoFeatures({BlockingCommand.XMLNS})
@Handles({
	@Handle(path = {Iq.ELEM_NAME, BlockingCommand.BLOCKLIST}, xmlns = BlockingCommand.XMLNS),
	@Handle(path = {Iq.ELEM_NAME, BlockingCommand.BLOCK}, xmlns = BlockingCommand.XMLNS),
	@Handle(path = {Iq.ELEM_NAME, BlockingCommand.UNBLOCK}, xmlns = BlockingCommand.XMLNS)})
@HandleStanzaTypes({StanzaType.set, StanzaType.get})
public class BlockingCommand extends XMPPProcessorAbstract implements XMPPProcessorIfc {

	private static final Logger log = Logger.getLogger(BlockingCommand.class.getName());
	protected static final String XMLNS = "urn:xmpp:blocking";
	protected static final String XMLNS_ERRORS = XMLNS + ":errors";
	protected static final String ID = XMLNS;

	protected static final String BLOCKLIST = "blocklist";
	protected static final String BLOCK = "block";
	protected static final String UNBLOCK = "unblock";
	private static final String ITEM = "item";
	private static final String _JID = "jid";
	
	private final RosterAbstract roster_util = RosterFactory.getRosterImplementation(true);

	@Override
	public void process(Packet packet, XMPPResourceConnection session,
			NonAuthUserRepository repo, Queue<Packet> results,
			Map<String, Object> settings) throws XMPPException {
		if (session == null || packet.getElemName() != Iq.ELEM_NAME) {
			return;
		}

		StanzaType type = packet.getType();
		try {
			switch (type) {
				case get:
					processGet(packet, session, results);
					break;
				case set:
					Element e = packet.getElement().findChild(c -> c.getXMLNS() == XMLNS);
					if (e != null) {
						switch (e.getName()) {
							case BLOCK:
								processSetBlock(packet, e, session, results);
								break;
							case UNBLOCK:
								processSetUnblock(packet, e, session, results);
								break;
							default:
								results.offer(Authorization.FEATURE_NOT_IMPLEMENTED.getResponseMessage(packet, null, true));
						}
					}
					break;
				default:
					break;
			}
		} catch (TigaseStringprepRuntimeException ex) {
			results.offer(Authorization.BAD_REQUEST.getResponseMessage(packet, ex.getMessage(), true));
		} catch (TigaseDBException ex) {
			results.offer(Authorization.INTERNAL_SERVER_ERROR.getResponseMessage(packet, "Database error", true));
		}
	}

	private void processGet(Packet packet, XMPPResourceConnection session, Queue<Packet> results) throws XMPPException, NotAuthorizedException, TigaseDBException {
		if (packet.getElement().getChild(BLOCKLIST, XMLNS) != null) {
			Element list = new Element(BLOCKLIST);
			list.setXMLNS(XMLNS);
			List<String> jids = Privacy.getBlocked(session);
			if (jids != null) {
				for (String jid : jids) {
					list.addChild(new Element(ITEM, new String[]{_JID}, new String[]{jid}));
				}
			}
			session.putSessionData(ID, ID);
			results.offer(packet.okResult(list, 0));
		} else {
			results.offer(Authorization.BAD_REQUEST.getResponseMessage(packet, "Bad request", true));
		}
	}

	private void processSetBlock(Packet packet, Element e, XMPPResourceConnection session, Queue<Packet> results) throws NotAuthorizedException, TigaseDBException, PacketErrorTypeException {
		List<JID> jids = collectJids(e);
		if (jids != null && !jids.isEmpty()) {
			for (JID jid : jids) {
				Privacy.block(session, jid.toString());
				sendBlockPresences(session, jid, results);
			}
			results.offer(packet.okResult((Element) null, 0));
			sendPush(session.getParentSession(), packet, results);
		} else {
			results.offer(Authorization.BAD_REQUEST.getResponseMessage(packet, "Bad request", true));
		}
	}

	private void processSetUnblock(Packet packet, Element e, XMPPResourceConnection session, Queue<Packet> results) throws NotAuthorizedException, TigaseDBException {
		List<JID> jids = collectJids(e);
		if (jids == null || jids.isEmpty()) {
			List<String> jidsStr = Privacy.getBlocked(session);
			if (jidsStr != null) {
				jids = new ArrayList<>(jidsStr.size());
				for (String jidStr : jidsStr) {
					jids.add(JID.jidInstanceNS(jidStr));
				}
			}
		}	
		
		if (jids != null) {
			for (JID jid : jids) {
				Privacy.unblock(session, jid.toString());
				sendUnblockPresences(session, jid, results);
			}
		}
		results.offer(packet.okResult((Element) null, 0));
		sendPush(session.getParentSession(), packet, results);
	}
	
	private List<JID> collectJids(Element el) {
		return el.mapChildren(item -> {
			String jid = item.getAttributeStaticStr(_JID);
			try {
				return JID.jidInstance(jid);
			} catch (TigaseStringprepException ex) {
				throw new TigaseStringprepRuntimeException("Invalid JID: " + jid, ex);
			}
		});
	}

	private void sendUnblockPresences(XMPPResourceConnection session, JID jid, Queue<Packet> results) throws NotAuthorizedException, TigaseDBException {
		SubscriptionType stype = roster_util.getBuddySubscription(session, jid);
		if (stype == SubscriptionType.both || stype == SubscriptionType.from) {
			PresenceAbstract.sendPresence(StanzaType.probe, JID.jidInstance(session.getBareJID()), jid, results, null);
		}
		if (stype == SubscriptionType.both || stype == SubscriptionType.to) {
			List<XMPPResourceConnection> conns = session.getActiveSessions();
			if (conns != null) {
				for (XMPPResourceConnection conn : conns) {
					Element pres = conn.getPresence();
					if (pres != null) {
						PresenceAbstract.sendPresence(null, conn.getjid(), jid, results, pres);
					}
				}
			}
		}
	}

	private void sendBlockPresences(XMPPResourceConnection session, JID jid, Queue<Packet> results) throws NotAuthorizedException, TigaseDBException {
		SubscriptionType stype = roster_util.getBuddySubscription(session, jid);
		JID[] froms = session.getAllResourcesJIDs();
		if (stype == SubscriptionType.both || stype == SubscriptionType.to) {
			if (froms != null) {
				for (JID from : froms) {
					PresenceAbstract.sendPresence(StanzaType.unavailable, from, jid, results, null);
				}
			}
		}
	}

	private void sendPush(XMPPSession session, Packet packet, Queue<Packet> results) {
		for (XMPPResourceConnection conn : session.getActiveResources()) {
			if (conn.getSessionData(ID) == ID) {
				try {
					Packet result = packet.copyElementOnly();
					result.initVars(null, conn.getJID());
					result.setPacketTo(conn.getConnectionId());
					results.offer(result);
				} catch (NotAuthorizedException ex) {
					log.log(Level.FINEST, "failed to send push notification as session is not yet authorized");
				} catch (NoConnectionIdException ex) {
					log.log(Level.FINEST, "failed to send push notification as session is do not have connection id");
				}
			}
		}
	}
	
	@Override
	public void processFromUserToServerPacket(JID connectionId, Packet packet, XMPPResourceConnection session, NonAuthUserRepository repo, Queue<Packet> results, Map<String, Object> settings) throws PacketErrorTypeException {
	}

	@Override
	public void processServerSessionPacket(Packet packet, XMPPResourceConnection session, NonAuthUserRepository repo, Queue<Packet> results, Map<String, Object> settings) throws PacketErrorTypeException {
	}
	
	private static class TigaseStringprepRuntimeException extends RuntimeException {

		public TigaseStringprepRuntimeException(String message, Throwable cause) {
			super(message, cause);
		}
		
	}
}
