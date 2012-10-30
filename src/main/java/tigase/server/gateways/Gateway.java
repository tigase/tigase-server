/*
 *   Tigase Jabber/XMPP Server
 *  Copyright (C) 2004-2012 "Artur Hefczyc" <artur.hefczyc@tigase.org>
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
 *
 * $Rev$
 * Last modified by $Author$
 * $Date$
 */

package tigase.server.gateways;

//~--- non-JDK imports --------------------------------------------------------

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import tigase.conf.Configurable;
import tigase.db.RepositoryFactory;
import tigase.db.TigaseDBException;
import tigase.db.UserExistsException;
import tigase.db.UserRepository;
import tigase.disco.ServiceEntity;
import tigase.disco.ServiceIdentity;
import tigase.disco.XMPPService;
import tigase.server.AbstractMessageReceiver;
import tigase.server.Packet;
import tigase.util.DBUtils;
import tigase.util.TigaseStringprepException;
import tigase.xml.Element;
import tigase.xml.XMLUtils;
import tigase.xmpp.Authorization;
import tigase.xmpp.BareJID;
import tigase.xmpp.JID;
import tigase.xmpp.PacketErrorTypeException;
import tigase.xmpp.StanzaType;

//~--- classes ----------------------------------------------------------------

/**
 * Describe class Gateway here. Created: Thu Nov 8 08:54:23 2007
 * 
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class Gateway extends AbstractMessageReceiver implements Configurable,
		XMPPService, GatewayListener {
	private static final String AUTHORIZED_KEY = "authorized-key";

	/** Field description */
	public static final String GEN_GW_ADMINS = "--gen-gw-admins";

	/** Field description */
	public static final String GEN_GW_DB = "--gen-gw-db";

	/** Field description */
	public static final String GEN_GW_DB_URI = "--gen-gw-db-uri";

	/** Field description */
	public static final String GW_CLASS_NAME_PROP_KEY = "gw-class-name";

	/** Field description */
	public static final String GW_CLASS_NAME_PROP_VAL =
			"tigase.extras.gateway.MsnConnection";

	/** Field description */
	public static final String GW_DOMAIN_NAME_PROP_KEY = "gw-domain-name";

	/** Field description */
	public static final String GW_DOMAIN_NAME_PROP_VAL = "msn.localhost";

	/** Field description */
	public static final String GW_MODERATED_PROP_KEY = "is-moderated";

	/** Field description */
	public static final String GW_REPO_CLASS_PROP_KEY = "gw-repo-class";

	/** Field description */
	public static final String GW_REPO_URL_PROP_KEY = "gw-repo-url";
	private static final String NAME_KEY = "authorized-key";
	private static final String PRESENCE_ELNAME = "presence";

	// public static final String HOSTNAMES_PROP_KEY = "hostnames";
	// public String[] HOSTNAMES_PROP_VAL = {"localhost", "hostname"};
	private static final String PRESENCE_SHOW = "presence-show";
	private static final String PRESENCE_TYPE = "presence-type";

	/**
	 * Private logger for class instancess.
	 */
	private static Logger log = Logger.getLogger("tigase.server.gateways.Gateway");

	/** Field description */
	public static final boolean GW_MODERATED_PROP_VAL = false;
	private static final String moderated_false = "false";
	private static final String moderated_key = "moderated-key";
	private static final String moderated_true = "true";
	private static final String password_key = "password-key";
	private static final String username_key = "user-name-key";

	private String[] ADMINS_PROP_VAL = { "admin@localhost", "admin@hostname" };

	// private String[] hostnames = HOSTNAMES_PROP_VAL;
	// private String gw_hostname = GW_DOMAIN_NAME_PROP_VAL;
	private String gw_desc = "empty";
	private String gw_name = "Undefined";
	private String gw_type = "unknown";
	private UserRepository repository = null;
	private ServiceEntity serviceEntity = null;
	private boolean is_moderated = GW_MODERATED_PROP_VAL;
	private Map<String, GatewayConnection> gw_connections =
			new LinkedHashMap<String, GatewayConnection>();
	private String gw_class_name = GW_CLASS_NAME_PROP_VAL;
	private String[] admins = ADMINS_PROP_VAL;

	/**
	 * Method description
	 * 
	 * @param jid
	 * @return
	 */
	@Override
	public String decodeLegacyName(String jid) {
		return jid.split("@")[0].replace("%", "@");
	}

	/**
	 * Method description
	 * 
	 * @param legacyName
	 * @return
	 */
	@Override
	public String formatJID(String legacyName) {
		return XMLUtils.escape(legacyName.replace("@", "%") + "@" + getComponentId());
	}

	/**
	 * Method description
	 * 
	 * @param gc
	 * @param exc
	 */
	@Override
	public void gatewayException(GatewayConnection gc, Throwable exc) {
		log.log(Level.WARNING, "Gateway exception", exc);
	}

	/**
	 * Method description
	 * 
	 * @param params
	 * @return
	 */
	@Override
	public Map<String, Object> getDefaults(final Map<String, Object> params) {
		Map<String, Object> defs = super.getDefaults(params);
		String repo_class = XML_REPO_CLASS_PROP_VAL;
		String repo_uri = XML_REPO_URL_PROP_VAL;
		String[] db_params = DBUtils.decodeDBParams(params, GEN_GW_DB, GEN_USER_DB);

		if (db_params[0] != null) {
			repo_class = db_params[0];
		}

		if (db_params[1] != null) {
			repo_uri = db_params[1];
		}

		if (params.get(GEN_GW_DB_URI) != null) {
			repo_uri = (String) params.get(GEN_GW_DB_URI);
		} else {
			if (params.get(GEN_USER_DB_URI) != null) {
				repo_uri = (String) params.get(GEN_USER_DB_URI);
			} // end of if (params.get(GEN_USER_DB_URI) != null)
		} // end of else

		defs.put(GW_REPO_CLASS_PROP_KEY, repo_class);
		defs.put(GW_REPO_URL_PROP_KEY, repo_uri);

		if (params.get(GEN_GW_ADMINS) != null) {
			ADMINS_PROP_VAL = ((String) params.get(GEN_GW_ADMINS)).split(",");
		} else {
			if (params.get(GEN_ADMINS) != null) {
				ADMINS_PROP_VAL = ((String) params.get(GEN_ADMINS)).split(",");
			} else {
				ADMINS_PROP_VAL = new String[1];
				ADMINS_PROP_VAL[0] = "admin@" + getDefHostName();
			}
		} // end of if (params.get(GEN_SREC_ADMINS) != null) else

		defs.put(ADMINS_PROP_KEY, ADMINS_PROP_VAL);
		defs.put(GW_CLASS_NAME_PROP_KEY, GW_CLASS_NAME_PROP_VAL);
		defs.put(GW_MODERATED_PROP_KEY, GW_MODERATED_PROP_VAL);
		defs.put(GW_DOMAIN_NAME_PROP_KEY, GW_DOMAIN_NAME_PROP_VAL);

		// if (params.get(GEN_VIRT_HOSTS) != null) {
		// HOSTNAMES_PROP_VAL = ((String)params.get(GEN_VIRT_HOSTS)).split(",");
		// } else {
		// HOSTNAMES_PROP_VAL = DNSResolver.getDefHostNames();
		// }
		// hostnames = new String[HOSTNAMES_PROP_VAL.length];
		// int i = 0;
		// for (String host: HOSTNAMES_PROP_VAL) {
		// hostnames[i++] = getName() + "." + host;
		// }
		// defs.put(HOSTNAMES_PROP_KEY, hostnames);
		return defs;
	}

	/**
	 * Method description
	 * 
	 * @param from
	 * @return
	 */
	@Override
	public List<Element> getDiscoFeatures(JID from) {
		return null;
	}

	/**
	 * Method description
	 * 
	 * @param node
	 * @param jid
	 * @param from
	 * @return
	 */
	@Override
	public Element getDiscoInfo(String node, JID jid, JID from) {
		if ((jid != null) && jid.toString().startsWith(getName() + ".")) {
			return serviceEntity.getDiscoInfo(node);
		}

		return null;
	}

	/**
	 * Method description
	 * 
	 * @param node
	 * @param jid
	 * @param from
	 * @return
	 */
	@Override
	public List<Element> getDiscoItems(String node, JID jid, JID from) {
		if (jid.toString().startsWith(getName() + ".")) {
			return serviceEntity.getDiscoItems(node, null);
		} else {
			return Arrays.asList(serviceEntity.getDiscoItem(null, getName() + "." + jid));
		}
	}

	/**
	 * Method description
	 * 
	 * @param jid
	 * @return
	 */
	@Override
	public boolean isAdmin(JID jid) {
		for (String adm : admins) {
			if (adm.equals(jid.getBareJID().toString())) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Method description
	 * 
	 * @param gc
	 */
	@Override
	public void loginCompleted(GatewayConnection gc) {
		JID[] jids = gc.getAllJids();

		for (JID username : jids) {
			try {
				addOutPacket(Packet.packetInstance(new Element(PRESENCE_ELNAME, new String[] {
						"from", "to" }, new String[] { getComponentId().toString(),
						username.toString() })));
			} catch (TigaseStringprepException ex) {
				Logger.getLogger(Gateway.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
	}

	/**
	 * Method description
	 * 
	 * @param gc
	 */
	@Override
	public void logout(GatewayConnection gc) {
		JID[] jids = gc.getAllJids();

		for (JID username : jids) {
			try {
				addOutPacket(Packet.packetInstance(new Element(PRESENCE_ELNAME, new String[] {
						"from", "to", "type" }, new String[] { getComponentId().toString(),
						username.toString(), "unavailable" })));
			} catch (TigaseStringprepException ex) {
				Logger.getLogger(Gateway.class.getName()).log(Level.SEVERE, null, ex);
			}

			List<RosterItem> roster = gc.getRoster();

			for (RosterItem item : roster) {
				String from = formatJID(item.getBuddyId());
				Element pres_el =
						new Element(PRESENCE_ELNAME, new String[] { "to", "from", "type" },
								new String[] { username.toString(), from, "unavailable" });

				try {
					Packet presence = Packet.packetInstance(pres_el);

					if (log.isLoggable(Level.FINEST)) {
						log.finest("Sending out presence: " + presence);
					}

					addOutPacket(presence);
				} catch (TigaseStringprepException ex) {
					Logger.getLogger(Gateway.class.getName()).log(Level.SEVERE, null, ex);
				}
			}
		}

		gw_connections.remove(jids[0]);
	}

	/**
	 * Method description
	 * 
	 * @param packet
	 */
	@Override
	public void packetReceived(Packet packet) {
		addOutPacket(packet);
	}

	/**
	 * Method description
	 * 
	 * @param packet
	 */
	@Override
	public void processPacket(final Packet packet) {
		try {
			if (packet.getStanzaTo() == null) {
				log.warning("Bad packet, 'to' is null: " + packet.toString());

				return;
			}

			if (packet.getElemName() == PRESENCE_ELNAME) {
				processPresence(packet);

				return;
			}

			if (packet.getStanzaTo().equals(getComponentId())) {

				// Local processing.
				log.fine("Local packet: " + packet.toString());
				processLocalPacket(packet);

				return;
			}

			GatewayConnection conn = findConnection(packet, false);

			if (conn != null) {
				try {
					conn.sendMessage(packet);
				} catch (GatewayException e) {
					log.log(Level.WARNING, "Error initializing gateway connection", e);
				}
			} else {
				if (log.isLoggable(Level.FINER)) {
					log.finer("Gateway not connected, sending packet back: " + packet.toString());
				}

				addOutPacket(Authorization.SERVICE_UNAVAILABLE.getResponseMessage(packet,
						"Gateway is not connected.", true));
			}
		} catch (TigaseStringprepException ex) {
			Logger.getLogger(Gateway.class.getName()).log(Level.SEVERE, null, ex);
		} catch (PacketErrorTypeException e) {
			log.info("This must have been an error already, dropping: " + packet.toString()
					+ ", exception: " + e);
		}
	}

	/**
	 * Method description
	 * 
	 * @param props
	 */
	@Override
	public void setProperties(final Map<String, Object> props) {
		super.setProperties(props);

		// hostnames = (String[])props.get(HOSTNAMES_PROP_KEY);
		// if (hostnames == null || hostnames.length == 0) {
		// log.warning("Hostnames definition is empty, setting 'localhost'");
		// hostnames = new String[] {getName() + ".localhost"};
		// } // end of if (hostnames == null || hostnames.length == 0)
		// Arrays.sort(hostnames);
		// clearRoutings();
		// for (String host: hostnames) {
		// addRouting(host);
		// } // end of for ()
		gw_class_name = (String) props.get(GW_CLASS_NAME_PROP_KEY);

		GatewayConnection gc = gwInstance();

		if (gc != null) {
			gw_type = gc.getType();
			gw_name = gc.getName();
			gw_desc = gc.getPromptMessage();
		}

		serviceEntity = new ServiceEntity(getName(), null, "Transport");
		serviceEntity.addIdentities(new ServiceIdentity("gateway", gw_type, gw_name));
		serviceEntity.addFeatures(DEF_FEATURES);
		serviceEntity.addFeatures("jabber:iq:register", "jabber:iq:gateway");

		// serviceEntity.addFeatures("jabber:iq:register");
		admins = (String[]) props.get(ADMINS_PROP_KEY);
		Arrays.sort(admins);

		// gw_hostname = (String)props.get(GW_DOMAIN_NAME_PROP_KEY);
		// addRouting(gw_hostname);
		is_moderated = (Boolean) props.get(GW_MODERATED_PROP_KEY);

		try {
			String cls_name = (String) props.get(GW_REPO_CLASS_PROP_KEY);
			String res_uri = (String) props.get(GW_REPO_URL_PROP_KEY);

			// if (!res_uri.contains("autoCreateUser=true")) {
			// res_uri += "&autoCreateUser=true";
			// } // end of if (!res_uri.contains("autoCreateUser=true"))
			repository = RepositoryFactory.getUserRepository(cls_name, res_uri, null);

			try {
                                if (!repository.userExists(getComponentId().getBareJID())) {
                                        repository.addUser(getComponentId().getBareJID());
                                }
			} catch (UserExistsException e) { /* Ignore, this is correct and expected */
			}
		} catch (Exception e) {
			log.log(Level.SEVERE, "Can't initialize repository", e);
		} // end of try-catch
	}

	/**
	 * Method description
	 * 
	 * @param gc
	 * @param item
	 */
	@Override
	public void updateStatus(GatewayConnection gc, RosterItem item) {
		JID[] jids = gc.getAllJids();
		JID id = jids[0];
		String from = formatJID(item.getBuddyId());
		String roster_node = id.toString() + "/roster/" + item.getBuddyId();

		try {
			if (item.getStatus().getType() != null) {
				repository.setData(getComponentId().getBareJID(), roster_node, PRESENCE_TYPE,
						item.getStatus().getType());
			} else {
				repository.setData(getComponentId().getBareJID(), roster_node, PRESENCE_TYPE,
						"null");
			}

			if (item.getStatus().getShow() != null) {
				repository.setData(getComponentId().getBareJID(), roster_node, PRESENCE_SHOW,
						item.getStatus().getShow());
			} else {
				repository.setData(getComponentId().getBareJID(), roster_node, PRESENCE_SHOW,
						"null");
			}
		} catch (TigaseDBException e) {
			log.log(Level.WARNING, "Problem updating repository data", e);
		}

		for (JID username : jids) {
			Element pres_el =
					new Element(PRESENCE_ELNAME, new String[] { "to", "from" }, new String[] {
							username.toString(), from });

			if (item.getStatus().getType() != null) {
				pres_el.setAttribute("type", item.getStatus().getType());
			}

			if (item.getStatus().getShow() != null) {
				Element show = new Element("show", item.getStatus().getShow());

				pres_el.addChild(show);
			}

			try {
				Packet presence = Packet.packetInstance(pres_el);

				if (log.isLoggable(Level.FINEST)) {
					log.finest("Sending out presence: " + presence);
				}

				addOutPacket(presence);
			} catch (TigaseStringprepException ex) {
				Logger.getLogger(Gateway.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
	}

	/**
	 * Method description
	 * 
	 * @param gc
	 */
	@Override
	public void userRoster(GatewayConnection gc) {
		JID[] jids = gc.getAllJids();
		JID id = jids[0];
		List<RosterItem> roster = gc.getRoster();

		for (RosterItem item : roster) {
			log.fine("Received roster entry: " + item.getBuddyId());

			String from = formatJID(item.getBuddyId());
			String roster_node = id.toString() + "/roster/" + item.getBuddyId();
			String authorized = "false";

			try {
				authorized =
						repository
								.getData(getComponentId().getBareJID(), roster_node, AUTHORIZED_KEY);

				if (authorized == null) {

					// Add item to the roster and send subscription request to user...
					authorized = "false";
					repository.setData(getComponentId().getBareJID(), roster_node, AUTHORIZED_KEY,
							authorized);
					repository.setData(getComponentId().getBareJID(), roster_node, NAME_KEY,
							item.getName());
				}

				if (item.getStatus().getType() != null) {
					repository.setData(getComponentId().getBareJID(), roster_node, PRESENCE_TYPE,
							item.getStatus().getType());
				} else {
					repository.setData(getComponentId().getBareJID(), roster_node, PRESENCE_TYPE,
							"null");
				}

				if (item.getStatus().getShow() != null) {
					repository.setData(getComponentId().getBareJID(), roster_node, PRESENCE_SHOW,
							item.getStatus().getShow());
				} else {
					repository.setData(getComponentId().getBareJID(), roster_node, PRESENCE_SHOW,
							"null");
				}
			} catch (TigaseDBException e) {
				log.log(Level.WARNING, "Problem updating repository data", e);
			}

			for (JID username : jids) {
				try {
					Packet presence =
							Packet.packetInstance(new Element(PRESENCE_ELNAME, new String[] { "to",
									"from", "type" },
									new String[] { username.toString(), from, "subscribe" }));

					if (log.isLoggable(Level.FINEST)) {
						log.finest("Sending out presence: " + presence);
					}

					addOutPacket(presence);
				} catch (TigaseStringprepException ex) {
					Logger.getLogger(Gateway.class.getName()).log(Level.SEVERE, null, ex);
				}
			}
		}

		try {
			updateRosterPresence(roster, jids);
		} catch (TigaseStringprepException ex) {
			Logger.getLogger(Gateway.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	private void closeConnection(GatewayConnection conn) {
		if (conn != null) {
			conn.logout();
		}
	}

	private GatewayConnection findConnection(Packet packet, boolean create)
			throws TigaseStringprepException {
		JID id = packet.getStanzaFrom().copyWithoutResource();
		GatewayConnection conn = gw_connections.get(id.toString());

		if ((conn != null) || !create) {
			if (conn != null) {
				conn.addJid(packet.getStanzaFrom());
				addOutPacket(Packet.packetInstance(new Element(PRESENCE_ELNAME, new String[] {
						"from", "to" }, new String[] { getComponentId().toString(),
						packet.getStanzaFrom().toString() }), getComponentId(),
						packet.getStanzaFrom()));
				updateRosterPresence(conn.getRoster(), packet.getStanzaFrom());
			}

			return conn;
		}

		try {
			if (is_moderated) {
				String moderated =
						repository.getData(getComponentId().getBareJID(), id.toString(),
								moderated_key);

				if ((moderated == null) || moderated.equals(moderated_true)) {
					addOutPacket(Authorization.NOT_ALLOWED.getResponseMessage(packet,
							"Administrator approval awaiting.", true));

					return null;
				}
			}

			String username =
					repository.getData(getComponentId().getBareJID(), id.toString(), username_key);
			String password =
					repository.getData(getComponentId().getBareJID(), id.toString(), password_key);

			if ((username != null) && (password != null)) {
				conn = gwInstance();
				conn.setGatewayListener(this);
				conn.setLogin(username, password);

				// conn.setGatewayDomain(getComponentId());
				conn.addJid(packet.getStanzaFrom());
				conn.init();
				conn.login();
				gw_connections.put(id.toString(), conn);

				return conn;
			}
		} catch (Exception e) {
			log.log(Level.WARNING, "Error initializing gateway connection", e);
		}

		return null;
	}

	@SuppressWarnings("unchecked")
	private GatewayConnection gwInstance() {
		try {
			Class<GatewayConnection> cls =
					(Class<GatewayConnection>) Class.forName(gw_class_name);
			GatewayConnection gc = cls.newInstance();

			return gc;
		} catch (Exception e) {
			log.log(Level.WARNING, "Problem instantating gateway connection object", e);
		}

		return null;
	}

	private void processGateway(Packet packet) throws PacketErrorTypeException {
		if (packet.getType() == null) {
			log.info("Bad gateway request: " + packet.toString());
			addOutPacket(Authorization.BAD_REQUEST.getResponseMessage(packet,
					"IQ request must have either 'set' or 'get' type.", true));

			return;
		}

		if (packet.getType() == StanzaType.get) {
			Element query = new Element("query");

			query.setXMLNS("jabber:iq:gateway");
			query.addChild(new Element("desc", gw_desc));
			query.addChild(new Element("prompt"));
			addOutPacket(packet.okResult(query, 0));
		}

		if (packet.getType() == StanzaType.set) {
			String legacyName = packet.getElemCData("/iq/query/prompt");
			String jid = formatJID(legacyName);

			addOutPacket(packet.okResult(new Element("prompt", jid), 1));
		}
	}

	private void processLocalPacket(Packet packet) throws PacketErrorTypeException,
			TigaseStringprepException {
		if (packet.isXMLNS("/iq/query", "jabber:iq:register")) {
			processRegister(packet);
		}

		if (packet.isXMLNS("/iq/query", "jabber:iq:gateway")) {
			processGateway(packet);
		}
	}

	private void processPresence(Packet packet) throws TigaseStringprepException {
		if (packet.getStanzaTo().toString().startsWith(getName() + ".")
				&& !packet.getStanzaTo().toString().contains("@")) {

			// if (Arrays.binarySearch(hostnames, packet.getElemTo()) >= 0) {
			if ((packet.getType() == null) || (packet.getType() == StanzaType.available)) {

				// Open new connection if it does not exist
				findConnection(packet, true);

				return;
			}

			if (packet.getType() == StanzaType.subscribe) {
				addOutPacket(packet.swapStanzaFromTo(StanzaType.subscribed));
			}

			if (packet.getType() == StanzaType.unavailable) {
				removeJid(packet);
			}
		} else {
			if ((packet.getType() == null) || (packet.getType() == StanzaType.available)) {

				// Ignore
				return;
			}

			JID id = packet.getStanzaFrom().copyWithoutResource();

			if (packet.getType() == StanzaType.subscribed) {
				addOutPacket(packet.swapStanzaFromTo(StanzaType.subscribed));

				String buddy = decodeLegacyName(packet.getStanzaTo().toString());

				log.fine("Received subscribed presence for: " + buddy);

				String roster_node = id + "/roster/" + buddy;
				String authorized = "true";
				String pres_type = "null";
				String pres_show = "null";

				try {
					repository.setData(getComponentId().getBareJID(), roster_node, AUTHORIZED_KEY,
							authorized);
					pres_type =
							repository.getData(getComponentId().getBareJID(), roster_node,
									PRESENCE_TYPE);
					pres_show =
							repository.getData(getComponentId().getBareJID(), roster_node,
									PRESENCE_SHOW);
					log.fine("Added buddy do repository for: " + buddy);
				} catch (TigaseDBException e) {
					log.log(Level.WARNING, "Problem updating repository data", e);
				}

				Element pres_el =
						new Element(PRESENCE_ELNAME, new String[] { "to", "from" }, new String[] {
								packet.getStanzaFrom().toString(), packet.getStanzaTo().toString() });

				if (!pres_type.equals("null")) {
					pres_el.setAttribute("type", pres_type);
				}

				if (!pres_show.equals("null")) {
					Element show = new Element("show", pres_show);

					pres_el.addChild(show);
				}

				Packet presence =
						Packet.packetInstance(pres_el, packet.getStanzaTo(), packet.getStanzaFrom());

				if (log.isLoggable(Level.FINEST)) {
					log.finest("Sending out presence: " + presence.toString());
				}

				addOutPacket(presence);
			}

			if (packet.getType() == StanzaType.subscribe) {
				addOutPacket(packet.swapStanzaFromTo(StanzaType.subscribe));

				String buddy = decodeLegacyName(packet.getStanzaTo().toString());

				log.fine("Received subscribe presence for: " + buddy);

				String nick = BareJID.parseJID(buddy)[0];

				if ((nick == null) || nick.isEmpty()) {
					nick = buddy;
				}

				GatewayConnection conn = findConnection(packet, true);

				if (conn != null) {
					try {
						conn.addBuddy(buddy, nick);
						log.fine("Added to roster buddy: " + buddy);
					} catch (GatewayException e) {
						log.log(Level.WARNING, "Problem with gateway when adding buddy: " + buddy, e);
					}
				}
			}

			if (packet.getType() == StanzaType.unsubscribe) {
				Packet presence = packet.swapStanzaFromTo(StanzaType.unsubscribe);

				if (log.isLoggable(Level.FINEST)) {
					log.finest("Sending out presence: " + presence.toString());
				}

				addOutPacket(presence);
			}

			if (packet.getType() == StanzaType.unsubscribed) {
				addOutPacket(packet.swapStanzaFromTo(StanzaType.unsubscribe));
				addOutPacket(packet.swapStanzaFromTo(StanzaType.unsubscribed));

				String buddy = decodeLegacyName(packet.getStanzaTo().toString());
				String roster_node = id + "/roster/" + buddy;

				log.fine("Received unsubscribed presence for buddy: " + buddy);

				try {
					repository.removeSubnode(getComponentId().getBareJID(), roster_node);
					log.fine("Removed from repository buddy: " + buddy);
				} catch (TigaseDBException e) {
					log.log(Level.WARNING, "Problem updating repository data", e);
				}

				GatewayConnection conn = findConnection(packet, true);

				if (conn != null) {
					try {
						conn.removeBuddy(buddy);
						log.fine("Removed from roster buddy: " + buddy);
					} catch (GatewayException e) {
						log.log(Level.WARNING, "Problem with gateway when removing buddy: " + buddy,
								e);
					}
				}
			}
		}
	}

	private void processRegister(Packet packet) throws PacketErrorTypeException,
			TigaseStringprepException {
		if (packet.getType() != null) {
			switch (packet.getType()) {
			case get:
				addOutPacket(packet.okResult(
						"<instructions>" + "Please enter your " + gw_type.toUpperCase()
								+ " account details" + " into the fields below." + "</instructions>"
								+ "<username/>" + "<password/>", 1));

				break;

			case set:
				JID jid = packet.getStanzaFrom().copyWithoutResource();
				String new_username = packet.getElemCData("/iq/query/username");
				String new_password = packet.getElemCData("/iq/query/password");

				try {
					repository.setData(getComponentId().getBareJID(), jid.toString(), username_key,
							new_username);
					repository.setData(getComponentId().getBareJID(), jid.toString(), password_key,
							new_password);
					addOutPacket(packet.okResult((String) null, 0));
					addOutPacket(Packet.packetInstance(new Element(PRESENCE_ELNAME, new String[] {
							"to", "from", "type" }, new String[] { jid.toString(),
							packet.getStanzaTo().toString(), "subscribe" }), packet.getStanzaTo(), jid));

					if (is_moderated && !isAdmin(jid)) {
						repository.setData(getComponentId().getBareJID(), jid.toString(),
								moderated_key, moderated_true);
						addOutPacket(Packet.packetInstance(
								new Element("message", new Element[] { new Element("body",
										"Your subscription to the gateway needs administrator approval."
												+ " You will be notified when your request has been processed"
												+ " and you will be able to use the gateway since then.") },
										new String[] { "to", "from", "type", "id" }, new String[] {
												jid.toString(), packet.getStanzaTo().toString(), "chat",
												"gw-ap-1" }), packet.getStanzaTo(), jid));
						sendToAdmins(new Element("message", new Element[] { new Element("body",
								"Gateway subscription request is awaiting for: " + jid) }, new String[] {
								"from", "type", "id" }, new String[] { packet.getStanzaTo().toString(),
								"chat", "gw-ap-1" }));
					} else {
						repository.setData(getComponentId().getBareJID(), jid.toString(),
								moderated_key, moderated_false);
					}
				} catch (tigase.db.UserNotFoundException e) {
					log.warning("This is most likely configuration error, please make"
							+ " sure you have set '&autoCreateUser=true' property in your"
							+ " database connection string.");
					addOutPacket(Authorization.INTERNAL_SERVER_ERROR.getResponseMessage(packet,
							"Please notify administrator with the message below:\n"
									+ "This is most likely configuration error, please make"
									+ " sure you have set '&autoCreateUser=true' property in your"
									+ " database connection string.", true));
				} catch (TigaseDBException e) {
					log.log(Level.WARNING, "Database access error: ", e);
					addOutPacket(Authorization.INTERNAL_SERVER_ERROR.getResponseMessage(packet,
							"Please notify administrator with the message below:\n"
									+ "Database access error: " + e, true));
				}

				break;

			default:
				break;
			}
		}
	}

	private void removeJid(Packet packet) {
		JID id = packet.getStanzaFrom().copyWithoutResource();
		GatewayConnection conn = gw_connections.get(id.toString());

		if (conn != null) {
			log.info("Stopping connection for: " + packet.getStanzaFrom());
			gw_connections.remove(id.toString());

			if ((conn.getAllJids() == null) || (conn.getAllJids().length == 0)) {
				closeConnection(conn);
			}
		} else {
			log.info("No connection for: " + packet.getStanzaFrom());
		}
	}

	private void sendToAdmins(Element elem) throws TigaseStringprepException {
		for (String adm : admins) {
			Element msg = elem.clone();

			msg.setAttribute("to", adm);
			addOutPacket(Packet.packetInstance(msg));
		}
	}

	private void updateRosterPresence(List<RosterItem> roster, JID... to)
			throws TigaseStringprepException {
		if (roster == null) {

			// It may happen when the transport's roster is not synchronized yet
			return;
		}

		for (RosterItem item : roster) {
			log.fine("Received roster entry: " + item.getBuddyId());

			String from = formatJID(item.getBuddyId());

			for (JID username : to) {
				Element pres_el =
						new Element(PRESENCE_ELNAME, new String[] { "to", "from" }, new String[] {
								username.toString(), from });

				if (item.getStatus().getType() != null) {
					pres_el.setAttribute("type", item.getStatus().getType());
				}

				if (item.getStatus().getShow() != null) {
					Element show = new Element("show", item.getStatus().getShow());

					pres_el.addChild(show);
				}

				Packet presence = Packet.packetInstance(pres_el);

				if (log.isLoggable(Level.FINEST)) {
					log.finest("Sending out presence: " + presence);
				}

				addOutPacket(presence);
			}
		}
	}
}

// ~ Formatted in Sun Code Convention

// ~ Formatted by Jindent --- http://www.jindent.com
