package tigase.xmpp.impl;

import org.junit.Before;
import org.junit.Test;
import tigase.server.Command;
import tigase.server.Iq;
import tigase.server.Packet;
import tigase.server.xmppsession.SessionManager;
import tigase.util.stringprep.TigaseStringprepException;
import tigase.xml.Element;
import tigase.xmpp.Authorization;
import tigase.xmpp.XMPPException;
import tigase.xmpp.XMPPResourceConnection;
import tigase.xmpp.jid.JID;

import java.util.HashMap;
import java.util.LinkedList;

import static junit.framework.TestCase.assertEquals;

public class JabberIqCommandTest extends ProcessorTestCase {

	@Before
	public void prepare() {
		getKernel().registerBean(SessionManager.DefaultHandlerProc.class).exec();
		getKernel().registerBean(JabberIqCommand.class).setActive(true).exec();
	}

	@Test
	public void testIqDelivery1() throws XMPPException, TigaseStringprepException {
		JID jid1 = JID.jidInstance("user1@example.com/res1");
		JID jid2 = JID.jidInstance("user1@example.com/res2");
		XMPPResourceConnection session = getSession(jid1, jid1);

		Element iqEl = new Element("iq", new String[] {"from", "to", "xmlns", "type" }, new String[] {jid1.toString(), jid2.toString(),
																									  Iq.CLIENT_XMLNS, "get" });
		iqEl.addChild(new Element("command", new String[] {"xmlns"}, new String[] {Command.XMLNS}));
		Packet iq = Packet.packetInstance(iqEl);
		iq.setPacketFrom(jid1);

		JabberIqCommand handler = getKernel().getInstance(JabberIqCommand.class);
		assertEquals(Authorization.AUTHORIZED, handler.canHandle(iq, session));

		LinkedList<Packet> results = new LinkedList<>();
		
		handler.process(iq, session, null, results, new HashMap<>());

		assertEquals(1, results.size());
	}

	@Test
	public void testIqDelivery2() throws XMPPException, TigaseStringprepException {
		JID jid1 = JID.jidInstance("user1@example.com/res1");
		JID jid2 = JID.jidInstance("user1@example.com/res2");
		XMPPResourceConnection session = getSession(jid2, jid2);

		Element iqEl = new Element("iq", new String[] { "from", "to", "xmlns", "type" }, new String[] {jid1.toString(), jid2.toString(),
																									   Iq.CLIENT_XMLNS, "get" });
		iqEl.addChild(new Element("command", new String[] {"xmlns"}, new String[] {Command.XMLNS}));
		Packet iq = Packet.packetInstance(iqEl);

		JabberIqCommand handler = getKernel().getInstance(JabberIqCommand.class);
		assertEquals(Authorization.AUTHORIZED, handler.canHandle(iq, session));

		LinkedList<Packet> results = new LinkedList<>();

		handler.process(iq, session, null, results, new HashMap<>());

		assertEquals(1, results.size());
	}

}
