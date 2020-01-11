/*
 * Tigase XMPP Server - The instant messaging server
 * Copyright (C) 2004 Tigase, Inc. (office@tigase.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 */
package tigase.xmpp.impl.push;

import tigase.kernel.beans.Bean;
import tigase.server.Message;
import tigase.server.Packet;
import tigase.util.Base64;
import tigase.xml.Element;
import tigase.xmpp.StanzaType;
import tigase.xmpp.XMPPException;
import tigase.xmpp.XMPPResourceConnection;
import tigase.xmpp.jid.BareJID;
import tigase.xmpp.jid.JID;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.Charset;
import java.security.Key;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Bean(name = "encrypted", parent = PushNotifications.class, active = true)
public class EncryptedPushNotificationExtension implements PushNotificationsExtension {

	private static final Logger log = Logger.getLogger(EncryptedPushNotificationExtension.class.getCanonicalName());

	public static final String XMLNS = "tigase:push:encrypt:0";
	private static final String AES128GCM_FEATURE = "tigase:push:encrypt:aes-128-gcm";
	private static final Charset UTF8 = Charset.forName("UTF-8");

	private static final Element[] DISCO_FEATURES = new Element[]{
			new Element("feature", new String[]{"var"}, new String[]{XMLNS}),
			new Element("feature", new String[]{"var"}, new String[]{AES128GCM_FEATURE})};

	private final SecureRandom random = new SecureRandom();

	@Override
	public Element[] getDiscoFeatures() {
		return DISCO_FEATURES;
	}

	@Override
	public boolean shouldSendNotification(Packet packet, BareJID userJid, XMPPResourceConnection session)
			throws XMPPException {
		return false;
	}

	@Override
	public void processEnableElement(Element enableEl, Element settingsEl) {
		Element encryptEl = enableEl.getChild("encrypt", XMLNS);
		if (encryptEl == null) {
			return;
		}
		
		settingsEl.addChild(encryptEl);
	}

	@Override
	public void prepareNotificationPayload(Element pushServiceSettings, Packet packet, long msgCount, Element notification) {
		Element encryptEl = pushServiceSettings.getChild("encrypt", XMLNS);
		if (encryptEl == null || packet == null) {
			return;
		}
		String alg = encryptEl.getAttributeStaticStr("alg");
		long maxSizeBytes = Optional.ofNullable(encryptEl.getAttributeStaticStr("max-size"))
				.map(Integer::parseInt)
				.orElse(Integer.MAX_VALUE);
		String keyStr = encryptEl.getCData();

		if (alg == null || keyStr == null) {
			return;
		}

		int maxSize = (int) ((maxSizeBytes * 6) / 8);

		if (!alg.equalsIgnoreCase("aes-128-gcm")) {
			return;
		}

		Element x = notification.getChild("x", "jabber:x:data");
		if (x != null) {
			notification.removeChild(x);
		}

		Map<String, Object> payload = new HashMap<>();
		payload.put("unread", msgCount);
		payload.put("sender", packet.getStanzaFrom().getBareJID());

		if (packet.getElemName() == Message.ELEM_NAME) {
			if (packet.getType() == StanzaType.groupchat) {
				payload.put("type", "groupchat");
				String nickname = packet.getStanzaFrom().getResource();
				if (nickname != null) {
					payload.put("nickname", nickname);
				}
			} else {
				payload.put("type", "chat");
			}
		}

		String content = valueToString(payload);

		String body = packet.getElemCDataStaticStr(tigase.server.Message.MESSAGE_BODY_PATH);
		if (body != null) {
			int currentContentLength = content.getBytes(UTF8).length + 64;
			while (maxSize < (currentContentLength + body.getBytes(UTF8).length)) {
				body = body.substring(0, maxSize - currentContentLength);
			}
			payload.put("message", body);
			content = valueToString(payload);
		}

		try {
			Key key = new SecretKeySpec(Base64.decode(keyStr), "AES");
			byte[] iv = new byte[12];
			random.nextBytes(iv);
			GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(128, iv);

			Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
			cipher.init(Cipher.ENCRYPT_MODE, key, gcmParameterSpec);

			byte[] data = cipher.doFinal(content.getBytes(UTF8));

			Element encryped = new Element("encrypted", Base64.encode(data));
			encryped.addAttribute("iv", Base64.encode(iv));
			encryped.setXMLNS(XMLNS);
			notification.addChild(encryped);
		} catch (Throwable ex) {
			log.log(Level.WARNING, "Could not encode payload", ex);
		}
	}

	private static String valueToString(Object value) {
		if (value instanceof Number) {
			return value.toString();
		} else if (value instanceof String | value instanceof BareJID | value instanceof JID) {
			return escapeValue(value.toString());
		} else if (value instanceof List) {
			return "[" + ((List) value).stream().map(EncryptedPushNotificationExtension::valueToString).collect(Collectors.joining(",")) + "]";
		} else if (value instanceof Map) {
			return "{" + ((Stream<Map.Entry>) ((Map) value).entrySet().stream()).map(
					(Map.Entry e) -> "\"" + e.getKey() + "\" : " + valueToString(e.getValue()))
					.collect(Collectors.joining(",")) + "}";
		} else {
			return "null";
		}
	}

	private static String escapeValue(String in) {
		return "\"" + in.replace("\"", "\\\"") + "\"";
	}
}
