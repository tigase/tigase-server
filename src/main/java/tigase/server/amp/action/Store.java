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
package tigase.server.amp.action;

import tigase.db.MsgRepositoryIfc;
import tigase.db.NonAuthUserRepositoryImpl;
import tigase.db.TigaseDBException;
import tigase.db.UserNotFoundException;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Initializable;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.UnregisterAware;
import tigase.server.Packet;
import tigase.server.amp.ActionAbstract;
import tigase.server.amp.AmpComponent;
import tigase.server.amp.cond.ExpireAt;
import tigase.util.stringprep.TigaseStringprepException;
import tigase.xml.Element;
import tigase.xmpp.Authorization;
import tigase.xmpp.PacketErrorTypeException;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created: May 1, 2010 11:32:59 AM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
*/
@Bean(name = "store", parent = AmpComponent.class, active = true)
public class Store
		extends ActionAbstract
		implements Initializable, UnregisterAware {

	private static final Logger log = Logger.getLogger(Store.class.getName());
	private static final String name = "store";

	private final SimpleDateFormat formatter;
	private final SimpleDateFormat formatter2;
	// ~--- fields ---------------------------------------------------------------
	private Thread expiredProcessor = null;
	@Inject
	private NonAuthUserRepositoryImpl nonAuthUserRepo;
	@Inject
	private MsgRepositoryIfc repo = null;

	{
		formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
		formatter2 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		formatter2.setTimeZone(TimeZone.getTimeZone("UTC"));
	}

	@Override
	public boolean execute(Packet packet, Element rule) {
		if (repo != null) {
			Date expired = null;
			String stamp = null;

			if (packet.getAttributeStaticStr(EXPIRED) == null) {
				if (rule == null) {
					rule = getExpireAtRule(packet);
				}
			} else {
				removeExpireAtRule(packet);
				rule = null;
			}
			if (rule != null) {
				try {
					String value = rule.getAttributeStaticStr("value");
					if (value != null) {
						if (value.contains(".")) {
							synchronized (formatter) {
								expired = formatter.parse(value);
							}
						} else {
							synchronized (formatter2) {
								expired = formatter2.parse(value);
							}
						}
					}
				} catch (Exception e) {
					log.log(Level.CONFIG, "Incorrect expire-at value: " + rule.getAttributeStaticStr("value"), e);
					expired = null;
				}
			}
			synchronized (formatter) {
				stamp = formatter.format(new Date());
			}
			removeTigasePayload(packet);
			try {
				Element elem = packet.getElement();

				if (elem.getChild("delay", "urn:xmpp:delay") == null) {
					Element x = new Element("delay", "Offline Storage", new String[]{"from", "stamp", "xmlns"},
											new String[]{packet.getStanzaTo().getDomain(), stamp, "urn:xmpp:delay"});

					elem.addChild(x);
				}
				repo.storeMessage(packet.getStanzaFrom(), packet.getStanzaTo(), expired, elem, nonAuthUserRepo);
			} catch (UserNotFoundException ex) {
				log.log(Level.CONFIG, "User not found for offline message: " + packet);
			} catch (TigaseDBException ex) {
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "Could not save packet for offline user " + packet, ex);
				}
				try {
					resultsHandler.addOutPacket(Authorization.INTERNAL_SERVER_ERROR.getResponseMessage(packet, null, true));
				} catch (PacketErrorTypeException e) {
					log.finest("Could not sent error for unsaved packet for offline user " + packet);
				}
			}
		}

		return false;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void initialize() {
		if ((repo != null) && (expiredProcessor == null)) {
			expiredProcessor = new Thread("expired-processor") {
				@Override
				public void run() {
					try {
						Thread.sleep(90 * 1000);
						while (true) {
							Element elem = repo.getMessageExpired(0, true);

							if (elem != null) {
								elem.addAttribute(OFFLINE, "1");
								elem.addAttribute(EXPIRED, "1");
								try {
									resultsHandler.addOutPacket(Packet.packetInstance(elem));
								} catch (TigaseStringprepException ex) {
									log.log(Level.CONFIG, "Stringprep error for offline message loaded from DB: " + elem);
								}
							}
							if (Thread.interrupted()) {
								log.log(Level.INFO, "stopping expired-processor");
								expiredProcessor = null;
								return;
							}
						}
					} catch (InterruptedException e) {
						log.log(Level.WARNING, "Could not initialize expired processor", e);
					}
				}
			};
			expiredProcessor.setDaemon(true);
			expiredProcessor.start();
		}

	}

	@Override
	public void beforeUnregister() {
		if (expiredProcessor != null) {
			expiredProcessor.interrupt();
		}
	}

	// ~--- get methods ----------------------------------------------------------
	private Element getExpireAtRule(Packet packet) {
		Element amp = packet.getElement().getChild("amp", AMP_XMLNS);
		List<Element> rules = amp.getChildren();
		Element rule = null;

		if ((rules != null) && (rules.size() > 0)) {
			for (Element r : rules) {
				String cond = r.getAttributeStaticStr(CONDITION_ATT);

				if ((cond != null) && cond.equals(ExpireAt.NAME)) {
					rule = r;

					break;
				}
			}
		}

		return rule;
	}

	// ~--- methods --------------------------------------------------------------
	private void removeExpireAtRule(Packet packet) {
		Element amp = packet.getElement().getChild("amp", AMP_XMLNS);
		List<Element> rules = amp.getChildren();

		if ((rules != null) && (rules.size() > 0)) {
			for (Element r : rules) {
				String cond = r.getAttributeStaticStr(CONDITION_ATT);

				if ((cond != null) && cond.equals(ExpireAt.NAME)) {
					amp.removeChild(r);

					break;
				}
			}
		}
		rules = amp.getChildren();
		if ((rules == null) || (rules.size() == 0)) {
			packet.getElement().removeChild(amp);
		}
	}
}

