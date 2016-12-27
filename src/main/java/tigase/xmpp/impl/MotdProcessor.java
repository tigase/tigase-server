package tigase.xmpp.impl;

import tigase.db.NonAuthUserRepository;
import tigase.db.TigaseDBException;
import tigase.db.UserRepository;
import tigase.eventbus.EventBus;
import tigase.eventbus.HandleEvent;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Initializable;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.UnregisterAware;
import tigase.server.*;
import tigase.server.Message;
import tigase.server.Presence;
import tigase.server.xmppsession.SessionManager;
import tigase.xml.Element;
import tigase.xmpp.*;
import tigase.xmpp.impl.annotation.AnnotatedXMPPProcessor;
import tigase.xmpp.impl.annotation.Handle;
import tigase.xmpp.impl.annotation.Handles;
import tigase.xmpp.impl.annotation.Id;

import java.io.Serializable;
import java.util.Map;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

import static tigase.xmpp.impl.MotdProcessor.ID;

/**
 * Created by andrzej on 10.12.2016.
 */
@Id(ID)
@Handles(@Handle(path = {Presence.ELEM_NAME}, xmlns = Presence.CLIENT_XMLNS))
@Bean(name = ID, parent = SessionManager.class, active = false)
public class MotdProcessor
		extends AnnotatedXMPPProcessor
		implements XMPPProcessorIfc, Initializable, UnregisterAware {
	protected static final String ID = "motd";
	private static final Logger log = Logger.getLogger(MotdProcessor.class.getCanonicalName());
	private static final long HOURS_24 = 24 * 60 * 60 * 1000;

	private static final String[] PRESENCE_PRIORITY_PATH = {"presence", "priority"};
	private static final BareJID smJid = BareJID.bareJIDInstanceNS("sess-man");
	@Inject
	private EventBus eventBus;
	private String motd = null;
	private Long motdTimestamp = null;
	@Inject
	private UserRepository userRepository;

	@Override
	public void beforeUnregister() {
		eventBus.unregisterAll(this);
	}

	@Override
	public void initialize() {
		try {
			motd = userRepository.getData(smJid, ID, "message");
			String stamp = userRepository.getData(smJid, ID, "timestamp");
			motdTimestamp = stamp == null ? null : Long.parseLong(stamp);
		} catch (TigaseDBException ex) {
			log.log(Level.WARNING, "failed to read current MOTD from user repository", ex);
		}
		eventBus.registerAll(this);
	}

	@Override
	public void process(Packet packet, XMPPResourceConnection session, NonAuthUserRepository repo,
						Queue<Packet> results, Map<String, Object> settings) throws XMPPException {

		if (session == null) {
			return;
		}

		if (packet.getStanzaTo() != null) {
			return;
		}

		if (session.getSessionData(ID) != null) {
			return;
		}

		String priority = packet.getElemCDataStaticStr(PRESENCE_PRIORITY_PATH);
		if (priority != null && Integer.parseInt(priority) < 0) {
			return;
		}

		session.putSessionData(ID, ID);

		if (motd == null || motdTimestamp == null) {
			return;
		}

		long deliveryTimestamp = getLastDeliveryTime(session);
		if (deliveryTimestamp > motdTimestamp && (deliveryTimestamp + HOURS_24) > System.currentTimeMillis()) {
			return;
		}

		setLastDeliveryTime(session);

		Element messageEl = new Element("message");
		messageEl.setXMLNS(Message.CLIENT_XMLNS);
		messageEl.addChild(new Element("body", motd));

		Message message = new Message(messageEl, session.getDomainAsJID(), session.getJID());
		message.setPacketTo(session.getConnectionId());

		results.add(message);
	}

	@HandleEvent
	public void onMotdChanged(MotdUpdatedEvent event) {
		this.motd = event.getMessage();
		this.motdTimestamp = event.getTimestmap();
	}

	public String getMotd() {
		return motd;
	}

	public void setMotd(String motd) throws TigaseDBException {
		Long timestamp = motd == null ? null : System.currentTimeMillis();
		userRepository.setData(smJid, ID, "message", motd);
		userRepository.setData(smJid, ID, "timestamp", timestamp == null ? null : String.valueOf(timestamp));
		eventBus.fire(new MotdUpdatedEvent(motd, timestamp));
	}

	private long getLastDeliveryTime(XMPPResourceConnection session) {
		try {
			String str = session.getData(ID, "last-delivery", null);
			if (str == null) {
				return 0;
			}

			return Long.parseLong(str);
		} catch (NotAuthorizedException | TigaseDBException ex) {
			log.log(Level.FINEST, session.toString() + ", could not retrieve last delivery timestamp", ex);
			return 0;
		}
	}

	private void setLastDeliveryTime(XMPPResourceConnection session) {
		try {
			String stamp = String.valueOf(System.currentTimeMillis());
			session.setData(ID, "last-delivery", stamp);
		} catch (NotAuthorizedException | TigaseDBException ex) {
			log.log(Level.FINEST, session.toString() + ", could not update last delivery timestamp", ex);
		}
	}

	public static class MotdUpdatedEvent
			implements Serializable {

		private String message;
		private Long timestamp;

		public MotdUpdatedEvent() {}

		public MotdUpdatedEvent(String msg, Long timestamp) {
			this.message = msg;
			this.timestamp = timestamp;
		}

		public String getMessage() {
			return message;
		}

		public Long getTimestmap() {
			return timestamp;
		}

	}
}
