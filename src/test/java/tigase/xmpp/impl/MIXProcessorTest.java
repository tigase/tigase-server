package tigase.xmpp.impl;

import org.junit.Before;
import org.junit.Test;
import tigase.component.exceptions.RepositoryException;
import tigase.db.TigaseDBException;
import tigase.eventbus.EventBusFactory;
import tigase.server.Message;
import tigase.server.Packet;
import tigase.server.PolicyViolationException;
import tigase.server.xmppsession.SessionManager;
import tigase.util.stringprep.TigaseStringprepException;
import tigase.xml.Element;
import tigase.xmpp.Authorization;
import tigase.xmpp.NotAuthorizedException;
import tigase.xmpp.XMPPException;
import tigase.xmpp.XMPPResourceConnection;
import tigase.xmpp.impl.roster.RosterAbstract;
import tigase.xmpp.impl.roster.RosterElement;
import tigase.xmpp.impl.roster.RosterFactory;
import tigase.xmpp.jid.BareJID;
import tigase.xmpp.jid.JID;

import java.util.ArrayDeque;
import java.util.Map;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.TestCase.assertEquals;
import static tigase.xmpp.impl.MIXProcessor.generateParticipantId;

public class MIXProcessorTest
	extends ProcessorTestCase {

	private JID mixChannel;
	private XMPPResourceConnection session;
	private JID user;

	@Test
	public void canHandleRetractedParticipantEvent() throws TigaseStringprepException {

		var packet = getRetractedParticipantMessage(mixChannel.getBareJID(), user.getBareJID());
		var mixProcessor = getKernel().getInstance(MIXProcessor.class);
		assertEquals(Authorization.AUTHORIZED, mixProcessor.canHandle(packet, session));
	}

	private Packet getRetractedParticipantMessage(BareJID fromMixChannel, BareJID toUser)
		throws TigaseStringprepException {
		var messageElement = new Element("message", new String[]{"xmlns", "from", "to"},
		                                 new String[]{Message.CLIENT_XMLNS, fromMixChannel.toString(), toUser.toString()});
		var event = new Element("event", new String[]{"xmlns"}, new String[]{"http://jabber.org/protocol/pubsub#event"});
		var items = new Element("items", new String[]{"node"}, new String[]{"urn:xmpp:mix:nodes:participants"});
		var retracted = new Element("retract");
		retracted.addAttribute("id", generateParticipantId(fromMixChannel, toUser));
		items.addChild(retracted);
		event.addChild(items);
		messageElement.addChild(event);
		var packet = Message.packetInstance(messageElement);
		packet.setPacketFrom(mixChannel);
		return packet;
	}

	@Before
	public void prepare() throws TigaseStringprepException, NotAuthorizedException {
		getKernel().registerBean("eventbus").asInstance(EventBusFactory.getInstance()).exportable().exec();
		getKernel().registerBean("sess-man").asInstance(new SessionManager()).setActive(true).exportable().exec();
		getKernel().registerBean(RosterFactory.Bean.class).setActive(true).exec();
		getKernel().getInstance(RosterFactory.Bean.class);
		getKernel().registerBean(MIXProcessor.class).setActive(true).exec();

		mixChannel = JID.jidInstance("d6af5eea-ccc2-4b94-8402-387fee89ece9@mix.domain.com");
		user = JID.jidInstance("user1@example.com/res1");
		session = getSession(user, user, true);
	}

	@Test
	public void testNotRemoveMixChannelFromRosterOnUserRetractedEventNotMatchingId()
		throws TigaseStringprepException, XMPPException, TigaseDBException, PolicyViolationException {

		RosterAbstract rosterUtil = RosterFactory.getRosterImplementation(true);
		var mixChannelRosterElement = new RosterElement(mixChannel, mixChannel.toString(), null);
		rosterUtil.addJidToRoster(getUserRepository(), session.getParentSession(), user.getBareJID(),
		                          mixChannelRosterElement);

		var rosterItems = rosterUtil.getRosterItems(session);
		assertFalse(rosterItems.isEmpty());
		var mixRosterItem = rosterUtil.getBuddyItem(session, mixChannel);
		assertNotNull(mixRosterItem);

		var processor = getKernel().getInstance(MIXProcessor.class);
		JID otherUser = JID.jidInstance("user2@example.com/res2");
		var packet = getRetractedParticipantMessage(mixChannel.getBareJID(), otherUser.getBareJID());
		var results = new ArrayDeque<Packet>();
		processor.process(packet, session, null, results, Map.of());
		assertEquals(0, results.size());
		var rosterItemsAfterProcessing = rosterUtil.getRosterItems(session);
		assertFalse(rosterItemsAfterProcessing.isEmpty());
		var mixRosterItemAfterProcessing = rosterUtil.getBuddyItem(session, mixChannel);
		assertNotNull(mixRosterItemAfterProcessing);

	}

	@Test
	public void testRemoveMixChannelFromRosterOnUserRetractedEventMatchingId()
		throws TigaseStringprepException, XMPPException, TigaseDBException, PolicyViolationException {

		RosterAbstract rosterUtil = RosterFactory.getRosterImplementation(true);
		var mixChannelRosterElement = new RosterElement(mixChannel, mixChannel.toString(), null);
		rosterUtil.addJidToRoster(getUserRepository(), session.getParentSession(), user.getBareJID(),
		                          mixChannelRosterElement);

		var rosterItems = rosterUtil.getRosterItems(session);
		assertFalse(rosterItems.isEmpty());
		var mixRosterItem = rosterUtil.getBuddyItem(session, mixChannel);
		assertNotNull(mixRosterItem);

		var processor = getKernel().getInstance(MIXProcessor.class);
		var packet = getRetractedParticipantMessage(mixChannel.getBareJID(), user.getBareJID());
		var results = new ArrayDeque<Packet>();
		processor.process(packet, session, null, results, Map.of());
		assertEquals(1, results.size());
		final Element rosterElement = results.getFirst().getElement();
		assertEquals("jabber:iq:roster", rosterElement.getChild("query").getXMLNS());
		assertEquals("remove", rosterElement.getChild("query").getChild("item").getAttributeStaticStr("subscription"));

		var rosterItemsAfterProcessing = rosterUtil.getRosterItems(session);
		assertTrue(rosterItemsAfterProcessing.isEmpty());
		var mixRosterItemAfterProcessing = rosterUtil.getBuddyItem(session, mixChannel);
		assertNull(mixRosterItemAfterProcessing);

	}

	@Test
	public void testSha() throws RepositoryException {
		mixChannel = JID.jidInstanceNS("3dc9222b-3316-41f6-927f-fb7f7dd4ad1f@mix.atlantiscity");
		user = JID.jidInstanceNS("tigase3@atlantiscity/atlantiscity");
		var participantId = generateParticipantId(mixChannel.getBareJID(), user.getBareJID());
		assertEquals("318cd48257571b007eb68d759f9c59a3736a1584", participantId);
	}
}