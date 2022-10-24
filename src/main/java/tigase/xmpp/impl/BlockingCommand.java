/*
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
package tigase.xmpp.impl;

import tigase.db.NonAuthUserRepository;
import tigase.db.TigaseDBException;
import tigase.eventbus.EventBus;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;
import tigase.server.Iq;
import tigase.server.Packet;
import tigase.server.xmppsession.SessionManager;
import tigase.util.stringprep.TigaseStringprepException;
import tigase.xml.Element;
import tigase.xmpp.*;
import tigase.xmpp.impl.annotation.*;
import tigase.xmpp.impl.roster.RosterAbstract;
import tigase.xmpp.impl.roster.RosterAbstract.SubscriptionType;
import tigase.xmpp.impl.roster.RosterFactory;
import tigase.xmpp.jid.JID;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * XEP-0191: Blocking Command. Based on privacy lists.
 *
 * @author Andrzej Wójcik
 * <br>
 * Originally submitted by:
 * @author Daniele Ricci
 * @author Behnam Hatami
 */
@Id(BlockingCommand.ID)
@DiscoFeatures({BlockingCommand.XMLNS})
@Handles({@Handle(path = {Iq.ELEM_NAME, BlockingCommand.BLOCKLIST}, xmlns = BlockingCommand.XMLNS),
		  @Handle(path = {Iq.ELEM_NAME, BlockingCommand.BLOCK}, xmlns = BlockingCommand.XMLNS),
		  @Handle(path = {Iq.ELEM_NAME, BlockingCommand.UNBLOCK}, xmlns = BlockingCommand.XMLNS)})
@HandleStanzaTypes({StanzaType.set, StanzaType.get})
@Bean(name = BlockingCommand.ID, parent = SessionManager.class, active = true)
public class BlockingCommand
		extends XMPPProcessorAbstract
		implements XMPPProcessorIfc {

	protected static final String XMLNS = "urn:xmpp:blocking";
	protected static final String XMLNS_ERRORS = XMLNS + ":errors";
	protected static final String ID = XMLNS;
	protected static final String BLOCKLIST = "blocklist";
	protected static final String BLOCK = "block";
	protected static final String UNBLOCK = "unblock";
	private static final Logger log = Logger.getLogger(BlockingCommand.class.getName());
	private static final String ITEM = "item";
	private static final String _JID = "jid";

	private final RosterAbstract roster_util = RosterFactory.getRosterImplementation(true);

	private Element[] DISCO_FEATURES;

	@Inject
	private EventBus eventBus;

	@Inject(nullAllowed = true)
	private List<SpamReportsConsumer> spamReportsConsumers = Collections.emptyList();

	@Override
	public Element[] supDiscoFeatures(XMPPResourceConnection session) {
		if (DISCO_FEATURES == null) {
			return super.supDiscoFeatures(session);
		} else {
			return DISCO_FEATURES;
		}
	}

	public void setSpamReportsConsumers(List<SpamReportsConsumer> spamReportsConsumers) {
		this.spamReportsConsumers = Optional.ofNullable(spamReportsConsumers).orElse(Collections.emptyList());
		if (spamReportsConsumers.isEmpty()) {
			DISCO_FEATURES = null;
		} else {
			DISCO_FEATURES = Stream.concat(Arrays.stream(super.supDiscoFeatures(null)),
										   Arrays.stream(SpamReportsConsumer.FEATURES)).toArray(Element[]::new);
		}
	}

	@Override
	public void process(Packet packet, XMPPResourceConnection session, NonAuthUserRepository repo,
						Queue<Packet> results, Map<String, Object> settings) throws XMPPException {
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
								results.offer(
										Authorization.FEATURE_NOT_IMPLEMENTED.getResponseMessage(packet, null, true));
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

	@Override
	public void processFromUserToServerPacket(JID connectionId, Packet packet, XMPPResourceConnection session,
											  NonAuthUserRepository repo, Queue<Packet> results,
											  Map<String, Object> settings) throws PacketErrorTypeException {
	}

	@Override
	public void processServerSessionPacket(Packet packet, XMPPResourceConnection session, NonAuthUserRepository repo,
										   Queue<Packet> results, Map<String, Object> settings)
			throws PacketErrorTypeException {
	}

	private void processGet(Packet packet, XMPPResourceConnection session, Queue<Packet> results)
			throws XMPPException, NotAuthorizedException, TigaseDBException {
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

	private void notifyPrivacyListChanged(XMPPResourceConnection session)
			throws NotAuthorizedException, TigaseDBException {
		String name = Privacy.getDefaultListName(session);
		if (name == null) {
			name = "default";
		}
		eventBus.fire(
				new JabberIqPrivacy.PrivacyListUpdatedEvent(session.getJID(), session.getJID().copyWithoutResource(),
															session.getParentSession(), name));
	}

	private void processSetBlock(Packet packet, Element e, XMPPResourceConnection session, Queue<Packet> results)
			throws NotAuthorizedException, TigaseDBException, PacketErrorTypeException {
		List<Entry> entries = collectEntries(e);
		if (entries != null && !entries.isEmpty() && entries.stream().filter(entry1 -> entry1.getJid() == null).findAny().isEmpty()) {
			Privacy.block(session, entries.stream().map(Entry::getJid).map(JID::toString).collect(Collectors.toList()));
			notifyPrivacyListChanged(session);
			for (Entry entry : entries) {
				sendBlockPresences(session, entry.getJid(), results);
			}
			results.offer(packet.okResult((Element) null, 0));
			sendPush(session.getParentSession(), packet, results);
			notifySpamReportsConsumers(entries);
		} else {
			results.offer(Authorization.BAD_REQUEST.getResponseMessage(packet, "Bad request", true));
		}
	}

	private void processSetUnblock(Packet packet, Element e, XMPPResourceConnection session, Queue<Packet> results)
			throws NotAuthorizedException, TigaseDBException {
		List<JID> jids = collectJids(e);
		if (jids == null || jids.isEmpty()) {
			List<String> jidsStr = Privacy.getBlocked(session);
			if (jidsStr != null) {
				Privacy.unblock(session, jidsStr);
				notifyPrivacyListChanged(session);
				for (String jid : jidsStr) {
					sendBlockPresences(session, JID.jidInstanceNS(jid), results);
				}
			}
		} else {
			Privacy.unblock(session, jids.stream().map(JID::toString).collect(Collectors.toList()));
			for (JID jid : jids) {
				sendUnblockPresences(session, jid, results);
			}
		}

		results.offer(packet.okResult((Element) null, 0));
		sendPush(session.getParentSession(), packet, results);
	}

	private List<Entry> collectEntries(Element el) {
		return el.mapChildren(Entry::new);
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

	private void notifySpamReportsConsumers(List<Entry> entries) {
		for (Entry entry : entries) {
			notifySpamReportsConsumers(entry);
		}
	}

	private void notifySpamReportsConsumers(Entry entry) {
		entry.getSpamReportType().ifPresent(reportType -> {
			for (SpamReportsConsumer consumer : spamReportsConsumers) {
				consumer.spamReportedFrom(entry.getJid().getBareJID(), reportType);
			}
		});
	}

	private void sendUnblockPresences(XMPPResourceConnection session, JID jid, Queue<Packet> results)
			throws NotAuthorizedException, TigaseDBException {
		if (jid.getLocalpart() == null && jid.getResource() == null) {
			JID[] buddies = roster_util.getBuddies(session);
			if (buddies != null) {
				for (JID buddy : buddies) {
					if (!jid.getDomain().equals(buddy.getDomain())) {
						continue;
					}
					if (buddy.getLocalpart() == null && buddy.getResource() == null) {
						continue;
					}
					sendUnblockPresences(session, buddy, results);
				}
			}
		}
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

	private void sendBlockPresences(XMPPResourceConnection session, JID jid, Queue<Packet> results)
			throws NotAuthorizedException, TigaseDBException {
		if (jid.getLocalpart() == null && jid.getResource() == null) {
			JID[] buddies = roster_util.getBuddies(session, EnumSet.of(SubscriptionType.both, SubscriptionType.to));
			if (buddies != null) {
				for (JID buddy : buddies) {
					if (!jid.getDomain().equals(buddy.getDomain())) {
						 continue;
					}
					if (buddy.getLocalpart() == null && buddy.getResource() == null) {
						continue;
					}
					sendBlockPresences(session, buddy, results);
				}
			}
		}
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

	private class Entry {
		private final JID jid;
		private final Optional<SpamReportsConsumer.ReportType> spamReportType;

		public Entry(Element element) {
			jid = JID.jidInstanceNS(element.getAttributeStaticStr("jid"));
			spamReportType = Optional.ofNullable(element.getChild("report", SpamReportsConsumer.XMLNS))
					.or(() -> Optional.ofNullable(element.getChild("report", SpamReportsConsumer.XMLNS1_PREFIX)))
					.map(SpamReportsConsumer.ReportType::fromReport);
		}

		public JID getJid() {
			return jid;
		}

		public Optional<SpamReportsConsumer.ReportType> getSpamReportType() {
			return spamReportType;
		}
	}

	private static class TigaseStringprepRuntimeException
			extends RuntimeException {

		public TigaseStringprepRuntimeException(String message, Throwable cause) {
			super(message, cause);
		}

	}
}
