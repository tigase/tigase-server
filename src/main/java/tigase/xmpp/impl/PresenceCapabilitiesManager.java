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

import tigase.disco.ServiceIdentity;
import tigase.server.DataForm;
import tigase.server.Iq;
import tigase.server.Packet;
import tigase.util.Base64;
import tigase.xml.Element;
import tigase.xmpp.StanzaType;
import tigase.xmpp.jid.JID;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PresenceCapabilitiesManager {

	public static final String CAPS_NODE = "https://tigase.net/tigase-xmpp-server";
	public final static String HASH_ALGORITHM = "SHA-1";
	public final static String charsetName = "UTF-8";
	// Map<capsNode,Set<feature>>
	private static final Map<String, String[]> nodeFeatures = new ConcurrentHashMap<String, String[]>(250);
	private static final ConcurrentMap<String, Set<String>> featureNodes = new ConcurrentHashMap<String, Set<String>>(
			250);
	private static final List<PresenceCapabilitiesListener> handlers = new CopyOnWriteArrayList<PresenceCapabilitiesListener>();
	private static long idCounter = 0;
	private static Logger log = Logger.getLogger(PresenceCapabilitiesManager.class.getName());

	private static MessageDigest addValues(String[] features, MessageDigest md) throws UnsupportedEncodingException {
		if (features != null) {
			Arrays.sort(features);

			for (String f : features) {
				md.update(f.getBytes(charsetName));
				md.update((byte) '<');
			}
		}
		return md;
	}

	public static String generateVerificationString(String[] identities, String[] features) {
		return generateVerificationString(identities, features, null);
	}

	public static String generateVerificationString(String[] identities, String[] features, Element extensions) {
		try {
			log.log(Level.FINEST, "Generating caps for identities: {0}, features: {1}, extensions: {2}",
					new String[]{Arrays.toString(identities), Arrays.toString(features), String.valueOf(extensions)});
			MessageDigest md = MessageDigest.getInstance(HASH_ALGORITHM);

			md = addValues(identities, md);
			md = addValues(features, md);

			if (extensions != null) {
				final Set<String> fields = DataForm.getFields(extensions);
				if (fields != null) {
					md.update(DataForm.getFormType(extensions).getBytes(charsetName));
					md.update((byte) '<');
					SortedSet<String> vars = new TreeSet<>(fields);
					for (String var : vars) {
						md.update(var.getBytes(charsetName));
						md.update((byte) '<');
						final String[] values = DataForm.getFieldValues(extensions, var);
						addValues(values, md);
					}
				}
			}

			byte[] digest = md.digest();
			return Base64.encode(digest);
		} catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
			log.warning("Cannot calculate verification string.");
		}
		return null;
	}

	public static String generateVerificationStringFromDiscoInfo(Element discoInfo) {
		String[] features = getFeaturesFromDiscoInfo(discoInfo);
		String[] identities = ServiceIdentity.getServiceIdentitiesCapsFromDiscoInfo(discoInfo);
		Element extension = discoInfo.getChild("x", "jabber:x:data");
		return generateVerificationString(identities, features, extension);
	}

	public static Element getCapsElement(String caps) {
		return new Element("c", new String[]{"xmlns", "hash", "node", "ver"},
						   new String[]{CAPS.XMLNS, HASH_ALGORITHM.toLowerCase(), CAPS_NODE, caps});
	}

	public static String[] getFeaturesFromDiscoInfo(Element discoInfo) {
		String[] features = null;
		final List<Element> featureElements = discoInfo.findChildren(child -> child.getName().equals("feature"));
		if (featureElements != null && !featureElements.isEmpty()) {
			List<String> list = new ArrayList<>();
			for (Element element : featureElements) {
				String var = element.getAttributeStaticStr("var");
				list.add(var);
			}
			features = list.toArray(new String[0]);
		}
		return features;
	}

	public static String[] getNodeFeatures(String capsNode) {
		return nodeFeatures.get(capsNode);
	}

	public static Set<String> getNodesWithFeature(String feature) {
		Set<String> nodes = featureNodes.get(feature);
		if (nodes == null) {
			return Collections.emptySet();
		} else {
			return Collections.unmodifiableSet(nodes);
		}
	}

	public static void handlePresence(JID owner, JID from, String[] capsNodes, Queue<Packet> results) {
		if (capsNodes == null) {
			return;
		}

		List<PresenceCapabilitiesListener> handlers = PresenceCapabilitiesManager.handlers;

		for (PresenceCapabilitiesListener handler : handlers) {
			handler.handlePresence(owner, from, capsNodes, results);
		}
	}

	public static void prepareCapsQueries(JID compJid, JID to, String[] caps_nodes, Queue<Packet> results) {
		if (caps_nodes != null) {
			for (String caps_node : caps_nodes) {
				if (!nodeFeatures.containsKey(caps_node)) {
					results.offer(prepareCapsQuery(to, compJid, caps_node));
				}
			}
		}
	}

	public static void prepareCapsQueriesEl(JID compJid, JID to, String[] caps_nodes, Queue<Element> results) {
		if (caps_nodes != null) {
			for (String caps_node : caps_nodes) {
				if (!nodeFeatures.containsKey(caps_node)) {
					results.offer(prepareCapsQueryEl(to, compJid, caps_node));
				}
			}
		}
	}

	public static Packet prepareCapsQuery(JID to, JID from, String node) {
		Element iq = prepareCapsQueryEl(to, from, node);
		final Iq iqPacket = new Iq(iq, from, to);
		iqPacket.setXMLNS(Packet.CLIENT_XMLNS);

		return iqPacket;
	}

	public static Element prepareCapsQueryEl(JID to, JID from, String node) {
		String id = String.valueOf(idCounter++);
		Element iq = new Element("iq", new String[]{"from", "to", "id", "type", Packet.XMLNS_ATT},
								 new String[]{from.toString(), to.toString(), id, "get", Packet.CLIENT_XMLNS});
		Element query = new Element("query", new String[]{"xmlns", "node"},
									new String[]{"http://jabber.org/protocol/disco#info", node});

		iq.addChild(query);

		return iq;
	}

	public static void processCapsQueryResponse(Packet packet) {

		// No need for checking to domain - processors and components should do this
//  if (VHostManager.isLocalDomainOrComponent(packet.getStanzaTo().getDomain())) {
		Element query = packet.getElement().getChild("query", "http://jabber.org/protocol/disco#info");

		if (query != null) {
			if (packet.getType() == StanzaType.result) {
				if (query.getAttributeStaticStr("node") == null) {
					if (log.isLoggable(Level.FINEST)) {
						log.finest("disco#info query without node attribute!");
					}

					return;
				}

				List<Element> ch = query.getChildren();

				if (ch != null) {
					Set<String> features = new ConcurrentSkipListSet<String>();

					for (Element item : ch) {
						if (!"feature".equals(item.getName())) {
							continue;
						}
						features.add(item.getAttributeStaticStr("var"));
					}
					setNodeFeatures(query.getAttributeStaticStr("node"), features.toArray(new String[features.size()]));
				}
			}

//      else if (packet.getType() == StanzaType.error && manager.getNodeFeatures(query.getAttribute("node")) == null) {
//          getInstance().setNodeFeatures(query.getAttribute("node"), NULL_NODES);
//      }
//      return;
		}

//  }
	}

	public static String[] processPresence(Element c) {
		Set<String> caps_nodes = null;

		if (c != null) {
			caps_nodes = new HashSet<String>();

			String caps_node = c.getAttributeStaticStr("node") + "#" + c.getAttributeStaticStr("ver");

			caps_nodes.add(caps_node);
			if ((c.getAttributeStaticStr("hash") == null) && (c.getAttributeStaticStr("ext") != null)) {
				for (String e : c.getAttributeStaticStr("ext").split(" ")) {
					caps_nodes.add(c.getAttributeStaticStr("node") + "#" + e);
				}
			}
		} else {
			return null;
		}

		return caps_nodes.toArray(new String[caps_nodes.size()]);
	}

	public static String[] processPresence(JID compJid, Packet p, Queue<Packet> results) {
		Element c = p.getElement().getChild("c");
		Set<String> features = new HashSet<String>();

		if (c != null) {
			String caps_node = c.getAttributeStaticStr("node") + "#" + c.getAttributeStaticStr("ver");

			// String[] nFeatures = nodeFeatures.get(caps_node);
			if (!nodeFeatures.containsKey(caps_node)) {
				Set<String> caps_nodes = new HashSet<String>();

				caps_nodes.add(caps_node);
				if ((c.getAttributeStaticStr("hash") == null) && (c.getAttributeStaticStr("ext") != null)) {
					for (String e : c.getAttributeStaticStr("ext").split(" ")) {
						caps_nodes.add(c.getAttributeStaticStr("node") + "#" + e);
					}
				}
				for (String node : caps_nodes) {
					if (!nodeFeatures.containsKey(node)) {
						results.offer(prepareCapsQuery(p.getFrom(), compJid, node));
					}
				}
			}
		}

		return features.toArray(new String[features.size()]);
	}

	public static void registerPresenceHandler(PresenceCapabilitiesListener handler) {
		handlers.add(handler);
	}

	public static void setNodeFeatures(String capsNode, String[] features) {
		if (log.isLoggable(Level.FINER)) {
			log.log(Level.FINER, "setting features for node = {0}, features = {1}",
					new Object[]{capsNode, Arrays.asList(features)});
		}
		Arrays.sort(features);
		nodeFeatures.put(capsNode, features);
		for (String feature : features) {
			Set<String> caps = featureNodes.get(feature);
			if (caps == null) {
				Set<String> tmp = new CopyOnWriteArraySet<String>();
				caps = featureNodes.putIfAbsent(feature, tmp);
				if (caps == null) {
					caps = tmp;
				}
			}
			caps.add(capsNode);
		}
	}

	public static void unregisterPresenceHandler(PresenceCapabilitiesListener handler) {
		handlers.remove(handler);
	}

	public static interface PresenceCapabilitiesListener {

		void handlePresence(JID owner, JID sender, String[] capsNodes, Queue<Packet> results);
	}
}

