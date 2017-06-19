/*
 * Broadcast.java
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2014 "Tigase, Inc." <office@tigase.com>
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

import tigase.db.RepositoryFactory;
import tigase.db.TigaseDBException;
import tigase.server.Command;
import tigase.server.Packet;
import tigase.server.Presence;
import tigase.server.amp.ActionResultsHandlerIfc;
import tigase.server.amp.AmpFeatureIfc;
import tigase.server.amp.MsgRepository;
import tigase.util.TigaseStringprepException;
import tigase.xml.Element;
import tigase.xmpp.JID;
import tigase.xmpp.StanzaType;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static tigase.server.amp.cond.ExpireAt.NAME;

/**
 *
 * @author andrzej
 */
public class Broadcast implements AmpFeatureIfc {
	
	private static final Logger log  = Logger.getLogger(Broadcast.class.getName());
	private static final String name = "broadcast";
	
	private MsgRepository repo = null;
	
	private final SimpleDateFormat formatter;
	private final SimpleDateFormat formatter2;
	private ActionResultsHandlerIfc resultsHandler;
	{
		formatter = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'" );
		formatter.setTimeZone( TimeZone.getTimeZone( "UTC" ) );
		formatter2 = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss'Z'" );
		formatter2.setTimeZone( TimeZone.getTimeZone( "UTC" ) );
	}	
	
	public boolean preprocess(Packet packet) {
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "processing packet = {0}", packet.toString());
		}
		if (packet.getElemName() == Presence.ELEM_NAME) {
			sendBroadcastMessage(packet.getStanzaFrom());
			return true;
		}
		
		Element broadcast = packet.getElement().getChild("broadcast", "http://tigase.org/protocol/broadcast");
		if (broadcast == null || packet.getAttributeStaticStr(FROM_CONN_ID) != null) {
			return false;
		}
		
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "processing broadcast packet = {0}", packet);
		}
		
		if (repo != null) {
			if (packet.getStanzaTo().getResource() == null) {
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "setting broadcast request for user {0}", packet.getStanzaTo());
				}
				Element amp = packet.getElement().getChild("amp", AMP_XMLNS);
				Element rule = null;
				for (Element elem : amp.getChildren()) {
					if ("rule".equals(elem.getName()) && "expire-at".equals(elem.getAttributeStaticStr(CONDITION_ATT))) {
						rule = elem;
						break;
					}
				}
				if (rule != null) {
					String value = rule.getAttributeStaticStr("value");
					Date expire = null;
					try {
						if (value != null) {
							if (value.contains(".")) {
								synchronized (formatter) {
									expire = formatter.parse(value);
								}
							} else {
								synchronized (formatter2) {
									expire = formatter2.parse(value);
								}
							}
							
							packet.getElement().removeAttribute(TO_CONN_ID);
							packet.getElement().removeAttribute(TO_RES);
							packet.getElement().removeAttribute(OFFLINE);
							packet.getElement().removeAttribute(FROM_CONN_ID);
							packet.getElement().removeAttribute(EXPIRED);							

							Element msg = packet.getElement().clone();
							msg.removeAttribute("to");

							String msgId = packet.getAttributeStaticStr("id");
							
							MsgRepository.BroadcastMsg bmsg = repo.getBroadcastMsg(msgId);
							boolean needToBroadcast = bmsg == null || !bmsg.needToSend(packet.getStanzaTo());
							
							repo.updateBroadcastMessage(msgId, msg, expire, packet.getStanzaTo().getBareJID());

							if (needToBroadcast) {
								Packet broadcastCmd = Command.BROADCAST_TO_ONLINE.getPacket(packet.getPacketTo(),
										JID.jidInstanceNS("sess-man", packet.getPacketTo().getDomain(), null), StanzaType.get, name);
								Command.addFieldValue(broadcastCmd, "to", packet.getStanzaTo().toString());
								msg = packet.getElement().clone();
								msg.removeAttribute("to");
								msg.setAttribute("xmlns", "http://tigase.org/protocol/broadcast");
								broadcastCmd.getElement().addChild(msg);

								resultsHandler.addOutPacket(broadcastCmd);
							}
						}
					} catch (ParseException ex) {
						log.info("Incorrect " + NAME + " condition value for rule: " + rule);
					}
					return true;
				}
			} else {
				String msgId = packet.getAttributeStaticStr("id");
				MsgRepository.BroadcastMsg msg = repo.getBroadcastMsg(msgId);
				if (msg != null) {
					packet.getElement().removeChild(broadcast);
					msg.markAsSent(packet.getStanzaTo());
					if (log.isLoggable(Level.FINEST)) {
						log.log(Level.FINEST, "marking broadcast of message = {0} for user {1} as done, result = {2}", 
								new Object[]{msgId, packet.getStanzaTo(), msg.needToSend(packet.getStanzaTo())});
					}
				} else {
					if (log.isLoggable(Level.FINEST)) {
						log.log(Level.FINEST, "not found broadcast request with id = {0} for user {1}, keys = {2}", new Object[]{msgId, packet.getStanzaTo(), repo.dumpBroadcastMessageKeys()});
					}
					
				}
				return packet.getPacketTo() == null || !packet.getPacketTo().getDomain().equals(packet.getPacketFrom().getDomain());
			}
		} else {
			log.log(Level.FINEST, "repository is NULL !!");
		}
		return false;
	}

	public void sendBroadcastMessage(JID jid) {
		if (repo != null) {
			for (Object o : repo.getBroadcastMessages()) {
				MsgRepository.BroadcastMsg msg = (MsgRepository.BroadcastMsg) o;
				if (msg.getDelay(TimeUnit.MILLISECONDS) > 0 && msg.needToSend(jid)) {
					try {
						sendBroadcastMessage(jid, msg);
					} catch (TigaseStringprepException ex) {
						log.log(Level.WARNING, "should not happen, contact developer", ex);
					}
				}
			}
		}		
	}
	
	public void sendBroadcastMessage(JID jid, MsgRepository.BroadcastMsg msg) throws TigaseStringprepException {
		Element msgEl = msg.msg.clone();
		msgEl.setAttribute("to", jid.toString());
		Packet p = Packet.packetInstance(msgEl);
		resultsHandler.addOutPacket(p);
	}
	
	@Override
	public String getName() {
		return name;
	}

	/**
	 * Method description
	 *
	 *
	 * @param params
	 *
	 * 
	 */

	public Map<String, Object> getDefaults(Map<String, Object> params) {
		Map<String, Object> defs = new HashMap<String,Object>();
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
	
	/**
	 * Method description
	 *
	 *
	 * @param props
	 * @param handler
	 */

	public void setProperties(Map<String, Object> props, ActionResultsHandlerIfc handler) {
		this.resultsHandler = handler;
		String db_uri = (String) props.get(AMP_MSG_REPO_URI_PROP_KEY);
		String db_cls = (String) props.get(AMP_MSG_REPO_CLASS_PROP_KEY);
		
		if (db_uri != null) {
			try {
				repo = (MsgRepository) MsgRepository.getInstance(db_cls, db_uri);
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
				
				repo.loadMessagesToBroadcast();
			} catch (TigaseDBException ex) {
				repo = null;
				log.log(Level.WARNING, "Problem initializing connection to DB: ", ex);
			}
		}
	}
}
