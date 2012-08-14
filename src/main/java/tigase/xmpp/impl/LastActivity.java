package tigase.xmpp.impl;

import java.util.Map;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

import tigase.db.NonAuthUserRepository;
import tigase.db.TigaseDBException;
import tigase.db.UserNotFoundException;
import tigase.server.Packet;
import tigase.xml.Element;
import tigase.xmpp.Authorization;
import tigase.xmpp.BareJID;
import tigase.xmpp.JID;
import tigase.xmpp.NotAuthorizedException;
import tigase.xmpp.PacketErrorTypeException;
import tigase.xmpp.StanzaType;
import tigase.xmpp.XMPPException;
import tigase.xmpp.XMPPProcessorAbstract;
import tigase.xmpp.XMPPResourceConnection;
import tigase.xmpp.XMPPStopListenerIfc;

/**
 * Implementation of <a
 * href='http://xmpp.org/extensions/xep-0012.html'>XEP-0012</a>: Last Activity.
 * 
 * @author bmalkow
 * 
 */
public abstract class LastActivity extends XMPPProcessorAbstract implements XMPPStopListenerIfc {

	private static final Element[] DISCO_FEATURES = { new Element("feature", new String[] { "var" },
			new String[] { "jabber:iq:last" }) };

	private static final String[] ELEMENTS = { "query", "presence", "message" };

	private final static String ID = "jabber:iq:last";

	private final static String LAST_ACTIVITY_KEY = "LAST_ACTIVITY_KEY";

	private static final Logger log = Logger.getLogger(LastActivity.class.getName());

	private final static String[] XMLNS = new String[] { "jabber:iq:last", "jabber:client", "jabber:client" };

	private static final long getTime(NonAuthUserRepository repo, BareJID requestedJid) throws UserNotFoundException {
		String data = repo.getPublicData(requestedJid, ID, LAST_ACTIVITY_KEY, null);

		if (data == null)
			return -1;

		try {
			return Long.parseLong(data);
		} catch (Exception e) {
			return -1;
		}
	}

	private static final long getTime(XMPPResourceConnection session) {
		final Long last = (Long) session.getSessionData(LAST_ACTIVITY_KEY);
		return last == null ? -1 : last.longValue();
	}

	@Override
	public String id() {
		return ID;
	}

	@Override
	public void process(Packet packet, XMPPResourceConnection session, NonAuthUserRepository repo, Queue<Packet> results,
			Map<String, Object> settings) throws XMPPException {
		if (packet.getElemName() != "iq" && session != null && session.getBareJID().equals(packet.getStanzaFrom().getBareJID())) {
			final long time = System.currentTimeMillis();
			if (log.isLoggable(Level.FINEST))
				log.finest("Updating last:activity of user " + session.getUserName() + " to " + time);

			session.putSessionData(LAST_ACTIVITY_KEY, time);
		}
		super.process(packet, session, repo, results, settings);
	}

	/*
	 * User odpytuje sam siebie
	 */
	@Override
	public void processFromUserToServerPacket(JID connectionId, Packet packet, XMPPResourceConnection session,
			NonAuthUserRepository repo, Queue<Packet> results, Map<String, Object> settings) throws PacketErrorTypeException {
		if (packet.getType() == StanzaType.get) {
			long last = getTime(session);
			Packet resp = packet.okResult((Element) null, 0);

			if (last != -1) {
				long result = (System.currentTimeMillis() - last) / 1000;
				Element q = new Element("query", new String[] { "xmlns", "seconds" }, new String[] { "jabber:iq:last",
						"" + result });
				resp.getElement().addChild(q);
				results.offer(resp);
			} else {
				results.offer(Authorization.ITEM_NOT_FOUND.getResponseMessage(packet, "Unknown last activity time", true));
			}
		} else
			results.offer(Authorization.BAD_REQUEST.getResponseMessage(packet, "Message type is incorrect", true));
	}

	/*
	 * User docelowy jest offline
	 */
	@Override
	public void processNullSessionPacket(Packet packet, NonAuthUserRepository repo, Queue<Packet> results,
			Map<String, Object> settings) throws PacketErrorTypeException {
		if (packet.getType() == StanzaType.get) {
			try {
				BareJID requestedJid = packet.getStanzaTo().getBareJID();
				final long last = getTime(repo, requestedJid);

				if (log.isLoggable(Level.FINEST))
					log.finest("Get last:activity of offline user " + requestedJid + ". value=" + last);

				if (last != -1) {
					long result = (System.currentTimeMillis() - last) / 1000;
					Packet resp = packet.okResult((Element) null, 0);
					Element q = new Element("query", new String[] { "xmlns", "seconds" }, new String[] { "jabber:iq:last",
							"" + result });
					resp.getElement().addChild(q);
					results.offer(resp);
				} else {
					results.offer(Authorization.ITEM_NOT_FOUND.getResponseMessage(packet, "Unknown last activity time", true));
				}
			} catch (UserNotFoundException e) {
				results.offer(Authorization.ITEM_NOT_FOUND.getResponseMessage(packet, "User not found", true));
			}
		} else if (packet.getType() == StanzaType.set)
			results.offer(Authorization.BAD_REQUEST.getResponseMessage(packet, "Message type is incorrect", true));
		else
			super.processNullSessionPacket(packet, repo, results, settings);
	}

	@Override
	public void processServerSessionPacket(Packet packet, XMPPResourceConnection session, NonAuthUserRepository repo,
			Queue<Packet> results, Map<String, Object> settings) throws PacketErrorTypeException {
	}

	/*
	 * User docelowy jest podłączony
	 */
	@Override
	public void processToUserPacket(Packet packet, XMPPResourceConnection session, NonAuthUserRepository repo,
			Queue<Packet> results, Map<String, Object> settings) throws PacketErrorTypeException {
		if (packet.getType() == StanzaType.get) {
			long last = getTime(session);
			if (last != -1) {
				long result = (System.currentTimeMillis() - last) / 1000;

				Packet resp = packet.okResult((Element) null, 0);
				Element q = new Element("query", new String[] { "xmlns", "seconds" }, new String[] { "jabber:iq:last",
						"" + result });
				resp.getElement().addChild(q);
				results.offer(resp);
			} else {
				results.offer(Authorization.ITEM_NOT_FOUND.getResponseMessage(packet, "Unknown last activity time", true));
			}
		} else if (packet.getType() == StanzaType.set)
			results.offer(Authorization.BAD_REQUEST.getResponseMessage(packet, "Message type is incorrect", true));
		else
			super.processToUserPacket(packet, session, repo, results, settings);
	}

	@Override
	public void stopped(XMPPResourceConnection session, Queue<Packet> results, Map<String, Object> settings) {
		if (session != null) {
			long last = getTime(session);
			try {
				if (log.isLoggable(Level.FINEST))
					log.finest("Persiting last:activity of user " + session.getUserName() + " in storage (value=" + last + ").");
				session.setPublicData(ID, LAST_ACTIVITY_KEY, String.valueOf(last));
			} catch (NotAuthorizedException e) {
				e.printStackTrace();
			} catch (TigaseDBException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public Element[] supDiscoFeatures(XMPPResourceConnection session) {
		return DISCO_FEATURES;
	}

	@Override
	public String[] supElements() {
		return ELEMENTS;
	}

	@Override
	public String[] supNamespaces() {
		return XMLNS;
	}

}
