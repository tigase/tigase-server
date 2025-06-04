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
package tigase.server.xmppserver;

import tigase.cert.CertCheckResult;
import tigase.cert.CertificateUtil;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.config.ConfigField;
import tigase.kernel.beans.selector.ConfigType;
import tigase.kernel.beans.selector.ConfigTypeEnum;
import tigase.kernel.core.Kernel;
import tigase.net.ConnectionType;
import tigase.server.ConnectionManager;
import tigase.server.Packet;
import tigase.server.Permissions;
import tigase.stats.StatisticsList;
import tigase.stats.StatisticsProviderIfc;
import tigase.vhosts.VHostItem;
import tigase.xml.Element;
import tigase.xmpp.Authorization;
import tigase.xmpp.PacketErrorTypeException;
import tigase.xmpp.StanzaType;
import tigase.xmpp.jid.JID;

import javax.script.Bindings;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created: Jun 14, 2010 11:59:38 AM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 */
@Bean(name = "s2s", parent = Kernel.class, active = true)
@ConfigType({ConfigTypeEnum.DefaultMode, ConfigTypeEnum.ConnectionManagersMode})
public class S2SConnectionManager
		extends ConnectionManager<S2SIOService>
		implements S2SConnectionHandlerIfc<S2SIOService> {

	public static final String CID_CONNECTIONS_BIND = "cidConnections";

	public static final String CID_KEY = "cid";

	public static final String CID_CONNECTIONS_TASKS_THREADS_KEY = "cid-connections-tasks-threads";

	public static final String MAX_CONNECTION_INACTIVITY_TIME_PROP_KEY = "max-inactivity-time";

	public static final String MAX_INCOMING_CONNECTIONS_PROP_KEY = "max-in-conns";

	public static final int MAX_INCOMING_CONNECTIONS_PROP_VAL = 4;

	public static final String MAX_OUT_PER_IP_CONNECTIONS_PROP_KEY = "max-out-per-ip-conns";

	public static final int MAX_OUT_PER_IP_CONNECTIONS_PROP_VAL = 1;

	public static final String MAX_OUT_TOTAL_CONNECTIONS_PROP_KEY = "max-out-total-conns";

	public static final int MAX_OUT_TOTAL_CONNECTIONS_PROP_VAL = 1;

	public static final String MAX_PACKET_WAITING_TIME_PROP_KEY = "max-packet-waiting-time";

	public static final String S2S_CONNECTION_SELECTOR_PROP_KEY = "s2s-conn-selector";

	public static final String S2S_CONNECTION_SELECTOR_PROP_VAL = "tigase.server.xmppserver.S2SRandomSelector";

	public static final String S2S_DOMAIN_MAPPING_PROP_KEY = "s2s-domain-mapping";

	public static final String S2S_DOMAIN_MAPPING_PROP_VAL = "";

	public static final String S2S_HT_TRAFFIC_THROTTLING_PROP_VAL = "xmpp:15k:0:disc,bin:120m:0:disc";
	public static final long MAX_PACKET_WAITING_TIME_PROP_VAL = 7 * MINUTE;

	// TODO: #1195 - estimate proper default value
	public static final int CID_CONNECTIONS_TASKS_THREADS_VAL = Runtime.getRuntime().availableProcessors();
	public static final String XMLNS_SERVER_VAL = "jabber:server";
	protected static final String DB_RESULT_EL_NAME = "db:result";
	protected static final String DB_VERIFY_EL_NAME = "db:verify";
	private static final Logger log = Logger.getLogger(S2SConnectionManager.class.getName());
	private static final String PROCESSORS_CONF_PROP_KEY = "processors-conf";
	private static final String XMLNS_CLIENT_VAL = "jabber:client";
	/**
	 * Outgoing and incoming connections for a given domains pair (localdomain, remotedomain)
	 */
	protected Map<CID, CIDConnections> cidConnections = new ConcurrentHashMap<CID, CIDConnections>(10000);
	@Inject
	private CIDConnections.CIDConnectionsOpenerService cidConnectionsOpenerService;
	// ~--- fields ---------------------------------------------------------------
	@Inject
	private S2SConnectionSelector connSelector;
	/**
	 * Holds list of manually entered mappings which provide substitutions for domains matching pattens with names of
	 * servers to which we should connect.
	 */
	@Inject
	private DomainServerNameMapper domainServerNameMapper;
	@Inject
	private List<S2SFilterIfc> filters = Collections.emptyList();
	private int maxINConnections = MAX_INCOMING_CONNECTIONS_PROP_VAL;
	private int maxOUTPerIPConnections = MAX_OUT_PER_IP_CONNECTIONS_PROP_VAL;
	private int maxOUTTotalConnections = MAX_OUT_TOTAL_CONNECTIONS_PROP_VAL;
	/**
	 * <code>maxPacketWaitingTime</code> keeps the maximum time packets can wait for sending in ServerPacketQueue.
	 * Packets are put in the queue only when connection to remote server is not established so effectively this timeout
	 * specifies the maximum time for connecting to remote server. If this time is exceeded then no more reconnecting
	 * attempts are performed and packets are sent back with error information.
	 * <br>
	 * Default TCP/IP timeout is 300 seconds so we can follow this convention but administrator can set different
	 * timeout in server configuration.
	 */
	private long maxPacketWaitingTime = MAX_PACKET_WAITING_TIME_PROP_VAL;

	@ConfigField(desc = "Accept self-signed certificates for outgoing S2S connections")
	private boolean acceptSelfSignedSslCertificates = false;
	@ConfigField(desc = "Whether s2s connection is required to be authenticated both ways before allowing transmission", alias = "one-way-authentication")
	private boolean oneWayAuthentication = false;
	/**
	 * List of processors which should handle all traffic incoming from the network. In most cases if not all, these
	 * processors handle just protocol traffic, all the rest traffic should be passed on to MR.
	 */
	@Inject
	private List<S2SProcessor> processors = Collections.emptyList();

	@Override
	public boolean addOutPacket(Packet packet) {
		if (packet.getPacketFrom() == null) {
			packet.setPacketFrom(getComponentId());
		}
		return super.addOutPacket(packet);
	}

	@Override
	public void addTimerTask(tigase.util.common.TimerTask task, long delay, TimeUnit unit) {
		super.addTimerTask(task, delay, unit);
	}

	@Override
	public boolean handlesNonLocalDomains() {
		return true;
	}

	@Override
	public int hashCodeForPacket(Packet packet) {

		// Calculate hash code from the destination domain name to make sure packets
		// for
		// a single domain are processed by the same thread to avoid race condition
		// creating new connection data structures for a destination domain
		if (packet.getStanzaTo() != null) {
			return packet.getStanzaTo().getDomain().hashCode();
		}

		// Otherwise, it might be a control packet which can be processed by single
		// thread
		return 1;
	}

	@Override
	public void initBindings(Bindings binds) {
		super.initBindings(binds);
		binds.put(CID_CONNECTIONS_BIND, cidConnections);
	}

	@Override
	public void initNewConnection(Map<String, Object> port_props) {
		addWaitingTask(port_props);
	}

	@Override
	public void processPacket(Packet packet) {
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "Processing packet: {0}", packet);
		}
		if ((packet.getStanzaTo() == null) || packet.getStanzaTo().getDomain().trim().isEmpty()) {
			log.log(Level.WARNING, "Missing ''to'' attribute, ignoring packet...{0}" +
					"\n This most likely happens due to missconfiguration of components" + " domain names.", packet);

			return;
		}
		if ((packet.getStanzaFrom() == null) || packet.getStanzaFrom().getDomain().trim().isEmpty()) {
			log.log(Level.WARNING, "Missing ''from'' attribute, ignoring packet...{0}", packet);

			return;
		}

		String to_hostname = packet.getStanzaTo().getDomain();

		try {
			String from_hostname = packet.getStanzaFrom().getDomain();
			CID cid = new CID(from_hostname, to_hostname);

			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "Connection ID is: {0}", cid);
			}
			try {
				CIDConnections cid_conns = getCIDConnections(cid, true);
				Packet server_packet = packet.copyElementOnly();

				server_packet.getElement().removeAttribute("xmlns");
				cid_conns.sendPacket(server_packet);
			} catch (NotLocalhostException e) {
				addOutPacket(Authorization.NOT_ACCEPTABLE.getResponseMessage(packet,
																			 "S2S - Incorrect source address (" +
																					 from_hostname +
																					 ") - none of any local virtual hosts or components.",
																			 true));
			} catch (LocalhostException e) {
				addOutPacket(Authorization.NOT_ACCEPTABLE.getResponseMessage(packet,
																			 "S2S - Incorrect destination address " +
																					 to_hostname +
																					 " - one of local virtual hosts or components.",
																			 true));
			}
		} catch (PacketErrorTypeException e) {
			log.log(Level.WARNING, "Packet processing exception", e);
		}
	}

	@Override
	public Queue<Packet> processSocketData(S2SIOService serv) {
		Queue<Packet> packets = serv.getReceivedPackets();
		Packet p = null;
		Queue<Packet> results = new ArrayDeque<Packet>(2);

		while ((p = packets.poll()) != null) {
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "Processing socket data: {0} [{1}]", new Object[]{p, serv});
			}
			if (p.getXMLNS() == null) {
				p.setXMLNS(XMLNS_SERVER_VAL);
			}

			boolean processed = false;

			for (S2SProcessor proc : processors) {
				processed |= proc.process(p, serv, results);
				writePacketsToSocket(serv, results);
			}

			if (!processed) {
				for (S2SFilterIfc filter : filters) {
					processed |= filter.filter(p, serv, results);
					writePacketsToSocket(serv, results);
				}
			}

			if (!processed) {

				// Sometimes xmlns is not set for the packet. Usually it does not
				// cause any problems but when the packet is sent over the s2s, ext
				// or cluster connection it may be quite problematic.
				// Let's force jabber:client xmlns for all packets received from s2s
				// connection
				// In theory null does not hurt, but if the packet goes through the
				// cluster
				// connection is gets cluster XMLNS
				if ((p.getXMLNS() == XMLNS_SERVER_VAL) || (p.getXMLNS() == null)) {
					p.setXMLNS(XMLNS_CLIENT_VAL);
				}
				try {
					if (isLocalDomainOrComponent(p.getStanzaTo().getDomain())) {
						if (log.isLoggable(Level.FINEST)) {
							log.log(Level.FINEST, "Adding packet out: {0} [{1}]", new Object[]{p, serv});
						}

						// TODO: not entirely sure if this is a good idea....
						// Let's check it out.
						p.setPermissions(Permissions.REMOTE);
						addOutPacket(p);
					} else {
						try {
							serv.addPacketToSend(Authorization.NOT_ACCEPTABLE.getResponseMessage(p,
																								 "Not a local virtual domain or component",
																								 true));
						} catch (PacketErrorTypeException ex) {
						}
					}
				} catch (Exception e) {
					log.log(Level.CONFIG, "Unexpected exception for packet: " + p, e);
				}
			}
		}    // end of while ()

		return null;
	}

	@Override
	public boolean processUndeliveredPacket(Packet packet, Long stamp, String errorMessage) {
		// re-add packet - this may be good as we would retry to send packet
		// which delivery failed due to IO error

		for (S2SProcessor proc : processors) {
			if (proc.shouldSkipUndelivered(packet)) {
				return false;
			}
		}

		addPacket(packet);
		return true;
	}

	@Override
	public void reconnectionFailed(Map<String, Object> port_props) {
		CID cid = (CID) port_props.get(CID_KEY);

		if (cid == null) {
			log.log(Level.WARNING, "Protocol error cid not set for outgoing connection: {0}", port_props);

			return;
		}

		CIDConnections cid_conns = getCIDConnections(cid);

		if (cid_conns == null) {
			log.log(Level.WARNING, "Protocol error cid_conns not found for outgoing connection: {0}", port_props);

			return;
		} else {
			cid_conns.reconnectionFailed(port_props);
		}
	}

	@Override
	public int schedulerThreads() {

		// TODO: #1195 - estimate proper default value
		return Runtime.getRuntime().availableProcessors();
	}

	@Override
	public boolean sendVerifyResult(String elem_name, CID connCid, CID keyCid, Boolean valid, String key_sessionId,
									String serv_sessionId, String cdata, boolean handshakingOnly) {
		return this.sendVerifyResult(elem_name, connCid, keyCid, valid, key_sessionId, serv_sessionId, cdata,
									 handshakingOnly, null);
	}

	@Override
	public boolean sendVerifyResult(String elem_name, CID connCid, CID keyCid, Boolean valid, String key_sessionId,
									String serv_sessionId, String cdata, boolean handshakingOnly, Element errorElem) {
		CIDConnections cid_conns = getCIDConnections(connCid);
		log.log(Level.FINEST,
				"Sending verification result: {1}, session: {2}, handshaking: {3}, cdata: {4}, error: {5} [{0}]",
				new Object[]{cid_conns, valid, serv_sessionId, handshakingOnly, cdata, errorElem});

		if (cid_conns != null) {
			StanzaType type = null;
			if (valid != null) {
				if (valid) {
					type = StanzaType.valid;
				} else {
					type = StanzaType.invalid;
				}
			}
			if (errorElem != null) {
				type = StanzaType.error;
			}
			Packet verify_valid = getValidResponse(elem_name, keyCid, key_sessionId, type, cdata);

			if (errorElem != null) {
				verify_valid.getElement().addChild(errorElem);
			}

			if (handshakingOnly) {
				cid_conns.sendHandshakingOnly(verify_valid);

				return true;
			} else {
				return cid_conns.sendControlPacket(serv_sessionId, verify_valid);
			}
		} else {
			if (log.isLoggable(Level.FINE)) {
				log.log(Level.FINE, "Can''t find CID connections for cid: {0}, can''t send verify response.", keyCid);
			}
		}

		return false;
	}

	@Override
	public void serviceConnected(S2SIOService service) {
		super.serviceConnected(service);
	}

	@Override
	public void serviceStarted(S2SIOService serv) {
		super.serviceStarted(serv);
		final CID cid = (CID)serv.getSessionData().get(CID_KEY);
		if (cid != null) {
			serv.setUserJid(cid.toString());
			serv.setConnectionId(JID.jidInstanceNS(cid.getLocalHost(), cid.getRemoteHost(), UUID.randomUUID().toString()));
		} else {
			serv.setConnectionId(JID.jidInstanceNS(null, serv.getUniqueId(), UUID.randomUUID().toString()));
		}
		log.log(Level.CONFIG, "s2s connection opened: {0}", serv);
		for (S2SProcessor proc : processors) {
			proc.serviceStarted(serv);
		}
	}

	@Override
	public boolean serviceStopped(S2SIOService serv) {
		boolean result = super.serviceStopped(serv);

		if (result) {
			for (S2SProcessor proc : processors) {
				proc.serviceStopped(serv);
			}
		}

		if (log.isLoggable(Level.CONFIG)) {
			log.log(Level.CONFIG, "[[{0}]] S2S Connection stopped: {1}", new Object[]{getName(), serv});
		}

		return result;
	}

	@Override
	public void tlsHandshakeCompleted(S2SIOService serv) {
		if ((!acceptSelfSignedSslCertificates) && serv.connectionType() == ConnectionType.connect) {
			if (serv.getSessionData().get(S2SIOService.CERT_CHECK_RESULT) != CertCheckResult.trusted) {
				serv.stop();
				return;
			}
		}
		for (S2SProcessor proc : processors) {
			proc.serviceStarted(serv);
		}
	}

	@Override
	public void writeRawData(S2SIOService ios, String data) {
		super.writeRawData(ios, data);
	}

	@Override
	public void xmppStreamClosed(S2SIOService serv) {
		if (log.isLoggable(Level.FINER)) {
			log.log(Level.FINER, "Stream closed. {0}", new Object[]{serv});
		}
		for (S2SProcessor proc : processors) {
			proc.streamClosed(serv);
		}
	}

	@Override
	public String[] xmppStreamOpened(S2SIOService serv, Map<String, String> attribs) {
		if (log.isLoggable(Level.FINER)) {
			log.log(Level.FINER, "Stream opened: {1} [{0}]", new Object[]{serv, attribs});
		}

		StringBuilder sb = new StringBuilder(256);

		for (S2SProcessor proc : processors) {
			String res = proc.streamOpened(serv, attribs);

			if (res != null) {
				sb.append(res);
			}
		}
		if (log.isLoggable(Level.FINER)) {
			log.log(Level.FINER, "Sending stream open: {1} [{0}]", new Object[]{serv, sb});
		}

		return (sb.length() == 0) ? null : new String[]{sb.toString()};
	}

	@Override
	public CIDConnections getCIDConnections(CID cid, boolean createNew)
			throws NotLocalhostException, LocalhostException {
		CIDConnections result = getCIDConnections(cid);

		if ((result == null) && createNew && (cid != null)) {
			result = createNewCIDConnections(cid);
		}

		return result;
	}

	@Override
	public void validateCIDConnection(CID cid) throws NotLocalhostException, LocalhostException {
		if (cid.getLocalHost() == null || !isLocalDomainOrComponent(cid.getLocalHost())) {
			throw new NotLocalhostException("This is not a valid local domain: " + cid.getLocalHost());
		}
		if (cid.getRemoteHost() != null && isLocalDomainOrComponent(cid.getRemoteHost())) {
			throw new LocalhostException("This is not a valid remotehost: " + cid.getRemoteHost());
		}
	}

	@Override
	public CIDConnections.CIDConnectionsOpenerService getConnectionOpenerService() {
		return cidConnectionsOpenerService;
	}

	@Override
	public String getDiscoCategoryType() {
		return "s2s";
	}

	@Override
	public String getDiscoDescription() {
		return "S2S connection manager";
	}

	/**
	 * {@inheritDoc}
	 * <br>
	 * Secret is used in generation of dialback key
	 */
	@Override
	public String getSecretForDomain(String domain) throws NotLocalhostException {
		VHostItem item = vHostManager.getVHostItem(domain);
		if (item == null) {
			if (this.isLocalDomainOrComponent(domain)) {
				int idx = domain.indexOf('.');
				if (idx > 0) {
					String basedomain = domain.substring(idx + 1);
					item = vHostManager.getVHostItem(basedomain);
				}

				if (item == null) {
					item = vHostManager.getVHostItem(vHostManager.getDefVHostItem().toString());
				}
			}
		}

		if (item == null) {
			throw new NotLocalhostException("This is not a valid localhost: " + domain);
		}

		return item.getS2sSecret();
	}

	@Override
	public String getServerNameForDomain(String domain) {
		return domainServerNameMapper.getServerNameForDomain(domain);
	}

	@Override
	public void getStatistics(StatisticsList list) {
		super.getStatistics(list);
		list.add(getName(), "CIDs number", cidConnections.size(), Level.INFO);
		if (list.checkLevel(Level.FINEST)) {
			long total_outgoing = 0;
			long total_outgoing_tls = 0;
			long total_outgoing_handshaking = 0;
			long total_incoming = 0;
			long total_incoming_tls = 0;
			long total_dbKeys = 0;
			long total_waiting = 0;
			long total_waiting_control = 0;

			for (Map.Entry<CID, CIDConnections> cid_conn : cidConnections.entrySet()) {
				int outgoing = cid_conn.getValue().getOutgoingCount();
				int outgoing_tls = cid_conn.getValue().getOutgoingTLSCount();
				int outgoing_handshaking = cid_conn.getValue().getOutgoingHandshakingCount();
				int incoming = cid_conn.getValue().getIncomingCount();
				int incoming_tls = cid_conn.getValue().getIncomingTLSCount();
				int dbKeys = cid_conn.getValue().getDBKeysCount();
				int waiting = cid_conn.getValue().getWaitingCount();
				int waiting_control = cid_conn.getValue().getWaitingControlCount();

				if (log.isLoggable(Level.FINEST)) {

					// Throwable thr = new Throwable();
					//
					// thr.fillInStackTrace();
					// log.log(Level.FINEST, "Called from: ", thr);
					log.log(Level.FINEST, "CID: {0}, OUT: {1}, OUT_HAND: {2}, IN: {3}, dbKeys: {4}, " +
									"waiting: {5}, waiting_control: {6}",
							new Object[]{cid_conn.getKey(), outgoing, outgoing_handshaking, incoming, dbKeys, waiting,
										 waiting_control});
				}
				total_outgoing += outgoing;
				total_outgoing_tls += outgoing_tls;
				total_outgoing_handshaking += outgoing_handshaking;
				total_incoming += incoming;
				total_incoming_tls += incoming_tls;
				total_dbKeys += dbKeys;
				total_waiting += waiting;
				total_waiting_control += waiting_control;
			}
			list.add(getName(), "Total outgoing", total_outgoing, Level.FINEST);
			list.add(getName(), "Total outgoing TLS", total_outgoing_tls, Level.FINEST);
			list.add(getName(), "Total outgoing handshaking", total_outgoing_handshaking, Level.FINEST);
			list.add(getName(), "Total incoming", total_incoming, Level.FINEST);
			list.add(getName(), "Total incoming TLS", total_incoming_tls, Level.FINEST);
			list.add(getName(), "Total DB keys", total_dbKeys, Level.FINEST);
			list.add(getName(), "Total waiting", total_waiting, Level.FINEST);
			list.add(getName(), "Total control waiting", total_waiting_control, Level.FINEST);
		}
		for (S2SProcessor processor : processors) {
			((StatisticsProviderIfc)processor).getStatistics(getName(), list);
		}
	}

	@Override
	public List<Element> getStreamFeatures(S2SIOService serv) {
		List<Element> results = new ArrayList<Element>(10);

		for (S2SProcessor proc : processors) {
			proc.streamFeatures(serv, results);
		}

		return results;
	}

	@Override
	public boolean isTlsRequired(String domain) {
		VHostItem item = vHostManager.getVHostItemDomainOrComponent(domain);
		return item != null && item.isTlsRequired();
	}

	@Override
	public boolean isTlsWantClientAuthEnabled() {
		return true;
	}

	@Override
	public boolean isTlsNeedClientAuthEnabled() {
		return false;
	}

	public void setProcessors(List<S2SProcessor> processors) {
		List<S2SProcessor> tmp_processors = new ArrayList<>(processors);
		Collections.sort(tmp_processors);
		this.processors = Collections.unmodifiableList(tmp_processors);
	}

	@Override
	protected int[] getDefPlainPorts() {
		return new int[]{5269};
	}

	/**
	 * Method from ConnectionManager is overriden as it uses local value S2S_HT_TRAFFIC_THROTTLING_PROP_VAL
	 */
	@Override
	protected String getDefTrafficThrottling() {
		String result = ST_TRAFFIC_THROTTLING_PROP_VAL;

		if (isHighThroughput()) {
			result = S2S_HT_TRAFFIC_THROTTLING_PROP_VAL;
		}

		return result;
	}

	@Override
	protected long getMaxInactiveTime() {
		return 120 * MINUTE;
	}

	@Override
	protected S2SIOService getXMPPIOServiceInstance() {
		return new S2SIOService();
	}

	@Override
	protected boolean isHighThroughput() {
		return true;
	}

	protected CIDConnections createNewCIDConnections(CID cid) throws NotLocalhostException, LocalhostException {
		if (!isLocalDomainOrComponent(cid.getLocalHost())) {
			throw new NotLocalhostException("This is not a valid localhost: " + cid.getLocalHost());
		}
		if (isLocalDomainOrComponent(cid.getRemoteHost())) {
			throw new LocalhostException("This is not a valid remotehost: " + cid.getRemoteHost());
		}

		CIDConnections cid_conns = new CIDConnections(cid, this, connSelector, maxINConnections, maxOUTTotalConnections,
													  maxOUTPerIPConnections, maxPacketWaitingTime, oneWayAuthentication);

		cidConnections.put(cid, cid_conns);

		return cid_conns;
	}

	// ~--- get methods ----------------------------------------------------------
	private CIDConnections getCIDConnections(CID cid) {
		if (cid == null) {
			return null;
		}

		return cidConnections.get(cid);
	}

	private Packet getValidResponse(String elem_name, CID cid, String id, StanzaType type, String cdata) {
		Element elem = new Element(elem_name);

		if (cdata != null) {
			elem.setCData(cdata);
		}
		if (type != null) {
			elem.addAttribute("type", type.name());
		}
		if (id != null) {
			elem.addAttribute("id", id);
		}

		Packet result = Packet.packetInstance(elem, JID.jidInstanceNS(cid.getLocalHost()),
											  JID.jidInstanceNS(cid.getRemoteHost()));

		return result;
	}

	@Bean(name = "domainServerNameMapper", parent = S2SConnectionManager.class, active = true)
	public static class DomainServerNameMapper {

		@ConfigField(desc = "Rules for mapping domains")
		private List<Entry> entries = new ArrayList<Entry>();

		public DomainServerNameMapper() {
		}

		public String getServerNameForDomain(String domain) {
			for (Entry e : entries) {
				if (e.matches(domain)) {
					return e.getServerName();
				}
			}
			return domain;
		}

		public Map<String, String> getEntries() {
			Map<String, String> result = new HashMap<>();
			for (Entry e : entries) {
				result.put(e.pattern, e.getServerName());
			}
			return result;
		}

		public void setEntries(Map<String, String> entries) {
			entries.clear();
			for (Map.Entry<String, String> e : entries.entrySet()) {
				addEntry(e.getKey(), e.getValue());
			}
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append(getClass().getName()).append("[");
			boolean first = true;
			for (Entry e : entries) {
				if (!first) {
					sb.append(",");
				}
				sb.append(e.pattern);
				sb.append("=");
				sb.append(e.serverName);
				first = false;
			}
			sb.append("]");
			return sb.toString();
		}

		protected void addEntry(String pattern, String serverName) {
			// clone list to fix possible concurrency issues with collection
			// could use CopyOnWriteArrayList but sorting this collection
			// is not possible on JDK7
			synchronized (this) {
				List<Entry> entries = new ArrayList<Entry>(this.entries);
				Entry e = new Entry(pattern, serverName);
				entries.add(e);
				Collections.sort(entries);
				this.entries = entries;
			}
		}

		private class Entry
				implements Comparable<Entry> {

			private final String pattern;
			private final String serverName;

			public Entry(String pattern, String serverName) {
				this.pattern = pattern.toLowerCase();
				this.serverName = serverName;
			}

			public String getServerName() {
				return serverName;
			}

			public boolean matches(String domain) {
				if ("*".equals(pattern)) {
					return true;
				}
				return CertificateUtil.match(domain, pattern);
			}

			@Override
			public boolean equals(Object obj) {
				if (obj instanceof Entry) {
					return pattern.equals(((Entry) obj).pattern);
				}
				return false;
			}

			@Override
			public int hashCode() {
				return pattern.hashCode();
			}

			@Override
			public int compareTo(Entry o) {
				if (o.pattern.contains("*")) {
					if (!pattern.contains("*")) {
						return -1;
					}
				} else {
					if (pattern.contains("*")) {
						return 1;
					}
				}
				int val = (pattern.split("\\.").length - o.pattern.split("\\.").length) * -1;
				if (val != 0) {
					return val;
				}
				return o.pattern.length() - pattern.length();
			}
		}

	}
}

