/*
 * ComponentProtocol.java
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2013 "Tigase, Inc." <office@tigase.com>
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



package tigase.server.ext;

import tigase.conf.ConfigurationException;
import tigase.db.comp.ComponentRepository;
import tigase.net.ConnectionType;
import tigase.server.ConnectionManager;
import tigase.server.Packet;
import tigase.server.ext.handlers.*;
import tigase.server.ext.lb.LoadBalancerIfc;
import tigase.stats.StatisticsList;
import tigase.util.TigaseStringprepException;
import tigase.xml.Element;
import tigase.xmpp.Authorization;
import tigase.xmpp.PacketErrorTypeException;

import javax.script.Bindings;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created: Sep 30, 2009 8:28:13 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class ComponentProtocol
				extends ConnectionManager<ComponentIOService>
				implements ComponentProtocolHandler {
	public static final String AUTHENTICATION_TIMEOUT_PROP_KEY = "auth-timeout";

	public static final String CLOSE_ON_SEQUENCE_ERROR_PROP_KEY = "close-on-seq-error";

	public static final String EXTCOMP_BIND_HOSTNAMES = "--bind-ext-hostnames";

	public static final String EXTCOMP_REPO_CLASS_PROP_KEY = "repository-class";

	public static final String EXTCOMP_REPO_CLASS_PROP_VAL =
			"tigase.server.ext.CompDBRepository";

	public static final String EXTCOMP_REPO_CLASS_PROPERTY = "--extcomp-repo-class";

	public static final String IDENTITY_TYPE_KEY = "identity-type";

	public static final String IDENTITY_TYPE_VAL = "generic";

	public static final String MAX_AUTH_ATTEMPTS_PROP_KEY = "max-auth-attempts";

	public static final String PACK_ROUTED_KEY = "pack-routed";

	public static final String RETURN_SERVICE_DISCO_KEY = "service-disco";

	/**
	 * Variable <code>log</code> is a class logger.
	 */
	private static final Logger log = Logger.getLogger(ComponentProtocol.class.getName());

	public static final boolean RETURN_SERVICE_DISCO_VAL = true;

	public boolean PACK_ROUTED_VAL = false;

	// In seconds
	private long authenticationTimeOut = 15;

	/**
	 * A map keeping all active connections by a connection JID or domain name.
	 * Since for each domain we can have 1..N connections the Map value is a List
	 * of connections.
	 */
	private Map<String, CopyOnWriteArrayList<ComponentConnection>> connections =
			new ConcurrentHashMap<String, CopyOnWriteArrayList<ComponentConnection>>();
	private String[]                          hostnamesToBind           = null;
	private int                               maxAuthenticationAttempts = 1;
	private ComponentRepository<CompRepoItem> repo                      = null;
	private Map<String, StreamOpenHandler>    streamOpenHandlers =
			new LinkedHashMap<String, StreamOpenHandler>();

	/**
	 * List of processors which should handle all traffic incoming from the
	 * network. In most cases if not all, these processors handle just protocol
	 * traffic, all the rest traffic should be passed on to MR.
	 */
	private Map<String, ExtProcessor> processors = new LinkedHashMap<String, ExtProcessor>(
			10);
	private UnknownXMLNSStreamOpenHandler unknownXMLNSHandler =
			new UnknownXMLNSStreamOpenHandler();
	private String  identity_type = IDENTITY_TYPE_VAL;
	private boolean experimental  = false;

	// private ServiceEntity serviceEntity = null;
	private boolean closeOnSequenceError = true;

	/**
	 * Constructs ...
	 *
	 */
	public ComponentProtocol() {
		super();

		StreamOpenHandler handler = new JabberClientStreamOpenHandler();

		if (handler.getXMLNSs() != null) {
			for (String xmlns : handler.getXMLNSs()) {
				streamOpenHandlers.put(xmlns, handler);
			}
		}
		handler = new ComponentAcceptStreamOpenHandler();
		if (handler.getXMLNSs() != null) {
			for (String xmlns : handler.getXMLNSs()) {
				streamOpenHandlers.put(xmlns, handler);
			}
		}
		handler = new ComponentConnectStreamOpenHandler();
		if (handler.getXMLNSs() != null) {
			for (String xmlns : handler.getXMLNSs()) {
				streamOpenHandlers.put(xmlns, handler);
			}
		}
	}

	@Override
	public void authenticated(ComponentIOService serv) {
		serv.setAuthenticated(true);

		String hostname = (String) serv.getSessionData().get(ComponentIOService.HOSTNAME_KEY);

		bindHostname(hostname, serv);
		if (hostnamesToBind != null) {
			serv.getSessionData().put(EXTCOMP_BIND_HOSTNAMES_PROP_KEY, hostnamesToBind);

			ExtProcessor proc = getProcessor("bind");

			if (proc != null) {
				Queue<Packet> results = new ArrayDeque<Packet>();

				proc.startProcessing(null, serv, this, results);
				writePacketsToSocket(serv, results);
			}
		}
	}

	@Override
	public void authenticationFailed(ComponentIOService serv, Packet packet) {
		writePacketToSocket(serv, packet);

		Integer fails = (Integer) serv.getSessionData().get("auth-fails");

		if (fails == null) {
			fails = 1;
		} else {
			fails += 1;
		}
		if (fails >= maxAuthenticationAttempts) {
			serv.stop();
		}
	}

	@Override
	protected String getDefTrafficThrottling() {
		return "xmpp:25m:0:disc,bin:20000m:0:disc";
	}

	@Override
	public void bindHostname(String hostname, ComponentIOService serv) {
		String[] routings = new String[] { hostname, ".*@" + hostname, ".*\\." + hostname };

		if (serv.connectionType() == ConnectionType.connect) {

			// Most likely we have an external component here which doesn't have any
			// connections managers. In such a case the best routings settings would
			// be: .*
			routings = new String[] { ".*" };
		}
		serv.setRoutings(routings[0]);
		updateRoutings(routings, true);
		if (log.isLoggable(Level.FINE)) {
			log.fine("Authenticated: " + hostname);
		}
		updateServiceDiscoveryItem(hostname, null, "ext-comp connected", false);

		// Now kind of trick to allow access to the external component in a more
		// direct way. However, we have to careful here to avoid disaster.
		if (experimental) {
			updateServiceDiscoForConnection(hostname, serv);
		}
		addComponentConnection(hostname, serv);
		addComponentDomain(hostname);
	}

	private void updateServiceDiscoForConnection(String hostname, ComponentIOService serv) {

		// Cut off the first, component part
		int    idx         = hostname.indexOf(".");
		String newhostname = hostname.substring(idx + 1);

		if (!isLocalDomain(newhostname)) {
			updateServiceDiscoveryItem(newhostname, "ext", serv.getUniqueId(), true);
		} else {

			// We don't do the trick because this would break stuff
		}
	}

	@Override
	public CompRepoItem getCompRepoItem(String hostname) {
		return repo.getItem(hostname);
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public Map<String, Object> getDefaults(Map<String, Object> params) {
		Map<String, Object> defs = super.getDefaults(params);

		experimental = Boolean.parseBoolean((String) params.get("--experimental"));

		String repo_class = (String) params.get(EXTCOMP_REPO_CLASS_PROPERTY);

		if (repo_class == null) {
			repo_class = EXTCOMP_REPO_CLASS_PROP_VAL;
		}
		defs.put(EXTCOMP_REPO_CLASS_PROP_KEY, repo_class);
		try {
			repo = (ComponentRepository<CompRepoItem>) Class.forName(repo_class).newInstance();
			repo.getDefaults(defs, params);
		} catch (Exception e) {
			log.log(Level.SEVERE, "Can not instantiate items repository for class: " +
					repo_class, e);
		}
		defs.put(PACK_ROUTED_KEY, PACK_ROUTED_VAL);
		defs.put(RETURN_SERVICE_DISCO_KEY, RETURN_SERVICE_DISCO_VAL);
		defs.put(IDENTITY_TYPE_KEY, IDENTITY_TYPE_VAL);

		String bind_hostnames = (String) params.get(EXTCOMP_BIND_HOSTNAMES);

		if (bind_hostnames != null) {
			defs.put(EXTCOMP_BIND_HOSTNAMES_PROP_KEY, bind_hostnames.split(","));
		} else {
			defs.put(EXTCOMP_BIND_HOSTNAMES_PROP_KEY, new String[] { "" });
		}
		defs.put(CLOSE_ON_SEQUENCE_ERROR_PROP_KEY, closeOnSequenceError);
		defs.put(MAX_AUTH_ATTEMPTS_PROP_KEY, maxAuthenticationAttempts);
		defs.put(AUTHENTICATION_TIMEOUT_PROP_KEY, authenticationTimeOut);

		return defs;
	}

	@Override
	public String getDiscoCategoryType() {
		return identity_type;
	}

	@Override
	public String getDiscoDescription() {
		return "External component";
	}

	@Override
	public ExtProcessor getProcessor(String key) {
		return processors.get(key);
	}

	@Override
	public void getStatistics(StatisticsList list) {
		super.getStatistics(list);

		// Warning size() for ConcurrentHashMap is very slow
		// unless we have a huge number of domains this should not be a problem
		// though.
		list.add(getName(), "Number of external domains", connections.size(), Level.FINE);

		int size = 0;

		for (CopyOnWriteArrayList<ComponentConnection> conns : connections.values()) {
			size += conns.size();
		}
		list.add(getName(), "Number of external component connections", size, Level.FINER);
	}

	@Override
	public List<Element> getStreamFeatures(ComponentIOService serv) {
		List<Element> results = new LinkedList<Element>();

		for (ExtProcessor proc : processors.values()) {
			List<Element> proc_res = proc.getStreamFeatures(serv, this);

			if (proc_res != null) {
				results.addAll(proc_res);
			}
		}

		return results;
	}

	@Override
	public StreamOpenHandler getStreamOpenHandler(String xmlns) {
		return streamOpenHandlers.get(xmlns);
	}

	@Override
	public void initBindings(Bindings binds) {
		super.initBindings(binds);
		binds.put(ComponentRepository.COMP_REPO_BIND, repo);
	}

	@Override
	public Queue<Packet> processSocketData(ComponentIOService serv) {
		Queue<Packet> packets = serv.getReceivedPackets();
		Packet        p       = null;
		Queue<Packet> results = new ArrayDeque<Packet>(2);

		while ((p = packets.poll()) != null) {
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "Processing socket data: {0}, from socket: {1}", new Object[] { p, serv });
			}

			boolean processed = false;

			for (ExtProcessor proc : processors.values()) {
				processed |= proc.process(p, serv, this, results);
				writePacketsToSocket(serv, results);
			}
			if (!processed) {

				// This might be a bit slow, need to be tested.
				// Possibly a local variable in XMPPIOService might be needed
				// to improve performance
				if (serv.isAuthenticated()) {
					Packet result = p;

					if (p.isRouted()) {
						try {
							result = p.unpackRouted();
						} catch (TigaseStringprepException ex) {
							log.log(Level.WARNING,
									"Packet stringprep addressing problem, dropping packet: {0}", p);

							return null;
						}
					}    // end of if (p.isRouted())
					result.getElement().setXMLNS("jabber:client");
					if (result.getStanzaFrom() != null) {
						serv.addRecentJID(result.getStanzaFrom());
					}
					addOutPacket(result);
				} else {
					try {
						Packet error = Authorization.NOT_AUTHORIZED.getResponseMessage(p,
								"Connection not yet authorized to send this packet.", true);

						writePacketToSocket(serv, error);
					} catch (PacketErrorTypeException ex) {

						// Already error packet, just ignore to prevent infinite loop
						log.log(Level.FINE,
								"Received an error packet from unauthorized connection: {0}", p);
					}
					if (closeOnSequenceError) {
						serv.stop();
					}
				}
			}
		}    // end of while ()

		return null;
	}

	@Override
	public boolean processUndeliveredPacket(Packet packet, Long stamp, String errorMessage) {
		// readd packet - this may be good as we would retry to send packet 
		// which delivery failed due to IO error
		addPacket(packet);
		return true;
	}
	
	@Override
	public void reconnectionFailed(Map<String, Object> port_props) {

		// TODO: handle this somehow
	}

	@Override
	public void serviceStarted(ComponentIOService serv) {
		super.serviceStarted(serv);
		addTimerTask(new AuthenticationTimerTask(serv), authenticationTimeOut, TimeUnit.SECONDS);

		String xmlns = ((CompRepoItem) serv.getSessionData().get(REPO_ITEM_KEY)).getXMLNS();

		if (log.isLoggable(Level.FINEST)) {
			log.finest("Connection started: " + serv.getRemoteAddress() + ", xmlns: " + xmlns +
					", type: " + serv.connectionType().toString() + ", id=" + serv.getUniqueId());
		}

		StreamOpenHandler handler = streamOpenHandlers.get(xmlns);
		String            result  = null;

		if (handler == null) {

			// Well, that's a but, we should not be here...
			log.fine("XMLNS not set, accepting a new connection with xmlns auto-detection.");
		} else {
			if ( log.isLoggable( Level.FINEST ) ){
				log.log( Level.FINEST, "cid: {0}, sending: {1}, sessionData: {2}",
								 new Object[] { serv.getSessionData().get( "cid" ), result, serv.getSessionData() } );
			}
			result = handler.serviceStarted(serv);
		}
		if (result != null) {
			serv.xmppStreamOpen(result);
		}
	}

	@Override
	public boolean serviceStopped(ComponentIOService service) {
		boolean result = super.serviceStopped(service);

		if (result) {
			Map<String, Object> sessionData = service.getSessionData();
			String              hostname = (String) sessionData.get(ComponentIOService
					.HOSTNAME_KEY);

			if ((hostname != null) &&!hostname.isEmpty()) {
				List<ComponentConnection> conns = service.getRefObject();

				if (conns != null) {
					for (ComponentConnection conn : conns) {
						boolean moreConnections = removeComponentConnection(conn.getDomain(), conn);

						if (!moreConnections) {
							removeRoutings(conn.getDomain());
						}
					}
				} else {

					// Nothing to do, let's log this however.
					log.finer(
							"Closing XMPPIOService has not yet set ComponentConnection as RefObject: " +
							hostname + ", id: " + service.getUniqueId());
				}
			} else {

				// Stopped service which hasn't sent initial stream open yet
				log.finer("Stopped service which hasn't sent initial stream open yet" + service
						.getUniqueId());
			}

			ConnectionType type = service.connectionType();

			if (type == ConnectionType.connect) {
				addWaitingTask(sessionData);

				// reconnectService(sessionData, connectionDelay);
			}    // end of if (type == ConnectionType.connect)
		}

		return result;
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public void setProperties(Map<String, Object> properties) throws ConfigurationException {
		if (properties.size() == 1) {

			// If props.size() == 1, it means this is a single property update
			// and this component does not support single property change for the rest
			// of it's settings
			return;
		}
		identity_type = (String) properties.get(IDENTITY_TYPE_KEY);
		super.setProperties(properties);

		String repo_class = (String) properties.get(EXTCOMP_REPO_CLASS_PROP_KEY);

		try {
			ComponentRepository<CompRepoItem> repo_tmp =
					(ComponentRepository<CompRepoItem>) Class.forName(repo_class).newInstance();

			repo_tmp.setProperties(properties);
			ComponentRepository<CompRepoItem> old_repo = repo;
			repo = repo_tmp;
			if (old_repo != null) {
				repo.destroy();
			}
		} catch (Exception e) {
			log.log(Level.SEVERE, "Can not create items repository instance for class: " +
					repo_class, e);
		}

		// Activate all connections for which parameters are defined in the
		// repository
		for (CompRepoItem repoItem : repo) {
			log.log(Level.CONFIG, "Loaded repoItem: {0}", repoItem.toString());
			if (repoItem.getPort() > 0) {
				String[] remote_host   = PORT_IFC_PROP_VAL;
				String   remote_domain = repoItem.getRemoteHost();

				if (repoItem.getRemoteHost() != null) {
					remote_host = repoItem.getRemoteHost().split(";");

					// The first item on the list is always the remote domain name, if
					// there are
					// more entries, the rest is just addresses to connect to for this
					// domain
					remote_domain = remote_host[0];
					if (remote_host.length > 1) {

						// Remove the first entry as this is domain name, whereas the rest
						// is the
						// address to connect to.
						String[] remote_host_copy = new String[remote_host.length - 1];

						System.arraycopy(remote_host, 1, remote_host_copy, 0, remote_host_copy
								.length);
						remote_host = remote_host_copy;
					}
				}
				for (String r_host : remote_host) {
					Map<String, Object> port_props = new LinkedHashMap<String, Object>();

					port_props.put(PORT_KEY, repoItem.getPort());
					if (repoItem.getDomain() != null) {
						port_props.put(PORT_LOCAL_HOST_PROP_KEY, repoItem.getDomain());
					}
					port_props.put(PORT_REMOTE_HOST_PROP_KEY, remote_domain);
					port_props.put(PORT_TYPE_PROP_KEY, repoItem.getConnectionType());
					port_props.put(PORT_SOCKET_PROP_KEY, repoItem.getSocket());
					port_props.put(PORT_IFC_PROP_KEY, new String[] { r_host });
					port_props.put(MAX_RECONNECTS_PROP_KEY, (int) (120 * MINUTE));
					port_props.put(REPO_ITEM_KEY, repoItem);
					log.config("Starting connection: " + port_props);
					addWaitingTask(port_props);
				}
			}
		}
		hostnamesToBind = (String[]) properties.get(EXTCOMP_BIND_HOSTNAMES_PROP_KEY);
		if ((hostnamesToBind.length == 1) && hostnamesToBind[0].isEmpty()) {
			hostnamesToBind = null;
		}
		log.config("Hostnames to bind: " + Arrays.toString(hostnamesToBind));
		processors = new LinkedHashMap<String, ExtProcessor>();

		ExtProcessor proc = new HandshakeProcessor();

		processors.put(proc.getId(), proc);
		proc = new StreamFeaturesProcessor();
		processors.put(proc.getId(), proc);
		proc = new StartTLSProcessor();
		processors.put(proc.getId(), proc);
		proc = new SASLProcessor();
		processors.put(proc.getId(), proc);
		proc = new BindProcessor();
		processors.put(proc.getId(), proc);
	}

	@Override
	public void tlsHandshakeCompleted(ComponentIOService service) {}

	@Override
	public void unbindHostname(String hostname, ComponentIOService serv) {
		CopyOnWriteArrayList<ComponentConnection> conns = connections.get(hostname);

		if (conns != null) {
			ComponentConnection conn = null;

			for (ComponentConnection componentConnection : conns) {
				if (componentConnection.getService() == serv) {
					conn = componentConnection;
				}
			}
			if (conn != null) {
				boolean moreConnections = removeComponentConnection(conn.getDomain(), conn);

				if (!moreConnections) {
					removeRoutings(conn.getDomain());
				}
			}
		}
	}

	@Override
	public boolean writePacketToSocket(ComponentIOService ios, Packet p) {

		// String xmlns = (String)ios.getSessionData().get("xmlns");
		// if (xmlns != null) {
		// p.getElement().setXMLNS(xmlns);
		// }
		p.getElement().removeAttribute("xmlns");

		return super.writePacketToSocket(ios, p);
	}

	@Override
	public void xmppStreamClosed(ComponentIOService serv) {}

	@Override
	public String[] xmppStreamOpened(ComponentIOService serv, Map<String, String> attribs) {
		if (log.isLoggable(Level.FINEST)) {
			log.finest("Stream opened: " + serv.getRemoteAddress() + ", xmlns: " + attribs.get(
					"xmlns") + ", type: " + serv.connectionType().toString() + ", uniqueId=" + serv
					.getUniqueId() + ", to=" + attribs.get("to"));
		}

		String            s_xmlns = attribs.get("xmlns");
		String            result  = null;
		StreamOpenHandler handler = streamOpenHandlers.get(s_xmlns);

		if ((handler == null) || (s_xmlns == null)) {
			log.finest("unknownXMLNSHandler is processing request");
			result = unknownXMLNSHandler.streamOpened(serv, attribs, this);
		} else {
			log.finest(handler.getClass().getName() + " is processing request");
			result = handler.streamOpened(serv, attribs, this);
		}
		if (log.isLoggable(Level.FINEST)) {
			log.finest("Sending back: " + result);
		}

		return result == null ? null : new String[] { result };
	}

	@Override
	protected long getMaxInactiveTime() {
		return 1000 * 24 * HOUR;
	}

	@Override
	protected Integer getMaxQueueSize(int def) {
		return def * 10;
	}

	@Override
	protected ComponentIOService getXMPPIOService(Packet p) {
		if (p.getStanzaTo() == null) {

			// This is a bad packet actually
			return null;
		}

		ComponentIOService                        result   = null;
		String                                    hostname = p.getStanzaTo().getDomain();
		CopyOnWriteArrayList<ComponentConnection> conns    = connections.get(hostname);

		// If there is no connections list for this domain and routings are set to *
		// we use the first available list.
		for (CopyOnWriteArrayList<ComponentConnection> c : connections.values()) {

			// Is there a better way to take the first available element?
			if ((c.size() > 0) && ".*".equals(c.get(0).getService().getRoutings())) {
				conns = c;

				break;
			}
		}
		if (conns != null) {

			// First we check whether the receiver has sent to us a packet through one
			// of connections as this would be wise to send response on the same connection
			for (ComponentConnection componentConnection : conns) {
				ComponentIOService serv = componentConnection.getService();

				if ((serv != null) && serv.isConnected() && serv.isRecentJID(p.getStanzaTo())) {
					result = serv;

					break;
				}
			}

			// Now, load balancer selects the best connection to send the packet
			if ((result == null) && (conns.size() > 1)) {
				CompRepoItem cmp_repo_item = getCompRepoItem(hostname);

				if (cmp_repo_item == null) {
					cmp_repo_item = repo.getItem(p.getStanzaFrom().getDomain());
				}

				LoadBalancerIfc lb = cmp_repo_item.getLoadBalancer();

				result = lb.selectConnection(p, conns);
			}

			// The above algorithm did not work for some reason. Now trying
			// traditional way to send a packet to the first available and working
			// connection
			if (result == null) {
				if (log.isLoggable(Level.FINEST)) {
					log.finest("LB could not select connection, or there is only one connection, trying traditional way");
				}
				for (ComponentConnection componentConnection : conns) {
					ComponentIOService serv = componentConnection.getService();

					if (serv != null) {
						if (serv.isConnected()) {
							result = serv;
						} else {
							log.info("Service is not connected for connection for hostname: " +
									hostname);
						}
					} else {
						log.info("Service is null for connection for hostname: " + hostname);
					}
					if (result != null) {
						break;
					}
				}
			}
		} else {
			log.info("No ext connection for hostname: " + hostname);
		}
		if (log.isLoggable(Level.FINEST)) {
			log.finest("Selected connection: " + result);
		}

		return result;
	}

	@Override
	protected ComponentIOService getXMPPIOServiceInstance() {
		return new ComponentIOService();
	}

	@Override
	protected boolean isHighThroughput() {
		return true;
	}

	private synchronized void addComponentConnection(String hostname,
			ComponentIOService s) {
		ComponentConnection       conn      = new ComponentConnection(hostname, s);
		List<ComponentConnection> refObject = s.getRefObject();

		if (refObject == null) {
			refObject = new CopyOnWriteArrayList<ComponentConnection>();
		}

		// keep all connections sorted, fix for #983
		synchronized (refObject) {
			refObject.add(conn);

			// workaround to sort CopyOnWriteArrayList
			ComponentConnection[] arr_list = refObject.toArray(
					new ComponentConnection[refObject.size()]);

			Arrays.sort(arr_list);
			refObject = new CopyOnWriteArrayList<ComponentConnection>(arr_list);
		}
		s.setRefObject(refObject);

		CopyOnWriteArrayList<ComponentConnection> conns = connections.get(hostname);

		if (conns == null) {
			conns = new CopyOnWriteArrayList<ComponentConnection>();
		}

		// Not very optimal, however this does not happen (should not) very often
		// and the data collections is optimized for fast object retrieval
		// by index (round robin balance for example)
		// keep all connections sorted, fix for #983
		boolean result;

		synchronized (conns) {
			result = conns.add(conn);

			// workaround to sort CopyOnWriteArrayList
			ComponentConnection[] arr_list = conns.toArray(
					new ComponentConnection[conns.size()]);

			Arrays.sort(arr_list);
			conns = new CopyOnWriteArrayList<ComponentConnection>(arr_list);
		}
		connections.put(hostname, conns);
		if (result) {
			log.finer("A new component connection added for: " + hostname);
		} else {
			log.fine("A new component connection NOT added for: " + hostname);
		}
	}

	private synchronized boolean removeComponentConnection(String hostname,
			ComponentConnection conn) {
		boolean                                   result = false;
		CopyOnWriteArrayList<ComponentConnection> conns  = connections.get(hostname);

		if (conns != null) {

			// This is slow, however this does not happen (should not) very often
			// and the data collections is optimized for fast object retrieval
			// by index (round robin balance for example)
			boolean removed = conns.remove(conn);

			if (removed) {
				log.finer("A component connection removed for: " + hostname);
			} else {
				log.fine("A component connection NOT removed for: " + hostname);
			}
			for (ComponentConnection compCon : conns) {
				ComponentIOService serv = compCon.getService();

				if ((serv != null) && serv.isConnected()) {

					// There is still an active connection for this host
					result = true;
				} else {
					log.warning("Null or disconnected service for ComponentConnection for host: " +
							hostname);
				}
			}
		} else {
			log.warning("That should not happen, ComponentConnection is not null but " +
					"the collection is: " + hostname);
		}

		return result;
	}

	private void removeRoutings(String hostname) {
		String[] routings = new String[] { hostname, ".*@" + hostname, ".*\\." + hostname };

		updateRoutings(routings, false);

		// removeRouting(serv.getRemoteHost());
		// String addr = (String)sessionData.get(PORT_REMOTE_HOST_PROP_KEY);
		if (log.isLoggable(Level.FINE)) {
			log.fine("Disonnected from: " + hostname);
		}
		updateServiceDiscoveryItem(hostname, null, "XEP-0114 disconnected", false);
		removeComponentDomain(hostname);
	}

	private void updateRoutings(String[] routings, boolean add) {
		if (add) {
			for (String route : routings) {
				try {
					addRegexRouting(route);
				} catch (Exception e) {
					log.warning("Can not add regex routing '" + route + "' : " + e);
				}
			}
		} else {
			for (String route : routings) {
				try {
					removeRegexRouting(route);
					log.fine("Removed routings: " + route);
				} catch (Exception e) {
					log.warning("Can not remove regex routing '" + route + "' : " + e);
				}
			}
		}
		log.finest("All regex routings: " + getRegexRoutings().toString());
	}

	private class AuthenticationTimerTask
					extends tigase.util.TimerTask {
		private ComponentIOService serv = null;

		private AuthenticationTimerTask(ComponentIOService serv) {
			this.serv = serv;
		}

		@Override
		public void run() {
			if (!serv.isAuthenticated()) {
				serv.stop();
			}
		}
	}
}
