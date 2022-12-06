package tigase.server.xmppsession;

import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import tigase.server.Packet;
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
public class SessionManagerHashCodeTest extends TestCase {

    private final SessionManager sessionManager = new SessionManager();
    private final String elementName;
    private final String stanzaType;
    private final String expected;
    private final String expectedValue;
    private final String stanzaFrom;
    private final String stanzaTo;
    private final String packetFrom;
    private final String packetTo;
    private final String command;

    public SessionManagerHashCodeTest(String elementName, String stanzaType, String expected, String expectedValue, String packetFrom, String stanzaFrom, String packetTo, String stanzaTo, String command) {
        this.elementName = elementName;
        this.stanzaType = stanzaType;
        this.expected = expected;
        this.expectedValue = expectedValue;
        this.packetFrom = packetFrom;
        this.stanzaFrom = stanzaFrom;
        this.packetTo = packetTo;
        this.stanzaTo = stanzaTo;
        this.command = command;
    }

    //@formatter:off
    @Parameterized.Parameters(name = "{index}: {0} Type:{1} Expected:{2} {3} = PacketFrom:{4} StanzaFrom:{5} PacketTo:{6} StanzaTo:{7} Command:{8}")
    public static Collection<Object[]> data() {

        String userJid = "3ccb918e3c85c8a3358ae2686abd3b055cda26e3@dev.me";
        String userFullJid = "3ccb918e3c85c8a3358ae2686abd3b055cda26e3@dev.me/+888000000001";
        String c2sJid = "c2s@localhost/127.0.0.1_5222_127.0.0.1_56848";
        String sessionManagerJid = "sess-man@localhost";
        String toUserJid = "817382cc4e5bdbbec00ffdec92ba38d04137eedb@dev.me";

        return Arrays.asList(new Object[][]{
                {"auth", null, "PACKET_FROM", c2sJid, c2sJid, null, sessionManagerJid, null, ""},
                {"iq", "get", "PACKET_FROM", c2sJid, c2sJid, null, sessionManagerJid, userJid, null},
                {"iq", "get", "PACKET_FROM", c2sJid, c2sJid, null, sessionManagerJid, null, null},
                {"iq", "get", "STANZA_FROM", c2sJid, null, c2sJid, null, sessionManagerJid, "GETFEATURES"},
                {"iq", "result", "PACKET_FROM", c2sJid, c2sJid, null, sessionManagerJid, null, null},
                {"iq", "result", "STANZA_TO", userFullJid, sessionManagerJid, userJid, c2sJid, userFullJid, null},
                {"iq", "result", "STANZA_TO", userFullJid, sessionManagerJid, null, c2sJid, userFullJid, null},
                {"iq", "result", "STANZA_TO", c2sJid, sessionManagerJid, sessionManagerJid, c2sJid, c2sJid, "GETFEATURES"},
                {"iq", "result", "STANZA_TO", c2sJid, sessionManagerJid, sessionManagerJid, c2sJid, c2sJid, null},
                {"iq", "set", "PACKET_FROM", c2sJid, c2sJid, null, sessionManagerJid, null, null},
                {"iq", "set", "STANZA_FROM", c2sJid, null, c2sJid, null, sessionManagerJid, "STREAM_OPENED"},
                {"iq", "set", "STANZA_FROM", c2sJid, null, c2sJid, null, sessionManagerJid, "TLS_HANDSHAKE_COMPLETE"},
                {"iq", "set", "STANZA_TO", userFullJid, sessionManagerJid, null, c2sJid, userFullJid, null},
                {"iq", "set", "STANZA_TO", c2sJid, sessionManagerJid, sessionManagerJid, null, c2sJid, "STARTTLS"},
                {"iq", "set", "STANZA_TO", c2sJid, sessionManagerJid, sessionManagerJid, null, c2sJid, "USER_LOGIN"},
                {"message", "chat", "PACKET_FROM", c2sJid, c2sJid, null, sessionManagerJid, toUserJid, ""},
                {"message", "chat", "STANZA_FROM", userFullJid, sessionManagerJid, userFullJid, null, toUserJid, ""},
                {"message", null, "STANZA_TO.BAREJID", "push@dev.me", sessionManagerJid, "dev.me", null, "push@dev.me", ""},
                {"presence", null, "PACKET_FROM", c2sJid, c2sJid, null, sessionManagerJid, null, ""},
                {"presence", null, "STANZA_FROM", userFullJid, sessionManagerJid, userFullJid, c2sJid, userJid, ""},
                {"presence", "subscribe", "PACKET_FROM", c2sJid, c2sJid, null, sessionManagerJid, toUserJid, ""},
                {"presence", "subscribe", "STANZA_TO.BAREJID", toUserJid, sessionManagerJid, userJid, null, toUserJid, ""},
                {"starttls", null, "PACKET_FROM", c2sJid, c2sJid, null, sessionManagerJid, null, ""},
                {"success", null, "PACKET_TO", c2sJid, sessionManagerJid, null, c2sJid, null, ""},
        });
    }
    //@formatter:on

    @Override
    @Before
    public void setUp() throws Exception {
        sessionManager.setCompId(jidInstanceNS("sess-man@localhost"));
    }

    @Test
    public void testParameterizedHashCodeForPacket() throws TigaseStringprepException {
        Packet packet = createPacketInstanceFromParameters();

        JID expectedJid = jidInstanceNS(expectedValue);
        int expectedHash = expected.contains("BAREJID") ? expectedJid.getBareJID().hashCode() : expectedJid.hashCode();
        int actualHash = sessionManager.hashCodeForPacket(packet);
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