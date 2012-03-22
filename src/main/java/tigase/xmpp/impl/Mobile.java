package tigase.xmpp.impl;

import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import tigase.db.NonAuthUserRepository;
import tigase.db.TigaseDBException;
import tigase.server.Packet;
import tigase.xml.Element;
import tigase.xmpp.*;

/**
 * Class responsible for queueing packets (usable in connections from mobile
 * clients - power usage optimalization)
 * 
 * @author andrzej
 */
public class Mobile extends XMPPProcessor implements XMPPProcessorIfc,
		XMPPPacketFilterIfc {

	private static final Logger log = Logger.getLogger(Mobile.class.getCanonicalName());
	private static final String MOBILE_EL_NAME = "mobile";
	private static final String XMLNS = "http://tigase.org/protocol/mobile";
	private static final String ID = MOBILE_EL_NAME;
	private static final String[] ELEMENTS = { MOBILE_EL_NAME };
	private static final String[] XMLNSS = { XMLNS };
	private static final Element[] SUP_FEATURES = { new Element(MOBILE_EL_NAME,
			new String[] { "xmlns" }, new String[] { XMLNS }) };

	// default values
	private static final int DEF_MAX_QUEUE_SIZE_VAL = 50;
	private static final long DEF_MAX_TIMEOUT_VAL = 6 * 60 * 1000;

	// keys
	private static final String LAST_TRANSFER_KEY = ID + "-last-transfer";
	private static final String MAX_QUEUE_SIZE_KEY = "max-queue-size";
	private static final String MAX_TIMEOUT_KEY = "max-timeout";
	private static final String QUEUE_KEY = ID + "-queue";
	private static final String TIMEOUT_KEY = ID + "-timeout";

	// ~--- fields ---------------------------------------------------------------

	private int maxQueueSize = DEF_MAX_QUEUE_SIZE_VAL;
	private long maxTimeout = DEF_MAX_TIMEOUT_VAL;

	/**
	 * Method description
	 * 
	 * 
	 * @return
	 */
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

		Long maxTimeoutVal = (Long) settings.get(MAX_TIMEOUT_KEY);
		if (maxTimeoutVal != null) {
			maxTimeout = maxTimeoutVal;
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
			final NonAuthUserRepository repo, final Queue<Packet> results,
			final Map<String, Object> settings) {
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
				case set:
					Element el = packet.getElement().getChild(MOBILE_EL_NAME);
					String valueStr = el.getAttribute("enable");

					// if value is true queuing will be enabled
					boolean value =
							valueStr != null && ("true".equals(valueStr) || "1".equals(valueStr));

					if (el.getAttribute("timeout") != null) {
						// we got timeout so we should set it for this session
						long timeout = Long.parseLong(el.getAttribute("timeout"));
						setTimeout(session, timeout);
					}

					session.putSessionData(XMLNS, value);
					if (session.getSessionData(QUEUE_KEY) == null) {
						session.putSessionData(QUEUE_KEY, new LinkedBlockingQueue<Packet>());
					}

					results.offer(packet.okResult((Element) null, 0));
					break;

				default:
					results.offer(Authorization.BAD_REQUEST.getResponseMessage(packet,
							"Mobile processing type is incorrect", false));
			}

		} catch (PacketErrorTypeException ex) {
			Logger.getLogger(Mobile.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	@Override
	public String[] supElements() {
		return ELEMENTS;
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
	public void filter(Packet packet, XMPPResourceConnection session,
			NonAuthUserRepository repo, Queue<Packet> results) {
		if ((session == null) || !session.isAuthorized() || (results == null)
				|| (results.size() == 0)) {
			return;
		}

		Queue<Packet> queue = (Queue<Packet>) session.getSessionData(QUEUE_KEY);

		// if queue is not enabled we do nothing
		if (!isQueueEnabled(session)) {
			if (queue != null && !queue.isEmpty()) {
				Packet res = null;
				while ((res = queue.poll()) != null) {
					results.offer(res);
				}
			}
			return;
		}

		for (Iterator<Packet> it = results.iterator(); it.hasNext();) {
			Packet res = it.next();

			try {
				if (session.getJID().equals(res.getStanzaFrom())) {
					continue;
				}
			} catch (NotAuthorizedException ex) {
				log.log(Level.WARNING, "session not authorized yet");
				continue;
			}

			if (res.getElemName() != "presence") {
				// maybe we should try to queue ping and disco#info?
				// if (res.getElemName() == "iq") {
				// if (res.getElement().getChild("ping", "urn:xmpp:ping") != null) {
				// queue.offer(res);
				// it.remove();
				// }
				// if (res.getType() == StanzaType.get
				// && res.getElement().getChild("query",
				// "http://jabber.org/protocol/disco#info") != null) {
				//
				// queue.offer(res);
				// it.remove();
				// }
				// }

				continue;
			}

			queue.offer(res);

			it.remove();
		}

		// now we need to check if we should send packets from queue
		if (!results.isEmpty() || queue.size() > maxQueueSize || isTimedOut(session)) {
			updateLastAccessTime(session);

			Packet res = null;
			while ((res = queue.poll()) != null) {
				results.offer(res);
			}
		}
	}

	/**
	 * Check if queuing is enabled
	 * 
	 * @param session
	 * @return
	 */
	protected boolean isQueueEnabled(XMPPResourceConnection session) {
		Boolean enabled = (Boolean) session.getSessionData(XMLNS);
		return enabled != null && enabled;
	}

	/**
	 * Check timeout for queue
	 * 
	 * @param session
	 * @return
	 */
	protected boolean isTimedOut(XMPPResourceConnection session) {
		Long lastAccessTime = (Long) session.getSessionData(LAST_TRANSFER_KEY);
		if (lastAccessTime == null) {
			return true;
		}

		if (lastAccessTime + getTimeout(session) < System.currentTimeMillis()) {
			return true;
		}

		return false;
	}

	/**
	 * Update last send time
	 * 
	 * @param session
	 */
	protected void updateLastAccessTime(XMPPResourceConnection session) {
		session.putSessionData(LAST_TRANSFER_KEY, System.currentTimeMillis());
	}

	/**
	 * Get timeout used for session queue
	 * 
	 * @param session
	 * @return
	 */
	private long getTimeout(XMPPResourceConnection session) {
		Long timeout = (Long) session.getSessionData(TIMEOUT_KEY);

		if (timeout == null) {
			return maxTimeout;
		}

		return timeout;
	}

	/**
	 * Set timeout for session queue
	 * 
	 * @param session
	 * @param timeout
	 */
	private void setTimeout(XMPPResourceConnection session, long timeout) {
		if (timeout == 0) {
			session.removeSessionData(TIMEOUT_KEY);
		} else {
			session.putSessionData(TIMEOUT_KEY, timeout);
		}
	}
}
