package tigase.xmpp.impl;

import tigase.db.NonAuthUserRepository;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.RegistrarBean;
import tigase.kernel.core.Kernel;
import tigase.server.Packet;
import tigase.server.xmppsession.SessionManager;
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
@Bean(name = ClientStateIndication.ID, parent = SessionManager.class, active = true)
public class ClientStateIndication extends AnnotatedXMPPProcessor
		implements XMPPProcessorIfc, XMPPPacketFilterIfc, RegistrarBean {

	private static final Logger log = Logger.getLogger(ClientStateIndication.class.getCanonicalName());

	protected static final String XMLNS = "urn:xmpp:csi:0";
	protected static final String ID = XMLNS;

	protected static final String ACTIVE_NAME = "active";
	protected static final String INACTIVE_NAME = "inactive";

	@Inject
	private Logic logic;

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

	@Override
	public void register(Kernel kernel) {
		kernel.registerBean("logic").asClass(MobileV2.class).exec();
	}

	@Override
	public void unregister(Kernel kernel) {

	}

	public interface Logic extends XMPPPacketFilterIfc {

		void activate(XMPPResourceConnection session, Queue<Packet> results);

		void deactivate(XMPPResourceConnection session, Queue<Packet> results);

	}
}
