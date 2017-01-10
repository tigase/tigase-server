/*
 * MobileV2.java
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
import tigase.server.Iq;
import tigase.server.Packet;
import tigase.xml.Element;
import tigase.xmpp.*;

import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

//~--- JDK imports ------------------------------------------------------------

/**
 * Class responsible for queuing packets (usable in connections from mobile
 * clients - power usage optimization) version 2
 *
 * @author andrzej
 */
public class MobileV2
				extends XMPPProcessor
				implements XMPPProcessorIfc, XMPPPacketFilterIfc, ClientStateIndication.Logic {
	// default values
	private static final int    DEF_MAX_QUEUE_SIZE_VAL = 50;
	private static final String ID                     = "mobile_v2";
	private static final Logger log = Logger.getLogger(MobileV2.class.getCanonicalName());

	// keys
	private static final String     MAX_QUEUE_SIZE_KEY = "max-queue-size";
	private static final String     MOBILE_EL_NAME     = "mobile";
	private static final String     XMLNS = "http://tigase.org/protocol/mobile#v2";
	private static final String[][] ELEMENT_PATHS      = {
		{ Iq.ELEM_NAME, MOBILE_EL_NAME }
	};
	private static final String[]   XMLNSS             = { XMLNS };
	private static final Element[]  SUP_FEATURES = { new Element(MOBILE_EL_NAME,
			new String[] { "xmlns" }, new String[] { XMLNS }) };
	private static final String QUEUE_KEY = ID + "-queue";

	//~--- fields ---------------------------------------------------------------

	private int maxQueueSize = DEF_MAX_QUEUE_SIZE_VAL;

	//~--- methods --------------------------------------------------------------

	@Override
	public String id() {
		return ID;
	}

	@Override
	public void init(Map<String, Object> settings) throws TigaseDBException {
		super.init(settings);

		Integer maxQueueSizeVal = (Integer) settings.get(MAX_QUEUE_SIZE_KEY);

		if (maxQueueSizeVal != null) {
			maxQueueSize = maxQueueSizeVal;
		}
	}

	@Override
	public void process(final Packet packet, final XMPPResourceConnection session,
			final NonAuthUserRepository repo, final Queue<Packet> results, final Map<String,
			Object> settings) {
		if (session == null) {
			return;
		}
		if (!session.isAuthorized()) {
			try {
				results.offer(session.getAuthState().getResponseMessage(packet,
						"Session is not yet authorized.", false));
			} catch (PacketErrorTypeException ex) {
				log.log(Level.FINEST,
						"ignoring packet from not authorized session which is already of type error");
			}

			return;
		}
		try {
			StanzaType type = packet.getType();

			switch (type) {
			case set :
				Element el       = packet.getElement().getChild(MOBILE_EL_NAME);
				String  valueStr = el.getAttributeStaticStr("enable");

				// if value is true queuing will be enabled
				boolean value = (valueStr != null) && ("true".equals(valueStr) || "1".equals(
						valueStr));

				if (value) {
					activate(session, results);
				} else {
					deactivate(session, results);
				}

				results.offer(packet.okResult((Element) null, 0));

				break;

			default :
				results.offer(Authorization.BAD_REQUEST.getResponseMessage(packet,
						"Mobile processing type is incorrect", false));
			}
		} catch (PacketErrorTypeException ex) {
			Logger.getLogger(MobileV2.class.getName()).log(Level.SEVERE, null, ex);
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

	@Override
	public Element[] supStreamFeatures(XMPPResourceConnection session) {
		if (session == null) {
			return null;
		}
		if (!session.isAuthorized()) {
			return null;
		}

		return SUP_FEATURES;
	}

	@Override
	public void activate(XMPPResourceConnection session, Queue<Packet> results) {
		if (session.getSessionData(QUEUE_KEY) == null) {

			// session.putSessionData(QUEUE_KEY, new
			// LinkedBlockingQueue<Packet>());
			session.putSessionData(QUEUE_KEY, new ConcurrentHashMap<JID, Packet>());
		}
		session.putSessionData(XMLNS, true);
	}

	@Override
	public void deactivate(XMPPResourceConnection session, Queue<Packet> results) {
		if (session.getSessionData(QUEUE_KEY) == null) {

			// session.putSessionData(QUEUE_KEY, new
			// LinkedBlockingQueue<Packet>());
			session.putSessionData(QUEUE_KEY, new ConcurrentHashMap<JID, Packet>());
		}
		session.putSessionData(XMLNS, false);

		flushQueue(session, results);
	}

	@Override
	@SuppressWarnings("unchecked")
	public void filter(Packet _packet, XMPPResourceConnection sessionFromSM,
			NonAuthUserRepository repo, Queue<Packet> results) {
		if ((sessionFromSM == null) ||!sessionFromSM.isAuthorized() || (results == null) ||
				(results.size() == 0)) {
			return;
		}
		for (Iterator<Packet> it = results.iterator(); it.hasNext(); ) {
			Packet res = it.next();

			// check if packet contains destination
			if ((res == null) || (res.getPacketTo() == null)) {
				if (log.isLoggable(Level.FINEST)) {
					log.finest("packet without destination");
				}

				continue;
			}

			// get parent session to look up for connection for destination
			XMPPSession parentSession = sessionFromSM.getParentSession();
			if (parentSession == null) {
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "no session for destination {0} for packet {1} - missing parent session",
							new Object[] { res.getPacketTo().toString(),
										   res.toString() });
				}
				continue;
			}

			// get resource connection for destination
			XMPPResourceConnection session = parentSession.getResourceForConnectionId(res.getPacketTo());
			if (session == null) {
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "no session for destination {0} for packet {1}",
							new Object[] { res.getPacketTo().toString(),
							res.toString() });
				}

				// if there is no session we should not queue
				continue;
			}

			// if queue is not enabled we do nothing
			if (!isQueueEnabled(session)) {
				if (log.isLoggable(Level.FINEST)) {
					log.finest("queue is no enabled");
				}
				flushQueue(session, results);

				continue;
			}

			Map<JID, Packet> queue = (Map<JID, Packet>) session.getSessionData(QUEUE_KEY);

			// lets check if packet should be queued
			if (filter(session, res, queue)) {
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "queuing packet = {0}", res.toString());
				}
				it.remove();
				if (queue.size() > maxQueueSize) {
					if (log.isLoggable(Level.FINEST)) {
						log.finest("sending packets from queue (OVERFLOW)");
					}
					for (Packet p : queue.values()) {
						results.offer(res);
					}
					queue.clear();

					// we are sending all items from map so there is no need
					// to filter it (fix for ConcurrentModificationException)
					break;
				}
			}
		}
	}

	/**
	 * Method description
	 *
	 *
	 * @param session
	 * @param res
	 * @param queue
	 *
	 * 
	 */
	public boolean filter(XMPPResourceConnection session, Packet res, Map<JID,
			Packet> queue) {
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "checking if packet should be queued {0}", res.toString());
		}
		if (res.getElemName() != "presence") {
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "ignoring packet, packet is not presence:  {0}", res
						.toString());
			}

			return false;
		}

		StanzaType type = res.getType();

		if ((type != null) && (type != StanzaType.unavailable) && (type != StanzaType
				.available)) {
			return false;
		}
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "queuing packet {0}", res.toString());
		}
		queue.put(res.getStanzaFrom(), res);

		return true;
	}

	//~--- get methods ----------------------------------------------------------

	protected void flushQueue(XMPPResourceConnection session, Queue<Packet> results) {
		Map<JID, Packet> queue = (Map<JID, Packet>) session.getSessionData(QUEUE_KEY);

		if ((queue != null) &&!queue.isEmpty()) {
			if (log.isLoggable(Level.FINEST)) {
				log.finest("sending packets from queue (DISABLED)");
			}
			for (Packet p : queue.values()) {
				try {
					// setting destination for packet in case if
					// stream was resumed and connId changed
					p.setPacketTo(session.getConnectionId());
					results.offer(p);
				} catch (NoConnectionIdException ex) {
					log.log(Level.FINEST, "should not happen, as connection is ready", ex);
				}
			}
			queue.clear();
		}
	}

	/**
	 * Check if queuing is enabled
	 *
	 * @param session
	 * 
	 */
	protected boolean isQueueEnabled(XMPPResourceConnection session) {
		Boolean enabled = (Boolean) session.getSessionData(XMLNS);

		return (enabled != null) && enabled;
	}
}


//~ Formatted in Tigase Code Convention on 13/03/16
