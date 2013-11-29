/*
 * MobileV3.java
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

import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.Date;
import tigase.db.NonAuthUserRepository;
import tigase.db.TigaseDBException;

import tigase.server.Iq;
import tigase.server.Packet;

import tigase.xml.Element;

import tigase.xmpp.*;

//~--- JDK imports ------------------------------------------------------------

import java.util.concurrent.ConcurrentHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Map;
import java.util.Queue;
import java.util.TimeZone;

/**
 * Class responsible for queuing packets (usable in connections from mobile
 * clients - power usage optimization) version 3
 *
 * @author andrzej
 */
public class MobileV3
				extends XMPPProcessor
				implements XMPPProcessorIfc, XMPPPacketFilterIfc {
	// default values
	private static final int    DEF_MAX_QUEUE_SIZE_VAL = 50;
	private static final String ID                     = "mobile_v3";
	private static final Logger log = Logger.getLogger(MobileV3.class.getCanonicalName());

	// keys
	private static final String     MAX_QUEUE_SIZE_KEY = "max-queue-size";
	private static final String     MOBILE_EL_NAME     = "mobile";
	private static final String     XMLNS = "http://tigase.org/protocol/mobile#v3";
	private static final String[][] ELEMENT_PATHS      = {
		{ Iq.ELEM_NAME, MOBILE_EL_NAME }
	};
	private static final String[]   XMLNSS             = { XMLNS };
	private static final Element[]  SUP_FEATURES = { new Element(MOBILE_EL_NAME,
			new String[] { "xmlns" }, new String[] { XMLNS }) };
	private static final String PRESENCE_QUEUE_KEY = ID + "-presence-queue";
	private static final String PACKET_QUEUE_KEY = ID + "-packet-queue";

	private static final String DELAY_ELEM_NAME = "delay";
	private static final String DELAY_XMLNS = "urn:xmpp:delay";
	private static final String MESSAGE_ELEM_NAME = "message";
	
	private static final ThreadLocal<Queue> prependResultsThreadQueue = new ThreadLocal<Queue>();
	
	//~--- fields ---------------------------------------------------------------

	private int maxQueueSize = DEF_MAX_QUEUE_SIZE_VAL;
	private SimpleDateFormat formatter;	
	{
		this.formatter = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'" );
		this.formatter.setTimeZone( TimeZone.getTimeZone( "UTC" ) );
	}
	
	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * 
	 */
	@Override
	public String id() {
		return ID;
	}

	/**
	 * Method description
	 *
	 *
	 * @param settings
	 *
	 * @throws TigaseDBException
	 */
	@Override
	public void init(Map<String, Object> settings) throws TigaseDBException {
		super.init(settings);

		Integer maxQueueSizeVal = (Integer) settings.get(MAX_QUEUE_SIZE_KEY);

		if (maxQueueSizeVal != null) {
			maxQueueSize = maxQueueSizeVal;
		}
	}

	/**
	 * Method description
	 *
	 *
	 * @param packet
	 * @param session
	 * @param repo
	 * @param results
	 * @param settings
	 */
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

				if (session.getSessionData(PRESENCE_QUEUE_KEY) == null) {

					// session.putSessionData(QUEUE_KEY, new
					// LinkedBlockingQueue<Packet>());
					session.putSessionData(PRESENCE_QUEUE_KEY, new ConcurrentHashMap<JID, Packet>());
				}
				if (session.getSessionData(PACKET_QUEUE_KEY) == null) {
					session.putSessionData(PACKET_QUEUE_KEY, new ArrayDeque<Packet>());
				}
				session.putSessionData(XMLNS, value);
				results.offer(packet.okResult((Element) null, 0));

				break;

			default :
				results.offer(Authorization.BAD_REQUEST.getResponseMessage(packet,
						"Mobile processing type is incorrect", false));
			}
		} catch (PacketErrorTypeException ex) {
			Logger.getLogger(MobileV3.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	/**
	 * Method description
	 *
	 *
	 * 
	 */
	@Override
	public String[][] supElementNamePaths() {
		return ELEMENT_PATHS;
	}

	/**
	 * Method description
	 *
	 *
	 * 
	 */
	@Override
	public String[] supNamespaces() {
		return XMLNSS;
	}

	/**
	 * Method description
	 *
	 *
	 * @param session
	 *
	 * 
	 */
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

	/**
	 * Method description
	 *
	 *
	 * @param _packet
	 * @param sessionFromSM
	 * @param repo
	 * @param results
	 */
	@Override
	@SuppressWarnings("unchecked")
	public void filter(Packet _packet, XMPPResourceConnection sessionFromSM,
			NonAuthUserRepository repo, Queue<Packet> results) {
		if ((sessionFromSM == null) ||!sessionFromSM.isAuthorized() || (results == null) ||
				(results.size() == 0)) {
			return;
		}
		
		Queue<Packet> prependResults = null;
		
		for (Iterator<Packet> it = results.iterator(); it.hasNext(); ) {
			Packet res = it.next();

			// check if packet contains destination
			if ((res == null) || (res.getPacketTo() == null)) {
				if (log.isLoggable(Level.FINEST)) {
					log.finest("packet without destination");
				}

				continue;
			}

			// get resource connection for destination
			XMPPResourceConnection session = sessionFromSM.getParentSession()
					.getResourceForConnectionId(res.getPacketTo());

			if (session == null) {
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "no session for destination {0} for packet {1}",
							new Object[] { res.getPacketTo().toString(),
							res.toString() });
				}

				// if there is no session we should not queue
				continue;
			}

			Map<JID, Packet> presenceQueue = (Map<JID, Packet>) session.getSessionData(PRESENCE_QUEUE_KEY);
			Queue<Packet> packetQueue = (Queue<Packet>) session.getSessionData(PACKET_QUEUE_KEY);
			
			QueueState state = QueueState.need_flush;
			if (!isQueueEnabled(session)) {
				if ((presenceQueue == null && packetQueue == null)
						|| (presenceQueue.isEmpty() && packetQueue.isEmpty())) {
					continue;
				}
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "mobile queues needs flushing - presences: {0}, packets: {1}", 
							new Object[] {presenceQueue.size(), packetQueue.size() });
				}
			} else {
				state = filter(session, res, presenceQueue, packetQueue);
				if (state == QueueState.queued) {
					it.remove();
					if (log.isLoggable(Level.FINEST))
						log.log(Level.FINEST, "queue packet = {0}", res.toString());
					if (presenceQueue.size() > maxQueueSize) {
						state = QueueState.need_flush;
					}
					else if (packetQueue.size() > maxQueueSize)
						state = QueueState.need_packet_flush;
				}
			}
			
			switch (state) {
				case need_flush:
					prependResults = prependResultsThreadQueue.get();
					if (prependResults == null) {
						prependResults = new ArrayDeque<Packet>();
						prependResultsThreadQueue.set(prependResults);
					}
					
					try {
						synchronized (presenceQueue) {
							JID connId = session.getConnectionId();
							for (Packet p : presenceQueue.values()) {
								// we need to set packet to again in case Stream
								// Management resumption happend in meanwhile
								p.setPacketTo(connId);
								prependResults.offer(p);
							}
							presenceQueue.clear();
						}
					}
					catch (NoConnectionIdException ex) {
						log.log(Level.SEVERE, "this should not happen", ex);
					}
					
				case need_packet_flush:
					if (prependResults == null) {
						prependResults = prependResultsThreadQueue.get();
						if (prependResults == null) {
							prependResults = new ArrayDeque<Packet>();
							prependResultsThreadQueue.set(prependResults);
						}
					}
					try {
						synchronized (packetQueue) {
							JID connId = session.getConnectionId();
							Packet p = null;
							while ((p = packetQueue.poll()) != null) {
								// we need to set packet to again in case Stream
								// Management resumption happend in meanwhile
								p.setPacketTo(connId);
								prependResults.offer(p);
							}
							packetQueue.clear();
						}
					}
					catch (NoConnectionIdException ex) {
						log.log(Level.SEVERE, "this should not happen", ex);
					}				
				case queued:					
					break;
					
				default:
					break;
			}
		}
		
		if (prependResults != null && !prependResults.isEmpty()) {
			if (log.isLoggable(Level.FINEST)) 
				log.log(Level.FINEST, "sending queued packets = {0}", prependResults.size());
			prependResults.addAll(results);
			results.clear();
			results.addAll(prependResults);
			prependResults.clear();
		}
	}

	/**
	 * Method description
	 *
	 *
	 * @param session
	 * @param res
	 * @param presenceQueue
	 *
	 * 
	 */
	private QueueState filter(XMPPResourceConnection session, Packet res, Map<JID,
			Packet> presenceQueue, Queue<Packet> packetQueue) {
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "checking if packet should be queued {0}", res.toString());
		}

		if (res.getElemName() == MESSAGE_ELEM_NAME) {
			List<Element> children = res.getElement().getChildren();
			for (Element child : children) {
				if (MessageCarbons.XMLNS.equals(child.getXMLNS())) {
					Element delay = res.getElement().getChild(DELAY_ELEM_NAME, DELAY_XMLNS);
					if (delay == null) {
						delay = createDelayElem(session);
						if (delay != null) {
							Element forward = child.getChild("forward", "urn:xmpp:forward:0");
							if (forward != null) {
								Element msg = forward.getChild(MESSAGE_ELEM_NAME);
								if (msg != null) {
									msg.addChild(delay);
								}
							}
						}
					}
					synchronized (packetQueue) {
						packetQueue.offer(res);
					}
					return QueueState.queued;
				}
			}
			return QueueState.need_packet_flush;
		}
		
		if (res.getElemName() != "presence") {
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "ignoring packet, packet is not presence:  {0}", res
						.toString());
			}

			return QueueState.need_packet_flush;
		}

		StanzaType type = res.getType();

		if ((type != null) && (type != StanzaType.unavailable) && (type != StanzaType
				.available)) {
			return QueueState.need_flush;
		}
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "queuing packet {0}", res.toString());
		}
		Element delay = res.getElement().getChild(DELAY_ELEM_NAME, DELAY_XMLNS);
		if (delay == null) {
			delay = createDelayElem(session);
			if (delay != null) {
				res.getElement().addChild(delay);
			}
		}
		synchronized (presenceQueue) {
			presenceQueue.put(res.getStanzaFrom(), res);
		}

		return QueueState.queued;
	}

	private Element createDelayElem(XMPPResourceConnection session) {
		String timestamp = null;
		synchronized (formatter) {
			timestamp = formatter.format(new Date());
		}
		
		try {
			return new Element(DELAY_ELEM_NAME, new String[] { "xmlns", "from", "stamp" }, 
					new String[] { DELAY_XMLNS, session.getBareJID().getDomain(), timestamp });
		}
		catch (NotAuthorizedException ex) {
			return null;
		}
	}
	
	//~--- get methods ----------------------------------------------------------

	/**
	 * Check if queuing is enabled
	 *
	 * @param session
	 * 
	 */
	protected static boolean isQueueEnabled(XMPPResourceConnection session) {
		Boolean enabled = (Boolean) session.getSessionData(XMLNS);

		return (enabled != null) && enabled;
	}
	
	private static enum QueueState {
		queued,
		need_flush,
		need_packet_flush
	}
}


//~ Formatted in Tigase Code Convention on 13/03/16
