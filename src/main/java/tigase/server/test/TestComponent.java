/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2008 "Artur Hefczyc" <artur.hefczyc@tigase.org>
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 * 
 * $Rev$
 * Last modified by $Author$
 * $Date$
 */

package tigase.server.test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.script.Bindings;
import tigase.server.AbstractMessageReceiver;
import tigase.server.Packet;
import tigase.stats.StatisticsList;
import tigase.util.JIDUtils;
import tigase.xmpp.StanzaType;

/**
 * A test component used to demonstrate API and for running different kinds of
 * tests on the Tigase server - generate local traffic for performance and load tests.
 * Created: Nov 28, 2009 9:22:36 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class TestComponent extends AbstractMessageReceiver {

	private static final Logger log =
			Logger.getLogger(TestComponent.class.getName());

	private static final String BAD_WORDS_KEY = "bad-words";
	private static final String WHITELIST_KEY = "white-list";
	private static final String PREPEND_TEXT_KEY = "log-prepend";
	private static final String SECURE_LOGGING_KEY = "secure-logging";
	private static final String ABUSE_ADDRESS_KEY = "abuse-address";
	private static final String NOTIFICATION_FREQ_KEY = "notification-freq";

	private static final String BAD_WORDS_VAR = "badWords";
	private static final String WHITE_LIST_VAR = "whiteList";
	private static final String[] INITIAL_BAD_WORDS = {"word1", "word2", "word3"};
	private static final String[] INITIAL_WHITE_LIST = {"admin@localhost"};

	/**
	 * This might be changed in one threads while it is iterated in
	 * processPacket(...) in another thread. We expect that changes are very rare
	 * and small, most of operations are just contains(...) and iteration.
	 */
	private Set<String> badWords = new CopyOnWriteArraySet<String>();
	/**
	 * This might be changed in one threads while it is iterated in
	 * processPacket(...) in another thread. We expect that changes are very rare
	 * and small, most of operations are just contains(...) and iteration.
	 */
	private Set<String> whiteList = new CopyOnWriteArraySet<String>();
	private String prependText = "Spam detected: ";
	private String abuseAddress = "abuse@locahost";
	private int notificationFrequency = 10;
	private int delayCounter = 0;
	private boolean secureLogging = false;
	private long spamCounter = 0;
	private long totalSpamCounter = 0;
	private long messagesCounter = 0;

	@Override
	public void processPacket(Packet packet) {
		// Is this packet a message?
		if ("message" == packet.getElemName()) {
			updateServiceDiscoveryItem(getName(), "messages",
					"Messages processed: [" + (++messagesCounter) + "]", true);
			String from = JIDUtils.getNodeID(packet.getElemFrom());
			// Is sender on the whitelist?
			if (!whiteList.contains(from)) {
				// The sender is not on whitelist so let's check the content
				String body = packet.getElemCData("/message/body");
				if (body != null && !body.isEmpty()) {
					body = body.toLowerCase();
					for (String word : badWords) {
						if (body.contains(word)) {
							log.finest(prependText + packet.toString(secureLogging));
							++spamCounter;
							updateServiceDiscoveryItem(getName(), "spam", "Spam caught: [" +
									(++totalSpamCounter) + "]", true);
							return;
						}
					}
				}
			}
		}
		// Not a SPAM, return it for further processing
		Packet result = packet.swapElemFromTo();
		addOutPacket(result);
	}

	@Override
	public int processingThreads() {
		return Runtime.getRuntime().availableProcessors();
	}

	@Override
	public int hashCodeForPacket(Packet packet) {
		if (packet.getElemTo() != null) {
			return packet.getElemTo().hashCode();
		}
		// This should not happen, every packet must have a destination
		// address, but maybe our SPAM checker is used for checking
		// strange kind of packets too....
		if (packet.getElemFrom() != null) {
			return packet.getElemFrom().hashCode();
		}
		// If this really happens on your system you should look carefully
		// at packets arriving to your component and decide a better way
		// to calculate hashCode
		return 1;
	}

	@Override
	public Map<String, Object> getDefaults(Map<String, Object> params) {
		Map<String, Object> defs = super.getDefaults(params);
		Collections.addAll(badWords, INITIAL_BAD_WORDS);
		Collections.addAll(whiteList, INITIAL_WHITE_LIST);
		defs.put(BAD_WORDS_KEY, INITIAL_BAD_WORDS);
		defs.put(WHITELIST_KEY, INITIAL_WHITE_LIST);
		defs.put(PREPEND_TEXT_KEY, prependText);
		defs.put(SECURE_LOGGING_KEY, secureLogging);
		defs.put(ABUSE_ADDRESS_KEY, abuseAddress);
		defs.put(NOTIFICATION_FREQ_KEY, notificationFrequency);
		return defs;
	}

	@Override
	public void setProperties(Map<String, Object> props) {
		super.setProperties(props);
		Collections.addAll(badWords, (String[])props.get(BAD_WORDS_KEY));
		Collections.addAll(whiteList, (String[])props.get(WHITELIST_KEY));
		prependText = (String)props.get(PREPEND_TEXT_KEY);
		secureLogging = (Boolean)props.get(SECURE_LOGGING_KEY);
		abuseAddress = (String)props.get(ABUSE_ADDRESS_KEY);
		notificationFrequency = (Integer)props.get(NOTIFICATION_FREQ_KEY);
		updateServiceDiscoveryItem(getName(), null, getDiscoDescription(),
				"automation", "spam-filtering", true,
				"tigase:x:spam-filter", "tigase:x:spam-reporting");
	}

	@Override
	public synchronized void everyMinute() {
		super.everyMinute();
		if ((++delayCounter) >= notificationFrequency) {
			addOutPacket(Packet.getMessage(abuseAddress, getComponentId(),
					StanzaType.chat, "Detected spam messages: " + spamCounter,
					"Spam counter", null, newPacketId("spam-")));
			delayCounter = 0;
			spamCounter = 0;
		}
	}

	@Override
	public String getDiscoDescription() {
		return "Spam filtering";
	}

	@Override
	public String getDiscoCategoryType() {
		return "spam";
	}

	@Override
	public void getStatistics(StatisticsList list) {
		super.getStatistics(list);
		list.add(getName(), "Spam messages found", totalSpamCounter, Level.INFO);
		list.add(getName(), "All messages processed", messagesCounter, Level.FINE);
		if (list.checkLevel(Level.FINEST)) {
			// Some very expensive statistics generation code...
		}
	}

	@Override
	public void initBindings(Bindings binds) {
		super.initBindings(binds);
		binds.put(BAD_WORDS_VAR, badWords);
		binds.put(WHITE_LIST_VAR, whiteList);
	}

}
