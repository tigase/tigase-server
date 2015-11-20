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
 * Source code provided on 2015-03-08 (https://projects.tigase.org/issues/2819) by:
 * - Behnam Hatami
 * - Daniele Ricci
 * under the “Shared code ownership agreement” as described at the address:
 * https://projects.tigase.org/projects/tigase-server/wiki/Submitting_patches_and_other_code
 *
 */

package tigase.xmpp.impl;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import tigase.db.NonAuthUserRepository;
import tigase.db.TigaseDBException;
import tigase.server.Iq;
import tigase.server.Packet;
import tigase.server.Permissions;
import tigase.server.Presence;
import tigase.util.TigaseStringprepException;
import tigase.xml.Element;
import tigase.xmpp.Authorization;
import tigase.xmpp.BareJID;
import tigase.xmpp.JID;
import tigase.xmpp.NoConnectionIdException;
import tigase.xmpp.NotAuthorizedException;
import tigase.xmpp.PacketErrorTypeException;
import tigase.xmpp.StanzaType;
import tigase.xmpp.XMPPPacketFilterIfc;
import tigase.xmpp.XMPPPreprocessorIfc;
import tigase.xmpp.XMPPProcessorAbstract;
import tigase.xmpp.XMPPProcessorIfc;
import tigase.xmpp.XMPPResourceConnection;
import tigase.xmpp.impl.annotation.DiscoFeatures;
import tigase.xmpp.impl.annotation.Handle;
import tigase.xmpp.impl.annotation.HandleStanzaTypes;
import tigase.xmpp.impl.annotation.Handles;
import tigase.xmpp.impl.annotation.Id;

/**
 * XEP-0191: Blocking Command. Based on privacy lists.
 *
 * @author Daniele Ricci
 * @author Behnam Hatami
 */

@Id(BlockingCommand.ID)
@DiscoFeatures({ BlockingCommand.XMLNS })
// @StreamFeatures({ @StreamFeature(elem = BlockingCommand.BLOCK_LIST, xmlns =
// BlockingCommand.XMLNS) })
@Handles({ @Handle(path = { Iq.ELEM_NAME, BlockingCommand.BLOCK_LIST }, xmlns = BlockingCommand.XMLNS),
		@Handle(path = { Iq.ELEM_NAME, BlockingCommand.BLOCK }, xmlns = BlockingCommand.XMLNS),
		@Handle(path = { Iq.ELEM_NAME, BlockingCommand.UNBLOCK }, xmlns = BlockingCommand.XMLNS) })
@HandleStanzaTypes({ StanzaType.set, StanzaType.get })
public class BlockingCommand extends XMPPProcessorAbstract implements XMPPProcessorIfc, XMPPPreprocessorIfc, XMPPPacketFilterIfc {

	private static Logger log = Logger.getLogger(BlockingCommand.class.getName());
	protected static final String XMLNS = "urn:xmpp:blocking";
	protected static final String XMLNS_ERRORS = XMLNS + ":errors";
	protected static final String ID = XMLNS;

	protected static final String BLOCK_LIST = "blocklist";
	protected static final String BLOCK = "block";
	protected static final String UNBLOCK = "unblock";
	protected static final String BLOCKED = "blocked";

	private static final String[] IQ_BLOCKLIST_PATH = { Iq.ELEM_NAME, BLOCK_LIST };
	private static final String[] IQ_BLOCK_PATH = { Iq.ELEM_NAME, BLOCK };
	private static final String[] IQ_UNBLOCK_PATH = { Iq.ELEM_NAME, UNBLOCK };

	private static final Element ERROR = new Element(BLOCKED, new String[] { Packet.XMLNS_ATT }, new String[] { XMLNS_ERRORS });

	private boolean allowed(Packet packet, XMPPResourceConnection session) {
		try {
			// If this is a preprocessing phase, always allow all packets to
			// make it possible for the client to communicate with the server.

			if (session.getConnectionId().equals(packet.getPacketFrom())) {
				return true;
			}
			// allow packets for clients originated by the server
			if (packet.getStanzaFrom() == null && session.getJID().equals(packet.getStanzaTo())) {
				return true;
			}
			// allow packets without from attribute and packets with from
			// attribute same as domain name
			if (packet.getStanzaFrom() != null && (packet.getStanzaFrom().getLocalpart() == null)
					&& session.getBareJID().getDomain().equals(packet.getStanzaFrom().getDomain())) {
				return true;
			}
			// allow packets without to attribute and packets with to attribute
			// same as domain name
			if ((packet.getStanzaTo() == null)
					|| ((packet.getStanzaTo().getLocalpart() == null) && session.getBareJID().getDomain().equals(packet.getStanzaTo().getDomain()))) {
				return true;
			}
			BareJID sessionUserId = session.getBareJID();
			BareJID jid = packet.getStanzaFrom() != null ? packet.getStanzaFrom().getBareJID() : null;
			if ((jid == null) || sessionUserId.equals(jid)) {
				jid = packet.getStanzaTo().getBareJID();
			}
			Set<BareJID> list = getBlocklist(session);
			if (list != null && list.contains(jid)) {
				if (log.isLoggable(Level.FINEST))
					log.log(Level.FINEST, "Packet not allowed: {0}", packet);
				return false;
			}
		} catch (NoConnectionIdException e) {
			// ignored
		} catch (NotAuthorizedException e) {
			// ignored
		} catch (TigaseDBException e) {
			log.log(Level.WARNING, "Database problem: " + e, e);
		}
		return true;
	}

	private String[] BareJIDSetToStringArray(Set<BareJID> list) {
		String[] result = new String[list.size()];
		int index = 0;
		for (BareJID bareJID : list)
			result[index++] = bareJID.toString();
		return result;
	}

	@Override
	public void filter(Packet packet, XMPPResourceConnection session, NonAuthUserRepository repo, Queue<Packet> results) {
		if ((session == null) || !session.isAuthorized() || (results == null) || (results.size() == 0)) {
			return;
		}

		Queue<Packet> errors = new ArrayDeque<Packet>(1);
		for (Iterator<Packet> it = results.iterator(); it.hasNext();) {
			Packet res = it.next();

			if (log.isLoggable(Level.FINEST))
				log.log(Level.FINEST, "Checking outbound packet: {0}", res);

			// Always allow presence unavailable to go, blocking command packets
			// and
			// all other which are allowed by block list rules
			if (res.getType() == StanzaType.unavailable || res.getType() == StanzaType.error || res.isXMLNSStaticStr(IQ_BLOCKLIST_PATH, XMLNS)
					|| res.isXMLNSStaticStr(IQ_BLOCK_PATH, XMLNS) || res.isXMLNSStaticStr(IQ_UNBLOCK_PATH, XMLNS) || allowed(res, session)) {
				continue;
			}
			try {
				if (res.getElemName() != Presence.ELEM_NAME) {
					Packet p = preventFromINFLoop(Authorization.NOT_ACCEPTABLE.getResponseMessage(res, null, true));
					p.getElement().getChild(StanzaType.error.name()).addChild(ERROR);
					errors.add(p);
				}
			} catch (PacketErrorTypeException e) {
			} // ignore
			it.remove();
		}
		results.addAll(errors);
	}

	/** Loads the user's block list. */
	private Set<BareJID> getBlocklist(XMPPResourceConnection session) throws NotAuthorizedException, TigaseDBException {

		@SuppressWarnings("unchecked")
		Set<BareJID> list = (Set<BareJID>) session.getCommonSessionData(BLOCK_LIST);

		if (list == null) {
			list = StringArrayToBareJIDSet(session.getDataList(ID, BLOCK_LIST));
			session.putCommonSessionData(BLOCK_LIST, list);
		}

		if (log.isLoggable(Level.FINEST))
			log.finest("blocking list of " + session.getUserName() + " is " + list.toString());

		return list;
	}

	private boolean handleBlocklistRequest(Packet packet, XMPPResourceConnection session, Queue<Packet> results) throws NotAuthorizedException,
			TigaseDBException, TigaseStringprepException {
		Element blocklist = new Element(BLOCK_LIST);
		blocklist.setXMLNS(XMLNS);

		Set<BareJID> list = getBlocklist(session);
		for (BareJID jid : list)
			blocklist.addChild(new Element("item", new String[] { "jid" }, new String[] { jid.toString() }));

		results.offer(packet.okResult(blocklist, 0));
		return true;
	}

	private void handleBlockRequest(Packet packet, XMPPResourceConnection session, Element block, Queue<Packet> results)
			throws NotAuthorizedException, TigaseDBException, TigaseStringprepException {
		int count = 0;
		List<Element> items = block.getChildren();
		Set<BareJID> list = null;
		if (items != null) {
			list = getBlocklist(session);

			for (Element item : items) {
				if ("item" == item.getName()) {
					String jidString = item.getAttributeStaticStr("jid");
					BareJID jid;
					if (jidString != null) {
						// if jid is not valid it will throw exception
						try {
							jid = BareJID.bareJIDInstance(jidString);

						} catch (TigaseStringprepException e) {
							// invalid jid
							throw new IllegalArgumentException("invalid jid: " + jidString);
						}
						count++;
						list.add(jid);
					}
				}
			}
		}

		if (count > 0) {
			setBlocklist(session, list);
			results.offer(packet.okResult((Element) null, 0));
		} else {
			// bad request
			throw new IllegalArgumentException();
		}
	}

	private void handleUnblockRequest(Packet packet, XMPPResourceConnection session, Element unblock, Queue<Packet> results)
			throws NotAuthorizedException, TigaseDBException, TigaseStringprepException {
		int count = 0;
		List<Element> items = unblock.getChildren();
		Set<BareJID> list = getBlocklist(session);
		if (items != null) {

			for (Element item : items) {
				if ("item" == item.getName()) {
					BareJID jid;
					String jidString = item.getAttributeStaticStr("jid");
					if (jidString != null) {
						// if jid is not valid it will throw exception
						try {
							jid = BareJID.bareJIDInstance(jidString);
						} catch (TigaseStringprepException e) {
							// invalid jid
							throw new IllegalArgumentException("invalid jid: " + jidString);
						}

						if (list.contains(jid)) {
							list.remove(jid);
							count++;
						}
					}
				}
			}
		} else {
			count = list.size();
			list.clear();
		}

		if (count > 0) {
			setBlocklist(session, list);
			results.offer(packet.okResult((Element) null, 0));
		} else {
			// bad request
			throw new IllegalArgumentException();
		}
	}

	@Override
	public boolean preProcess(Packet packet, XMPPResourceConnection session, NonAuthUserRepository repo, Queue<Packet> results,
			Map<String, Object> settings) {
		if ((session == null) || !session.isAuthorized() || packet.isXMLNSStaticStr(IQ_BLOCKLIST_PATH, XMLNS)
				|| packet.isXMLNSStaticStr(IQ_BLOCK_PATH, XMLNS) || packet.isXMLNSStaticStr(IQ_UNBLOCK_PATH, XMLNS)
				|| (packet.getType() == StanzaType.error && packet.getPermissions() == Permissions.TRUSTED)) {
			return false;
		}

		if (!allowed(packet, session)) {
			try {
				if (log.isLoggable(Level.FINEST))
					log.log(Level.FINEST, "Packet not allowed to come: {0}", packet);
				if (packet.getElemName() == Message.ELEM_NAME
						|| (packet.getElemName() == Iq.ELEM_NAME && (packet.getType() == StanzaType.get || packet.getType() == StanzaType.set)))
					results.offer(preventFromINFLoop(Authorization.SERVICE_UNAVAILABLE.getResponseMessage(packet, null, true)));
			} catch (PacketErrorTypeException e) {
			}
			packet.processedBy(ID);
			return true;
		} else
			return false;
	}

	@Override
	public void processFromUserToServerPacket(JID connectionId, Packet packet, XMPPResourceConnection session, NonAuthUserRepository repo,
			Queue<Packet> results, Map<String, Object> settings) throws PacketErrorTypeException {
		if (log.isLoggable(Level.FINEST))
			log.finest("Processing packet: " + packet.toString());
		packet.processedBy(ID);
		try {
			switch (packet.getType()) {
			case get:
				if (packet.getElement().getChild(BLOCK_LIST, XMLNS) != null) {
					handleBlocklistRequest(packet, session, results);
				} else {
					throw new IllegalArgumentException();
				}

				break;

			case set:
				Element block = packet.getElement().getChild(BLOCK, XMLNS);
				if (block != null) {
					handleBlockRequest(packet, session, block, results);
				}

				Element unblock = packet.getElement().getChild(UNBLOCK, XMLNS);
				if (unblock != null) {
					handleUnblockRequest(packet, session, unblock, results);
				}

				if (!packet.wasProcessedBy(ID)) {
					throw new IllegalArgumentException();
				}

				break;

			default:
				throw new IllegalArgumentException();
			}

		}

		catch (IllegalArgumentException e) {
			results.offer(Authorization.BAD_REQUEST.getResponseMessage(packet, "Bad request.", true));
		}

		catch (NotAuthorizedException e) {
			results.offer(Authorization.NOT_AUTHORIZED.getResponseMessage(packet, "You are not authorized to access block lists [" + e.getMessage()
					+ "]", true));
		} catch (TigaseDBException e) {
			log.warning("Database problem: " + e);
			results.offer(Authorization.INTERNAL_SERVER_ERROR.getResponseMessage(packet, "Database access problem, please contact administrator.",
					true));
		} catch (TigaseStringprepException e) {
			log.finest("Malformed JID: " + e);
			results.offer(Authorization.BAD_REQUEST.getResponseMessage(packet, "Malformed JID.", true));
		}
	}

	@Override
	public void processServerSessionPacket(Packet arg0, XMPPResourceConnection arg1, NonAuthUserRepository arg2, Queue<Packet> arg3,
			Map<String, Object> arg4) throws PacketErrorTypeException {
		// not used
	}

	/** Saves the user's block list. */
	private void setBlocklist(XMPPResourceConnection session, Set<BareJID> list) throws NotAuthorizedException, TigaseDBException {
		session.setDataList(ID, BLOCK_LIST, BareJIDSetToStringArray(list));
		session.putCommonSessionData(BLOCK_LIST, list);
	}

	private Set<BareJID> StringArrayToBareJIDSet(String[] list) {
		Set<BareJID> result = new ConcurrentSkipListSet<BareJID>();
		if (list != null) {
			for (String item : list)
				try {
					result.add(BareJID.bareJIDInstance(item));
				} catch (TigaseStringprepException e) {
					log.log(Level.WARNING, "Database incorrect data format", e);
				}
		}
		return result;
	}

	private static Packet preventFromINFLoop(Packet packet) {
		packet.setPacketTo(null);
		packet.setPermissions(Permissions.TRUSTED);
		return packet;
	}

}