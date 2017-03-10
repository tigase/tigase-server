/*
 * TestComponent.java
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2012 "Artur Hefczyc" <artur.hefczyc@tigase.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 *
 */



package tigase.server.test;

//~--- non-JDK imports --------------------------------------------------------

import tigase.server.AbstractMessageReceiver;
import tigase.server.Message;
import tigase.server.Packet;

import tigase.stats.StatisticsList;

import tigase.util.TigaseStringprepException;

import tigase.xmpp.JID;
import tigase.xmpp.StanzaType;

//~--- JDK imports ------------------------------------------------------------

import java.util.Collections;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Map;
import java.util.Set;

import javax.script.Bindings;
import tigase.conf.ConfigurationException;

/**
 * A test component used to demonstrate API and for running different kinds of
 * tests on the Tigase server - generate local traffic for performance and load tests.
 * Created: Nov 28, 2009 9:22:36 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class TestComponent
				extends AbstractMessageReceiver {
	private static final String ABUSE_ADDRESS_KEY     = "abuse-address";
	private static final String BAD_WORDS_KEY         = "bad-words";
	private static final String BAD_WORDS_VAR         = "badWords";
	private static final String[] INITIAL_BAD_WORDS   = { "word1", "word2", "word3" };
	private static final String[] INITIAL_WHITE_LIST  = { "admin@localhost" };
	private static final String NOTIFICATION_FREQ_KEY = "notification-freq";
	private static final String PREPEND_TEXT_KEY      = "log-prepend";
	private static final String SECURE_LOGGING_KEY    = "secure-logging";
	private static final String WHITE_LIST_VAR        = "whiteList";
	private static final String WHITELIST_KEY         = "white-list";
	private static final Logger log                   =
		Logger.getLogger(TestComponent.class.getName());

	//~--- fields ---------------------------------------------------------------

	private JID abuseAddress = null;

	/**
	 * This might be changed in one threads while it is iterated in
	 * processPacket(...) in another thread. We expect that changes are very rare
	 * and small, most of operations are just iterations.
	 */
	private Set<String> badWords      = new CopyOnWriteArraySet<String>();
	private int delayCounter          = 0;
	private long messagesCounter      = 0;
	private int notificationFrequency = 10;
	private String prependText        = "Spam detected: ";
	private long spamCounter          = 0;
	private long totalSpamCounter     = 0;

	/**
	 * This might be changed in one threads while it is iterated in
	 * processPacket(...) in another thread. We expect that changes are very rare
	 * and small, most of operations are just contains(...).
	 */
	private Set<String> whiteList = new ConcurrentSkipListSet<String>();
	private boolean secureLogging = false;

	//~--- methods --------------------------------------------------------------

	@Override
	public synchronized void everyMinute() {
		super.everyMinute();
		if ((++delayCounter) >= notificationFrequency) {
			addOutPacket(Message.getMessage(getComponentId(), abuseAddress, StanzaType.chat,
																			"Detected spam messages: " + spamCounter,
																			"Spam counter", null, newPacketId("spam-")));
			delayCounter = 0;
			spamCounter  = 0;
		}
	}

	//~--- get methods ----------------------------------------------------------

	@Override
	public Map<String, Object> getDefaults(Map<String, Object> params) {
		Map<String, Object> defs = super.getDefaults(params);

		Collections.addAll(badWords, INITIAL_BAD_WORDS);
		Collections.addAll(whiteList, INITIAL_WHITE_LIST);
		defs.put(BAD_WORDS_KEY, INITIAL_BAD_WORDS);
		defs.put(WHITELIST_KEY, INITIAL_WHITE_LIST);
		defs.put(PREPEND_TEXT_KEY, prependText);
		defs.put(SECURE_LOGGING_KEY, secureLogging);
		defs.put(ABUSE_ADDRESS_KEY, "admin@localhost");
		defs.put(NOTIFICATION_FREQ_KEY, notificationFrequency);

		return defs;
	}

	@Override
	public String getDiscoCategoryType() {
		return "spam";
	}

	@Override
	public String getDiscoDescription() {
		return "Spam filtering";
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

	//~--- methods --------------------------------------------------------------

	@Override
	public int hashCodeForPacket(Packet packet) {
		if (packet.getStanzaTo() != null) {
			return packet.getStanzaTo().hashCode();
		}

		// This should not happen, every packet must have a destination
		// address, but maybe our SPAM checker is used for checking
		// strange kind of packets too....
		if (packet.getStanzaFrom() != null) {
			return packet.getStanzaFrom().hashCode();
		}

		// If this really happens on your system you should look carefully
		// at packets arriving to your component and decide a better way
		// to calculate hashCode
		return 1;
	}

	@Override
	public void initBindings(Bindings binds) {
		super.initBindings(binds);
		binds.put(BAD_WORDS_VAR, badWords);
		binds.put(WHITE_LIST_VAR, whiteList);
	}

	@Override
	public void processPacket(Packet packet) {

		// Is this packet a message?
		if ("message" == packet.getElemName()) {
			updateServiceDiscoveryItem(getName(), "messages",
																 "Messages processed: [" + (++messagesCounter) + "]",
																 true);

			JID from = packet.getStanzaFrom();

			// Is sender on the whitelist?
			if (!whiteList.contains(from.getBareJID().toString())) {

				// The sender is not on whitelist so let's check the content
				String body = packet.getElemCDataStaticStr(Message.MESSAGE_BODY_PATH);

				if ((body != null) &&!body.isEmpty()) {
					body = body.toLowerCase();
					for (String word : badWords) {
						if (body.contains(word)) {
							log.finest(prependText + packet.toString(secureLogging));
							++spamCounter;
							updateServiceDiscoveryItem(getName(), "spam",
																				 "Spam caught: [" + (++totalSpamCounter) + "]",
																				 true);

							return;
						}
					}
				}
			}
		}

		// Not a SPAM, return it for further processing
		Packet result = packet.swapStanzaFromTo();

		addOutPacket(result);
	}

	@Override
	public int processingInThreads() {
		return Runtime.getRuntime().availableProcessors();
	}

	@Override
	public int processingOutThreads() {
		return Runtime.getRuntime().availableProcessors();
	}

	//~--- set methods ----------------------------------------------------------

	@Override
	public void setProperties(Map<String, Object> props) throws ConfigurationException {
		super.setProperties(props);
		Collections.addAll(badWords, (String[]) props.get(BAD_WORDS_KEY));
		Collections.addAll(whiteList, (String[]) props.get(WHITELIST_KEY));
		prependText   = (String) props.get(PREPEND_TEXT_KEY);
		secureLogging = (Boolean) props.get(SECURE_LOGGING_KEY);
		try {
			abuseAddress = JID.jidInstance((String) props.get(ABUSE_ADDRESS_KEY));
		} catch (TigaseStringprepException ex) {
			log.warning("Incorrect abuseAddress, stringprep error: " +
									(String) props.get(ABUSE_ADDRESS_KEY));
		}
		notificationFrequency = (Integer) props.get(NOTIFICATION_FREQ_KEY);
		updateServiceDiscoveryItem(getName(), null, getDiscoDescription(), "automation",
															 "spam-filtering", true, "tigase:x:spam-filter",
															 "tigase:x:spam-reporting");
	}
}


//~ Formatted in Tigase Code Convention on 13/02/19
