package tigase.xmpp.impl;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import junit.framework.TestCase;
import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import tigase.server.Packet;
import tigase.util.TigaseStringprepException;
import tigase.xml.Element;
import tigase.xmpp.Authorization;
import tigase.xmpp.JID;
import tigase.xmpp.XMPPResourceConnection;

/**
 * Test class for JabberIqPrivacyTest
 * 
 * Currently class tests validateList method checking validation of type, 
 * subscription and action. Other cases are not tested due to missing instance 
 * of XMPPResourceConnection
 */
public class JabberIqPrivacyTest extends ProcessorTestCase {

	private JabberIqPrivacy privacyFilter;
	private ArrayDeque<Packet> results;

	@Before
	@Override
	public void setUp() throws Exception {
		super.setUp();
		privacyFilter = new JabberIqPrivacy();
		privacyFilter.init(new HashMap<String, Object>());
		results = new ArrayDeque<Packet>();
	}

	@After
	@Override
	public void tearDown() throws Exception {
		super.tearDown();
		privacyFilter = null;
	}
	
        @Test
        public void testValidateListGood() {
             List<Element> items = new ArrayList<Element>();
             
             Authorization result = null;
                         
             items.add(new Element("item", new String[] { "type", "value", "action", "order" },
                     new String[] { "subscription", "both", "allow", "10" }));
             items.add(new Element("item", new String[] { "action", "order" },
                     new String[] { "deny", "15" }));
             
             // session is allowed to be null here
             result = JabberIqPrivacy.validateList(null, items);
             assertEquals(null, result);
        }

        @Test
        public void testValidateListBadAction() {
             List<Element> items = new ArrayList<Element>();
             
             Authorization result = null;
             
             
             items.add(new Element("item", new String[] { "type", "value", "action", "order" },
                     new String[] { "subscription", "both", "ignore", "10" }));
             items.add(new Element("item", new String[] { "action", "order" },
                     new String[] { "deny", "15" }));
             
             // session is allowed to be null here
             result = JabberIqPrivacy.validateList(null, items);
             assertEquals(Authorization.BAD_REQUEST, result);
        }
        
        @Test
        public void testValidateListBadSubscription() {
             List<Element> items = new ArrayList<Element>();
             
             Authorization result = null;
                          
             items.add(new Element("item", new String[] { "type", "value", "action", "order" },
                     new String[] { "subscription", "or", "allow", "10" }));
             items.add(new Element("item", new String[] { "action", "order" },
                     new String[] { "deny", "15" }));
             
             // session is allowed to be null here
             result = JabberIqPrivacy.validateList(null, items);
             assertEquals(Authorization.BAD_REQUEST, result);
        }
        
        @Test
        public void testValidateListBadType() {
             List<Element> items = new ArrayList<Element>();
             
             Authorization result = null;
                          
             items.add(new Element("item", new String[] { "type", "value", "action", "order" },
                     new String[] { "other", "both", "allow", "10" }));
             items.add(new Element("item", new String[] { "action", "order" },
                     new String[] { "deny", "15" }));
             
             // session is allowed to be null here
             result = JabberIqPrivacy.validateList(null, items);
             assertEquals(Authorization.BAD_REQUEST, result);
        }
        
        @Test
        public void testValidateListOrderUnsignedInt() {
             List<Element> items = new ArrayList<Element>();
             
             Authorization result = null;
                          
             items.add(new Element("item", new String[] { "type", "value", "action", "order" },
                     new String[] { "subscription", "both", "allow", "-10" }));
             items.add(new Element("item", new String[] { "action", "order" },
                     new String[] { "deny", "15" }));
             
             // session is allowed to be null here
             result = JabberIqPrivacy.validateList(null, items);
             assertEquals(Authorization.BAD_REQUEST, result);
             
        }

        @Test
        public void testValidateListOrderAttributeDuplicate() {
             List<Element> items = new ArrayList<Element>();
             
             Authorization result = null;
                          
             items.add(new Element("item", new String[] { "type", "value", "action", "order" },
                     new String[] { "subscription", "both", "allow", "10" }));
             items.add(new Element("item", new String[] { "action", "order" },
                     new String[] { "deny", "10" }));
             
             // session is allowed to be null here
             result = JabberIqPrivacy.validateList(null, items);
             assertEquals(Authorization.BAD_REQUEST, result);
             
        }
        
		@Test
		public void testFilterPresenceOut() throws Exception {
			JID jid = JID.jidInstance("test@example/res-1");
			JID connId = JID.jidInstance("c2s@example.com/asdasd");
			XMPPResourceConnection session = getSession(connId, jid);
			
			//List<Element> items = new ArrayList<Element>();
			Element list = new Element("list", new String[] { "name" }, new String[] { "default" });
			Element item = new Element("item", new String[]{"type", "value", "action", "order"},
					new String[]{"jid", "test1.example.com", "deny", "100"});
			item.addChild(new Element("presence-out"));
			list.addChild(item);
			list.addChild(new Element("item", new String[]{"action", "order"},
					new String[]{"allow", "110"}));
			
			session.putSessionData("active-list", list);
			
			Packet presence = Packet.packetInstance(new Element("presence", 
					new String[] { "from", "to" }, 
					new String[] { "test@example/res-1", "test1.example.com" }));
					
			assertFalse(privacyFilter.allowed(presence, session));
			
			presence = Packet.packetInstance(new Element("presence", 
					new String[] { "to", "from" }, 
					new String[] { "test@example/res-1", "test1.example.com" }));			
			
			assertTrue(privacyFilter.allowed(presence, session));
		}

	@Test
	public void testFilterJidCase() throws Exception {
		JID jid = JID.jidInstance( "test@example/res-1" );
		JID connId = JID.jidInstance( "c2s@example.com/asdasd" );
		XMPPResourceConnection session = getSession( connId, jid );

		String blockedJID = "CapitalisedJID@test.domain.com";

		Element list = new Element( "list", new String[] { "name" }, new String[] { "default" } );
		list.addChild( new Element( "item", new String[] { "type", "value", "action", "order" },
																new String[] { "jid", blockedJID.toLowerCase(), "deny", "100" } ) );
		list.addChild( new Element( "item", new String[] { "action", "order" },
																new String[] { "allow", "110" } ) );

		session.putSessionData( "active-list", list );

		Packet presence = Packet.packetInstance( new Element( "presence",
																													new String[] { "from", "to" },
																													new String[] { blockedJID, jid.toString() } ) );

		assertFalse( privacyFilter.allowed( presence, session ) );

		presence = Packet.packetInstance( new Element( "presence",
																													new String[] { "from", "to" },
																													new String[] { blockedJID.toLowerCase(), jid.toString() } ) );


		assertFalse( privacyFilter.allowed( presence, session ) );

		presence = Packet.packetInstance( new Element( "presence",
																													new String[] { "from", "to" },
																													new String[] { jid.toString(), blockedJID } ) );

		assertFalse( privacyFilter.allowed( presence, session ) );

		presence = Packet.packetInstance( new Element( "presence",
																													new String[] { "from", "to" },
																													new String[] { jid.toString(), blockedJID.toLowerCase() } ) );


		assertFalse( privacyFilter.allowed( presence, session ) );


	}
}
