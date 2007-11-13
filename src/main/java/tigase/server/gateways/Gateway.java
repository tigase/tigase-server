/*  Tigase Jabber/XMPP Server
 *  Copyright (C) 2004-2008 "Artur Hefczyc" <artur.hefczyc@tigase.org>
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
package tigase.server.gateways;

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
import tigase.db.UserNotFoundException;
import tigase.db.UserRepository;
import tigase.disco.ServiceEntity;
import tigase.disco.ServiceIdentity;
import tigase.disco.XMPPService;
import tigase.server.AbstractMessageReceiver;
import tigase.server.Packet;
import tigase.util.DBUtils;
import tigase.util.JIDUtils;
import tigase.xml.XMLUtils;
import tigase.xml.Element;
import tigase.xmpp.StanzaType;

/**
 * Describe class Gateway here.
 *
 *
 * Created: Thu Nov  8 08:54:23 2007
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class Gateway extends AbstractMessageReceiver
	implements Configurable, XMPPService, GatewayListener {

  /**
   * Private logger for class instancess.
   */
  private static Logger log =
		Logger.getLogger("tigase.server.gateways.Gateway");

	public static final String GEN_GW_DB = "--gen-gw-db";
	public static final String GEN_GW_DB_URI = "--gen-gw-db-uri";
	public static final String GEN_GW_ADMINS = "--gen-gw-admins";
	public static final String GW_REPO_CLASS_PROP_KEY = "gw-repo-class";
	public static final String GW_REPO_URL_PROP_KEY = "gw-repo-url";
	public static final String GW_CLASS_NAME_PROP_KEY = "gw-class-name";
	public static final String GW_CLASS_NAME_PROP_VAL =
		"tigase.server.gateways.MsnConnection";

	private static final String username_key = "user-name-key";
	private static final String password_key = "password-key";

	private String[] ADMINS_PROP_VAL =	{"admin@localhost", "admin@hostname"};

	private ServiceEntity serviceEntity = null;
	private String[] admins = ADMINS_PROP_VAL;
	private String gw_class_name = GW_CLASS_NAME_PROP_VAL;
	private String gw_name = "Undefined";
	private String gw_type = "unknown";
	private UserRepository repository = null;
	private Map<String, GatewayConnection> gw_connections =
		new LinkedHashMap<String, GatewayConnection>();

	public void setProperties(final Map<String, Object> props) {
		super.setProperties(props);

		gw_class_name = (String)props.get(GW_CLASS_NAME_PROP_KEY);
		GatewayConnection gc = gwInstance();
		if (gc != null) {
			gw_type = gc.getType();
			gw_name = gc.getName();
		}

		serviceEntity = new ServiceEntity(getName(), null, "Transport");
		serviceEntity.addIdentities(
			new ServiceIdentity("gateway", gw_type, gw_name));
		serviceEntity.addFeatures(DEF_FEATURES);
		serviceEntity.addFeatures("jabber:iq:register", "jabber:iq:gateway");

		admins = (String[])props.get(ADMINS_PROP_KEY);
		Arrays.sort(admins);

		try {
			String cls_name = (String)props.get(GW_REPO_CLASS_PROP_KEY);
			String res_uri = (String)props.get(GW_REPO_URL_PROP_KEY);
// 			if (!res_uri.contains("autoCreateUser=true")) {
// 				res_uri += "&autoCreateUser=true";
// 			} // end of if (!res_uri.contains("autoCreateUser=true"))
			repository = RepositoryFactory.getUserRepository(getName(),
				cls_name, res_uri);
			try {
				repository.addUser(myDomain());
			} catch (UserExistsException e) { /*Ignore, this is correct and expected*/	}
		} catch (Exception e) {
			log.log(Level.SEVERE, "Can't initialize repository", e);
		} // end of try-catch


	}

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
			repo_uri = (String)params.get(GEN_GW_DB_URI);
		} else {
			if (params.get(GEN_USER_DB_URI) != null) {
				repo_uri = (String)params.get(GEN_USER_DB_URI);
			} // end of if (params.get(GEN_USER_DB_URI) != null)
		} // end of else
		defs.put(GW_REPO_CLASS_PROP_KEY, repo_class);
		defs.put(GW_REPO_URL_PROP_KEY, repo_uri);
		if (params.get(GEN_GW_ADMINS) != null) {
			ADMINS_PROP_VAL = ((String)params.get(GEN_GW_ADMINS)).split(",");
		} else {
			if (params.get(GEN_ADMINS) != null) {
				ADMINS_PROP_VAL = ((String)params.get(GEN_ADMINS)).split(",");
			} else {
				ADMINS_PROP_VAL = new String[1];
				ADMINS_PROP_VAL[0] = "admin@" + getDefHostName();
			}
		} // end of if (params.get(GEN_SREC_ADMINS) != null) else
		defs.put(ADMINS_PROP_KEY, ADMINS_PROP_VAL);

		defs.put(GW_CLASS_NAME_PROP_KEY, GW_CLASS_NAME_PROP_VAL);

		return defs;
	}

  @SuppressWarnings("unchecked")
	private GatewayConnection gwInstance() {
		try {
			Class<GatewayConnection> cls =
				(Class<GatewayConnection>)Class.forName(gw_class_name);
			GatewayConnection gc = cls.newInstance();
			return gc;
		} catch (Throwable e) {
			log.log(Level.WARNING, "Problem instantating gateway connection object", e);
		}
		return null;
	}

	private void processRegister(Packet packet) {
		if (packet.getType() != null) {
			switch (packet.getType()) {
			case get:
				addOutPacket(packet.okResult(
						"<instructions>"
						+ "Please type your " + gw_name + " account details"
						+ " into the username field and your password."
						+ "</instructions>"
						+ "<username/>"
						+ "<password/>", 1));
				break;
			case set:
				String id = JIDUtils.getNodeID(packet.getElemFrom());
				String new_username = packet.getElemCData("/iq/query/username");
				String new_password = packet.getElemCData("/iq/query/password");
				try {
					repository.setData(myDomain(), id, username_key, new_username);
					repository.setData(myDomain(), id, password_key, new_password);
				} catch (tigase.db.UserNotFoundException e) {
					log.warning("This is most likely configuration error, please make"
						+ " sure you have set '&autoCreateUser=true' property in your"
						+ " database connection string.");
				} catch (TigaseDBException e) {
					log.log(Level.WARNING, "Database access error: ", e);
				}
				addOutPacket(packet.okResult((String)null, 0));
				addOutPacket(new Packet(new Element("presence",
							new String[] {"to", "from", "type"},
							new String[] {id, myDomain(), "subscribe"})));
				break;
			default:
				break;
			}
		}
	}

	private void processPresence(Packet packet) {
		if (packet.getElemTo().equals(myDomain())) {
			if (packet.getType() == null) {
				// Open new connection if ti does not exist
				findConnection(packet);
				return;
			}
			if (packet.getType() == StanzaType.subscribe) {
				addOutPacket(packet.swapElemFromTo(StanzaType.subscribed));
			}
		} else {
			if (packet.getType() == null) {
				// Ignore
				return;
			}
			String id = JIDUtils.getNodeID(packet.getElemFrom());
			if (packet.getType() == StanzaType.subscribe) {
				addOutPacket(packet.swapElemFromTo(StanzaType.subscribed));
			}
			if (packet.getType() == StanzaType.subscribed) {
				String buddy =
					XMLUtils.unescape(packet.getElemTo().split("@")[0].replace("%", "@"));
				String roster_node = id + "/roster/" + buddy;
				String authorized = "true";
				try {
					repository.setData(myDomain(), roster_node, AUTHORIZED_KEY, authorized);
				} catch (TigaseDBException e) {
					log.log(Level.WARNING, "Problem updating repository data", e);
				}
				Packet presence = new Packet(new Element("presence",
						new String[] {"to", "from"},
						new String[] {packet.getElemFrom(), packet.getElemTo()}));
				log.finest("Sending out presence: " + presence.toString());
				addOutPacket(presence);
			}
		}
	}

	private void processLocalPacket(Packet packet) {
		if (packet.isXMLNS("/iq/query", "jabber:iq:register")) {
			processRegister(packet);
		}
	}

	private GatewayConnection findConnection(Packet packet) {
		String id = JIDUtils.getNodeID(packet.getElemFrom());
		GatewayConnection conn = gw_connections.get(id);
		if (conn != null) { return conn; }
		try {
			String username = repository.getData(myDomain(), id, username_key);
			String password = repository.getData(myDomain(), id, password_key);
			if (username != null && password != null) {
				conn = gwInstance();
				conn.setGatewayListener(this);
				conn.setLogin(username, password);
				conn.setGatewayDomain(myDomain());
				conn.addJid(packet.getElemFrom());
				conn.init();
				conn.login();
				gw_connections.put(id, conn);
				return conn;
			}
		} catch (Exception e) {
			log.log(Level.WARNING, "Error initializing gateway connection", e);
		}
		return null;
	}

	public void processPacket(final Packet packet) {
		if (packet.getElemTo() == null) {
			log.warning("Bad packet, 'to' is null: " + packet.toString());
			return;
		}
		if (packet.getElemName().equals("presence")) {
			processPresence(packet);
		}
		if (packet.getElemTo().equals(myDomain())) {
			// Local processing.
			log.fine("Local packet: " + packet.toString());
			processLocalPacket(packet);
			return;
		}
		GatewayConnection conn = findConnection(packet);
		if (conn != null) {
			try {
				conn.sendMessage(packet);
			} catch (GatewayException e) {
				log.log(Level.WARNING, "Error initializing gateway connection", e);
			}
		}
	}

	public 	List<Element> getDiscoFeatures() { return null; }

	public List<Element> getDiscoItems(String node, String jid) {
		if (jid.startsWith(getName()+".")) {
			return serviceEntity.getDiscoItems(node, null);
		} else {
 			return
				Arrays.asList(serviceEntity.getDiscoItem(null, getName() + "." + jid));
		}
	}

	public Element getDiscoInfo(String node, String jid) {
		if (jid != null && jid.startsWith(getName()+".")) {
			return serviceEntity.getDiscoInfo(node);
		}
		return null;
	}

	public void packetReceived(Packet packet) {
		addOutPacket(packet);
	}

	public void logout(String username) {
		addOutPacket(new Packet(new Element("presence",
					new String[] {"from", "to", "type"},
					new String[] {myDomain(), JIDUtils.getNodeID(username), "unavailable"})));
	}

	public void loginCompleted(String username) {
		addOutPacket(new Packet(new Element("presence",
					new String[] {"from", "to"},
					new String[] {myDomain(), JIDUtils.getNodeID(username)})));
	}

	public void gatewayException(String username, Throwable exc) {
		log.log(Level.WARNING, "Gateway exception", exc);
	}

	private static final String AUTHORIZED_KEY = "authorized-key";
	private static final String NAME_KEY = "authorized-key";

	public void userRoster(String username, List<RosterItem> roster) {
		String id = JIDUtils.getNodeID(username);
		for (RosterItem item: roster) {
			String from = XMLUtils.escape(item.getBuddyId().replace("@", "%")
				+ "@" + myDomain());
			String roster_node = id + "/roster/" + item.getBuddyId();
			String authorized = "false";
			try {
				authorized = repository.getData(myDomain(), roster_node, AUTHORIZED_KEY);
				if (authorized == null) {
					// Add item to the roster and send subscription request to user...
					authorized = "false";
					repository.setData(myDomain(), roster_node, AUTHORIZED_KEY, authorized);
					repository.setData(myDomain(), roster_node, NAME_KEY, item.getName());
				}
			} catch (TigaseDBException e) {
				log.log(Level.WARNING, "Problem updating repository data", e);
			}
			if (authorized.equals("false")) {
				// Send authorization request...
				Packet presence = new Packet(new Element("presence",
						new String[] {"to", "from", "type"},
						new String[] {username, from, "subscribe"}));
				log.finest("Sending out presence: " + presence.toString());
				addOutPacket(presence);
			}
			Element pres_el = new Element("presence",
				new String[] {"to", "from"},
				new String[] {username, from});
			if (item.getStatus().getType() != null) {
				pres_el.setAttribute("type", item.getStatus().getType());
			}
			if (item.getStatus().getShow() != null) {
				Element show = new Element("show", item.getStatus().getShow());
				pres_el.addChild(show);
			}
			Packet presence = new Packet(pres_el);
			log.finest("Sending out presence: " + presence.toString());
			addOutPacket(presence);
		}
	}

}
