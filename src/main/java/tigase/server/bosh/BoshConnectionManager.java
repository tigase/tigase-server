/*  Tigase Project
 *  Copyright (C) 2001-2007
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
package tigase.server.bosh;

import java.io.IOException;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import tigase.net.IOService;
import tigase.server.ConnectionManager;
import tigase.server.Packet;
import tigase.util.DNSResolver;
import tigase.xmpp.XMPPIOService;

/**
 * Describe class BoshConnectionManager here.
 *
 *
 * Created: Sat Jun  2 12:24:29 2007
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class BoshConnectionManager extends ConnectionManager {

  /**
   * Variable <code>log</code> is a class logger.
   */
  private static final Logger log =
    Logger.getLogger("tigase.server.bosh.BoshConnectionManager");

	private static final int DEF_PORT_NO = 5280;
	private static int[] PORTS = {DEF_PORT_NO};
	private static final String HOSTNAMES_PROP_KEY = "hostnames";
	private static String[] HOSTNAMES_PROP_VAL =	{"localhost", "hostname"};

	private String defHostName = null;
	private Set<String> hostnames = new TreeSet<String>();
	private Map<UUID, BoshSession> sessions =
		new LinkedHashMap<UUID, BoshSession>();

	public void processPacket(Packet packet) {
		log.finer("Processing packet: " + packet.getElemName()
			+ ", type: " + packet.getType());
		log.finest("Processing packet: " + packet.getStringData());
		writePacketToSocket(packet);
	}

	public Queue<Packet> processSocketData(XMPPIOService serv) {
		Packet p = null;
		while ((p = serv.getReceivedPackets().poll()) != null) {
			log.finer("Processing packet: " + p.getElemName()
				+ ", type: " + p.getType());
			log.finest("Processing socket data: " + p.getStringData());
			try {
				serv.writeRawData("HTTP/1.1 200 OK\r\n"
					+ "Content-Type: text/xml; charset=utf-8\r\n"
					+ "Content-Length: 128\r\n"
					+ "\r\n"
					+ "<body wait='60' inactivity='30'polling='5' requests='2' hold='1'"
					+ " ack='1573741820' accept='deflate,gzip' maxpause='120' sid='SomeSID'"
					+ " ver='1.6' from='localhost' secure='true'"
					+ " xmlns='http://jabber.org/protocol/httpbind'/>");
			} catch (IOException e) {
				log.log(Level.INFO, "Exception", e);
			}
		} // end of while ()
		return null;
	}

	public Map<String, Object> getDefaults(Map<String, Object> params) {
		Map<String, Object> props = super.getDefaults(params);
		if (params.get(GEN_VIRT_HOSTS) != null) {
			HOSTNAMES_PROP_VAL = ((String)params.get(GEN_VIRT_HOSTS)).split(",");
		} else {
			HOSTNAMES_PROP_VAL = DNSResolver.getDefHostNames();
		}
		props.put(HOSTNAMES_PROP_KEY, HOSTNAMES_PROP_VAL);
		return props;
	}

	public void setProperties(Map<String, Object> props) {
		super.setProperties(props);
		String[] hnames = (String[])props.get(HOSTNAMES_PROP_KEY);
		clearRoutings();
		hostnames.clear();
		defHostName = null;
		for (String host: hnames) {
			addRouting(getName() + "@" + host);
			hostnames.add(host);
			if (defHostName == null) {
				defHostName = host;
			} // end of if (defHostName == null)
		} // end of for ()
	}

	protected int[] getDefPlainPorts() {
		return PORTS;
	}

	public void serviceStopped(final IOService service) {
		super.serviceStopped(service);
	}

	public void serviceStarted(final IOService service) {
		super.serviceStarted(service);
	}

	/**
	 * Method <code>getMaxInactiveTime</code> returns max keep-alive time
	 * for inactive connection. we shoulnd not really close external component
	 * connection at all, so let's say something like: 1000 days...
	 *
	 * @return a <code>long</code> value
	 */
	protected long getMaxInactiveTime() {
		return 1000*10*MINUTE;
	}

	public void xmppStreamClosed(XMPPIOService serv) {
		log.finer("Stream closed.");
	}

	public String xmppStreamOpened(XMPPIOService serv,
		Map<String, String> attribs) {
		return null;
	}

}
