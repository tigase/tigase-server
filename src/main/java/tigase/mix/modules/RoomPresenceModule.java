/*
 * Tigase MIX - MIX component for Tigase
 * Copyright (C) 2020 Tigase, Inc. (office@tigase.com)
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
package tigase.mix.modules;

import tigase.component.exceptions.ComponentException;
import tigase.component.exceptions.RepositoryException;
import tigase.criteria.Criteria;
import tigase.criteria.ElementCriteria;
import tigase.eventbus.EventBus;
import tigase.eventbus.HandleEvent;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Initializable;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.UnregisterAware;
import tigase.mix.IMixComponent;
import tigase.mix.Mix;
import tigase.mix.model.*;
import tigase.pubsub.AbstractPubSubModule;
import tigase.pubsub.CollectionItemsOrdering;
import tigase.pubsub.exceptions.PubSubException;
import tigase.pubsub.modules.mam.PubSubQuery;
import tigase.pubsub.repository.IItems;
import tigase.server.Packet;
import tigase.util.datetime.TimestampHelper;
import tigase.util.stringprep.TigaseStringprepException;
import tigase.xml.Element;
import tigase.xmpp.Authorization;
import tigase.xmpp.StanzaType;
import tigase.xmpp.jid.BareJID;
import tigase.xmpp.jid.JID;

import java.io.Serializable;
import java.text.ParseException;
import java.util.Collection;
import java.util.Date;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

@Bean(name = "roomPresenceModule", parent = IMixComponent.class, active = true)
public class RoomPresenceModule
		extends AbstractPubSubModule implements Initializable, UnregisterAware {

	private static final Logger logger = Logger.getLogger(RoomPresenceModule.class.getCanonicalName());

	public static final String MUC_XMLNS = "http://jabber.org/protocol/muc";
	public static final String MUC_USER_XMLNS = MUC_XMLNS + "#user";

	private static final Criteria CRIT = ElementCriteria.name("presence");
	private static final String[] FEATURES = new String[] { MUC_XMLNS };

	private final TimestampHelper timestampHelper = new TimestampHelper(true);
	@Inject
	private EventBus eventBus;
	@Inject
	private MixLogic mixLogic;
	@Inject
	private IMixRepository mixRepository;
	@Inject
	private RoomPresenceRepository roomPresenceRepository;
	@Inject(nullAllowed = true)
	private MucMessageBroadcastFilter messageBroadcastFilter;

	@Override
	public boolean canHandle(Packet packet) {
		if (packet.getStanzaTo() == null || packet.getStanzaTo().getResource() == null) {
			return false;
		}
		return super.canHandle(packet);
	}

	@Override
	public String[] getFeatures() {
		return FEATURES;
	}

	@Override
	public Criteria getModuleCriteria() {
		return CRIT;
	}

	@Override
	public void process(Packet packet) throws ComponentException, TigaseStringprepException {
		if (packet.getStanzaTo() == null) {
			throw new ComponentException(Authorization.BAD_REQUEST);
		}
		BareJID channelJID = packet.getStanzaTo().getBareJID();
		String nick = packet.getStanzaTo().getResource();
		StanzaType type = Optional.ofNullable(packet.getType()).orElse(StanzaType.available);
		try {
			switch (type) {
				case available:
					Element x = packet.getElemChild("x", MUC_XMLNS);
					if (x == null) {
						// this is not a MUC-JOIN, ignore it..
						return;
					}
					if (join(channelJID, packet.getStanzaFrom(), nick)) {
						Element historyEl = x.getChild("history");
						PubSubQuery query = getRepository().newQuery();
						query.setComponentJID(JID.jidInstanceNS(channelJID));
						query.setQuestionerJID(packet.getStanzaFrom());
						query.getRsm().setHasBefore(true);
						query.setPubsubNode(Mix.Nodes.MESSAGES);
						if (historyEl != null) {
							if (!"0".equals(historyEl.getAttributeStaticStr("maxchars"))) {
								Optional.ofNullable(historyEl.getAttributeStaticStr("maxstanzas"))
										.map(this::parseIntOrNull)
										.ifPresent(value -> query.getRsm().setMax(value));
								Optional.ofNullable(historyEl.getAttributeStaticStr("since"))
										.map(this::parseTimestampOrNull)
										.ifPresent(query::setStart);
								if (query.getStart() == null) {
									Optional.ofNullable(historyEl.getAttributeStaticStr("seconds"))
											.map(this::parseIntOrNull)
											.map(s -> new Date(System.currentTimeMillis() - (s * 1000)))
											.ifPresent(query::setStart);
								}
							} else {
								query.getRsm().setMax(0);
							}
						}
						if (query.getRsm().getMax() > 0) {
							getRepository().queryItems(query, (q, item) -> {
								String senderNick = Optional.ofNullable(item.getMessage().getChild("mix"))
										.map(mix -> mix.getChild("nick"))
										.map(Element::getCData)
										.get();
								item.getMessage()
										.addChild(new Element("delay").withAttribute("xmlns", "urn:xmpp:delay")
														  .withAttribute("from", channelJID.toString())
														  .withAttribute("stamp", timestampHelper.formatWithMs(
																  item.getTimestamp())));
								packetWriter.write(Packet.packetInstance(item.getMessage(), JID.jidInstanceNS(channelJID, senderNick),
																		 packet.getStanzaFrom()));
							});
						}
					}
					break;
				case error:
				case unavailable:
					// we should ignore rest as they are processed and handled PubSub component presence module
					leave(channelJID, packet.getStanzaFrom(), nick);
					break;
				default:
					break;
			}
		} catch (RepositoryException ex) {
			throw new ComponentException(Authorization.INTERNAL_SERVER_ERROR, ex.getMessage(), ex);
		}
	}

	private Date parseTimestampOrNull(String str) {
		try {
			return timestampHelper.parseTimestamp(str);
		} catch (ParseException ex) {
			return null;
		}
	}

	private Integer parseIntOrNull(String str) {
		try {
			return Integer.parseInt(str);
		} catch (NumberFormatException ex) {
			return null;
		}
	}

	public void broadcastMessage(BareJID channelJID, String senderNick, Element message) {
		Element mixEl = message.getChild("mix");
		if (mixEl != null) {
			message.removeChild(mixEl);
		}

		eventBus.fire(new MucMessageBroadcastEvent(config.getComponentName(), channelJID, senderNick, message));
	}

	@HandleEvent
	public void handleBroadcastMessageEvent(MucMessageBroadcastEvent event) {
		if (!config.getComponentName().equals(event.componentName)) {
			return;
		}


		for (JID recipient : roomPresenceRepository.getRoomParticipantJids(event.getChannelJID())) {
			if (shouldBroadcast(recipient)) {
				logger.log(Level.FINEST, () -> "sending message from " + event.getChannelJID() + " + with id " +
						event.message.getAttributeStaticStr("id") + " to " + recipient);
				packetWriter.write(Packet.packetInstance(event.getMessage().clone(),
														 JID.jidInstanceNS(event.getChannelJID(),
																		   event.getSenderNick()), recipient));
			} else {
				logger.log(Level.FINEST, () -> "not sending message from " + event.getChannelJID() + " + with id " +
						event.message.getAttributeStaticStr("id") + " to " + recipient);
			}
		}
	}

	@HandleEvent
	public void handleKickoutEvent(RoomGhostbuster.KickoutEvent event) {
		if (!config.getComponentName().equals(event.getComponentName())) {
			return;
		}

		try {
			leave(event.getChannelJID(), event.getOccupantJID(), null);
		} catch (Exception ex) {
			// noting to do..
		}
	}

	@Override
	public void initialize() {
		eventBus.registerAll(this);
	}

	@Override
	public void beforeUnregister() {
		if (eventBus != null) {
			eventBus.unregisterAll(this);
		}
	}

	private boolean shouldBroadcast(JID recipient) {
		return messageBroadcastFilter == null || messageBroadcastFilter.shouldBroadcastMucMessage(recipient);
	}

	public void participantJoined(BareJID channelJID, JID occupant, String nick) {
		Collection<JID> participants = roomPresenceRepository.getRoomParticipantJids(channelJID);
		if (!participants.isEmpty()) {
			Element presence = preparePresence(true, false);
			for (JID recipient : participants) {
				if (occupant != null && occupant.equals(recipient)) {
					continue;
				}
				packetWriter.write(Packet.packetInstance(new Element(presence), JID.jidInstanceNS(channelJID, nick), recipient));
			}
		}
	}

	public void participantLeft(BareJID channelJID, JID occupant, String nick) {
		Collection<JID> participants = roomPresenceRepository.getRoomParticipantJids(channelJID);
		if (!participants.isEmpty()) {
			Element presence = preparePresence(false, false);
			for (JID recipient : participants) {
				if (occupant != null && occupant.equals(recipient)) {
					continue;
				}
				packetWriter.write(Packet.packetInstance(new Element(presence), JID.jidInstanceNS(channelJID, nick), recipient));
			}
		}
	}

	private boolean join(BareJID channelJID, JID occupantJID, String nick) throws ComponentException, RepositoryException {
		if (nick == null) {
			throw new ComponentException(Authorization.JID_MALFORMED);
		}

		ChannelConfiguration config = mixRepository.getChannelConfiguration(channelJID);
		if (config == null) {
			throw new PubSubException(Authorization.ITEM_NOT_FOUND);
		}

		mixLogic.checkPermission(channelJID, occupantJID.getBareJID(), MixAction.join);

		if (roomPresenceRepository.isNicknameInUse(channelJID, occupantJID, nick)) {
			throw new ComponentException(Authorization.CONFLICT);
		}

		String participantId = mixLogic.generateTempParticipantId(channelJID, occupantJID);
		IParticipant participant = mixRepository.getParticipant(channelJID, participantId);
		if (participant == null || !nick.equals(participant.getNick())) {
			mixRepository.updateTempParticipant(channelJID, occupantJID, nick);
		}

		roomPresenceRepository.addTempParticipant(channelJID, occupantJID, nick);

		IItems items = getRepository().getNodeItems(channelJID, Mix.Nodes.PARTICIPANTS);
		if (items != null) {
			String[] itemsIds = items.getItemsIds(CollectionItemsOrdering.byUpdateDate);
			if (itemsIds != null) {
				for (String itemId : itemsIds) {
					if (itemId.equals(participantId)) {
						continue;
					}
					Optional.ofNullable(mixRepository.getParticipant(channelJID, itemId))
							.map(p -> Packet.packetInstance(preparePresence(true, false),
															JID.jidInstanceNS(channelJID, p.getNick()), occupantJID))
							.ifPresent(packetWriter::write);
				}
			}
		}

		participantJoined(channelJID, occupantJID, nick);

		packetWriter.write(Packet.packetInstance(preparePresence(true, true), JID.jidInstanceNS(channelJID, nick), occupantJID));

		return true;
	}

	private void leave(BareJID channelJID, JID occupantJID, String nick) throws ComponentException, RepositoryException {
		String participantId = mixLogic.generateTempParticipantId(channelJID, occupantJID);
		IParticipant participant = mixRepository.getParticipant(channelJID, participantId);
		if (participant != null) {
			mixRepository.removeTempParticipant(channelJID, occupantJID);
			roomPresenceRepository.removeTempParticipant(channelJID, occupantJID, nick);
			participantLeft(channelJID, occupantJID, participant.getNick());
			packetWriter.write(Packet.packetInstance(preparePresence(false, true),
													 JID.jidInstanceNS(channelJID, participant.getNick()),
													 occupantJID));
		}
	}

	private Element preparePresence(boolean enter, boolean self) {
		Element presence = new Element("presence");
		if (!enter) {
			presence.withAttribute("type", "unavailable");
		}

		return presence.withElement("x", MUC_USER_XMLNS, (el) -> {
			el.withElement("item", item -> {
				item.withAttribute("affiliation", "member").withAttribute("role", enter ? "participant" : "none");
			});
			if (self) {
				el.withElement("status", (status) -> status.withAttribute("code", "110"));
			}
		});
	}

	public static class MucMessageBroadcastEvent implements Serializable {

		private String componentName;
		private BareJID channelJID;
		private String senderNick;
		private Element message;

		public MucMessageBroadcastEvent() {
		}

		public MucMessageBroadcastEvent(String componentName, BareJID channelJID, String senderNick, Element message) {
			this.componentName = componentName;
			this.channelJID = channelJID;
			this.senderNick = senderNick;
			this.message = message;
		}

		public BareJID getChannelJID() {
			return channelJID;
		}

		public void setChannelJID(BareJID channelJID) {
			this.channelJID = channelJID;
		}

		public String getSenderNick() {
			return senderNick;
		}

		public void setSenderNick(String senderNick) {
			this.senderNick = senderNick;
		}

		public Element getMessage() {
			return message;
		}

		public void setMessage(Element message) {
			this.message = message;
		}
	}

	@FunctionalInterface
	public interface MucMessageBroadcastFilter {

		boolean shouldBroadcastMucMessage(JID recipient);

	}
}
