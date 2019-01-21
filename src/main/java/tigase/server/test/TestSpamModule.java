/**
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
package tigase.server.test;

import tigase.component.exceptions.ComponentException;
import tigase.component.modules.AbstractModule;
import tigase.criteria.Criteria;
import tigase.criteria.ElementCriteria;
import tigase.criteria.Or;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.config.ConfigField;
import tigase.server.Message;
import tigase.server.Packet;
import tigase.util.stringprep.TigaseStringprepException;
import tigase.xmpp.StanzaType;
import tigase.xmpp.jid.JID;

import javax.script.Bindings;
import java.util.Arrays;
import java.util.logging.Logger;

@Bean(name = "test-spam-module", parent = TestComponent.class, active = true)
public class TestSpamModule
		extends AbstractModule {

	private static final Logger log = Logger.getLogger(TestSpamModule.class.getCanonicalName());
	private static final String BAD_WORDS_KEY = "bad-words";
	private static final String WHITELIST_KEY = "white-list";
	/**
	 * This might be changed in one threads while it is iterated in processPacket(...) in another thread. We expect that
	 * changes are very rare and small, most of operations are just iterations.
	 */
	@ConfigField(desc = "Bad words", alias = "bad-words")
	protected String[] badWords = {"word1", "word2", "word3"};
	@ConfigField(desc = "White listed addresses", alias = "white-list")
	protected String[] whiteList = {"admin@localhost"};
	private Criteria CRITERIA = ElementCriteria.name("message");
	@ConfigField(desc = "Abuse notification address", alias = "abuse-address")
	private JID abuseAddress = JID.jidInstanceNS("abuse@locahost");
	@Inject
	private TestComponent component;
	private int delayCounter = 0;
	private long messagesCounter = 0;
	@ConfigField(desc = "Frequency of notification", alias = "notification-frequency")
	private int notificationFrequency = 10;
	@ConfigField(desc = "Logged packet types", alias = "packet-types")
	private String[] packetTypes = {"message", "presence", "iq"};
	@ConfigField(desc = "Prefix", alias = "log-prepend")
	private String prependText = "Spam detected: ";
	@ConfigField(desc = "Secure logging", alias = "secure-logging")
	private boolean secureLogging = false;
	private long spamCounter = 0;
	private long totalSpamCounter = 0;

	public void everyMinute() {
		if ((++delayCounter) >= notificationFrequency) {
			write(Message.getMessage(abuseAddress, component.getComponentId(), StanzaType.chat,
									 "Detected spam messages: " + spamCounter, "Spam counter", null,
									 component.newPacketId("spam-")));
			delayCounter = 0;
			spamCounter = 0;
		}
	}

	@Override
	public Criteria getModuleCriteria() {
		return CRITERIA;
	}

	@Override
	public void process(Packet packet) throws ComponentException, TigaseStringprepException {
		// Is this packet a message?
		if ("message" == packet.getElemName()) {
			messagesCounter++;
			String from = packet.getStanzaFrom().toString();
			// Is sender on the whitelist?
			if (Arrays.binarySearch(whiteList, from) < 0) {
				// The sender is not on whitelist so let's check the content
				String body = packet.getElemCDataStaticStr(Message.MESSAGE_BODY_PATH);
				if (body != null && !body.isEmpty()) {
					body = body.toLowerCase();
					for (String word : badWords) {
						if (body.contains(word)) {
							log.finest(prependText + packet.toString(secureLogging));
							++spamCounter;
							component.updateServiceDiscoveryItem(component.getName(), "spam",
																 "Spam caught: [" + (++totalSpamCounter) + "]", true);
							return;
						}
					}
				}
			}
		}

		// Not a SPAM, return it for further processing
		Packet result = packet.swapFromTo();
		write(result);
	}

	public long getMessagesCounter() {
		return messagesCounter;
	}

	public long getTotalSpamCounter() {
		return totalSpamCounter;
	}

	public void setPacketTypes(String[] packetTypes) {
		this.packetTypes = packetTypes;
		Criteria crit = new Or();
		for (String packetType : packetTypes) {
			crit.add(ElementCriteria.name(packetType));
		}
		CRITERIA = crit;
	}

	public void initBindings(Bindings binds) {
		binds.put(BAD_WORDS_KEY, badWords);
		binds.put(WHITELIST_KEY, whiteList);
	}

}
