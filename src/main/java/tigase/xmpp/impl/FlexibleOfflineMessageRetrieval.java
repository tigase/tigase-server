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

import tigase.db.MsgRepositoryIfc;
import tigase.db.NonAuthUserRepository;
import tigase.db.TigaseDBException;
import tigase.db.UserNotFoundException;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;
import tigase.server.Command;
import tigase.server.DataForm;
import tigase.server.Iq;
import tigase.server.Packet;
import tigase.server.amp.db.MsgRepository;
import tigase.server.xmppsession.SessionManager;
import tigase.util.stringprep.TigaseStringprepException;
import tigase.xml.Element;
import tigase.xmpp.*;
import tigase.xmpp.jid.JID;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static tigase.disco.XMPPService.INFO_XMLNS;
import static tigase.disco.XMPPService.ITEMS_XMLNS;
import static tigase.server.Message.ELEM_NAME;

@Bean(name = FlexibleOfflineMessageRetrieval.ID, parent = SessionManager.class, active = false)
public class FlexibleOfflineMessageRetrieval
		//		extends XMPPProcessor
		extends XMPPProcessorAbstract
		implements XMPPProcessorIfc {

	/** Field holds the xmlns of XEP-0013: Flexible offline messages retrieval */
	public static final String FLEXIBLE_OFFLINE_XMLNS = "http://jabber.org/protocol/offline";
	public static final String ITEM_ELEMENT_NAME = "item";
	public static final String NODE_ATTRIBUTE_NAME = "node";
	public static final String[] MESSAGE_EVENT_PATH = {ELEM_NAME, "event"};
	public static final String[] MESSAGE_HEADER_PATH = {ELEM_NAME, "header"};
	protected static final String ID = FLEXIBLE_OFFLINE_XMLNS;
	private static final Logger log = Logger.getLogger(FlexibleOfflineMessageRetrieval.class.getName());
	private static final String OFFLINE_ELEMENT_NAME = "offline";
	private static final String ITEM_ACTION_ATTRIBUTE = "action";
	private static final String PURGE_ELEMENT_NAME = "purge";
	private static final String FETCH_ELEMENT_NAME = "fetch";
	private static final String[] XMLNSS = {INFO_XMLNS, ITEMS_XMLNS, FLEXIBLE_OFFLINE_XMLNS};
	private static final String[] IQ_OFFLINE = {Iq.ELEM_NAME, OFFLINE_ELEMENT_NAME};
	private static final String[][] ELEMENTS = {Iq.IQ_QUERY_PATH, Iq.IQ_QUERY_PATH, IQ_OFFLINE};
	private static final Element[] DISCO_FEATURES = {
			new Element("feature", new String[]{"var"}, new String[]{FLEXIBLE_OFFLINE_XMLNS})};
	private static final Element identity = new Element("identity", new String[]{"category", "type"},
														new String[]{"automation", "message-list"});
	private static final Element feature = new Element("feature", new String[]{"var"},
													   new String[]{FLEXIBLE_OFFLINE_XMLNS});

	private static final String form_type = "FORM_TYPE";
	private static final String NUMBER_OF_ = "number_of_";
	private final MsgRepository.OfflineMessagesProcessor offlineMessagesStamper = new MsgStamper();
	@Inject
	private MsgRepositoryIfc msg_repo = null;

	@Override
	public Authorization canHandle(Packet packet, XMPPResourceConnection conn) {
		if (packet.isServiceDisco()) {
			if (packet.getStanzaTo() == null) {
				String node = packet.getAttributeStaticStr(Iq.IQ_QUERY_PATH, "node");
				if (FLEXIBLE_OFFLINE_XMLNS.equals(node)) {
					return Authorization.AUTHORIZED;
				}
			}
			return null;
		}
		return super.canHandle(packet, conn);
	}

	@Override
	public String id() {
		return ID;
	}

	;

	@Override
	public void processFromUserToServerPacket(JID connectionId, Packet packet, XMPPResourceConnection session,
											  NonAuthUserRepository repo, Queue<Packet> results,
											  Map<String, Object> settings) throws PacketErrorTypeException {
		Element query = packet.getElement().findChildStaticStr(Iq.IQ_QUERY_PATH);
		Element offlineElement = packet.getElement().findChildStaticStr(IQ_OFFLINE);

		if (null != query) {
			query = query.clone();
			// processing Service Discovery - info about number of elements and list of elements
			String queryXmlns = query.getXMLNS();
			final String node = query.getAttributeStaticStr(NODE_ATTRIBUTE_NAME);

			if (node != null && node.equals(FLEXIBLE_OFFLINE_XMLNS)) {
				// prevent restoring offline messags in other connections
				session.putCommonSessionData(FLEXIBLE_OFFLINE_XMLNS, FLEXIBLE_OFFLINE_XMLNS);
			}

			if (node != null && queryXmlns != null) {
				switch (queryXmlns) {
					case INFO_XMLNS:
						addDiscoInfo(session, query);
						break;
					case ITEMS_XMLNS:
						addDiscoItems(session, query);
						break;
				}
				results.offer(packet.okResult(query, 0));
			}

		} else if (null != offlineElement) {
			// processing retrieve/remove stored message/presence query
			final List<Element> offlineElementChildren = offlineElement.getChildren();
			List<Element> itemChildren = new LinkedList<Element>();
			boolean fetch = false;
			boolean purge = false;

			for (Element child : offlineElementChildren) {
				String name = child.getName();
				switch (name) {
					case ITEM_ELEMENT_NAME:
						itemChildren.add(child);
						break;
					case PURGE_ELEMENT_NAME:
						purge = true;
						break;
					case FETCH_ELEMENT_NAME:
						fetch = true;
						break;
				}
			}

			if (itemChildren.isEmpty() && (purge || fetch)) {
				// we don't have any items elements, only purge or fetch
				try {
					if (fetch && !purge) {
						Queue<Packet> restorePacketForOffLineUser = restorePacketForOffLineUser(null, session,
																								msg_repo);
						results.addAll(restorePacketForOffLineUser);
					} else if (purge && !fetch) {
						msg_repo.deleteMessagesToJID(null, session);
					}
				} catch (TigaseDBException | NotAuthorizedException ex) {
					log.log(Level.WARNING, "Problem retrieving messages from repository: ", ex);
				}
				results.offer(packet.okResult(query, 0));
			} else if (offlineElementChildren.size() == itemChildren.size()) {
				// ok, we have items elements and all of the children are items, no fetch or purge

				// check if all elements have same action (view/remove)
				List<String> itemsView = new LinkedList<>();
				List<String> itemsRemove = new LinkedList<>();
				for (Element item : itemChildren) {
					String actionString = item.getAttributeStaticStr(ITEM_ACTION_ATTRIBUTE);
					ACTION action = ACTION.valueOf(actionString.toLowerCase());
					switch (action) {
						case view:
							itemsView.add(item.getAttributeStaticStr(NODE_ATTRIBUTE_NAME));
							break;
						case remove:
							itemsRemove.add(item.getAttributeStaticStr(NODE_ATTRIBUTE_NAME));
							break;
					}
				}
				try {
					if (!itemsView.isEmpty() && itemsRemove.isEmpty()) {
						// ok, all items are 'view' type
						Queue<Packet> restorePacketForOffLineUser = restorePacketForOffLineUser(itemsView, session,
																								msg_repo);
						if (restorePacketForOffLineUser != null & !restorePacketForOffLineUser.isEmpty()) {
							results.addAll(restorePacketForOffLineUser);
							results.offer(packet.okResult(query, 0));
						} else {
							Packet err = Authorization.ITEM_NOT_FOUND.getResponseMessage(packet,
																						 "Requested item was not found",
																						 true);
							results.offer(err);
						}
					} else if (itemsView.isEmpty() && !itemsRemove.isEmpty()) {
						// ok, all items are 'remove' type
						int deleteMessagesToJID = msg_repo.deleteMessagesToJID(itemsRemove, session);
						if (deleteMessagesToJID == 0) {
							Packet err = Authorization.ITEM_NOT_FOUND.getResponseMessage(packet,
																						 "Requested item was not found",
																						 true);
							results.offer(err);
						} else {
							results.offer(packet.okResult(query, 0));
						}
					} else {
						Packet err = Authorization.NOT_ACCEPTABLE.getResponseMessage(packet,
																					 "All query items should have same action",
																					 true);
						results.offer(err);
					}
				} catch (UserNotFoundException | NotAuthorizedException ex) {
					log.log(Level.WARNING, "Problem retrieving messages from repository: ", ex);
				} catch (TigaseDBException ex) {
					results.offer(Authorization.INTERNAL_SERVER_ERROR.getResponseMessage(packet, null, true));
				}
			}
		}
	}

	public Queue<Packet> restorePacketForOffLineUser(List<String> db_ids, XMPPResourceConnection conn,
													 MsgRepositoryIfc repo)
			throws UserNotFoundException, NotAuthorizedException, TigaseDBException {
		Queue<Element> elems = repo.loadMessagesToJID(db_ids, conn, false, offlineMessagesStamper);

		if (elems != null) {
			LinkedList<Packet> pacs = new LinkedList<Packet>();
			Element elem = null;

			while ((elem = elems.poll()) != null) {
				try {
					final Packet packetInstance = Packet.packetInstance(elem);
					if (packetInstance.getElemName() == Iq.ELEM_NAME) {
						packetInstance.initVars(packetInstance.getStanzaFrom(), conn.getJID());
					} else {
						packetInstance.setPacketTo(conn.getConnectionId());
					}
					pacs.offer(packetInstance);
				} catch (TigaseStringprepException | NoConnectionIdException ex) {
					log.warning("Packet addressing problem, stringprep failed: " + elem);
				}
			}    // end of while (elem = elems.poll() != null)
			try {
				Collections.sort(pacs, new OfflineMessages.StampComparator());
			} catch (NullPointerException e) {
				try {
					log.warning("Can not sort off line messages: " + pacs + ",\n" + e);
				} catch (Exception exc) {
					log.log(Level.WARNING, "Can not print log message.", exc);
				}
			}

			return pacs;
		}

		return null;
	}

	@Override
	public void processServerSessionPacket(Packet packet, XMPPResourceConnection session, NonAuthUserRepository repo,
										   Queue<Packet> results, Map<String, Object> settings)
			throws PacketErrorTypeException {
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

	private void addDiscoInfo(XMPPResourceConnection session, Element query) {

		try {

			Map<Enum, Long> messagesCount = msg_repo.getMessagesCount(session.getJID());

			if (messagesCount != null && !messagesCount.isEmpty()) {
				query.addChild(identity);
				query.addChild(feature);

				DataForm.addDataForm(query, Command.DataType.result);
				DataForm.addHiddenField(query, form_type, FLEXIBLE_OFFLINE_XMLNS);

				for (Map.Entry<Enum, Long> entrySet : messagesCount.entrySet()) {
					DataForm.addFieldValue(query, NUMBER_OF_ + entrySet.getKey(), entrySet.getValue().toString());
				}
			}

		} catch (NotAuthorizedException ex) {
			log.log(Level.WARNING, "Problem retrieving messages from repository: ", ex);
		} catch (TigaseDBException ex) {

		}
	}

	private void addDiscoItems(XMPPResourceConnection session, Element query) {

		try {
			List<Element> messagesList = msg_repo.getMessagesList(session.getJID());
			if (null != messagesList && !messagesList.isEmpty()) {
				query.addChildren(messagesList);
			}
		} catch (NotAuthorizedException | TigaseDBException ex) {
			log.log(Level.WARNING, "Problem retrieving messages from repository: ", ex);
		}
	}

	private enum ACTION {

		view,
		remove
	}

	private static class MsgStamper
			implements MsgRepository.OfflineMessagesProcessor {

		public static Element offlineElementIns = new Element(OFFLINE_ELEMENT_NAME,
															  new Element[]{new Element(ITEM_ELEMENT_NAME)},
															  new String[]{"xmlns"},
															  new String[]{FLEXIBLE_OFFLINE_XMLNS});

		@Override
		public void stamp(Element msg, String msgID) {
			Element clone = offlineElementIns.clone();
			final Element item = clone.getChild(FlexibleOfflineMessageRetrieval.ITEM_ELEMENT_NAME);
			item.setAttribute(FlexibleOfflineMessageRetrieval.NODE_ATTRIBUTE_NAME, msgID);
			msg.addChild(clone);

		}

	}

}
