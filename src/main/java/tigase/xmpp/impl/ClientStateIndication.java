package tigase.xmpp.impl;

import tigase.db.NonAuthUserRepository;
import tigase.db.TigaseDBException;
import tigase.osgi.ModulesManagerImpl;
import tigase.server.Packet;
import tigase.xml.Element;
import tigase.xmpp.*;
import tigase.xmpp.impl.annotation.*;

import java.util.Map;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by andrzej on 25.06.2016.
 */
@Id(ClientStateIndication.ID)
@Handles({
		@Handle(path = {ClientStateIndication.ACTIVE_NAME}, xmlns = ClientStateIndication.XMLNS),
		@Handle(path = {ClientStateIndication.INACTIVE_NAME}, xmlns = ClientStateIndication.XMLNS)
})
@StreamFeatures({
		@StreamFeature(elem = "csi", xmlns = ClientStateIndication.XMLNS)
})
public class ClientStateIndication extends AnnotatedXMPPProcessor
		implements XMPPProcessorIfc, XMPPPacketFilterIfc {

	private static final Logger log = Logger.getLogger(ClientStateIndication.class.getCanonicalName());

	protected static final String XMLNS = "urn:xmpp:csi:0";
	protected static final String ID = XMLNS;

	protected static final String ACTIVE_NAME = "active";
	protected static final String INACTIVE_NAME = "inactive";

	private Logic logic = new MobileV2();

	@Override
	public void init(Map<String, Object> settings) throws TigaseDBException {
		super.init(settings);

		if (settings.containsKey("logic")) {
			String cls = (String) settings.get("logic");
			try {
				logic = (Logic) ModulesManagerImpl.getInstance().forName(cls).newInstance();
			} catch (ClassNotFoundException | InstantiationException | IllegalAccessException ex) {
				log.log(Level.SEVERE, "Could not create instance of class", ex);
			}
		}

		logic.init(settings);
	}

	@Override
	public void filter(Packet packet, XMPPResourceConnection session, NonAuthUserRepository repo, Queue<Packet> results) {
		logic.filter(packet, session, repo, results);
	}

	@Override
	public void process(Packet packet, XMPPResourceConnection session, NonAuthUserRepository repo, Queue<Packet> results, Map<String, Object> settings) throws XMPPException {
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

		switch (packet.getElemName()) {
			case ACTIVE_NAME:
				logic.deactivate(session, results);
				break;
			case INACTIVE_NAME:
				logic.activate(session, results);
				break;
			default:
				results.offer(Authorization.FEATURE_NOT_IMPLEMENTED.getResponseMessage(packet, null, true));
		}
	}

	@Override
	public Element[] supStreamFeatures(XMPPResourceConnection session) {
		if (session == null || !session.isAuthorized())
			return null;
		return super.supStreamFeatures(session);
	}

	public interface Logic extends XMPPPacketFilterIfc {

		void activate(XMPPResourceConnection session, Queue<Packet> results);

		void deactivate(XMPPResourceConnection session, Queue<Packet> results);

	}
}
