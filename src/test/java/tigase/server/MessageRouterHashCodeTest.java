
package tigase.server;

import junit.framework.TestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import tigase.server.xmppclient.ClientConnectionManager;
import tigase.util.stringprep.TigaseStringprepException;
import tigase.xml.Element;
import tigase.xmpp.jid.JID;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static tigase.server.Packet.*;
import static tigase.xmpp.jid.JID.jidInstanceNS;

@RunWith(value = Parameterized.class)
public class MessageRouterHashCodeTest extends TestCase {

    private final MessageRouter messageRouter = new MessageRouter();
    private final String elementName;
    private final String stanzaType;
    private final String expected;
    private final String expectedValue;
    private final String stanzaFrom;
    private final String stanzaTo;
    private final String packetFrom;
    private final String packetTo;
    private final String command;

    public MessageRouterHashCodeTest(String elementName, String stanzaType, String expected, String expectedValue, String packetFrom, String stanzaFrom, String packetTo, String stanzaTo, String command) {
        this.elementName = elementName;
        this.stanzaType = stanzaType;
        this.expected = expected;
        this.expectedValue = expectedValue;
        this.stanzaFrom = stanzaFrom;
        this.stanzaTo = stanzaTo;
        this.packetFrom = packetFrom;
        this.packetTo = packetTo;
        this.command = command;
        ClientConnectionManager connectionManager = new ClientConnectionManager();
        connectionManager.setCompId(jidInstanceNS("c2s@localhost"));
        connectionManager.setName("c2s");

        messageRouter.addConnectionManager(connectionManager);
        messageRouter.setCompId(jidInstanceNS("message-router@localhost"));

    }

    //@formatter:off
    @Parameterized.Parameters(name = "{index}: {0} Type:{1} Expected:{2} {3} = PacketFrom:{4} StanzaFrom:{5} PacketTo:{6} StanzaTo:{7} Command:{8}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"auth", null, "PACKET_FROM", "c2s@localhost/127.0.0.1_5222_127.0.0.1_58041", "c2s@localhost/127.0.0.1_5222_127.0.0.1_58041", null, "sess-man@localhost", null, ""},
                {"iq", "get", "PACKET_FROM", "c2s@localhost/127.0.0.1_5222_127.0.0.1_58041", "c2s@localhost/127.0.0.1_5222_127.0.0.1_58041", null, "sess-man@localhost", "3ccb918e3c85c8a3358ae2686abd3b055cda26e3@dev.me", null},
                {"iq", "get", "PACKET_FROM", "c2s@localhost/127.0.0.1_5222_127.0.0.1_58041", "c2s@localhost/127.0.0.1_5222_127.0.0.1_58041", null, "sess-man@localhost", null, null},
                {"iq", "get", "STANZA_FROM", "c2s@localhost/127.0.0.1_5222_127.0.0.1_58041", null, "c2s@localhost/127.0.0.1_5222_127.0.0.1_58041", null, "sess-man@localhost", "GETFEATURES"},
                {"iq", "result", "PACKET_FROM", "c2s@localhost/127.0.0.1_5222_127.0.0.1_58041", "c2s@localhost/127.0.0.1_5222_127.0.0.1_58041", null, "sess-man@localhost", null, null},
                {"iq", "result", "STANZA_TO", "3ccb918e3c85c8a3358ae2686abd3b055cda26e3@dev.me/+888000000001", "sess-man@localhost", "3ccb918e3c85c8a3358ae2686abd3b055cda26e3@dev.me", "c2s@localhost/127.0.0.1_5222_127.0.0.1_58041", "3ccb918e3c85c8a3358ae2686abd3b055cda26e3@dev.me/+888000000001", null},
                {"iq", "result", "STANZA_TO", "3ccb918e3c85c8a3358ae2686abd3b055cda26e3@dev.me/+888000000001", "sess-man@localhost", null, "c2s@localhost/127.0.0.1_5222_127.0.0.1_58041", "3ccb918e3c85c8a3358ae2686abd3b055cda26e3@dev.me/+888000000001", null},
                {"iq", "result", "STANZA_TO", "c2s@localhost/127.0.0.1_5222_127.0.0.1_58041", "sess-man@localhost", "sess-man@localhost", "c2s@localhost/127.0.0.1_5222_127.0.0.1_58041", "c2s@localhost/127.0.0.1_5222_127.0.0.1_58041", "GETFEATURES"},
                {"iq", "result", "STANZA_TO", "c2s@localhost/127.0.0.1_5222_127.0.0.1_58041", "sess-man@localhost", "sess-man@localhost", "c2s@localhost/127.0.0.1_5222_127.0.0.1_58041", "c2s@localhost/127.0.0.1_5222_127.0.0.1_58041", null},
                {"iq", "set", "PACKET_FROM", "c2s@localhost/127.0.0.1_5222_127.0.0.1_58041", "c2s@localhost/127.0.0.1_5222_127.0.0.1_58041", null, "sess-man@localhost", null, null},
                {"iq", "set", "STANZA_FROM", "c2s@localhost/127.0.0.1_5222_127.0.0.1_58041", "c2s@localhost/127.0.0.1_5222_127.0.0.1_58041", "c2s@localhost/127.0.0.1_5222_127.0.0.1_58041", "sess-man@localhost", "sess-man@localhost", "STREAM_CLOSED"},
                {"iq", "set", "STANZA_FROM", "c2s@localhost/127.0.0.1_5222_127.0.0.1_58041", "c2s@localhost/127.0.0.1_5222_127.0.0.1_58041", "c2s@localhost/127.0.0.1_5222_127.0.0.1_58041", "sess-man@localhost", "sess-man@localhost", "STREAM_FINISHED"},
                {"iq", "set", "STANZA_FROM", "c2s@localhost/127.0.0.1_5222_127.0.0.1_58041", null, "c2s@localhost/127.0.0.1_5222_127.0.0.1_58041", null, "sess-man@localhost", "STREAM_OPENED"},
                {"iq", "set", "STANZA_FROM", "c2s@localhost/127.0.0.1_5222_127.0.0.1_58041", null, "c2s@localhost/127.0.0.1_5222_127.0.0.1_58041", null, "sess-man@localhost", "TLS_HANDSHAKE_COMPLETE"},
                {"iq", "set", "STANZA_TO", "3ccb918e3c85c8a3358ae2686abd3b055cda26e3@dev.me/+888000000001", "sess-man@localhost", null, "c2s@localhost/127.0.0.1_5222_127.0.0.1_58041", "3ccb918e3c85c8a3358ae2686abd3b055cda26e3@dev.me/+888000000001", null},
                {"iq", "set", "STANZA_TO", "c2s@localhost/127.0.0.1_5222_127.0.0.1_58041", "sess-man@localhost", "sess-man@localhost", null, "c2s@localhost/127.0.0.1_5222_127.0.0.1_58041", "STARTTLS"},
                {"iq", "set", "STANZA_TO", "c2s@localhost/127.0.0.1_5222_127.0.0.1_58041", "sess-man@localhost", "sess-man@localhost", null, "c2s@localhost/127.0.0.1_5222_127.0.0.1_58041", "USER_LOGIN"},
                {"message", "chat", "PACKET_FROM", "c2s@localhost/127.0.0.1_5222_127.0.0.1_58041", "c2s@localhost/127.0.0.1_5222_127.0.0.1_58041", null, "sess-man@localhost", "817382cc4e5bdbbec00ffdec92ba38d04137eedb@dev.me", ""},
                {"message", "chat", "STANZA_FROM", "3ccb918e3c85c8a3358ae2686abd3b055cda26e3@dev.me/+888000000001", "sess-man@localhost", "3ccb918e3c85c8a3358ae2686abd3b055cda26e3@dev.me/+888000000001", null, "817382cc4e5bdbbec00ffdec92ba38d04137eedb@dev.me", ""},
                {"message", null, "STANZA_TO.BAREJID", "push@dev.me", "sess-man@localhost", "dev.me", null, "push@dev.me", ""},
                {"presence", null, "PACKET_FROM", "c2s@localhost/127.0.0.1_5222_127.0.0.1_58041", "c2s@localhost/127.0.0.1_5222_127.0.0.1_58041", null, "sess-man@localhost", null, ""},
                {"presence", null, "STANZA_FROM", "3ccb918e3c85c8a3358ae2686abd3b055cda26e3@dev.me/+888000000001", "sess-man@localhost", "3ccb918e3c85c8a3358ae2686abd3b055cda26e3@dev.me/+888000000001", "c2s@localhost/127.0.0.1_5222_127.0.0.1_58041", "3ccb918e3c85c8a3358ae2686abd3b055cda26e3@dev.me", ""},
                {"presence", "subscribe", "PACKET_FROM", "c2s@localhost/127.0.0.1_5222_127.0.0.1_58041", "c2s@localhost/127.0.0.1_5222_127.0.0.1_58041", null, "sess-man@localhost", "17dccb4c4ecb50f546d06137d2a2296ef009087d@dev.me", ""},
                {"presence", "subscribe", "STANZA_TO.BAREJID", "17dccb4c4ecb50f546d06137d2a2296ef009087d@dev.me", "sess-man@localhost", "3ccb918e3c85c8a3358ae2686abd3b055cda26e3@dev.me", null, "17dccb4c4ecb50f546d06137d2a2296ef009087d@dev.me", ""},
                {"presence", "unavailable", "PACKET_FROM", "c2s@localhost/127.0.0.1_5222_127.0.0.1_58041", "c2s@localhost/127.0.0.1_5222_127.0.0.1_58041", null, "sess-man@localhost", null, ""},
                {"starttls", null, "PACKET_FROM", "c2s@localhost/127.0.0.1_5222_127.0.0.1_58041", "c2s@localhost/127.0.0.1_5222_127.0.0.1_58041", null, "sess-man@localhost", null, ""},
                {"success", null, "PACKET_TO", "c2s@localhost/127.0.0.1_5222_127.0.0.1_58041", "sess-man@localhost", null, "c2s@localhost/127.0.0.1_5222_127.0.0.1_58041", null, ""},
        });
    }
    //@formatter:on

    @Test
    public void testParameterizedHashCodeForPacket() throws TigaseStringprepException {
        Packet packet = createPacketInstanceFromParameters();

        JID expectedJid = jidInstanceNS(expectedValue);
        int expectedHash = expected.contains("BAREJID") ? expectedJid.getBareJID().hashCode() : expectedJid.hashCode();
        int actualHash = messageRouter.hashCodeForPacket(packet);
        assertEquals(expectedHash, actualHash);
    }

    private Packet createPacketInstanceFromParameters() throws TigaseStringprepException {
        Element element = new Element(elementName);

        Map<String, String> elementAttributes = new HashMap<>();
        if (stanzaFrom != null) {
            elementAttributes.put(FROM_ATT, stanzaFrom);
        }
        if (stanzaTo != null) {
            elementAttributes.put(TO_ATT, stanzaTo);
        }
        if (stanzaType != null) {
            elementAttributes.put(TYPE_ATT, stanzaType);
        }
        element.addAttributes(elementAttributes);

        Packet packet = packetInstance(element);
        packet.setPacketFrom(jidInstanceNS(packetFrom));
        packet.setPacketTo(jidInstanceNS(packetTo));
        return packet;
    }

}