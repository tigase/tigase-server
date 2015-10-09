package tigase.xmpp.impl;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Queue;
import java.util.TimeZone;

import tigase.db.NonAuthUserRepository;
import tigase.server.Packet;
import tigase.xml.Element;
import tigase.xmpp.Authorization;
import tigase.xmpp.JID;
import tigase.xmpp.PacketErrorTypeException;
import tigase.xmpp.StanzaType;
import tigase.xmpp.XMPPProcessorAbstract;
import tigase.xmpp.XMPPResourceConnection;

/**
 * This supports the implementation of
 * <a href='http://xmpp.org/extensions/xep-0202.html'>XEP-0202</a>: Entity Time.
 *
 */
public class EntityTime extends XMPPProcessorAbstract {

	private static final String XMLNS = "urn:xmpp:time";

	private static final Element[] DISCO_FEATURES = { new Element("feature", new String[] { "var" }, new String[] { XMLNS }) };

	private static final String[] ELEMENTS = { "time" };

	private final static String ID = XMLNS;

	private final static String[] XMLNSS = new String[] { XMLNS };

	private static String getUtcOffset() {
		SimpleDateFormat sdf = new SimpleDateFormat("Z");
		sdf.setTimeZone(TimeZone.getDefault());
		String dateTimeString = sdf.format(new Date());

		return dateTimeString.substring(0, 3) + ":" + dateTimeString.substring(3);
	}

	private static String getUtcTime() {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
		String dateTimeString = sdf.format(new Date());
		return dateTimeString;
	}

	@Override
	public String id() {
		return ID;
	}

	@Override
	public void processFromUserOutPacket(JID connectionId, Packet packet, XMPPResourceConnection session,
			NonAuthUserRepository repo, Queue<Packet> results, Map<String, Object> settings) throws PacketErrorTypeException {
		if (packet.getType() == StanzaType.get) {
			sendTimeResult(packet, results);
		} else if (packet.getType() == StanzaType.set)
			results.offer(Authorization.BAD_REQUEST.getResponseMessage(packet, "Message type is incorrect", true));
		else
			super.processFromUserOutPacket(connectionId, packet, session, repo, results, settings);
	}

	@Override
	public void processFromUserToServerPacket(JID connectionId, Packet packet, XMPPResourceConnection session,
			NonAuthUserRepository repo, Queue<Packet> results, Map<String, Object> settings) throws PacketErrorTypeException {
		if (packet.getType() == StanzaType.get) {
			sendTimeResult(packet, results);
		} else
			results.offer(Authorization.BAD_REQUEST.getResponseMessage(packet, "Message type is incorrect", true));
	}

	@Override
	public void processNullSessionPacket(Packet packet, NonAuthUserRepository repo, Queue<Packet> results,
			Map<String, Object> settings) throws PacketErrorTypeException {
		if (packet.getType() == StanzaType.get) {
			sendTimeResult(packet, results);
		} else if (packet.getType() == StanzaType.set)
			results.offer(Authorization.BAD_REQUEST.getResponseMessage(packet, "Message type is incorrect", true));
		else
			super.processNullSessionPacket(packet, repo, results, settings);
	}

	@Override
	public void processServerSessionPacket(Packet packet, XMPPResourceConnection session, NonAuthUserRepository repo,
			Queue<Packet> results, Map<String, Object> settings) throws PacketErrorTypeException {
	}

	@Override
	public void processToUserPacket(Packet packet, XMPPResourceConnection session, NonAuthUserRepository repo,
			Queue<Packet> results, Map<String, Object> settings) throws PacketErrorTypeException {
		if (packet.getType() == StanzaType.get) {
			sendTimeResult(packet, results);
		} else if (packet.getType() == StanzaType.set)
			results.offer(Authorization.BAD_REQUEST.getResponseMessage(packet, "Message type is incorrect", true));
		else
			super.processToUserPacket(packet, session, repo, results, settings);
	}

	private void sendTimeResult(Packet packet, Queue<Packet> results) {
		Packet resp = packet.okResult((Element) null, 0);

		Element time = new Element("time", new String[] { "xmlns" }, new String[] { XMLNS });

		Element tzo = new Element("tzo");
		tzo.setCData(getUtcOffset());
		time.addChild(tzo);

		Element utc = new Element("utc");
		utc.setCData(getUtcTime());
		time.addChild(utc);

		resp.getElement().addChild(time);
		results.offer(resp);
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
		return XMLNSS;
	}

}
