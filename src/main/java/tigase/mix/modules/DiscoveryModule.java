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
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;
import tigase.mix.IMixComponent;
import tigase.mix.model.IMixRepository;
import tigase.mix.model.MixLogic;
import tigase.pubsub.repository.IPubSubRepository;
import tigase.server.Packet;
import tigase.xml.Element;
import tigase.xmpp.jid.BareJID;
import tigase.xmpp.jid.JID;
import tigase.xmpp.rsm.RSM;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;

@Bean(name = tigase.pubsub.modules.DiscoveryModule.ID, parent = IMixComponent.class, active = true)
public class DiscoveryModule extends tigase.pubsub.modules.DiscoveryModule {

	private static final Set<String> FEATURES = Set.of("urn:xmpp:mix:core:1", "urn:xmpp:mix:core:1#searchable", DISCO_ITEMS_XMLNS, DISCO_INFO_XMLNS);
	private static final Set<String> FEATURES_WITH_CREATE = Set.of("urn:xmpp:mix:core:1", "urn:xmpp:mix:core:1#searchable", "urn:xmpp:mix:core:1#create-channel", DISCO_ITEMS_XMLNS, DISCO_INFO_XMLNS);

	@Inject
	private MixLogic mixLogic;

	@Inject
	private IMixRepository mixRepository;

	@Inject
	private IPubSubRepository pubSubRepository;

	@Inject(nullAllowed = true)
	private RoomPresenceModule roomPresenceModule;

	@Override
	public Set<String> getAvailableFeatures(BareJID serviceJID, String node, BareJID senderJID) {
		if (serviceJID.getLocalpart() == null) {
			return mixLogic.isChannelCreationAllowed(serviceJID, senderJID) ? FEATURES_WITH_CREATE : FEATURES;
		} else {
			return super.getAvailableFeatures(serviceJID, node, senderJID);
		}
	}

	@Override
	protected Packet prepareDiscoInfoResponse(Packet packet, JID jid, String node, JID senderJID) {
		if (node == null && jid.getLocalpart() != null) {
			Element resultQuery = new Element("query", new String[]{"xmlns"},
											  new String[]{"http://jabber.org/protocol/disco#info"});

			Packet resultIq = packet.okResult(resultQuery, 0);
			Element mixIdentity = new Element("identity", new String[]{"category", "type"},
										   new String[]{"conference", "mix"});
			Optional<Element> mucIdentity = Optional.ofNullable(roomPresenceModule).map(x -> new Element("identity", new String[]{"category", "type"},
													   new String[]{"conference", "text"}));
			try {
				Optional.ofNullable(mixRepository.getChannelName(jid.getBareJID())).filter(s -> !s.isEmpty()).ifPresent(name -> {
					mixIdentity.setAttribute("name", name);
					mucIdentity.ifPresent(identity -> identity.setAttribute("name", name));
				});
			} catch (RepositoryException ex) {
				log.log(Level.FINEST, "Could not retrieve info for channel " + jid.toString(), ex);
			}
			resultQuery.addChild(mixIdentity);
			mucIdentity.ifPresent(resultQuery::addChild);

			for (String f : getAvailableFeatures(jid.getBareJID(), node, senderJID.getBareJID())) {
				resultQuery.addChild(new Element("feature", new String[]{"var"}, new String[]{f}));
			}
			Element form = component.getDiscoExtensionsForm(jid.getDomain());
			if (form != null) {
				resultQuery.addChild(form);
			}

			return resultIq;
		} else {
			return super.prepareDiscoInfoResponse(packet, jid, node, senderJID);
		}
	}

	@Override
	protected List<Element> prepareDiscoItems(JID serviceJID, String nodeName, JID senderJID, RSM rsm)
			throws ComponentException, RepositoryException {
		if (serviceJID.getLocalpart() == null) {
			List<BareJID> channels = getRepository().getServices(BareJID.bareJIDInstanceNS(serviceJID.getDomain()), true);
			if (rsm != null) {
				int count = channels.size();
				Integer begin = 0;
				Integer end = count;
				
				if (rsm.getAfter() != null) {
					for (int i=0; i<channels.size(); i++) {
						if (rsm.getAfter().equals(channels.get(i).toString())) {
							begin = i + 1;
							break;
						}
					}
				}
				if (rsm.hasBefore()) {
					if (rsm.getBefore() != null) {
						for (int i=begin; i<channels.size(); i++) {
							if (rsm.getBefore().equals(channels.get(i).toString())) {
								end = i + 1;
								break;
							}
						}
						if (begin < end) {
							int limit = Math.min(rsm.getMax(), end - begin);
							end = begin + limit;
						}
					} else {
						if (begin < count) {
							int limit =  Math.min(rsm.getMax(), end - begin);
							begin = end - limit;
						}
					}
				}
				if (begin < end) {
					channels = channels.subList(begin, end);
				} else {
					channels = Collections.emptyList();
				}
				if (rsm != null && !channels.isEmpty()) {
					rsm.setResults(count, channels.get(0).toString(),
								   channels.get(channels.size() - 1).toString());
					rsm.setIndex(begin);
				}
			}

			return channels.stream()
					.map(channelJID -> {
						Element el = new Element("item", new String[]{"jid"}, new String[]{channelJID.toString()});
						try {
							Optional.ofNullable(mixRepository.getChannelName(channelJID))
									.filter(s -> !s.isEmpty())
									.ifPresent(name -> el.setAttribute("name", name));
						} catch (RepositoryException ex) {
							log.log(Level.FINEST, "could not retrieve channel " + channelJID.toString() + " name", ex);
						}
						return el;
					})
					.collect(Collectors.toList());
		} else {
			return super.prepareDiscoItems(serviceJID, nodeName, senderJID, rsm);
		}
	}

}
