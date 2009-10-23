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
import tigase.server.script.CommandIfc;
import java.security.Security;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Queue;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.concurrent.CopyOnWriteArraySet;
import javax.script.Bindings;
import tigase.auth.TigaseSaslProvider;
import tigase.conf.Configurable;
import tigase.db.DataOverwriteException;
import tigase.db.NonAuthUserRepository;
import tigase.db.RepositoryFactory;
import tigase.db.TigaseDBException;
import tigase.db.UserAuthRepository;
import tigase.db.UserNotFoundException;
import tigase.db.UserRepository;
import tigase.disco.XMPPService;
import tigase.server.AbstractMessageReceiver;
import tigase.server.Command;
import tigase.server.Packet;
import tigase.server.Permissions;
import tigase.server.ReceiverEventHandler;
import tigase.server.XMPPServer;
import tigase.stats.StatisticsList;
import tigase.sys.OnlineJidsReporter;
import tigase.sys.TigaseRuntime;
import tigase.util.JIDUtils;
import tigase.util.PriorityQueue;
import tigase.util.ProcessingThreads;
import tigase.util.QueueItem;
import tigase.util.WorkerThread;
import tigase.vhosts.VHostItem;
import tigase.xml.Element;
import tigase.xmpp.Authorization;
import tigase.xmpp.NotAuthorizedException;
import tigase.xmpp.PacketErrorTypeException;
import tigase.xmpp.ProcessorFactory;
import tigase.xmpp.StanzaType;
import tigase.xmpp.XMPPPostprocessorIfc;
import tigase.xmpp.XMPPPreprocessorIfc;
import tigase.xmpp.XMPPProcessorIfc;
import tigase.xmpp.XMPPResourceConnection;
import tigase.xmpp.XMPPSession;
import tigase.xmpp.XMPPStopListenerIfc;
import tigase.xmpp.ConnectionStatus;

import tigase.xmpp.XMPPException;
import tigase.xmpp.XMPPPacketFilterIfc;
import static tigase.server.xmppsession.SessionManagerConfig.*;

/**
 * Class SessionManager
 *
 *
 * Created: Tue Nov 22 07:07:11 2005
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @author <a href="mailto:peter@stardoll.com">Peter Sandström</a> - multi-threadin
 * support for pluggins processors.
 * @version $Rev$
 */
public class SessionManager extends AbstractMessageReceiver
	implements Configurable, SessionManagerHandler,	OnlineJidsReporter {

  /**
   * Variable <code>log</code> is a class logger.
   */
  private static final Logger log =
					Logger.getLogger(SessionManager.class.getName());

	protected static final String SESSION_PACKETS = "session-packets";
	protected static final String ADMIN_COMMAND_NODE =
					"http://jabber.org/protocol/admin";

	private UserRepository user_repository = null;
	private UserAuthRepository auth_repository = null;
	private NonAuthUserRepository naUserRepository = null;
	private PacketFilter filter = null;

	//{"admin@localhost"};
	private Set<String> trusted = new CopyOnWriteArraySet<String>();
	//{"admin@localhost"};

	/**
	 * A Map with bare user JID as a key and a user session object as a value.
	 */
	private ConcurrentHashMap<String, XMPPSession> sessionsByNodeId =
		new ConcurrentHashMap<String, XMPPSession>();
	/**
	 * A Map with connectionID as a key and an object with all the user connection
	 * data as a value
	 */
	protected ConcurrentHashMap<String, XMPPResourceConnection> connectionsByFrom =
		new ConcurrentHashMap<String, XMPPResourceConnection>();

	private Map<String, XMPPPreprocessorIfc> preProcessors =
			new ConcurrentSkipListMap<String, XMPPPreprocessorIfc>();
	private Map<String, ProcessingThreads<ProcessorWorkerThread>> processors =
			new ConcurrentSkipListMap<String, ProcessingThreads<ProcessorWorkerThread>>();
	private Map<String, XMPPPostprocessorIfc> postProcessors =
			new ConcurrentSkipListMap<String, XMPPPostprocessorIfc>();
	private Map<String, XMPPStopListenerIfc> stopListeners =
			new ConcurrentSkipListMap<String, XMPPStopListenerIfc>();
	private Map<String, Map<String, Object>> plugin_config =
			new ConcurrentSkipListMap<String, Map<String, Object>>();
	private Map<String, XMPPPacketFilterIfc> outFilters =
			new ConcurrentSkipListMap<String, XMPPPacketFilterIfc>();
	private ProcessingThreads<SessionCloseWorkerThread> sessionCloseThread =
			new ProcessingThreads<SessionCloseWorkerThread>(
			new SessionCloseWorkerThread(), 4, 1, maxQueueSize, "session-close");
	private ProcessingThreads<SessionOpenWorkerThread> sessionOpenThread =
			new ProcessingThreads<SessionOpenWorkerThread>(
			new SessionOpenWorkerThread(this), 1, 1, maxQueueSize, "session-open");
	private Timer authenticationWatchdog = new Timer("SM authentocation watchdog");

	private ConnectionCheckCommandHandler connectionCheckCommandHandler =
					new ConnectionCheckCommandHandler();

	//private Set<String> anonymous_domains = new HashSet<String>();

	//private XMPPResourceConnection serverSession = null;

//	private ServiceEntity adminDisco = null;

	private int maxUserSessions = 0;
	private int maxUserConnections = 0;
	private long totalUserSessions = 0;
	private long totalUserConnections = 0;
	private long closedConnections = 0;
	private long authTimeouts = 0;
	private int maxPluginsNo = 0;
	private boolean skipPrivacy = false;

	private Timer reaperTask = null;
	private long reaperInterval = 60 * 1000;
	private long maxIdleTime = 86400 * 1000;

//	@Override
//	public void start() {
//		super.start();
//
//		reaperTask = new Timer("Session reaper task", true);
//		reaperTask.schedule(new TimerTask() {
//			@Override
//			public void run() {
//				long currentTime = System.currentTimeMillis();
//				for (Enumeration<XMPPResourceConnection> e = connectionsByFrom.elements(); e.hasMoreElements(); ) {
//					XMPPResourceConnection xrc = e.nextElement();
//					if (!"session-id-sess-man".equals(xrc.getSessionId())) {
//						if (currentTime - xrc.getLastAccessed() > maxIdleTime && currentTime - xrc.getCreationTime() > reaperInterval) {
//							if (log.isLoggable(Level.WARNING)) {
//								log.info("Logging out " + xrc.getSessionId() + " after >" + (maxIdleTime/1000) + " seconds of inactivity");
//							}
//							try {
//								xrc.logout();
//							} catch (NotAuthorizedException ex) {
//								if (log.isLoggable(Level.WARNING)) {
//									log.warning("Could not logout " + xrc.getSessionId() + ": " + ex.getMessage());
//								}
//							}
//						}
//					}
//				}
//			}
//		}, reaperInterval, reaperInterval);
//	}
//
//	@Override
//	public void stop() {
//		super.stop();
//
//		reaperTask.cancel();
//		reaperTask = null;
//	}

	@Override
	public void setName(String name) {
		super.setName(name);
//		serviceEntity = new ServiceEntity(name, "sm", "Session manager");
//		serviceEntity.addIdentities(
//			new ServiceIdentity("component", "sm", "Session manager"));
//		CommandIfc command = new AddScriptCommand();
//		command.init(CommandIfc.ADD_SCRIPT_CMD, "New command script");
//		adminCommands.put(command.getCommandId(), command);
//		ServiceEntity item = new ServiceEntity(getName(),
//						"http://jabber.org/protocol/admin#" + command.getCommandId(),
//						command.getDescription());
//		item.addIdentities(
//						new ServiceIdentity("component", "generic", command.getDescription()),
//						new ServiceIdentity("automation", "command-node",	command.getDescription()));
//		item.addFeatures(CMD_FEATURES);
//		serviceEntity.addItems(item);
//		command = new RemoveScriptCommand();
//		command.init(CommandIfc.DEL_SCRIPT_CMD, "Remove command script");
//		adminCommands.put(command.getCommandId(), command);
//		item = new ServiceEntity(getName(),
//						"http://jabber.org/protocol/admin#" + command.getCommandId(),
//						command.getDescription());
//		item.addIdentities(
//						new ServiceIdentity("component", "generic", command.getDescription()),
//						new ServiceIdentity("automation", "command-node",	command.getDescription()));
//		item.addFeatures(CMD_FEATURES);
//		serviceEntity.addItems(item);
		TigaseRuntime.getTigaseRuntime().addOnlineJidsReporter(this);
	}

//	/**
//	 * This method can be overwritten in extending classes to get a different
//	 * packets distribution to different threads. For PubSub, probably better
//	 * packets distribution to different threads would be based on the
//	 * sender address rather then destination address.
//	 * @param packet
//	 * @return
//	 */
//	@Override
//	public int hashCodeForPacket(Packet packet) {
//		if (packet.getFrom() != null && packet.getFrom() != packet.getElemFrom()) {
//			// This comes from connection manager so the best way is to get hashcode
//			// by the connectionId, which is in the getFrom()
//			return packet.getFrom().hashCode();
//		}
//		// If not, then a better way is to get hashCode from the elemTo address
//		// as this would be by the destination address user name:
//		if (packet.getElemTo() != null) {
//			return packet.getElemTo().hashCode();
//		}
//		// Otherwise, just get the default....
//		return super.hashCodeForPacket(packet);
//	}



//	private void debug_packet(String msg, Packet packet, String to) {
//		if (packet.getElemTo().equals(to)) {
//			log.finest(msg + ", packet: " + packet.getStringData());
//		}
//	}

	protected XMPPResourceConnection getXMPPResourceConnection(String connId) {
		return connectionsByFrom.get(connId);
	}

	protected XMPPResourceConnection getXMPPResourceConnection(Packet p) {
		XMPPResourceConnection conn = null;
		final String from = p.getFrom();
		if (from != null) {
			conn = connectionsByFrom.get(from);
			if (conn != null) {
				return conn.getConnectionStatus() == ConnectionStatus.TEMP ? null : conn;
			}
		}
		// It might be a message _to_ some user on this server
		// so let's look for established session for this user...
		final String to = p.getElemTo();
		if (to != null) {
			if (log.isLoggable(Level.FINEST)) {
				log.finest("Searching for resource connection for: " + to);
			}
			conn = getResourceConnection(to);
			if (conn != null && conn.getConnectionStatus() == ConnectionStatus.TEMP) {
				conn = null;
			}
		} else {
			// Hm, not sure what should I do now....
			// Maybe I should treat it as message to admin....
			log.info("Message without TO attribute set, don't know what to do wih this: "
				+ p.getStringData());
		} // end of else

		return conn;
	}

	protected boolean isBrokenPacket(Packet p) {
		if (!p.getFrom().equals(p.getElemFrom()) && (!p.isCommand()
				|| (p.isCommand() && p.getCommand() == Command.OTHER))) {
			// Sometimes (Bosh) connection is gone and this is an error packet
			// sent back to the original sender. This original sender might
			// not local....
			if (p.getElemFrom() != null && 
							!isLocalDomain(JIDUtils.getNodeHost(p.getElemFrom()))) {
				// ok just forward it there....
				p.setFrom(null);
				p.setTo(null);
				fastAddOutPacket(p);
				return true;
			}
			// It doesn't look good, there should really be a connection for
			// this packet....
			// returning error back...
			log.fine("Broken packet: " + p.toString());
			try {
				Packet error =
						Authorization.SERVICE_UNAVAILABLE.getResponseMessage(p,
							"Service not available.", true);
				error.setTo(p.getFrom());
				fastAddOutPacket(error);
			} catch (PacketErrorTypeException e) {
				log.fine("Packet is error type already: " + p.toString());
			}
			return true;
		}
		return false;
	}

	@Override
	public void processPacket(final Packet packet) {
		if (log.isLoggable(Level.FINEST)) {
			log.finest("Received packet: " + packet.toString());
		}
		if (packet.isCommand() && processCommand(packet)) {
			packet.processedBy("SessionManager");
			// No more processing is needed for command packet
			return;
		} // end of if (pc.isCommand())
		XMPPResourceConnection conn = getXMPPResourceConnection(packet);
		if (conn == null && (isBrokenPacket(packet) ||
						processAdminsOrDomains(packet))) {
			return;
		}
		processPacket(packet, conn);
	}

	protected void processPacket(Packet packet, XMPPResourceConnection conn) {
		packet.setTo(getComponentId());
		if (log.isLoggable(Level.FINEST)) {
			log.finest("processing packet: " + packet.toString() +
							", connectionID: " +
							(conn != null ? conn.getConnectionId() : "null"));
		}
		Queue<Packet> results = new LinkedList<Packet>();

		boolean stop = false;
		if (!stop) {
			if (filter.preprocess(packet, conn, naUserRepository, results)) {
				packet.processedBy("filter-foward");
				if (log.isLoggable(Level.FINEST)) {
					log.finest("Packet preprocessed: " + packet.toString());
					if (results.size() > 0) {
						for (Packet p: results) {
							log.finest("Preprocess result: " + p.toString());
						}
					}
				}
				addOutPackets(packet, conn, results);
				return;
			}
		}

		// Preprocess..., all preprocessors get all messages to look at.
		// I am not sure if this is correct for now, let's try to do it this
		// way and maybe change it later.
		// If any of them returns true - it means processing should stop now.
		// That is needed for preprocessors like privacy lists which should
		// block certain packets.

		if (!stop) {
			for (XMPPPreprocessorIfc preproc: preProcessors.values()) {
				stop |= preproc.preProcess(packet, conn, naUserRepository, results);
			} // end of for (XMPPPreprocessorIfc preproc: preProcessors)
		}

		if (!stop) {
			if (filter.forward(packet, conn, naUserRepository, results)) {
				packet.processedBy("filter-foward");
				if (log.isLoggable(Level.FINEST)) {
					log.finest("Packet forwarded: " + packet.toString());
				}
				addOutPackets(packet, conn, results);
				return;
			}
		}

		if (!stop) {
			walk(packet, conn, packet.getElement(), results);
		}

		if (!stop) {
			for (XMPPPostprocessorIfc postproc: postProcessors.values()) {
				postproc.postProcess(packet, conn, naUserRepository, results);
			} // end of for (XMPPPostprocessorIfc postproc: postProcessors)
		} // end of if (!stop)

		if (!stop && !packet.wasProcessed() && !isLocalDomain(packet.getElemTo())
			&& filter.process(packet, conn, naUserRepository, results)) {
			packet.processedBy("filter-process");
		}

		setPermissions(conn, results);
		addOutPackets(packet, conn, results);

		if (!packet.wasProcessed()) {
			if (log.isLoggable(Level.FINEST)) {
				log.finest("Packet not processed: " + packet.toString());
			}
			Packet error = null;
			if (stop
				|| (conn == null
					&& packet.getElemFrom() != null && packet.getElemTo() != null
					&& packet.getElemTo() != getComponentId()
					&& (packet.getElemName().equals("iq")
						|| packet.getElemName().equals("message")))) {
				try {
					error =	Authorization.SERVICE_UNAVAILABLE.getResponseMessage(packet,
						"Service not available.", true);
				} catch (PacketErrorTypeException e) {
					log.fine("Service not available. Packet is error type already: " + packet.toString());
				}
			} else {
				if (packet.getElemFrom() != null || conn != null) {
					try {
						error = Authorization.FEATURE_NOT_IMPLEMENTED.getResponseMessage(packet,
							"Feature not supported yet.", true);
					} catch (PacketErrorTypeException e) {
						log.fine("Feature not supported yet. Packet is error type already: " + packet.toString());
					}
				}
			}
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
			if (log.isLoggable(Level.FINEST)) {
				log.finest("Packet processed by: " + packet.getProcessorsIds().toString());
			}
		} // end of else
	}

	private void setPermissions(XMPPResourceConnection conn,
		Queue<Packet> results) {
		Permissions perms = Permissions.NONE;
		if (conn != null) {
			perms = Permissions.LOCAL;
			if (conn.isAuthorized()) {
				perms = Permissions.AUTH;
				if (conn.isAnonymous()) {
					perms = Permissions.ANONYM;
				} else {
					try {
						String id = conn.getUserId();
						if (isTrusted(id)) {
							perms = Permissions.TRUSTED;
						}
						if (isAdmin(id)) {
							perms = Permissions.ADMIN;
						}
					} catch (NotAuthorizedException e) {
						perms = Permissions.NONE;
					}
				}
			}
		}
		for (Packet res: results) {
			res.setPermissions(perms);
		}
	}

	@Override
	public int processingThreads() {
		return Runtime.getRuntime().availableProcessors();
	}

	protected boolean isTrusted(String jid) {
		if (trusted.contains(JIDUtils.getNodeID(jid))) {
			return true;
		}
		return isAdmin(jid);
	}

	protected boolean addTrusted(String jid) {
		return trusted.add(JIDUtils.getNodeID(jid));
	}

	protected boolean delTrusted(String jid) {
		return trusted.remove(JIDUtils.getNodeID(jid));
	}

	protected boolean processAdminsOrDomains(Packet packet) {
		final String to = packet.getElemTo();
		if (isLocalDomain(to)) {
			if (packet.getElemName().equals("message")) {
				// Yes this packet is for admin....
				if (log.isLoggable(Level.FINER)) {
					log.finer("Packet for admin: " + packet.getStringData());
				}
				sendToAdmins(packet);
			} else {
				if (log.isLoggable(Level.FINER)) {
					log.finer("Packet for hostname: " + packet.getStringData());
				}
				Packet host_pac =
          new Packet(packet.getElement().clone());
				host_pac.getElement().setAttribute("to", getComponentId());
				host_pac.getElement().setAttribute(Packet.OLDTO, packet.getElemTo());
				host_pac.getElement().setAttribute(Packet.OLDFROM, packet.getElemFrom());
				processPacket(host_pac);
			}
			return true;
		} // end of if (isInRoutings(to))
		return false;
	}

	protected void sendToAdmins(Packet packet) {
		for (String admin: admins) {
			if (log.isLoggable(Level.FINER)) {
				log.finer("Sending packet to admin: " + admin);
			}
			Packet admin_pac =
        new Packet(packet.getElement().clone());
			admin_pac.getElement().setAttribute("to", admin);
			processPacket(admin_pac);
		}
	}

	protected XMPPSession getSession(String jid) {
		return sessionsByNodeId.get(JIDUtils.getNodeID(jid));
	}

	protected XMPPResourceConnection getResourceConnection(String jid) {
		XMPPSession session = getSession(jid);
		if (session != null) {
			if (log.isLoggable(Level.FINEST)) {
				log.finest("Session not null, getting resource for jid: " + jid);
			}
			return session.getResourceConnection(jid);
		} // end of if (session != null)
		return null;
	}

	private void walk(final Packet packet,
		final XMPPResourceConnection connection, final Element elem,
		final Queue<Packet> results) {
		for (ProcessingThreads<ProcessorWorkerThread> proc_t: processors.values()) {
			String xmlns = elem.getXMLNS();
			if (xmlns == null) { xmlns = "jabber:client";	}
			XMPPProcessorIfc processor = proc_t.getWorkerThread().processor;
			if (processor.isSupporting(elem.getName(), xmlns)) {
				if (log.isLoggable(Level.FINEST)) {
					log.finest("XMPPProcessorIfc: "+processor.getClass().getSimpleName()+
						" ("+processor.id()+")"+"\n Request: "+elem.toString()
						+ (connection != null ? ", " + connection.getConnectionId() : " null"));
				}
				if (proc_t.addItem(packet, connection)) {
					packet.processedBy(processor.id());
				} else {
//					proc_t.debugQueue();
					if (log.isLoggable(Level.FINE)) {
						log.fine("Can not add packet: " + packet.toString() +
										" to processor: " + proc_t.getName() +
										" internal queue full.");
					}
				}
			} // end of if (proc.isSupporting(elem.getName(), elem.getXMLNS()))
		} // end of for ()
		Collection<Element> children = elem.getChildren();
		if (children != null) {
			for (Element child: children) {
				walk(packet, connection, child, results);
			} // end of for (Element child: children)
		} // end of if (children != null)
	}

	@Override
	protected Integer getMaxQueueSize(int def) {
		return def*10;
	}

//	private boolean isAnonymousEnabled(String domain) {
//		return vHostManager != null ? vHostManager.isAnonymousEnabled(domain) :
//			false;
//	}

	protected boolean processCommand(Packet pc) {
		if (!(pc.getElemTo() == null) &&
						!getComponentId().equals(pc.getElemTo()) &&
						!isLocalDomain(pc.getElemTo())) {
			return false;
		}
		boolean processing_result = false;
		if (log.isLoggable(Level.FINER)) {
			log.finer(pc.getCommand().toString() + " command from: " + pc.getFrom());
		}
		//Element command = pc.getElement();
		XMPPResourceConnection connection =	connectionsByFrom.get(pc.getFrom());
		switch (pc.getCommand()) {
		case STREAM_OPENED:
			// Response is sent from the thread when opening user session is
			// completed.
			//fastAddOutPacket(pc.okResult((String) null, 0));
			sessionOpenThread.addItem(pc, connection);
			processing_result = true;
			break;
		case GETFEATURES:
			if (pc.getType() == StanzaType.get) {
				List<Element> features =
					getFeatures(connectionsByFrom.get(pc.getFrom()));
				Packet result = pc.commandResult(null);
				Command.setData(result, features);
				addOutPacket(result);
			} // end of if (pc.getType() == StanzaType.get)
			processing_result = true;
			break;
		case STREAM_CLOSED:
			fastAddOutPacket(pc.okResult((String)null, 0));
			sessionCloseThread.addItem(pc, connection);
			//closeConnection(pc.getFrom(), false);
			processing_result = true;
			break;
		case STREAM_CLOSED_UPDATE:
			// Note! We don't send response to this packet....
			if (connectionsByFrom.get(pc.getFrom()) != null) {
				sessionCloseThread.addItem(pc, null);
			}
			//closeConnection(pc.getFrom(), false);
			processing_result = true;
			break;
		case BROADCAST_TO_ONLINE:
			String from = pc.getFrom();
			boolean trusted = false;
			try {
				trusted = (from != null && isTrusted(from))
					|| (connection != null && isTrusted(connection.getUserId()));
			} catch (NotAuthorizedException e) {
				trusted = false;
			}
			try {
				if (trusted) {
					List<Element> packets = Command.getData(pc);
					if (packets != null) {
						for (XMPPResourceConnection conn: connectionsByFrom.values()) {
							if (conn.isAuthorized()) {
								try {
									for (Element el_pack: packets) {
										Element el_copy = el_pack.clone();
										el_copy.setAttribute("to", conn.getJID());
										Packet out_packet = new Packet(el_copy);
										out_packet.setTo(conn.getConnectionId());
										addOutPacket(out_packet);
									}
								} catch (NotAuthorizedException e) {
									log.warning("Something wrong, connection is authenticated but "
										+ "NoAuthorizedException is thrown.");
								}
							}
						}
					} else {
						addOutPacket(Authorization.BAD_REQUEST.getResponseMessage(pc,
								"Missing packets for broadcast.", true));
					}
				} else {
					addOutPacket(Authorization.FORBIDDEN.getResponseMessage(pc,
							"You don't have enough permission to brodcast packet.", true));
				}
			} catch (PacketErrorTypeException e) {
				log.fine("Packet is error type already: " + pc.toString());
			}
			processing_result = true;
			break;
		case USER_STATUS:
			try {
				if (isTrusted(pc.getElemFrom())
					|| isTrusted(JIDUtils.getNodeHost(pc.getElemFrom()))) {
					String av = Command.getFieldValue(pc, "available");
					boolean available = !(av != null && av.equalsIgnoreCase("false"));
					if (available) {
						Packet presence = null;
						Element p = pc.getElement().getChild("command").getChild("presence");
						if (p != null) {
// +							// use this hack to break XMLNS
// +							Element el = new Element("presence");
// +							el.setChildren(p.getChildren());
							Element elem = p.clone();
							elem.setXMLNS("jabber:client");
							presence = new Packet(elem);
						}
						connection = connectionsByFrom.get(pc.getElemFrom());
						if (connection == null) {
							String user_jid = Command.getFieldValue(pc, "jid");
							String hostname = JIDUtils.getNodeHost(user_jid);
							connection = loginUserSession(pc.getElemFrom(), hostname,
											JIDUtils.getNodeID(user_jid),
											JIDUtils.getNodeResource(user_jid), ConnectionStatus.NORMAL,
											"USER_STATUS");
							connection.putSessionData("jingle", "active");
							fastAddOutPacket(pc.okResult((String)null, 0));
							if (presence == null) {
								presence =
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
							}
						} else {
// 							addOutPacket(Authorization.CONFLICT.getResponseMessage(pc,
// 									"The user resource already exists.", true));
							if (log.isLoggable(Level.FINEST)) {
								log.finest("USER_STATUS set to true for user who is already available: "
									+ pc.toString());
							}
						}
						if (presence != null) {
							presence.setFrom(pc.getElemFrom());
							presence.setTo(getComponentId());
							addOutPacket(presence);
						}
					} else {
						connection = connectionsByFrom.remove(pc.getElemFrom());
						if (connection != null) {
							closeSession(connection, false);
							addOutPacket(pc.okResult((String)null, 0));
						} else {
							addOutPacket(Authorization.ITEM_NOT_FOUND.getResponseMessage(pc,
									"The user resource you want to remove does not exist.", true));
							log.info("Can not find resource connection for packet: " +
								pc.toString());
						}
					}
				} else {
					try {
						addOutPacket(Authorization.FORBIDDEN.getResponseMessage(pc,
								"Only trusted entity can do it.", true));
					} catch (PacketErrorTypeException e) {
						log.warning("Packet error type when not expected: " + pc.toString());
					}
				}
			} catch (Exception e) {
				try {
					addOutPacket(Authorization.UNDEFINED_CONDITION.getResponseMessage(pc,
							"Unexpected error occured during the request: " + e, true));
				} catch (Exception ex) { ex.printStackTrace(); }
				log.log(Level.WARNING, "USER_STATUS session creation error: ", e);
			}
			processing_result = true;
			break;
		case REDIRECT:
			if (connection != null) {
				String action = Command.getFieldValue(pc, "action");
				if (action.equals("close")) {
					if (log.isLoggable(Level.FINE)) {
						log.fine("Closing redirected connections: " + pc.getFrom());
					}
					sendAllOnHold(connection);
					closeConnection(pc.getFrom(), true);
				} else {
					if (log.isLoggable(Level.FINE)) {
						log.fine("Activating redirected connections: " + pc.getFrom());
					}
				}
			} else {
				if (log.isLoggable(Level.FINE)) {
					log.fine("Redirect for non-existen connection: " + pc.toString());
				}
			}
			processing_result = true;
			break;
		case OTHER:
//			String strCommand = pc.getStrCommand();
//			if (strCommand != null && strCommand.contains(ADMIN_COMMAND_NODE)) {
//				Command.Action action = Command.getAction(pc);
//				if (action != Command.Action.cancel) {
//					boolean admin = false;
//					try {
//						admin = connection != null && connection.isAuthorized() &&
//										isAdmin(connection.getUserId());
//						if (admin) {
//							if (log.isLoggable(Level.FINER)) {
//								log.finer("Processing admin command: " + pc.toString());
//							}
//							int hashIdx = strCommand.indexOf('#');
//							String scriptId = strCommand.substring(hashIdx + 1);
//							CommandIfc com = adminCommands.get(scriptId);
//							if (com == null) {
//								Packet result = pc.commandResult(Command.DataType.result);
//								Command.addTextField(result, "Error", "The command: " + scriptId +
//												" is not available yet.");
//								fastAddOutPacket(result);
//							} else {
//								Bindings binds = scriptEngineManager.getBindings();
//								initBindings(binds);
//								Queue<Packet> results = new LinkedList<Packet>();
//								com.runCommand(pc, binds, results);
//								addOutPackets(results);
//							}
//						}
//					} catch (NotAuthorizedException e) {
//						admin = false;
//					} catch (Exception e) {
//						log.log(Level.WARNING,
//										"Unknown admin command processing exception: " +
//										pc.toString(), e);
//					}
//					if (!admin) {
//						try {
//							if (log.isLoggable(Level.FINER)) {
//								log.finer("Command rejected non-admin detected: " +
//												(connection != null ? (connection.isAuthorized() +
//												": " + connection.getUserId())
//												: "null"));
//							}
//							addOutPacket(Authorization.FORBIDDEN.getResponseMessage(pc,
//											"Only Administrator can call the command.", true));
//						} catch (Exception e) {
//							log.info("Problem sending FORBIDDEN error: " + e +
//											", packet: " + pc.toString());
//						}
//					}
//				} else {
//					Packet result = pc.commandResult(Command.DataType.result);
//					Command.addTextField(result, "Note", "Command canceled.");
//					fastAddOutPacket(result);
//
//				}
//				processing_result = true;
//			} else {
//				log.info("Other command found: " + pc.getStrCommand());
//			}
//			break;
		default:
			break;
		} // end of switch (pc.getCommand())
		return processing_result;
	}

	public void initBindings(Bindings binds) {
		super.initBindings(binds);
		binds.put(CommandIfc.AUTH_REPO, auth_repository);
		binds.put(CommandIfc.USER_CONN, connectionsByFrom);
		binds.put(CommandIfc.USER_REPO, user_repository);
		binds.put(CommandIfc.USER_SESS, sessionsByNodeId);
	}

	@SuppressWarnings("unchecked")
	protected void sendAllOnHold(XMPPResourceConnection conn) {
		String remote_smId = (String)conn.getSessionData("redirect-to");
		LinkedList<Packet> packets =
      (LinkedList<Packet>)conn.getSessionData(SESSION_PACKETS);
		if (remote_smId == null) {
			if (log.isLoggable(Level.FINEST)) {
				log.finest("No address for remote SM to redirect packets, processing locally.");
			}
			if (packets != null) {
				Packet sess_pack = null;
				while (((sess_pack = packets.poll()) != null) &&
								// Temporarily fix, need a better solution. For some reason
								// the mode has been sent back from normal to on_hold during
								// loop execution leading to infinite loop.
								// Possibly buggy client sent a second authentication packet
								// executing a second handleLogin call....
								(conn.getConnectionStatus() != ConnectionStatus.ON_HOLD)) {
					processPacket(sess_pack);
				}
			}
			return;
		}
		conn.setConnectionStatus(ConnectionStatus.REDIRECT);
		if (packets != null) {
			Packet sess_pack = null;
			while ((sess_pack = packets.poll()) != null) {
				sess_pack.setTo(remote_smId);
				fastAddOutPacket(sess_pack);
			}
		}
	}

	protected void closeConnection(String connectionId, boolean closeOnly) {
		if (log.isLoggable(Level.FINER)) {
			log.finer("Stream closed from: " + connectionId);
		}
		XMPPResourceConnection connection = connectionsByFrom.remove(connectionId);
		if (connection != null) {
			closeSession(connection, closeOnly);
		} else {
			log.fine("Can not find resource connection for packet: " + connectionId);
		} // end of if (conn != null) else
	}

	protected void closeSession(XMPPResourceConnection conn, boolean closeOnly) {
		if (!closeOnly) {
			Queue<Packet> results = new LinkedList<Packet>();
			for (XMPPStopListenerIfc stopProc: stopListeners.values()) {
				stopProc.stopped(conn, results, plugin_config.get(stopProc.id()));
			} // end of for ()
//			// TESTING ONLY, to be removed
//			try {
//				String nick = conn.getUserName();
//				String nick_num = nick.substring("load-tester_".length());
//				int num = Integer.parseInt(nick_num);
//				if (num < 11) {
//					log.warning("Disconnected: " + nick +
//									", connId: " + conn.getConnectionId() +
//									", sending: " + results.toString());
//				}
//			} catch (Exception e) {
//				log.log(Level.WARNING, "some problem: " + conn.getConnectionId(), e);
//			}
//			// TESTING ONLY, to be removed
			addOutPackets(null, conn, results);
		}
		try {
			if (conn.isAuthorized()
				|| (conn.getConnectionStatus() == ConnectionStatus.TEMP)) {
				String userId = conn.getUserId();
				if (log.isLoggable(Level.FINE)) {
					log.fine("Closing connection for: " + userId);
				}
				XMPPSession session = conn.getParentSession();
				if (session != null) {
					if (log.isLoggable(Level.FINE)) {
						log.fine("Found parent session for: " + userId);
					}
					if (session.getActiveResourcesSize() <= 1) {
						session = sessionsByNodeId.remove(userId);
						if (session == null) {
							log.info("UPS can't remove, session not found in map: " + userId);
						} else {
							if (log.isLoggable(Level.FINER)) {
								log.finer("Number of user sessions: " + sessionsByNodeId.size());
							}
						} // end of else
						if (conn.getConnectionStatus() == ConnectionStatus.NORMAL) {
							auth_repository.logout(userId);
						}
					} else {
						if (log.isLoggable(Level.FINER)) {
							StringBuilder sb = new StringBuilder();
							for (XMPPResourceConnection res_con: session.getActiveResources()) {
								sb.append(", res=" + res_con.getResource() + " ("
									+ res_con.getConnectionStatus() + ")");
							}
							log.finer("Number of connections is "
								+ session.getActiveResourcesSize() + " for the user: " + userId
								+ sb.toString());
						}
					} // end of else
				} // end of if (session.getActiveResourcesSize() == 0)
			}
		} catch (NotAuthorizedException e) {
			log.info("Closed not authorized session: " + e);
		} catch (Exception e) {
			log.log(Level.WARNING, "Exception closing session... ", e);
		}
		++closedConnections;
		conn.streamClosed();
	}

	@Override
	protected boolean addOutPacket(Packet packet) {
		String oldto = packet.getAttribute(Packet.OLDTO);
		if (oldto != null) {
			packet.getElement().setAttribute("from", oldto);
			packet.getElement().removeAttribute(Packet.OLDTO);
		}
		String oldfrom = packet.getAttribute(Packet.OLDFROM);
		if (oldfrom != null) {
			packet.getElement().setAttribute("to", oldfrom);
			packet.getElement().removeAttribute(Packet.OLDFROM);
		}
		return super.addOutPacket(packet);
	}

	protected boolean fastAddOutPacket(Packet packet) {
		return super.addOutPacket(packet);
	}
	
	protected void addOutPackets(Packet packet, XMPPResourceConnection conn,
					Queue<Packet> results) {
		for (XMPPPacketFilterIfc outfilter : outFilters.values()) {
			outfilter.filter(packet, conn, naUserRepository, results);
		} // end of for (XMPPPostprocessorIfc postproc: postProcessors)
		addOutPackets(results);
	}

	private List<Element> getFeatures(XMPPResourceConnection session) {
		List<Element> results = new LinkedList<Element>();
		for (ProcessingThreads<ProcessorWorkerThread> proc_t: processors.values()) {
			Element[] features = proc_t.getWorkerThread().processor.supStreamFeatures(session);
			if (features != null) {
				results.addAll(Arrays.asList(features));
			} // end of if (features != null)
		} // end of for ()
		return results;
	}

	@Override
	public Map<String, Object> getDefaults(Map<String, Object> params) {
		Map<String, Object> props = super.getDefaults(params);
		SessionManagerConfig.getDefaults(props, params);
		return props;
	}

	private void addPlugin(String plug_id, Integer conc) {
		XMPPProcessorIfc proc = ProcessorFactory.getProcessor(plug_id);
		int concurrency = (conc != null ? conc 
						: (proc != null ? proc.concurrentQueuesNo() : 0));
		System.out.println("Loading plugin: " + plug_id + "=" + concurrency + " ...");
		boolean loaded = false;
		if (proc != null) {
			ProcessorWorkerThread worker = new ProcessorWorkerThread(proc);
			ProcessingThreads<ProcessorWorkerThread> pt =
							new ProcessingThreads<ProcessorWorkerThread>(worker,
							concurrency, proc.concurrentThreadsPerQueue(),
							maxQueueSize, proc.id());
			processors.put(plug_id, pt);
			log.config("Added processor: " + proc.getClass().getSimpleName()
				+ " for plugin id: " + plug_id);
			loaded = true;
		}
		XMPPPreprocessorIfc preproc = ProcessorFactory.getPreprocessor(plug_id);
		if (preproc != null) {
			preProcessors.put(plug_id, preproc);
			log.config("Added preprocessor: " + preproc.getClass().getSimpleName()
				+ " for plugin id: " + plug_id);
			loaded = true;
		}
		XMPPPostprocessorIfc postproc = ProcessorFactory.getPostprocessor(plug_id);
		if (postproc != null) {
			postProcessors.put(plug_id, postproc);
			log.config("Added postprocessor: " + postproc.getClass().getSimpleName()
				+ " for plugin id: " + plug_id);
			loaded = true;
		}
		XMPPStopListenerIfc stoplist = ProcessorFactory.getStopListener(plug_id);
		if (stoplist != null) {
			stopListeners.put(plug_id, stoplist);
			log.config("Added stopped processor: " + stoplist.getClass().getSimpleName()
				+ " for plugin id: " + plug_id);
			loaded = true;
		}
		XMPPPacketFilterIfc filterproc = ProcessorFactory.getPacketFilter(plug_id);
		if (filterproc != null) {
			outFilters.put(plug_id, filterproc);
			log.config("Added packet filter: " + filterproc.getClass().getSimpleName()
				+ " for plugin id: " + plug_id);
			loaded = true;
		}
		if (!loaded) {
			log.warning("No implementation found for plugin id: " + plug_id);
		} // end of if (!loaded)
	}

	@Override
	public void setProperties(Map<String, Object> props) {
		super.setProperties(props);

		Security.insertProviderAt(new TigaseSaslProvider(), 6);

		skipPrivacy = (Boolean)props.get(SKIP_PRIVACY_PROP_KEY);

		filter = new PacketFilter();
		// Is there a shared user repository pool? If so I want to use it:
		user_repository = (UserRepository) props.get(SHARED_USER_REPO_POOL_PROP_KEY);
		if (user_repository == null) {
			// Is there shared user repository instance? If so I want to use it:
			user_repository = (UserRepository) props.get(SHARED_USER_REPO_PROP_KEY);
		} else {
			log.config("Using shared repository pool.");
		}
		if (user_repository != null) {
			log.config("Using shared repository instance: " +
							user_repository.getClass().getName());
		} else {
			Map<String, String> user_repo_params = new LinkedHashMap<String, String>();
			for (Map.Entry<String, Object> entry : props.entrySet()) {
				if (entry.getKey().startsWith(USER_REPO_PARAMS_NODE)) {
					// Split the key to configuration nodes separated with '/'
					String[] nodes = entry.getKey().split("/");
					// The plugin ID part may contain many IDs separated with comma ','
					if (nodes.length > 1) {
						user_repo_params.put(nodes[1], entry.getValue().toString());
					}
				}
			}
			try {
				String cls_name = (String) props.get(USER_REPO_CLASS_PROP_KEY);
				String res_uri = (String) props.get(USER_REPO_URL_PROP_KEY);
				user_repository = RepositoryFactory.getUserRepository(getName(),
								cls_name, res_uri, user_repo_params);
				log.config("Initialized " + cls_name + " as user repository: " + res_uri);
			} catch (Exception e) {
				log.log(Level.SEVERE, "Can't initialize user repository: ", e);
			} // end of try-catch
		}
		auth_repository = (UserAuthRepository) props.get(SHARED_AUTH_REPO_PROP_KEY);
		if (auth_repository != null) {
			log.config("Using shared auth repository instance: " +
							auth_repository.getClass().getName());
		} else {
			Map<String, String> auth_repo_params = new LinkedHashMap<String, String>();
			for (Map.Entry<String, Object> entry : props.entrySet()) {
				if (entry.getKey().startsWith(AUTH_REPO_PARAMS_NODE)) {
					// Split the key to configuration nodes separated with '/'
					String[] nodes = entry.getKey().split("/");
					// The plugin ID part may contain many IDs separated with comma ','
					if (nodes.length > 1) {
						auth_repo_params.put(nodes[1], entry.getValue().toString());
					}
				}
			}
			try {
				String cls_name = (String) props.get(AUTH_REPO_CLASS_PROP_KEY);
				String res_uri = (String) props.get(AUTH_REPO_URL_PROP_KEY);
				auth_repository = RepositoryFactory.getAuthRepository(getName(),
								cls_name, res_uri, auth_repo_params);
				log.config("Initialized " + cls_name + " as auth repository: " + res_uri);
			} catch (Exception e) {
				log.log(Level.SEVERE, "Can't initialize auth repository: ", e);
			} // end of try-catch
		}
//		try {
//			// Add component ID to database if it is not there yet.
//			auth_repository.addUser(getComponentId(),
//							UUID.randomUUID().toString());
//		} catch (UserExistsException e) {
//			// Just ignore....
//		} catch (TigaseDBException ex) {
//			log.log(Level.WARNING, "Problem accessing auth repository: ", ex);
//		}

		naUserRepository = new NARepository(user_repository);

		LinkedHashMap<String, Integer> plugins_concurrency =
						new LinkedHashMap<String, Integer>();
		String[] plugins_conc =
						((String)props.get(PLUGINS_CONCURRENCY_PROP_KEY)).split(",");
		log.config("Loading concurrency plugins list: " + Arrays.toString(plugins_conc));
		if (plugins_conc != null && plugins_conc.length > 0) {
			for (String plugc : plugins_conc) {
				log.config("Loading: " + plugc);
				if (!plugc.trim().isEmpty()) {
					String[] pc = plugc.split("=");
					try {
						int conc = Integer.parseInt(pc[1]);
						plugins_concurrency.put(pc[0], conc);
						log.config("Concurrency for plugin: " + pc[0] + " set to: " + conc);
					} catch (Exception e) {
						log.log(Level.WARNING, "Plugin concurrency parsing error for: " +
										plugc + ", ", e);
					}
				}
			}
		}

		String[] plugins = (String[])props.get(PLUGINS_PROP_KEY);
		log.config("Loaded plugins list: " + Arrays.toString(plugins));
		maxPluginsNo = plugins.length;
		processors.clear();
		for (String plug_id: plugins) {
			if (plug_id.equals("presence")) {
				log.warning("Your configuration is outdated!"
					+ " Note 'presence' and 'jaber:iq:roster' plugins are no longer exist."
					+ " Use 'roster-presence' plugin instead, loading automaticly...");
				plug_id = "roster-presence";
			}
			log.config("Loading and configuring plugin: " + plug_id);
			addPlugin(plug_id, plugins_concurrency.get(plug_id));
			Map<String, Object> plugin_settings =
				new ConcurrentSkipListMap<String, Object>();
			for (Map.Entry<String, Object> entry: props.entrySet()) {
				if (entry.getKey().startsWith(PLUGINS_CONF_PROP_KEY)) {
					// Split the key to configuration nodes separated with '/'
					String[] nodes = entry.getKey().split("/");
					// The plugin ID part may contain many IDs separated with comma ','
					if (nodes.length > 2) {
						String[] ids = nodes[1].split(",");
						Arrays.sort(ids);
						if (Arrays.binarySearch(ids, plug_id) >= 0) {
							plugin_settings.put(nodes[2], entry.getValue());
						}
					}
				}
			}
			if (plugin_settings.size() > 0) {
				if (log.isLoggable(Level.CONFIG)) {
					log.config("Plugin configuration: " + plugin_settings.toString());
				}
				plugin_config.put(plug_id, plugin_settings);
			}
		} // end of for (String comp_id: plugins)
		registerNewSession(getComponentId(), createUserSession(NULL_ROUTING, getDefHostName()));
//		loginUserSession(NULL_ROUTING, getDefHostName(),
//						getComponentId(), null, ConnectionStatus.NORMAL, getComponentId());
//		String[] admins_tmp = (String[])props.get(ADMINS_PROP_KEY);
//		if (admins_tmp != null) {
//			for (String admin : admins_tmp) {
//				admins.add(admin);
//			}
//		}
		String[] trusted_tmp = (String[])props.get(TRUSTED_PROP_KEY);
		if (trusted_tmp != null) {
			for (String trust : trusted_tmp) {
				trusted.add(trust);
			}
		}
		// Loading admin scripts....
//		String descrStr = "AS:Description: ";
//		String cmdIdStr = "AS:CommandId: ";
//		String scriptsPath = (String) props.get(ADMIN_SCRIPTS_PROP_KEY);
//		File file = null;
//		AddScriptCommand addCommand = new AddScriptCommand();
//		Bindings binds = scriptEngineManager.getBindings();
//		initBindings(binds);
//		try {
//			File adminDir = new File(scriptsPath);
//			if (adminDir != null && adminDir.exists()) {
//				for (File f : adminDir.listFiles()) {
//					String cmdId = null;
//					String cmdDescr = null;
//					file = f;
//					StringBuilder sb = new StringBuilder();
//					BufferedReader buffr = new BufferedReader(new FileReader(file));
//					String line = null;
//					while ((line = buffr.readLine()) != null) {
//						sb.append(line + "\n");
//						int idx = line.indexOf(descrStr);
//						if (idx >= 0) {
//							cmdDescr = line.substring(idx + descrStr.length());
//						}
//						idx = line.indexOf(cmdIdStr);
//						if (idx >= 0) {
//							cmdId = line.substring(idx + cmdIdStr.length());
//						}
//					}
//					buffr.close();
//					if (cmdId == null || cmdDescr == null) {
//						log.warning("Admin script found but it has no command ID or command description: " +
//										file);
//						continue;
//					}
//					int idx = file.toString().lastIndexOf(".");
//					String ext = file.toString().substring(idx + 1);
//					addCommand.addAdminScript(cmdId, cmdDescr, sb.toString(), null,
//									ext, binds);
//					log.config("Loaded admin command from file: " + file +
//									", id: " + cmdId + ", ext: " + ext + ", descr: " + cmdDescr);
//				}
//			} else {
//				log.warning("Admin scripts directory is missing: " + adminDir);
//			}
//		} catch (Exception e) {
//			log.log(Level.WARNING, "Can't load the admin script file: " + file, e);
//		}
	}

	@Override
	public boolean handlesLocalDomains() {
		return true;
	}

	protected XMPPResourceConnection createUserSession(String conn_id,
		String domain) {
		XMPPResourceConnection connection = new XMPPResourceConnection(conn_id,
			user_repository, auth_repository, this);
		VHostItem vitem = null;
		if (domain != null) {
			if (log.isLoggable(Level.FINEST)) {
				log.finest("Setting hostname " + domain + " for connection: " + conn_id);
			}
			vitem = getVHostItem(domain);
		}
		if (vitem == null) {
			// This shouldn't generally happen. Must mean misconfiguration.
			if (log.isLoggable(Level.INFO)) {
				log.info("Can't get VHostItem for domain: " + domain +
								", using default one instead: " + getDefHostName());
			}
			vitem = new VHostItem(getDefHostName());
		}
		connection.setDomain(vitem.getUnmodifiableVHostItem());
		if (log.isLoggable(Level.FINEST)) {
			log.finest("Domain set for connectionId " + conn_id);
		}
		//connection.setAnonymousPeers(anon_peers);
		connectionsByFrom.put(conn_id, connection);
		int currSize = connectionsByFrom.size();
		if (currSize > maxUserConnections) {
			maxUserConnections = currSize;
		}
		++totalUserConnections;
		return connection;
	}

	protected XMPPResourceConnection loginUserSession(String conn_id,
					String domain, String user_id, String resource,
					ConnectionStatus conn_st, String xmpp_sessionId) {
		try {
			XMPPResourceConnection conn = createUserSession(conn_id, domain);
			conn.setConnectionStatus(conn_st);
			conn.setSessionId(xmpp_sessionId);
			user_repository.setData(user_id, "tokens", xmpp_sessionId, conn_id);
			Authorization auth = conn.loginToken(user_id, xmpp_sessionId, conn_id);
			if (auth == Authorization.AUTHORIZED) {
				handleLogin(JIDUtils.getNodeNick(user_id), conn);
				//registerNewSession(JIDUtils.getNodeID(user_id), conn);
				if (resource != null) {
					conn.setResource(resource);
				}
			} else {
				connectionsByFrom.remove(conn_id);
				return null;
			}
			return conn;
		} catch (Exception ex) {
			log.log(Level.WARNING,
							"Problem logging user: " + user_id + "/" + resource, ex);
		}
		return null;
	}

	protected void registerNewSession(String userId, XMPPResourceConnection conn) {
		XMPPSession session = sessionsByNodeId.get(userId);
		if (session == null) {
			session = new XMPPSession(JIDUtils.getNodeNick(userId));
			sessionsByNodeId.put(userId, session);
			int currSize = sessionsByNodeId.size();
			if (currSize > maxUserSessions) {
				maxUserSessions = currSize;
			}
			++totalUserSessions;
			if (log.isLoggable(Level.FINEST)) {
				log.finest("Created new XMPPSession for: " + userId);
			}
		} else {
			// Check all other connections whether they are still alive....
			List<XMPPResourceConnection> connections = session.getActiveResources();
			if (connections != null) {
				for (XMPPResourceConnection connection : connections) {
					if (connection != conn) {
						addOutPacketWithTimeout(Command.CHECK_USER_CONNECTION.getPacket(
										getComponentId(), connection.getConnectionId(),
										StanzaType.get, UUID.randomUUID().toString()),
										connectionCheckCommandHandler, 30l, TimeUnit.SECONDS);
					}
				}
			}
		}
		session.addResourceConnection(conn);
	}

	@Override
	public void handleLogin(String userName, XMPPResourceConnection conn) {
		if (log.isLoggable(Level.FINEST)) {
			log.finest("handleLogin called for: " + userName + ", conn_id: " +
							conn.getConnectionId());
		}
		String userId = JIDUtils.getNodeID(userName, conn.getDomain());
		registerNewSession(userId, conn);
		if (conn.getConnectionStatus() != ConnectionStatus.REMOTE) {
			conn.setConnectionStatus(ConnectionStatus.NORMAL);
		}
	}

	@Override
	public void handleResourceBind(XMPPResourceConnection conn) {

	}

	@Override
	public void handlePresenceSet(XMPPResourceConnection conn) {

	}

	@Override
	public void handleLogout(String userName, XMPPResourceConnection conn) {
		String domain = conn.getDomain();
		String userId = JIDUtils.getNodeID(userName, domain);
		XMPPSession session = sessionsByNodeId.get(userId);
		if (session != null && session.getActiveResourcesSize() <= 1) {
			sessionsByNodeId.remove(userId);
		} // end of if (session.getActiveResourcesSize() == 0)
		connectionsByFrom.remove(conn.getConnectionId());
		fastAddOutPacket(Command.CLOSE.getPacket(getComponentId(),
				conn.getConnectionId(), StanzaType.set, conn.nextStanzaId()));
	}

	@Override
	public Element getDiscoInfo(String node, String jid, String from) {
		if (jid != null && (getName().equals(JIDUtils.getNodeNick(jid)) ||
						isLocalDomain(jid))) {
			Element query = super.getDiscoInfo(node, jid, from);
			if (query == null) {
				query = new Element("query");
				query.setXMLNS(XMPPService.INFO_XMLNS);
			}
			if (node == null) {
				for (ProcessingThreads<ProcessorWorkerThread> proc_t : processors.values()) {
					Element[] discoFeatures = proc_t.getWorkerThread().processor.supDiscoFeatures(null);
					if (discoFeatures != null) {
						query.addChildren(Arrays.asList(discoFeatures));
					} // end of if (discoFeatures != null)
				}
			}
			if (log.isLoggable(Level.FINEST)) {
				log.finest("Found disco info: " +
						(query != null ? query.toString() : null));
			}
			return query;
		}
		if (log.isLoggable(Level.FINEST)) {
			log.finest("Not found disco info for node: " + node + ", jid: " + jid);
		}
		return null;
	}

	@Override
	public List<Element> getDiscoFeatures(String from) {
		List<Element> features = new LinkedList<Element>();
		List<Element> tmp = super.getDiscoFeatures(from);
		if (tmp != null) {
			features.addAll(tmp);
		}
		for (ProcessingThreads<ProcessorWorkerThread> proc_t: processors.values()) {
			Element[] discoFeatures = proc_t.getWorkerThread().processor.supDiscoFeatures(null);
			if (discoFeatures != null) {
				features.addAll(Arrays.asList(discoFeatures));
			} // end of if (discoFeatures != null)
		}
		return features;
	}

	@Override
	public String getDiscoDescription() {
		return "Session manager";
	}

	@Override
	public String getDiscoCategory() {
		return "sm";
	}

//	@Override
//	public List<Element> getDiscoItems(String node, String jid) {
//		List<Element> result = serviceEntity.getDiscoItems(node, jid);
//		if (log.isLoggable(Level.FINEST)) {
//			log.finest("Found disco items: " +
//					(result != null ? result.toString() : null));
//		}
//		return result;
//	}

	@Override
	public void getStatistics(StatisticsList list) {
		super.getStatistics(list);
		if (list.checkLevel(Level.FINEST)) {
			list.add(getName(), "Registered accounts",
							user_repository.getUsersCount(), Level.FINEST);
		}
		list.add(getName(), "Open user connections",
				connectionsByFrom.size(), Level.INFO);
		list.add(getName(), "Maximum user connections",
				maxUserConnections, Level.INFO);
		list.add(getName(), "Total user connections",
				totalUserConnections, Level.FINER);
		list.add(getName(), "Closed user connections",
						closedConnections, Level.FINER);
		list.add(getName(), "Open user sessions",
				sessionsByNodeId.size(), Level.FINE);
		list.add(getName(), "Maximum user sessions",
				maxUserSessions, Level.FINE);
		list.add(getName(), "Total user sessions",
				totalUserSessions, Level.FINER);
		list.add(getName(), "Authentication timouts",
						authTimeouts, Level.INFO);
		for (Map.Entry<String, ProcessingThreads<ProcessorWorkerThread>> procent : processors.entrySet()) {
			ProcessingThreads<ProcessorWorkerThread> proc = procent.getValue();
			if (list.checkLevel(Level.INFO, proc.getTotalQueueSize() + proc.getDroppedPackets())) {
				list.add(getName(), "Processor: " + procent.getKey(),
								"Queue: " + proc.getTotalQueueSize() +
								", AvTime: " + proc.getAverageProcessingTime() +
								", Runs: " + proc.getTotalRuns() + ", Lost: " + proc.getDroppedPackets(),
								Level.INFO);
			}
		}
		if (list.checkLevel(Level.INFO,
						sessionCloseThread.getTotalQueueSize() + sessionCloseThread.getDroppedPackets())) {
			list.add(getName(), "Processor: " +
							sessionCloseThread.getName(),
							"Queue: " + sessionCloseThread.getTotalQueueSize() +
							", AvTime: " + sessionCloseThread.getAverageProcessingTime() +
							", Runs: " + sessionCloseThread.getTotalRuns() + ", Lost: " +
							sessionCloseThread.getDroppedPackets(),
							Level.INFO);
		}
		if (list.checkLevel(Level.INFO,
						sessionOpenThread.getTotalQueueSize() + sessionOpenThread.getDroppedPackets())) {
			list.add(getName(), "Processor: " +
							sessionOpenThread.getName(),
							"Queue: " + sessionOpenThread.getTotalQueueSize() +
							", AvTime: " + sessionOpenThread.getAverageProcessingTime() +
							", Runs: " + sessionOpenThread.getTotalRuns() + ", Lost: " +
							sessionOpenThread.getDroppedPackets(),
							Level.INFO);
		}
	}

	@Override
	public boolean isLocalDomain(String domain, boolean includeComponents) {
		if (includeComponents) {
			return isLocalDomainOrComponent(domain);
		} else {
			return isLocalDomain(domain);
		}
	}

//	@Override
//	public Set<String> getOnlineJids() {
//		return sessionsByNodeId.keySet();
//	}

	@Override
	public boolean hasCompleteJidsInfo() {
		return true;
	}

	@Override
	public boolean containsJid(String jid) {
		return sessionsByNodeId.containsKey(jid);
	}

	public boolean skipPrivacy() {
		return skipPrivacy;
	}

	@Override
	public String[] getConnectionIdsForJid(String jid) {
		if (skipPrivacy()) {
			XMPPSession session = sessionsByNodeId.get(jid);
			if (session != null) {
				return session.getConnectionIds();
			}
		}
		return null;
	}

	private class SessionOpenWorkerThread extends WorkerThread {

		private SessionManager sm = null;

		public SessionOpenWorkerThread(SessionManager sm) {
			this.sm = sm;
		}

		@Override
		public WorkerThread getNewInstance(PriorityQueue<QueueItem> queue) {
			SessionOpenWorkerThread worker = new SessionOpenWorkerThread(sm);
			worker.setQueue(queue);
			return worker;
		}

		@Override
		public void process(QueueItem item) {
			// It might be existing opened stream after TLS/SASL authorization
			// If not, it means this is new stream
			if (item.conn == null) {
				if (log.isLoggable(Level.FINER)) {
					log.finer("Adding resource connection for: " + item.packet.getFrom());
				}
				final String hostname = Command.getFieldValue(item.packet, "hostname");
				item.conn = createUserSession(item.packet.getFrom(), hostname);
				authenticationWatchdog.schedule(new AuthenticationTimer(item.packet.getFrom()),
								2*MINUTE);
			} else {
				if (log.isLoggable(Level.FINEST)) {
					log.finest("Stream opened for existing session, authorized: "
						+ item.conn.isAuthorized());
				}
			} // end of else
			item.conn.setSessionId(Command.getFieldValue(item.packet, "session-id"));
			item.conn.setDefLang(Command.getFieldValue(item.packet, "xml:lang"));
			if (log.isLoggable(Level.FINEST)) {
				log.finest("Setting session-id " + item.conn.getSessionId()
					+ " for connection: " + item.conn.getConnectionId());
			}
			fastAddOutPacket(item.packet.okResult((String) null, 0));
		}
		
	}

	private class SessionCloseWorkerThread extends WorkerThread {

		@Override
		public WorkerThread getNewInstance(PriorityQueue<QueueItem> queue) {
			SessionCloseWorkerThread worker = new SessionCloseWorkerThread();
			worker.setQueue(queue);
			return worker;
		}

		@Override
		public void process(QueueItem item) {
			closeConnection(item.packet.getFrom(), false);
		}

	}

	private class ProcessorWorkerThread extends WorkerThread {

		private XMPPProcessorIfc processor = null;
		private LinkedList<Packet> local_results = new LinkedList<Packet>();

		public ProcessorWorkerThread(XMPPProcessorIfc processor) {
			this.processor = processor;
		}

		@Override
		public WorkerThread getNewInstance(PriorityQueue<QueueItem> queue) {
			ProcessorWorkerThread worker = new ProcessorWorkerThread(processor);
			worker.setQueue(queue);
			return worker;
		}

		@Override
		public void process(QueueItem item) {
			try {
				if (item.conn != null) {
					processor.process(item.packet, item.conn, naUserRepository,
									local_results, plugin_config.get(processor.id()));
					setPermissions(item.conn, local_results);
				} else {
					processor.process(item.packet, null, naUserRepository,
									local_results, plugin_config.get(processor.id()));
				}
				addOutPackets(item.packet, item.conn, local_results);
			} catch (PacketErrorTypeException e) {
				log.info("Already error packet, ignoring: " + item.packet.toString());
			} catch (XMPPException e) {
				log.log(Level.WARNING, "Exception during packet processing: " +
								item.packet.toString(), e);
			}
		}
		
	}

	private class AuthenticationTimer extends TimerTask {

		private String connId = null;

		private AuthenticationTimer(String connId) {
			this.connId = connId;
		}

		@Override
		public void run() {
			XMPPResourceConnection conn = connectionsByFrom.get(connId);
			if (conn != null && !conn.isAuthorized()) {
				connectionsByFrom.remove(connId);
				++authTimeouts;
				log.info("Authentication timeout expired, closing connection: " + connId);
				fastAddOutPacket(Command.CLOSE.getPacket(getComponentId(),
								connId, StanzaType.set, conn.nextStanzaId()));
			}
		}

	}

	private class ConnectionCheckCommandHandler implements ReceiverEventHandler {

		@Override
		public void timeOutExpired(Packet packet) {
   			if (log.isLoggable(Level.FINER)) {
    			log.finer("Connection checker timeout expired, closing connection: " +
							packet.getTo());
            }
			closeConnection(packet.getTo(), false);
		}

		@Override
		public void responseReceived(Packet packet, Packet response) {
			if (response.getType() == StanzaType.error) {
    			if (log.isLoggable(Level.FINER)) {
    				log.finer("Connection checker error received, closing connection: " +
							packet.getTo());
                }
				// The connection is not longer active, closing the user session here.
				closeConnection(packet.getTo(), false);
			}
		}

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

		@Override
		public String getPublicData(String user, String subnode, String key,
			String def)	throws UserNotFoundException {
			try {
				return (rep.userExists(user) ?
          rep.getData(user, calcNode(PUBLIC_DATA_NODE, subnode), key, def) :
					null);
			}	catch (TigaseDBException e) {
				log.log(Level.SEVERE, "Problem accessing repository data.", e);
				return null;
			} // end of try-catch
		}

		@Override
		public String[] getPublicDataList(String user, String subnode, String key)
			throws UserNotFoundException {
			try {
				return (rep.userExists(user) ?
					rep.getDataList(user, calcNode(PUBLIC_DATA_NODE, subnode), key) :
					null);
			}	catch (TigaseDBException e) {
				log.log(Level.SEVERE, "Problem accessing repository data.", e);
				return null;
			} // end of try-catch
		}

		@Override
		public void addOfflineDataList(String user, String subnode, String key,
			String[] list) throws UserNotFoundException {
			try {
				if (rep.userExists(user)) {
					rep.addDataList(user, calcNode(OFFLINE_DATA_NODE, subnode), key, list);
				} else {
					throw new UserNotFoundException("User: " + user
						+ " has not been found inthe repository.");
				}
			} catch (UserNotFoundException e) {
				// This is quite normal for anonymous users.
				log.info("User not found in repository: " + user);
			}	catch (TigaseDBException e) {
				log.log(Level.SEVERE, "Problem accessing repository data.", e);
			} // end of try-catch
		}

		@Override
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
