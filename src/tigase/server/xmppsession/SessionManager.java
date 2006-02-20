/*
 *  Package Tigase XMPP/Jabber Server
 *  Copyright (C) 2004, 2005, 2006
 *  "Artur Hefczyc" <artur.hefczyc@gmail.com>
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

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.TreeMap;
import java.util.logging.Logger;
import javax.security.auth.login.AppConfigurationEntry.LoginModuleControlFlag;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;
import tigase.conf.Configurable;
import tigase.db.UserRepository;
import tigase.db.xml.XMLRepository;
import tigase.server.AbstractMessageReceiver;
import tigase.server.MessageReceiver;
import tigase.server.Packet;
import tigase.server.XMPPService;
import tigase.util.JID;
import tigase.xml.Element;
import tigase.xmpp.IqType;
import tigase.xmpp.ProcessorFactory;
import tigase.xmpp.XMPPProcessorIfc;
import tigase.xmpp.XMPPResourceConnection;
import tigase.xmpp.XMPPSession;
import tigase.auth.TigaseConfiguration;
import tigase.auth.CommitHandler;

/**
 * Class SessionManager
 *
 *
 * Created: Tue Nov 22 07:07:11 2005
 *
 * @author <a href="mailto:artur.hefczyc@gmail.com">Artur Hefczyc</a>
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
	{"jabber:iq:register", "jabber:iq:auth", "urn:ietf:params:xml:ns:xmpp-sasl"};

	public static final String SECURITY_PROP_KEY = "security";

	public static final String AUTHENTICATION_IDS_PROP_KEY = "authentication-ids";
	public static final String[] AUTHENTICATION_IDS_PROP_VAL =
	{"auth-plain", "auth-digest"};

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
	public static final String AUTH_SASL_CLASS_PROP_VAL =
		"tigase.auth.SaslAuth";
	public static final String AUTH_SASL_FLAG_PROP_KEY = "auth-sasl/flag";
	public static final String AUTH_SASL_FLAG_PROP_VAL =	"sufficient";

	private UserRepository repository = null;

	private Map<String, XMPPSession> sessionsByNodeId =
		new TreeMap<String, XMPPSession>();
	private Map<String, XMPPResourceConnection> connectionsByFrom =
		new TreeMap<String, XMPPResourceConnection>();
	private Map<String, XMPPProcessorIfc> processors =
		new TreeMap<String, XMPPProcessorIfc>();
	private TigaseConfiguration authConfig = null;

	public void processPacket(final Packet packet) {
		log.finest("Processing packet: " + packet.getStringData());
		Packet pc = packet;
		if (packet.isRouted()) {
			pc = packet.unpackRouted();
		} // end of if (packet.isRouted())
		if (pc.isCommand()) {
			processCommand(pc, packet.isRouted());
		} // end of if (pc.isCommand())
		else {
			XMPPResourceConnection conn = getXMPPResourceConnection(packet);
			Queue<Packet> results = new LinkedList<Packet>();
			walk(pc, conn, pc.getElement(), results);
			for (Packet res: results) {
				log.finest("Handling response: " + res.getStringData());
				if (packet.isRouted()) {
					addOutPacket(res.packRouted(packet.getFrom(), packet.getTo()));
				} // end of if (packet.isRouted())
				else {
					addOutPacket(res);
				} // end of if (packet.isRouted()) else
			} // end of for ()
		} // end of else
	}

	private void walk(final Packet packet,
		final XMPPResourceConnection connection, final Element elem,
		final Queue<Packet> results) {
		for (XMPPProcessorIfc proc: processors.values()) {
			if (proc.isSupporting(elem.getName(), elem.getXMLNS())) {
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

	private void processCommand(Packet pc, boolean routed) {
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
					connection.setDomain(hostname);
				} // end of if (hostname != null)
				else {
					connection.setDomain(getDefHostName());
				} // end of if (hostname != null) else
				connection.setSessionId(pc.getElemCData("/STREAM_OPENED/session-id"));
				connectionsByFrom.put(pc.getFrom(), connection);
			} else {
				log.finest("Stream opened for existing session, authorized: "
					+ connection.isAuthorized());
			} // end of else
			break;
		case GETFEATURES:
			if (pc.getType() == IqType.get) {
				String features = getFeatures(connectionsByFrom.get(pc.getFrom()));
				Packet result = pc.commandResult(features);
				if (routed) {
					result = result.packRouted();
				} // end of if (packet.isRouted())
				addOutPacket(result);
			} // end of if (pc.getType() == IqType.get)
			break;
		case STREAM_CLOSED:
			XMPPResourceConnection conn = connectionsByFrom.remove(pc.getFrom());
			if (conn != null) {
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

	private String getFeatures(XMPPResourceConnection session) {
		StringBuilder sb = new StringBuilder();
		for (XMPPProcessorIfc proc: processors.values()) {
			String[] features = proc.supStreamFeatures(session);
			if (features != null) {
				for (String f: features) {
					sb.append(f);
				} // end of for ()
			} // end of if (features != null)
		} // end of for ()
		return sb.toString();
	}

	public Map<String, Object> getDefaults() {
		Map<String, Object> props = super.getDefaults();
		props.put(USER_REPOSITORY_PROP_KEY, USER_REPOSITORY_PROP_VAL);
		props.put(COMPONENTS_PROP_KEY, COMPONENTS_PROP_VAL);
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
		return props;
	}

	public void setProperties(Map<String, Object> props) {
		super.setProperties(props);
		repository = new XMLRepository((String)props.get(USER_REPOSITORY_PROP_KEY));
		String[] components = (String[])props.get(COMPONENTS_PROP_KEY);
		processors.clear();
		for (String comp_id: components) {
			processors.put(comp_id, ProcessorFactory.getProcessor(comp_id));
		} // end of for (String comp_id: components)

		String[] auth_ids = (String[])props.get(SECURITY_PROP_KEY + "/" +
			AUTHENTICATION_IDS_PROP_KEY);
		Map<String, AppConfigurationEntry[]> config =
			new HashMap<String, AppConfigurationEntry[]>();
		for (String id: auth_ids) {
			String class_name = (String)props.get(SECURITY_PROP_KEY + "/" + id + "/class");
			String flag = (String)props.get(SECURITY_PROP_KEY + "/" + id + "/flag");
			Map<String, Object> options = new HashMap<String, Object>();
			options.put(CommitHandler.COMMIT_HANDLER_KEY, this);
			options.put(UserRepository.class.getSimpleName(), repository);
			AppConfigurationEntry ace =
				new AppConfigurationEntry(class_name, parseFlag(flag), options);
			config.put(id, new AppConfigurationEntry[] {ace});
		} // end of for ()
		authConfig = new TigaseConfiguration(config);
		Configuration.setConfiguration(authConfig);
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
		XMPPSession session =
			sessionsByNodeId.get(JID.getNodeID(userName, conn.getDomain()));
		if (session == null) {
			session = new XMPPSession(userName);
		} // end of if (session == null)
		session.addResourceConnection(conn);
	}

}
