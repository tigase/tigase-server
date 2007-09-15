/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2007 "Artur Hefczyc" <artur.hefczyc@tigase.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License.
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

package tigase.server.xmppsession;

//import tigase.auth.TigaseConfiguration;
import java.net.UnknownHostException;
import java.security.Security;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.security.auth.login.AppConfigurationEntry.LoginModuleControlFlag;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;
import tigase.auth.LoginHandler;
import tigase.auth.TigaseSaslProvider;
import tigase.conf.Configurable;
import tigase.db.DataOverwriteException;
import tigase.db.NonAuthUserRepository;
import tigase.db.RepositoryFactory;
import tigase.db.TigaseDBException;
import tigase.db.UserAuthRepository;
import tigase.db.UserNotFoundException;
import tigase.db.UserRepository;
import tigase.server.XMPPServer;
import tigase.server.AbstractMessageReceiver;
import tigase.server.MessageReceiver;
import tigase.server.Packet;
import tigase.server.Command;
import tigase.server.Permissions;
import tigase.server.MessageRouter;
import tigase.disco.XMPPService;
import tigase.disco.ServiceEntity;
import tigase.disco.ServiceIdentity;
import tigase.stats.StatRecord;
import tigase.util.ElementUtils;
import tigase.util.JIDUtils;
import tigase.xml.Element;
import tigase.xmpp.Authorization;
import tigase.xmpp.NotAuthorizedException;
import tigase.xmpp.ProcessorFactory;
import tigase.xmpp.StanzaType;
import tigase.xmpp.XMPPPostprocessorIfc;
import tigase.xmpp.XMPPPreprocessorIfc;
import tigase.xmpp.XMPPProcessorIfc;
import tigase.xmpp.XMPPResourceConnection;
import tigase.xmpp.XMPPSession;
import tigase.xmpp.XMPPStopListenerIfc;

import static tigase.server.xmppsession.SessionManagerConfig.*;

/**
 * Class SessionManager
 *
 *
 * Created: Tue Nov 22 07:07:11 2005
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class SessionManager extends AbstractMessageReceiver
	implements Configurable, XMPPService, LoginHandler {

  /**
   * Variable <code>log</code> is a class logger.
   */
  private static final Logger log =
    Logger.getLogger("tigase.server.xmppsession.SessionManager");

	private UserRepository user_repository = null;
	private UserAuthRepository auth_repository = null;
	private NonAuthUserRepository naUserRepository = null;
	private PacketFilter filter = null;

	private String[] admins = {"admin@localhost"};

	private Map<String, XMPPSession> sessionsByNodeId =
		new ConcurrentSkipListMap<String, XMPPSession>();
	private Map<String, XMPPResourceConnection> connectionsByFrom =
		new ConcurrentSkipListMap<String, XMPPResourceConnection>();

	private Map<String, XMPPPreprocessorIfc> preProcessors =
		new ConcurrentSkipListMap<String, XMPPPreprocessorIfc>();
	private Map<String, XMPPProcessorIfc> processors =
		new ConcurrentSkipListMap<String, XMPPProcessorIfc>();
	private Map<String, XMPPPostprocessorIfc> postProcessors =
		new ConcurrentSkipListMap<String, XMPPPostprocessorIfc>();
	private Map<String, XMPPStopListenerIfc> stopListeners =
		new ConcurrentSkipListMap<String, XMPPStopListenerIfc>();

	private ServiceEntity serviceEntity = null;

	private long closedConnections = 0;

	public void setName(String name) {
		super.setName(name);
		serviceEntity = new ServiceEntity(name, "sm", "Session manager");
		serviceEntity.addIdentities(
			new ServiceIdentity("component", "sm", "Session manager"));
	}

	private void debug_packet(String msg, Packet packet, String to) {
		if (packet.getElemTo().equals(to)) {
			log.finest(msg + ", packet: " + packet.getStringData());
		}
	}

	public void processPacket(Packet packet) {
		log.finest("Processing packet: " + packet.toString());
		if (packet.isCommand()) {
			processCommand(packet);
			packet.processedBy("SessionManager");
			// No more processing is needed for command packet
			// 			return;
		} // end of if (pc.isCommand())
		XMPPResourceConnection conn = getXMPPResourceConnection(packet);
		if (conn == null) {

			if (packet.getFrom() != packet.getElemFrom()) {
				// It doesn't look good, there should reaaly be a connection for
				// this packet....
				// returning error back...
				if (packet.getType() == StanzaType.error) {
					// Drop packet.
				} else {
					Packet error =
						Authorization.SERVICE_UNAVAILABLE.getResponseMessage(packet,
						"Service not available.", true);
					error.setTo(packet.getFrom());
					addOutPacket(error);
					return;
				}
			}

			// It might be a message _to_ some user on this server
			// so let's look for established session for this user...
			final String to = packet.getElemTo();
			if (to != null) {
				conn = getResourceConnection(to);
				if (conn == null) {
					// It might be message to admin
					if (processAdmins(packet)) {
						// No more processing is needed....
						return;
					}
				}
			} else {
				// Hm, not sure what should I do now....
				// Maybe I should treat it as message to admin....
				log.info("Message without TO attribute set, don't know what to do wih this: "
					+ packet.getStringData());
			} // end of else
		} // end of if (conn == null)

		// Preprocess..., all preprocessors get all messages to look at.
		// I am not sure if this is correct for now, let's try to do it this
		// way and maybe change it later.
		// If any of them returns true - it means processing should stop now.
		// That is needed for preprocessors like privacy lists which should
		// block certain packets.

		Queue<Packet> results = new LinkedList<Packet>();

		boolean stop = false;
		for (XMPPPreprocessorIfc preproc: preProcessors.values()) {
			stop |= preproc.preProcess(packet, conn, naUserRepository, results);
		} // end of for (XMPPPreprocessorIfc preproc: preProcessors)

		if (!stop) {
			if (filter.forward(packet, conn, naUserRepository, results)) {
				packet.processedBy("filter-foward");
				log.finest("Packet forwarded: " + packet.toString());
				addOutPackets(results);
				return;
			}
		}

		if (!stop) {
			walk(packet, conn, packet.getElement(), results);
			setPermissions(conn, results);
		}

		if (!stop) {
			for (XMPPPostprocessorIfc postproc: postProcessors.values()) {
				postproc.postProcess(packet, conn, naUserRepository, results);
			} // end of for (XMPPPostprocessorIfc postproc: postProcessors)
		} // end of if (!stop)

		if (!stop && !packet.wasProcessed() && !isInRoutings(packet.getTo())
			&& filter.process(packet, conn, naUserRepository, results)) {
			packet.processedBy("filter-process");
		}

		addOutPackets(results);

		if (!packet.wasProcessed()) {
			log.finest("Packet not processed: " + packet.toString());
			Packet error = null;
			if (stop
				|| (conn == null
					&& packet.getElemFrom() != null && packet.getElemTo() != null
					&& (packet.getElemName().equals("iq")
						|| packet.getElemName().equals("message")))) {
				// Infinite loop detection code:
				if (packet.getType() == StanzaType.error) {
					// Let's just ignore all error stanzas if there is no plugin to process
					// them and no alive connection to receive them.
				} else {
					error =	Authorization.SERVICE_UNAVAILABLE.getResponseMessage(packet,
						"Service not available.", true);
				}
			} else {
				if ((packet.getElemFrom() != null || conn != null)
					&& packet.getType() != StanzaType.error) {
					error = Authorization.FEATURE_NOT_IMPLEMENTED.getResponseMessage(packet,
						"Feature not supported yet.", true);
				} else {
					log.warning("Lost packet: " + packet.getStringData());
				} // end of else
			} // end of if (stop) else
			if (error != null) {
				if (error.getElemTo() != null) {
					conn = getResourceConnection(error.getElemTo());
				} // end of if (error.getElemTo() != null)
				if (conn != null) {
					error.setTo(conn.getConnectionId());
				} // end of if (conn != null)
				addOutPacket(error);
			}
		} else {
			log.finest("Packet processed by: " + packet.getProcessorsIds().toString());
		} // end of else
	}

	private void setPermissions(XMPPResourceConnection conn,
		Queue<Packet> results) {
		Permissions perms = Permissions.NONE;
		if (conn != null) {
			perms = Permissions.LOCAL;
			if (conn.isAuthorized()) {
				perms = Permissions.AUTH;
				try {
					String id = conn.getUserId();
					if (isAdmin(id)) {
						perms = Permissions.ADMIN;
					}
				} catch (NotAuthorizedException e) {
					perms = Permissions.NONE;
				}
			}
		}
		for (Packet res: results) {
			res.setPermissions(perms);
		}
	}

	private boolean isAdmin(String jid) {
		for (String adm: admins) {
			if (adm.equals(JIDUtils.getNodeID(jid))) {
				return true;
			}
		}
		return false;
	}

	private boolean processAdmins(Packet packet) {
		final String to = packet.getElemTo();
		if (isInRoutings(to) && packet.getElemName().equals("message")) {
			// Yes this packet is for admin....
			log.finer("Packet for admin: " + packet.getStringData());
			for (String admin: admins) {
				log.finer("Sending packet to admin: " + admin);
				Packet admin_pac =
          new Packet(packet.getElement().clone());
				admin_pac.getElement().setAttribute("to", admin);
				processPacket(admin_pac);
			} // end of for (String admin: admins)
			return true;
		} // end of if (isInRoutings(to))
		return false;
	}

	private XMPPSession getSession(String jid) {
		return sessionsByNodeId.get(JIDUtils.getNodeID(jid));
	}

	private XMPPResourceConnection getResourceConnection(String jid) {
		XMPPSession session = getSession(jid);
		if (session != null) {
			return session.getResourceConnection(jid);
		} // end of if (session != null)
		return null;
	}

	private String getConnectionId(String jid) {
		XMPPResourceConnection res = getResourceConnection(jid);
		if (res != null) {
			return res.getConnectionId();
		} // end of if (res != null)
		return null;
	}

	private void walk(final Packet packet,
		final XMPPResourceConnection connection, final Element elem,
		final Queue<Packet> results) {
		for (XMPPProcessorIfc proc: processors.values()) {
			String xmlns = elem.getXMLNS();
			if (xmlns == null) { xmlns = "jabber:client";	}
			if (proc.isSupporting(elem.getName(), xmlns)) {
				log.finest("XMPPProcessorIfc: "+proc.getClass().getSimpleName()+
					" ("+proc.id()+")"+"\n Request: "+elem.toString()
					+ (connection != null ? ", " + connection.getConnectionId() : " null"));
				proc.process(packet, connection, naUserRepository, results);
				packet.processedBy(proc.id());
			} // end of if (proc.isSupporting(elem.getName(), elem.getXMLNS()))
		} // end of for ()
		Collection<Element> children = elem.getChildren();
		if (children != null) {
			for (Element child: children) {
				walk(packet, connection, child, results);
			} // end of for (Element child: children)
		} // end of if (children != null)
	}

	public void processPacket(final Packet pc, final Queue<Packet> results) {

		if (!pc.isCommand()) {
			return;
		}

		log.finest("Command received: " + pc.getStringData());
		XMPPResourceConnection connection = null;
		switch (pc.getCommand()) {
		case USER_STATUS:
			String user_jid = Command.getFieldValue(pc, "jid");
			String hostname = JIDUtils.getNodeHost(user_jid);
			String av = Command.getFieldValue(pc, "available");
			boolean available = !(av != null && av.equalsIgnoreCase("false"));
			if (available) {
				connection = connectionsByFrom.get(pc.getElemFrom());
				if (connection == null) {
					connection = createUserSession(pc.getElemFrom(), hostname, user_jid);
					connection.putSessionData("jingle", "active");
					Packet presence =
						new Packet(new Element("presence",
								new Element[] {
									new Element("priority", "-1"),
									new Element("c",
										new String[] {"node", "ver", "ext", "xmlns"},
										new String[] {"http://www.google.com/xmpp/client/caps",
																	XMPPServer.getImplementationVersion(),
																	"voice-v1",
																	"http://jabber.org/protocol/caps"})},
								null, null));
					presence.setFrom(pc.getElemFrom());
					presence.setTo(getName() + "@" + pc.getTo());
					addOutPacket(presence);
				} else {
					log.finest("USER_STATUS set to true for user who is already available: "
						+ pc.toString());
				}
			} else {
				connection = connectionsByFrom.remove(pc.getElemFrom());
				if (connection != null) {
					closeSession(connection);
				} else {
					log.info("Can not find resource connection for packet: " +
						pc.toString());
				}
			}
			break;
		default:
			break;
		} // end of switch (pc.getCommand())
	}

	private XMPPResourceConnection createUserSession(String conn_id,
		String domain, String user_jid) {
		XMPPResourceConnection connection = new XMPPResourceConnection(conn_id,
			user_repository, auth_repository, this);
		connection.setDomain(domain);
		// Dummy session ID, we might decide later to set real thing here
		connection.setSessionId("session-id");
		connectionsByFrom.put(conn_id, connection);
		handleLogin(JIDUtils.getNodeNick(user_jid), connection);
		connection.setResource(JIDUtils.getNodeResource(user_jid));
		return connection;
	}


	private void processCommand(Packet pc) {
		log.finer(pc.getCommand().toString() + " command from: " + pc.getFrom());
		//Element command = pc.getElement();
		switch (pc.getCommand()) {
		case STREAM_OPENED:
			// It might be existing opened stream after TLS/SASL authorization
			// If not, it means this is new stream
			XMPPResourceConnection connection =	connectionsByFrom.get(pc.getFrom());
			if (connection == null) {
				log.finer("Adding resource connection for: " + pc.getFrom());
				final String hostname = Command.getFieldValue(pc, "hostname");
				connection = new XMPPResourceConnection(pc.getFrom(),
					user_repository, auth_repository, this);
				if (hostname != null) {
					log.finest("Setting hostname " + hostname
						+ " for connection: " + connection.getConnectionId());
					connection.setDomain(hostname);
				} // end of if (hostname != null)
				else {
					connection.setDomain(getDefHostName());
				} // end of if (hostname != null) else
				connectionsByFrom.put(pc.getFrom(), connection);
			} else {
				log.finest("Stream opened for existing session, authorized: "
					+ connection.isAuthorized());
			} // end of else
			connection.setSessionId(Command.getFieldValue(pc, "session-id"));
			log.finest("Setting session-id " + connection.getSessionId()
				+ " for connection: " + connection.getConnectionId());
			break;
		case GETFEATURES:
			if (pc.getType() == StanzaType.get) {
				List<Element> features =
					getFeatures(connectionsByFrom.get(pc.getFrom()));
				Packet result = pc.commandResult(null);
				Command.setData(result, features);
				addOutPacket(result);
			} // end of if (pc.getType() == StanzaType.get)
			break;
		case STREAM_CLOSED:
			log.fine("Stream closed from: " + pc.getFrom());
			++closedConnections;
			connection = connectionsByFrom.remove(pc.getFrom());
			if (connection != null) {
				closeSession(connection);
			} else {
				log.info("Can not find resource connection for packet: " +
					pc.toString());
			} // end of if (conn != null) else
			break;
		case OTHER:
			log.info("Other command found: " + pc.getStrCommand());
			break;
		default:
			break;
		} // end of switch (pc.getCommand())
	}

	private void closeSession(XMPPResourceConnection conn) {
		try {
			String userId = conn.getUserId();
			log.info("Closing connection for: " + userId);
			XMPPSession session = conn.getParentSession();
			if (session != null) {
				log.info("Found parent session for: " + userId);
				if (session.getActiveResourcesSize() <= 1) {
					session = sessionsByNodeId.remove(userId);
					if (session == null) {
						log.info("UPS can't remove session, not found in map: " + userId);
					} else {
						log.finer("Number of authorized connections: "
							+ sessionsByNodeId.size());
					} // end of else
					auth_repository.logout(userId);
				} else {
					log.finer("Number of connections is "
						+ session.getActiveResourcesSize() + " for the user: " + userId);
				} // end of else
			} // end of if (session.getActiveResourcesSize() == 0)
		} catch (NotAuthorizedException e) {
			log.info("Closed not authorized session: " + e);
		} catch (Exception e) {
			log.info("Exception closing session... " + e);
		}
		Queue<Packet> results = new LinkedList<Packet>();
		for (XMPPStopListenerIfc stopProc: stopListeners.values()) {
			stopProc.stopped(conn, results);
		} // end of for ()
		addOutPackets(results);
		conn.streamClosed();
	}

	private XMPPResourceConnection getXMPPResourceConnection(Packet p) {
		if (p.getFrom() != null) {
			return connectionsByFrom.get(p.getFrom());
		}
		return null;
	}

	private XMPPSession getXMPPSession(Packet p) {
		return connectionsByFrom.get(p.getFrom()).getParentSession();
	}

	private List<Element> getFeatures(XMPPResourceConnection session) {
		List<Element> results = new LinkedList<Element>();
		for (XMPPProcessorIfc proc: processors.values()) {
			Element[] features = proc.supStreamFeatures(session);
			if (features != null) {
				results.addAll(Arrays.asList(features));
			} // end of if (features != null)
		} // end of for ()
		return results;
	}

	public Map<String, Object> getDefaults(Map<String, Object> params) {
		Map<String, Object> props = super.getDefaults(params);
		SessionManagerConfig.getDefaults(props, params);
		return props;
	}

	private void addPlugin(String comp_id) {
		System.out.println("Loading plugin: " + comp_id + " ...");
		XMPPProcessorIfc proc = ProcessorFactory.getProcessor(comp_id);
		boolean loaded = false;
		if (proc != null) {
			processors.put(comp_id, proc);
			log.config("Added processor: " + proc.getClass().getSimpleName()
				+ " for plugin id: " + comp_id);
			loaded = true;
		}
		XMPPPreprocessorIfc preproc = ProcessorFactory.getPreprocessor(comp_id);
		if (preproc != null) {
			preProcessors.put(comp_id, preproc);
			log.config("Added preprocessor: " + preproc.getClass().getSimpleName()
				+ " for plugin id: " + comp_id);
			loaded = true;
		}
		XMPPPostprocessorIfc postproc = ProcessorFactory.getPostprocessor(comp_id);
		if (postproc != null) {
			postProcessors.put(comp_id, postproc);
			log.config("Added postprocessor: " + postproc.getClass().getSimpleName()
				+ " for plugin id: " + comp_id);
			loaded = true;
		}
		XMPPStopListenerIfc stoplist = ProcessorFactory.getStopListener(comp_id);
		if (stoplist != null) {
			stopListeners.put(comp_id, stoplist);
			log.config("Added stopped processor: " + stoplist.getClass().getSimpleName()
				+ " for plugin id: " + comp_id);
			loaded = true;
		}
		if (!loaded) {
			log.warning("No implementation found for plugin id: " + comp_id);
		} // end of if (!loaded)
	}

	public void setProperties(Map<String, Object> props) {
		super.setProperties(props);

		Security.insertProviderAt(new TigaseSaslProvider(), 6);

		filter = new PacketFilter();

		try {
			String cls_name = (String)props.get(USER_REPO_CLASS_PROP_KEY);
			String res_uri = (String)props.get(USER_REPO_URL_PROP_KEY);
			user_repository = RepositoryFactory.getUserRepository(getName(),
				cls_name, res_uri);
			log.config("Initialized " + cls_name + " as user repository: " + res_uri);
		} catch (Exception e) {
			log.severe("Can't initialize user repository: " + e);
			e.printStackTrace();
			System.exit(1);
		} // end of try-catch
		try {
			String cls_name = (String)props.get(AUTH_REPO_CLASS_PROP_KEY);
			String res_uri = (String)props.get(AUTH_REPO_URL_PROP_KEY);
			auth_repository =	RepositoryFactory.getAuthRepository(getName(),
				cls_name, res_uri);
			log.config("Initialized " + cls_name + " as auth repository: " + res_uri);
		} catch (Exception e) {
			log.severe("Can't initialize auth repository: " + e);
			e.printStackTrace();
			System.exit(1);
		} // end of try-catch

		naUserRepository = new NARepository(user_repository);
		String[] plugins = (String[])props.get(PLUGINS_PROP_KEY);
		processors.clear();
		for (String comp_id: plugins) {
			addPlugin(comp_id);
		} // end of for (String comp_id: plugins)
		String[] hostnames = (String[])props.get(HOSTNAMES_PROP_KEY);
		clearRoutings();
		for (String host: hostnames) {
			addRouting(host);
			XMPPResourceConnection conn = createUserSession(NULL_ROUTING, host, host);
			conn.setDummy(true);
		} // end of for ()

		admins = (String[])props.get(ADMINS_PROP_KEY);

	}

	public void handleLogin(final String userName,
		final XMPPResourceConnection conn) {
		String userId = JIDUtils.getNodeID(userName, conn.getDomain());
		XMPPSession session = sessionsByNodeId.get(userId);
		if (session == null) {
			session = new XMPPSession(userName);
			sessionsByNodeId.put(userId, session);
			log.finest("Created new XMPPSession for: " + userId);
		} // end of if (session == null)
		session.addResourceConnection(conn);
	}


	public void handleLogout(final String userName,
		final XMPPResourceConnection conn) {
		String userId = JIDUtils.getNodeID(userName, conn.getDomain());
		XMPPSession session = sessionsByNodeId.get(userId);
		if (session != null && session.getActiveResourcesSize() <= 1) {
			sessionsByNodeId.remove(userId);
		} // end of if (session.getActiveResourcesSize() == 0)
	}

	public Element getDiscoInfo(String node, String jid) {
		if (jid != null && jid.startsWith(getName()+".")) {
			Element query = serviceEntity.getDiscoInfo(node);
			for (XMPPProcessorIfc proc: processors.values()) {
				Element[] discoFeatures = proc.supDiscoFeatures(null);
				if (discoFeatures != null) {
					query.addChildren(Arrays.asList(discoFeatures));
				} // end of if (discoFeatures != null)
			}
			return query;
		}
		return null;
	}

	public List<Element> getDiscoFeatures() {
		List<Element> features = new LinkedList<Element>();
		for (XMPPProcessorIfc proc: processors.values()) {
			Element[] discoFeatures = proc.supDiscoFeatures(null);
			if (discoFeatures != null) {
				features.addAll(Arrays.asList(discoFeatures));
			} // end of if (discoFeatures != null)
		}
		return features;
	}

	public List<Element> getDiscoItems(String node, String jid) {
		if (jid != null && jid.startsWith(getName()+".")) {
			return serviceEntity.getDiscoItems(node, jid);
		} else {
// 			return
// 				Arrays.asList(serviceEntity.getDiscoItem(null, getName() + "." + jid));
			return null;
		}
	}

	public List<StatRecord> getStatistics() {
		List<StatRecord> stats = super.getStatistics();
		stats.add(new StatRecord(getName(), "Open connections", "int",
				connectionsByFrom.size(), Level.FINE));
		stats.add(new StatRecord(getName(), "Open authorized sessions", "int",
				sessionsByNodeId.size(), Level.INFO));
		stats.add(new StatRecord(getName(), "Closed connections", "long",
				closedConnections, Level.FINER));
		return stats;
	}

	private static class NARepository implements NonAuthUserRepository {

		UserRepository rep = null;

		NARepository(UserRepository userRep) {
			rep = userRep;
		}

		private String calcNode(String base, String subnode) {
			if (subnode == null) {
				return base;
			} // end of if (subnode == null)
			return base + "/" + subnode;
		}

		public String getPublicData(String user, String subnode, String key,
			String def)	throws UserNotFoundException {
			try {
				return rep.getData(user, calcNode(PUBLIC_DATA_NODE, subnode), key, def);
			}	catch (TigaseDBException e) {
				log.log(Level.SEVERE, "Problem accessing repository data.", e);
				return null;
			} // end of try-catch
		}

		public String[] getPublicDataList(String user, String subnode, String key)
			throws UserNotFoundException {
			try {
				return rep.getDataList(user, calcNode(PUBLIC_DATA_NODE, subnode), key);
			}	catch (TigaseDBException e) {
				log.log(Level.SEVERE, "Problem accessing repository data.", e);
				return null;
			} // end of try-catch
		}

		public void addOfflineDataList(String user, String subnode, String key,
			String[] list) throws UserNotFoundException {
			try {
				rep.addDataList(user, calcNode(OFFLINE_DATA_NODE, subnode), key, list);
			}	catch (TigaseDBException e) {
				log.log(Level.SEVERE, "Problem accessing repository data.", e);
			} // end of try-catch
		}

		public void addOfflineData(String user, String subnode, String key,
			String value) throws UserNotFoundException, DataOverwriteException {
			String node = calcNode(OFFLINE_DATA_NODE, subnode);
			try {
				String data = rep.getData(user,	node, key);
				if (data == null) {
					rep.setData(user,	node, key, value);
				} else {
					throw new
						DataOverwriteException("Not authorized attempt to overwrite data.");
				} // end of if (data == null) else
			}	catch (TigaseDBException e) {
				log.log(Level.SEVERE, "Problem accessing repository data.", e);
			} // end of try-catch
		}

	}

}
