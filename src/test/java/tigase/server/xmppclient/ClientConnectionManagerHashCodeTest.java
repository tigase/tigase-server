package tigase.server.xmppclient;

import junit.framework.TestCase;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import tigase.server.Packet;
import tigase.util.stringprep.TigaseStringprepException;
import tigase.xml.Element;
import tigase.xmpp.jid.JID;

import static tigase.server.Packet.FROM_ATT;
import static tigase.server.Packet.TO_ATT;
import static tigase.server.Packet.TYPE_ATT;
import static tigase.server.Packet.packetInstance;
import static tigase.xmpp.jid.JID.jidInstanceNS;

@RunWith(value = Parameterized.class)
public class ClientConnectionManagerHashCodeTest extends TestCase {

    private final ClientConnectionManager connectionManager = new ClientConnectionManager();
    private final String elementName;
    private final String stanzaType;
    private final String expected;
    private final String expectedValue;
    private final String stanzaFrom;
    private final String stanzaTo;
    private final String packetFrom;
    private final String packetTo;
    private final String command;

    public ClientConnectionManagerHashCodeTest(String elementName, String stanzaType, String expected, String expectedValue, String packetFrom, String stanzaFrom, String packetTo, String stanzaTo, String command) {
        this.elementName = elementName;
        this.stanzaType = stanzaType;
        this.expected = expected;
        this.expectedValue = expectedValue;
        this.stanzaFrom = stanzaFrom;
        this.stanzaTo = stanzaTo;
        this.packetFrom = packetFrom;
        this.packetTo = packetTo;
        this.command = command;
        connectionManager.setCompId(jidInstanceNS("c2s@localhost"));
        connectionManager.setName("c2s");
    }

    //@formatter:off
    @Parameterized.Parameters(name = "{index}: {0} Type:{1} Expected:{2} {3} = PacketFrom:{4} StanzaFrom:{5} PacketTo:{6} StanzaTo:{7} Command:{8}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"iq", "get", "PACKET_FROM", "c2s@localhost/127.0.0.1_5222_127.0.0.1_58041", "c2s@localhost/127.0.0.1_5222_127.0.0.1_58041", null, "sess-man@localhost", "3ccb918e3c85c8a3358ae2686abd3b055cda26e3@dev.me", null},
                {"iq", "get", "PACKET_FROM", "c2s@localhost/127.0.0.1_5222_127.0.0.1_58041", "c2s@localhost/127.0.0.1_5222_127.0.0.1_58041", null, "sess-man@localhost", null, null},
                {"iq", "get", "STANZA_FROM", "c2s@localhost/127.0.0.1_5222_127.0.0.1_58041", null, "c2s@localhost/127.0.0.1_5222_127.0.0.1_58041", null, "sess-man@localhost", "GETFEATURES"},
                {"iq", "result", "PACKET_FROM", "c2s@localhost/127.0.0.1_5222_127.0.0.1_58041", "c2s@localhost/127.0.0.1_5222_127.0.0.1_58041", null, "sess-man@localhost", null, null},
                {"iq", "result", "PACKET_TO?STANZA_TO", "c2s@localhost/127.0.0.1_5222_127.0.0.1_58041", "sess-man@localhost", "3ccb918e3c85c8a3358ae2686abd3b055cda26e3@dev.me", "c2s@localhost/127.0.0.1_5222_127.0.0.1_58041", "3ccb918e3c85c8a3358ae2686abd3b055cda26e3@dev.me/+888000000001", null},
                {"iq", "result", "PACKET_TO?STANZA_TO", "c2s@localhost/127.0.0.1_5222_127.0.0.1_58041", "sess-man@localhost", null, "c2s@localhost/127.0.0.1_5222_127.0.0.1_58041", "3ccb918e3c85c8a3358ae2686abd3b055cda26e3@dev.me/+888000000001", null},
                {"iq", "result", "PACKET_TO?STANZA_TO", "c2s@localhost/127.0.0.1_5222_127.0.0.1_58041", "sess-man@localhost", "sess-man@localhost", "c2s@localhost/127.0.0.1_5222_127.0.0.1_58041", "c2s@localhost/127.0.0.1_5222_127.0.0.1_58041", "GETFEATURES"},
                {"iq", "result", "PACKET_TO?STANZA_TO", "c2s@localhost/127.0.0.1_5222_127.0.0.1_58041", "sess-man@localhost", "sess-man@localhost", "c2s@localhost/127.0.0.1_5222_127.0.0.1_58041", "c2s@localhost/127.0.0.1_5222_127.0.0.1_58041", null},
                {"iq", "set", "PACKET_FROM", "c2s@localhost/127.0.0.1_5222_127.0.0.1_58041", "c2s@localhost/127.0.0.1_5222_127.0.0.1_58041", null, "sess-man@localhost", null, null},
                {"iq", "set", "PACKET_TO?STANZA_TO", "c2s@localhost/127.0.0.1_5222_127.0.0.1_58041", "sess-man@localhost", null, "c2s@localhost/127.0.0.1_5222_127.0.0.1_58041", "3ccb918e3c85c8a3358ae2686abd3b055cda26e3@dev.me/+888000000001", null},
                {"iq", "set", "PACKET_TO?STANZA_TO", "c2s@localhost/127.0.0.1_5222_127.0.0.1_58041", "sess-man@localhost", "sess-man@localhost", null, "c2s@localhost/127.0.0.1_5222_127.0.0.1_58041", "STARTTLS"},
                {"iq", "set", "PACKET_TO?STANZA_TO", "c2s@localhost/127.0.0.1_5222_127.0.0.1_58041", "sess-man@localhost", "sess-man@localhost", null, "c2s@localhost/127.0.0.1_5222_127.0.0.1_58041", "USER_LOGIN"},
                {"iq", "set", "STANZA_FROM", "c2s@localhost/127.0.0.1_5222_127.0.0.1_58041", "c2s@localhost/127.0.0.1_5222_127.0.0.1_58041", "c2s@localhost/127.0.0.1_5222_127.0.0.1_58041", "sess-man@localhost", "sess-man@localhost", "STREAM_CLOSED"},
                {"iq", "set", "STANZA_FROM", "c2s@localhost/127.0.0.1_5222_127.0.0.1_58041", "c2s@localhost/127.0.0.1_5222_127.0.0.1_58041", "c2s@localhost/127.0.0.1_5222_127.0.0.1_58041", "sess-man@localhost", "sess-man@localhost", "STREAM_FINISHED"},
                {"iq", "set", "STANZA_FROM", "c2s@localhost/127.0.0.1_5222_127.0.0.1_58041", null, "c2s@localhost/127.0.0.1_5222_127.0.0.1_58041", null, "sess-man@localhost", "STREAM_OPENED"},
                {"iq", "set", "STANZA_FROM", "c2s@localhost/127.0.0.1_5222_127.0.0.1_58041", null, "c2s@localhost/127.0.0.1_5222_127.0.0.1_58041", null, "sess-man@localhost", "TLS_HANDSHAKE_COMPLETE"},
                {"message", "chat", "PACKET_FROM", "c2s@localhost/127.0.0.1_5222_127.0.0.1_58041", "c2s@localhost/127.0.0.1_5222_127.0.0.1_58041", null, "sess-man@localhost", "817382cc4e5bdbbec00ffdec92ba38d04137eedb@dev.me", ""},
                {"presence", null, "PACKET_FROM", "c2s@localhost/127.0.0.1_5222_127.0.0.1_58041", "c2s@localhost/127.0.0.1_5222_127.0.0.1_58041", null, "sess-man@localhost", null, ""},
                {"presence", null, "STANZA_FROM", "3ccb918e3c85c8a3358ae2686abd3b055cda26e3@dev.me/+888000000001", "sess-man@localhost", "3ccb918e3c85c8a3358ae2686abd3b055cda26e3@dev.me/+888000000001", "c2s@localhost/127.0.0.1_5222_127.0.0.1_58041", "3ccb918e3c85c8a3358ae2686abd3b055cda26e3@dev.me", ""},
                {"presence", "subscribe", "PACKET_FROM", "c2s@localhost/127.0.0.1_5222_127.0.0.1_58041", "c2s@localhost/127.0.0.1_5222_127.0.0.1_58041", null, "sess-man@localhost", "17dccb4c4ecb50f546d06137d2a2296ef009087d@dev.me", ""},
                {"presence", "unavailable", "PACKET_FROM", "c2s@localhost/127.0.0.1_5222_127.0.0.1_58041", "c2s@localhost/127.0.0.1_5222_127.0.0.1_58041", null, "sess-man@localhost", null, ""},
                {"starttls", null, "PACKET_FROM", "c2s@localhost/127.0.0.1_5222_127.0.0.1_58041", "c2s@localhost/127.0.0.1_5222_127.0.0.1_58041", null, "sess-man@localhost", null, ""},
                {"success", null, "PACKET_TO?STANZA_TO", "c2s@localhost/127.0.0.1_5222_127.0.0.1_58041", "sess-man@localhost", null, "c2s@localhost/127.0.0.1_5222_127.0.0.1_58041", null, ""},
        });
    }
    //@formatter:on

    @Test
    public void testParameterizedHashCodeForPacket() throws TigaseStringprepException {
        Packet packet = createPacketInstanceFromParameters();

        JID expectedJid = jidInstanceNS(expectedValue);
        int expectedHash = expected.contains("BAREJID") ? expectedJid.getBareJID().hashCode() : expectedJid.hashCode();
        int actualHash = connectionManager.hashCodeForPacket(packet);
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