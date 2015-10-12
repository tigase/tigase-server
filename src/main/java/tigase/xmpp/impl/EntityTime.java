package tigase.xmpp.impl;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Queue;
import java.util.TimeZone;

import tigase.db.NonAuthUserRepository;
import tigase.server.Iq;
import tigase.server.Packet;
import tigase.xml.Element;
import tigase.xmpp.Authorization;
import tigase.xmpp.JID;
import tigase.xmpp.PacketErrorTypeException;
import tigase.xmpp.StanzaType;
import tigase.xmpp.XMPPProcessorAbstract;
import tigase.xmpp.XMPPResourceConnection;
import tigase.xmpp.impl.annotation.DiscoFeatures;
import tigase.xmpp.impl.annotation.Handle;
import tigase.xmpp.impl.annotation.Handles;
import tigase.xmpp.impl.annotation.Id;

/**
 * This supports the implementation of
 * <a href='http://xmpp.org/extensions/xep-0202.html'>XEP-0202</a>: Entity Time.
 *
 */
@Id(EntityTime.XMLNS)
@Handles({ @Handle(path = { Iq.ELEM_NAME, EntityTime.TIME }, xmlns = EntityTime.XMLNS) })
@DiscoFeatures({ EntityTime.XMLNS })
public class EntityTime extends XMPPProcessorAbstract {

	protected static final String XMLNS = "urn:xmpp:time";

	protected static final String TIME = "time";

	private final static String ID = XMLNS;

	private static String getUtcOffset() {
		SimpleDateFormat sdf = new SimpleDateFormat("Z");
		sdf.setTimeZone(TimeZone.getDefault());
		String dateTimeString = sdf.format(new Date());

		return dateTimeString.substring(0, 3) + ":" + dateTimeString.substring(3);
	}

	private static String getUtcTime() {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
		return sdf.format(new Date());
	}

	@Override
	public String id() {
		return ID;
	}

	@Override
	public void processFromUserOutPacket(JID connectionId, Packet packet, XMPPResourceConnection session,
			NonAuthUserRepository repo, Queue<Packet> results, Map<String, Object> settings) throws PacketErrorTypeException {
		super.processFromUserOutPacket(connectionId, packet, session, repo, results, settings);
	}

	@Override
	public void processFromUserToServerPacket(JID connectionId, Packet packet, XMPPResourceConnection session,
			NonAuthUserRepository repo, Queue<Packet> results, Map<String, Object> settings) throws PacketErrorTypeException {
		if (packet.getStanzaTo() != null && packet.getStanzaFrom() != null
				&& packet.getStanzaTo().equals(packet.getStanzaFrom())) {
			processFromUserOutPacket(connectionId, packet, session, repo, results, settings);
		} else if (packet.getType() == StanzaType.get) {
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

}
