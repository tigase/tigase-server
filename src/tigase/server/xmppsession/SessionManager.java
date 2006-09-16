/*
 *  Package Tigase XMPP/Jabber Server
 *  Copyright (C) 2004, 2005, 2006
 *  "Artur Hefczyc" <artur.hefczyc@tigase.org>
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software Foundation,
 *  Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 * $Rev$
 * Last modified by $Author$
 * $Date$
 */

package tigase.server.xmppsession;

import java.util.Arrays;
import java.util.List;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.TreeMap;
import java.util.logging.Logger;
import tigase.db.UserNotFoundException;
import java.util.concurrent.ConcurrentSkipListMap;
import tigase.util.DNSResolver;
import java.net.UnknownHostException;
import javax.security.auth.login.AppConfigurationEntry.LoginModuleControlFlag;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;
import tigase.auth.CommitHandler;
import tigase.auth.TigaseConfiguration;
import tigase.conf.Configurable;
import tigase.db.UserRepository;
import tigase.db.xml.XMLRepository;
import tigase.server.AbstractMessageReceiver;
import tigase.server.MessageReceiver;
import tigase.server.Packet;
import tigase.server.XMPPService;
import tigase.util.JID;
import tigase.xml.Element;
import tigase.xmpp.NotAuthorizedException;
import tigase.db.UserNotFoundException;
import tigase.xmpp.ProcessorFactory;
import tigase.xmpp.StanzaType;
import tigase.xmpp.XMPPProcessorIfc;
import tigase.xmpp.XMPPResourceConnection;
import tigase.xmpp.XMPPSession;
import tigase.xmpp.OfflineMessageStorage;
import tigase.stats.StatRecord;

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
	implements Configurable, XMPPService, CommitHandler {

  /**
   * Variable <code>log</code> is a class logger.
   */
  private static final Logger log =
    Logger.getLogger("tigase.server.xmppsession.SessionManager");

	public static final String USER_REPOSITORY_PROP_KEY = "repository-url";
	public static final String USER_REPOSITORY_PROP_VAL = "user-repository.xml";
	public static final String COMPONENTS_PROP_KEY = "components";
	public static final String[] COMPONENTS_PROP_VAL =
	{"jabber:iq:register", "jabber:iq:auth", "urn:ietf:params:xml:ns:xmpp-sasl",
	 "urn:ietf:params:xml:ns:xmpp-bind", "urn:ietf:params:xml:ns:xmpp-session",
	 "message", "jabber:iq:roster", "presence", "jabber:iq:version",
	 "jabber:iq:stats", "starttls", "disco"};

	public static final String HOSTNAMES_PROP_KEY = "hostnames";
	public static String[] HOSTNAMES_PROP_VAL =	{"localhost", "hostname"};

	public static final String SECURITY_PROP_KEY = "security";

	public static final String AUTHENTICATION_IDS_PROP_KEY = "authentication-ids";
	public static final String[] AUTHENTICATION_IDS_PROP_VAL =
	{"auth-plain", "auth-digest", "auth-sasl"};

	public static final String AUTH_PLAIN_CLASS_PROP_KEY = "auth-plain/class";
	public static final String AUTH_PLAIN_CLASS_PROP_VAL =
		"tigase.auth.PlainAuth";
	public static final String AUTH_PLAIN_FLAG_PROP_KEY = "auth-plain/flag";
	public static final String AUTH_PLAIN_FLAG_PROP_VAL =	"sufficient";

	public static final String AUTH_DIGEST_CLASS_PROP_KEY = "auth-digest/class";
	public static final String AUTH_DIGEST_CLASS_PROP_VAL =
		"tigase.auth.DigestAuth";
	public static final String AUTH_DIGEST_FLAG_PROP_KEY = "auth-digest/flag";
	public static final String AUTH_DIGEST_FLAG_PROP_VAL =	"sufficient";

	public static final String AUTH_SASL_CLASS_PROP_KEY = "auth-sasl/class";
	public static final String AUTH_SASL_CLASS_PROP_VAL =	"None";
	public static final String AUTH_SASL_FLAG_PROP_KEY = "auth-sasl/flag";
	public static final String AUTH_SASL_FLAG_PROP_VAL =	"sufficient";

	private UserRepository repository = null;
	private OfflineMessageStorage offlineMessages = null;

	private Map<String, XMPPSession> sessionsByNodeId =
		new ConcurrentSkipListMap<String, XMPPSession>();
	private Map<String, XMPPResourceConnection> connectionsByFrom =
		new ConcurrentSkipListMap<String, XMPPResourceConnection>();
	private Map<String, XMPPProcessorIfc> processors =
		new ConcurrentSkipListMap<String, XMPPProcessorIfc>();
	//	private TigaseConfiguration authConfig = null;

	private long closedConnections = 0;

	public void processPacket(final Packet packet) {
		log.finest("Processing packet: " + packet.getStringData());
		if (packet.isCommand()) {
			processCommand(packet);
			// No more processing is needed for command packet
			return;
		} // end of if (pc.isCommand())
		XMPPResourceConnection conn = getXMPPResourceConnection(packet);
		if (conn == null) {
			// It might be a message _to_ some user on this server
			// so let's look for established session for this user...
			final String to = packet.getElemTo();
			if (to != null) {
				XMPPSession session =	sessionsByNodeId.get(JID.getNodeID(to));
				if (session != null) {
					conn = session.getResourceConnection(packet.getElemTo());
				} else {
					// It might be a message for off-line user....
					// I will put support for off-line message retrieval here...
					if (offlineMessages != null) {
						try {
							offlineMessages.savePacketForOffLineUser(packet);
						} // end of try
						catch (UserNotFoundException e) {
							log.fine("Problem saving off-line message: " + e
								+ ", for packet: " + packet.getStringData());
						} // end of try-catch
					} // end of if (offlineMessages != null)
					// No more processing is needed....
					return;
				} // end of if (session != null) else
			} else {
				// It might be presence message for already off-line user...
				// Just ignore for now...
			} // end of else
		} // end of if (conn == null)
		if (conn != null) {
			Queue<Packet> results = new LinkedList<Packet>();
			walk(packet, conn, packet.getElement(), results);
			for (Packet res: results) {
				log.finest("Handling response: " + res.getStringData());
				addOutPacket(res);
			} // end of for ()
		} else {
			// It might be a message or something else for off-line user....
			// Just ignore for now...
		} // end of else
	}

	private void walk(final Packet packet,
		final XMPPResourceConnection connection, final Element elem,
		final Queue<Packet> results) {
		for (XMPPProcessorIfc proc: processors.values()) {
			String xmlns = elem.getXMLNS();
			if (xmlns == null) { xmlns = "jabber:client";	}
			if (proc.isSupporting(elem.getName(), xmlns)) {
				log.finest("XMPPProcessorIfc: "+proc.getClass().getSimpleName()+
					" ("+proc.id()+")"+"\n Request: "+elem.toString());
				proc.process(packet, connection, results);
			} // end of if (proc.isSupporting(elem.getName(), elem.getXMLNS()))
		} // end of for ()
		Collection<Element> children = elem.getChildren();
		if (children != null) {
			for (Element child: children) {
				walk(packet, connection, child, results);
			} // end of for (Element child: children)
		} // end of if (children != null)
	}

	private void processCommand(Packet pc) {
		log.finer(pc.getCommand().toString() + " command from: " + pc.getFrom());
		switch (pc.getCommand()) {
		case STREAM_OPENED:
			// It maybe existing opened stream after TLS/SASL authorization
			XMPPResourceConnection connection =	connectionsByFrom.get(pc.getFrom());
			// If not, it means this is new stream
			if (connection == null) {
				log.finer("Adding resource connection for: " + pc.getFrom());
				final String hostname = pc.getElemCData("/STREAM_OPENED/hostname");
				connection = new XMPPResourceConnection(pc.getFrom(), repository);
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
			connection.setSessionId(pc.getElemCData("/STREAM_OPENED/session-id"));
			log.finest("Setting session-id " + connection.getSessionId()
				+ " for connection: " + connection.getConnectionId());
			break;
		case GETFEATURES:
			if (pc.getType() == StanzaType.get) {
				List<Element> features =
					getFeatures(connectionsByFrom.get(pc.getFrom()));
				Packet result = pc.commandResult(features);
				addOutPacket(result);
			} // end of if (pc.getType() == StanzaType.get)
			break;
		case STREAM_CLOSED:
			log.fine("Stream closed from: " + pc.getFrom());
			++closedConnections;
			final XMPPResourceConnection conn =
				connectionsByFrom.remove(pc.getFrom());
			if (conn != null) {
				try {
					String userId = conn.getUserId();
					XMPPSession session = conn.getParentSession();
					if (session != null) {
						if (session.getActiveResourcesSize() == 1) {
							sessionsByNodeId.remove(userId);
						}
					} // end of if (session.getActiveResourcesSize() == 0)
				} catch (NotAuthorizedException e) {}
				Queue<Packet> results = new LinkedList<Packet>();
				for (XMPPProcessorIfc proc: processors.values()) {
					proc.stopped(conn, results);
				} // end of for ()
				for (Packet res: results) {
					log.finest("Handling response: " + res.getStringData());
					addOutPacket(res);
				} // end of for ()
				conn.streamClosed();
			} // end of if (conn != null)
			else {
				log.warning("Can not find resource connection for packet: " +
					pc.toString());
			} // end of if (conn != null) else
			break;
		case GETDISCO:

			break;
		default:
			break;
		} // end of switch (pc.getCommand())
	}

	private XMPPResourceConnection getXMPPResourceConnection(Packet p) {
		return connectionsByFrom.get(p.getFrom());
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

	public Map<String, Object> getDefaults() {
		Map<String, Object> props = super.getDefaults();
		props.put(USER_REPOSITORY_PROP_KEY, USER_REPOSITORY_PROP_VAL);
		props.put(COMPONENTS_PROP_KEY, COMPONENTS_PROP_VAL);
		HOSTNAMES_PROP_VAL = DNSResolver.getDefHostNames();
		props.put(HOSTNAMES_PROP_KEY, HOSTNAMES_PROP_VAL);
		props.put(SECURITY_PROP_KEY + "/" + AUTHENTICATION_IDS_PROP_KEY,
			AUTHENTICATION_IDS_PROP_VAL);
		props.put(SECURITY_PROP_KEY + "/" + AUTH_PLAIN_CLASS_PROP_KEY,
			AUTH_PLAIN_CLASS_PROP_VAL);
		props.put(SECURITY_PROP_KEY + "/" + AUTH_PLAIN_FLAG_PROP_KEY,
			AUTH_PLAIN_FLAG_PROP_VAL);
		props.put(SECURITY_PROP_KEY + "/" + AUTH_DIGEST_CLASS_PROP_KEY,
			AUTH_DIGEST_CLASS_PROP_VAL);
		props.put(SECURITY_PROP_KEY + "/" + AUTH_DIGEST_FLAG_PROP_KEY,
			AUTH_DIGEST_FLAG_PROP_VAL);
		props.put(SECURITY_PROP_KEY + "/" + AUTH_SASL_CLASS_PROP_KEY,
			AUTH_SASL_CLASS_PROP_VAL);
		props.put(SECURITY_PROP_KEY + "/" + AUTH_SASL_FLAG_PROP_KEY,
			AUTH_SASL_FLAG_PROP_VAL);
		return props;
	}

	public void setProperties(Map<String, Object> props) {
		super.setProperties(props);
		repository = new XMLRepository((String)props.get(USER_REPOSITORY_PROP_KEY));
		offlineMessages = new OfflineMessageStorage(repository);
		String[] components = (String[])props.get(COMPONENTS_PROP_KEY);
		processors.clear();
		for (String comp_id: components) {
			XMPPProcessorIfc proc = ProcessorFactory.getProcessor(comp_id);
			processors.put(comp_id, proc);
			log.config("Added processor: " + proc.getClass().getSimpleName()
				+ " for component id: " + comp_id);
		} // end of for (String comp_id: components)
		String[] hostnames = (String[])props.get(HOSTNAMES_PROP_KEY);
		clearRoutings();
		for (String host: hostnames) {
			addRouting(host);
		} // end of for ()

		String[] auth_ids = (String[])props.get(SECURITY_PROP_KEY + "/" +
			AUTHENTICATION_IDS_PROP_KEY);
		TigaseConfiguration authConfig = TigaseConfiguration.getConfiguration();
		for (String id: auth_ids) {
			String class_name = (String)props.get(SECURITY_PROP_KEY + "/" + id + "/class");
			String flag = (String)props.get(SECURITY_PROP_KEY + "/" + id + "/flag");
			Map<String, Object> options = new HashMap<String, Object>();
			options.put(CommitHandler.COMMIT_HANDLER_KEY, this);
			options.put(UserRepository.class.getSimpleName(), repository);
			AppConfigurationEntry ace =
				new AppConfigurationEntry(class_name, parseFlag(flag), options);
			authConfig.putAppConfigurationEntry(id,
				new AppConfigurationEntry[] {ace});
			log.config("Added security module: " + class_name
				+ " for auth id: " + id + ", flag: " + flag);
		} // end of for ()
	}

	private LoginModuleControlFlag parseFlag(final String flag) {
		if (flag.equalsIgnoreCase("REQUIRED"))
			return LoginModuleControlFlag.REQUIRED;
		else if (flag.equalsIgnoreCase("REQUISITE"))
			return LoginModuleControlFlag.REQUISITE;
		else if (flag.equalsIgnoreCase("SUFFICIENT"))
			return LoginModuleControlFlag.SUFFICIENT;
		else if (flag.equalsIgnoreCase("OPTIONAL"))
			return LoginModuleControlFlag.OPTIONAL;
		return null;
	}

	public void handleLoginCommit(final String userName,
		final XMPPResourceConnection conn) {
		String userId = JID.getNodeID(userName, conn.getDomain());
		XMPPSession session = sessionsByNodeId.get(userId);
		if (session == null) {
			session = new XMPPSession(userName);
			sessionsByNodeId.put(userId, session);
		} // end of if (session == null)
		session.addResourceConnection(conn);
		try {
			Queue<Packet> packets =
				offlineMessages.restorePacketForOffLineUser(userId);
 			if (packets != null) {
				addPackets(packets);
// 				Packet p = null;
// 				while ((p = packets.poll()) != null) {
// 					processPacket(p);
// 				} // end of while ((p = packets.poll()) != null)
 			} // end of if (packets != null)
		} // end of try
		catch (UserNotFoundException e) {

		} // end of try-catch
	}

	public void handleLogout(final String userName,
		final XMPPResourceConnection conn) {
		String userId = JID.getNodeID(userName, conn.getDomain());
		XMPPSession session = sessionsByNodeId.get(userId);
		if (session != null && session.getActiveResourcesSize() == 0) {
			sessionsByNodeId.remove(userId);
		} // end of if (session.getActiveResourcesSize() == 0)
	}

	public List<String> getDiscoFeatures() {
		List<String> results = new LinkedList<String>();
		for (XMPPProcessorIfc proc: processors.values()) {
			String[] discoFeatures = proc.supDiscoFeatures(null);
			if (discoFeatures != null) {
				results.addAll(Arrays.asList(discoFeatures));
			} // end of if (discoFeatures != null)
		}
		return results;
	}

	public List<StatRecord> getStatistics() {
		List<StatRecord> stats = super.getStatistics();
		stats.add(new StatRecord("Open connections", "int",
				connectionsByFrom.size()));
		stats.add(new StatRecord("Open authorized sessions", "int",
				sessionsByNodeId.size()));
		stats.add(new StatRecord("Closed connections", "long", closedConnections));
		return stats;
	}

}
