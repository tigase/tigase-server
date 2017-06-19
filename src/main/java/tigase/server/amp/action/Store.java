/*
 * Store.java
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



package tigase.server.amp.action;

//~--- non-JDK imports --------------------------------------------------------

import tigase.db.*;
import tigase.server.Packet;
import tigase.server.amp.ActionAbstract;
import tigase.server.amp.ActionResultsHandlerIfc;
import tigase.server.amp.MsgRepository;
import tigase.server.amp.cond.ExpireAt;
import tigase.util.TigaseStringprepException;
import tigase.xml.Element;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created: May 1, 2010 11:32:59 AM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class Store
				extends ActionAbstract {
	private static final Logger log  = Logger.getLogger(Store.class.getName());
	private static final String name = "store";

	//~--- fields ---------------------------------------------------------------

	// ~--- fields ---------------------------------------------------------------
	private Thread expiredProcessor          = null;
	private MsgRepositoryIfc repo               = null;
	private final SimpleDateFormat formatter;
	private final SimpleDateFormat formatter2;
	private NonAuthUserRepositoryImpl nonAuthUserRepo;

	{
		formatter = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'" );
		formatter.setTimeZone( TimeZone.getTimeZone( "UTC" ) );
		formatter2 = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss'Z'" );
		formatter2.setTimeZone( TimeZone.getTimeZone( "UTC" ) );
	}

	//~--- methods --------------------------------------------------------------

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
					log.log(Level.INFO,
									"Incorrect expire-at value: " + rule.getAttributeStaticStr("value"),
									e);
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
					Element x = new Element("delay", "Offline Storage", new String[] { "from",
									"stamp", "xmlns" }, new String[] { packet.getStanzaTo().getDomain(),
									stamp, "urn:xmpp:delay" });

					elem.addChild(x);
				}
				repo.storeMessage(packet.getStanzaFrom(), packet.getStanzaTo(), expired, elem, nonAuthUserRepo);
			} catch (UserNotFoundException ex) {
				log.info("User not found for offline message: " + packet);
			}
		}

		return false;
	}

	//~--- get methods ----------------------------------------------------------

	@Override
	public Map<String, Object> getDefaults(Map<String, Object> params) {
		Map<String, Object> defs = super.getDefaults(params);
		String db_uri            = (String) params.get(AMP_MSG_REPO_URI_PARAM);
		String db_cls			 = (String) params.get(AMP_MSG_REPO_CLASS_PARAM);

		if (db_uri == null) {
			db_uri = (String) params.get(RepositoryFactory.GEN_USER_DB_URI);
		}
		if (db_uri != null) {
			defs.put(AMP_MSG_REPO_URI_PROP_KEY, db_uri);
		}
		if (db_cls != null) {
			defs.put(AMP_MSG_REPO_CLASS_PROP_KEY, db_cls);
		}

		return defs;
	}

	@Override
	public String getName() {
		return name;
	}

	//~--- set methods ----------------------------------------------------------

	@Override
	public void setProperties(Map<String, Object> props, ActionResultsHandlerIfc handler) {
		super.setProperties(props, handler);

		UserRepository user_repository = (UserRepository) props.get(RepositoryFactory.SHARED_USER_REPO_PROP_KEY);
		nonAuthUserRepo = new NonAuthUserRepositoryImpl(user_repository, null, true);
		
		String db_uri = (String) props.get(AMP_MSG_REPO_URI_PROP_KEY);
		String db_cls = (String) props.get(AMP_MSG_REPO_CLASS_PROP_KEY);
		
		if (db_uri != null) {
			try {
				repo = MsgRepository.getInstance(db_cls, db_uri);
				Map<String, String> db_props = new HashMap<String, String>(4);

				for (Map.Entry<String, Object> entry : props.entrySet()) {

					// Entry happens to be null for (shared-user-repo-params, null)
					// TODO: Not sure if this is supposed to happen, more investigation is needed.
					if (entry.getValue() != null) {
						log.log(Level.CONFIG,
										"Reading properties: (" + entry.getKey() + ", " + entry.getValue() +
										")");
						if (entry.getValue() instanceof String[]) {
							String[] val = (String[]) entry.getValue();
							db_props.put(entry.getKey(), Stream.of(val).collect(Collectors.joining(",")));
						} else {
							db_props.put(entry.getKey(), entry.getValue().toString());
						}
					}
				}

				// Initialization of repository can be done here and in MessageAmp
				// class so repository related parameters for JDBCMsgRepository
				// should be specified for AMP plugin and AMP component
				repo.initRepository(db_uri, db_props);
			} catch (TigaseDBException ex) {
				repo = null;
				log.log(Level.WARNING, "Problem initializing connection to DB: ", ex);
			}
		}	
		if ((repo != null) && (expiredProcessor == null)) {
			expiredProcessor = new Thread("expired-processor") {
				@Override
				public void run() {
					while (true) {
						Element elem = repo.getMessageExpired(0, true);

						if (elem != null) {
							elem.addAttribute(OFFLINE, "1");
							elem.addAttribute(EXPIRED, "1");
							try {
								resultsHandler.addOutPacket(Packet.packetInstance(elem));
							} catch (TigaseStringprepException ex) {
								log.info("Stringprep error for offline message loaded from DB: " + elem);
							}
						}
					}
				}
			};
			expiredProcessor.setDaemon(true);
			expiredProcessor.start();
		}
	}

	//~--- get methods ----------------------------------------------------------

	// ~--- get methods ----------------------------------------------------------
	private Element getExpireAtRule(Packet packet) {
		Element amp         = packet.getElement().getChild("amp", AMP_XMLNS);
		List<Element> rules = amp.getChildren();
		Element rule        = null;

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

	//~--- methods --------------------------------------------------------------

	// ~--- methods --------------------------------------------------------------
	private void removeExpireAtRule(Packet packet) {
		Element amp         = packet.getElement().getChild("amp", AMP_XMLNS);
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



// ~ Formatted in Sun Code Convention

// ~ Formatted by Jindent --- http://www.jindent.com


//~ Formatted in Tigase Code Convention on 13/02/20
