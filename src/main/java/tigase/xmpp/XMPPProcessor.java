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
package tigase.xmpp;

import tigase.db.TigaseDBException;
import tigase.kernel.beans.config.ConfigField;
import tigase.server.ComponentInfo;
import tigase.server.Packet;
import tigase.stats.StatisticsList;
import tigase.xml.Element;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <code>XMPPProcessor</code> abstract class contains basic definition for <em>XMPP</em> processor. To create new
 * processor implementing particular <em>XMPP</em> functionality it is enough to extend this class and implement one
 * abstract method.<br> Additionally to allow system properly recognise this processor you need also to implement own
 * constructor which sets proper values to parent constructor. You must implement exactly one constructor with zero
 * parameters which calls parent constructor with proper values. Refer to constructor documentation for information
 * about required parameters.<br> To fully interact with entity connected to the session or with other entities in
 * <em>XMPP</em> network you should be also familiar with <code>addReply(...)</code>, <code>addMessage(...)</code> and
 * <code>addBroadcast(...)</code> methods.<br> There is also partially implemented functionality to send messages to
 * entities in other networks like <em>SMTP</em> or other implemented by the server. Once this implementation is
 * finished there will be more information available. If you, however, are interested in this particular feature send a
 * question to author.
 * <br>
 * <p> Created: Tue Oct  5 20:31:23 2004 </p>
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
*/
public abstract class XMPPProcessor
		implements XMPPImplIfc, XMPPProcessorConcurrencyAwareIfc {

	protected static final String ALL_NAMES = "*";

	protected static final String[][] ALL_PATHS = {{"*"}};
	private static final Logger log = Logger.getLogger(XMPPProcessor.class.getName());
	protected static ComponentInfo cmpInfo = null;
	@ConfigField(desc = "Queue size which should be used by processor")
	private Integer queueSize = null;
	@ConfigField(desc = "Numbers of threads which should be used by processor")
	private int threadsNo = concurrentQueuesNo();

	{
		cmpInfo = new ComponentInfo(id(), this.getClass());
	}

	protected XMPPProcessor() {
	}

	@Override
	public Authorization canHandle(Packet packet, XMPPResourceConnection conn) {
		Authorization result = null;
		String[][] elemPaths = supElementNamePaths();

		if (elemPaths != null) {

			// This is the new API style
			String[] elemXMLNS = supNamespaces();
			Set<StanzaType> types = supTypes();

			result = checkPacket(packet, elemPaths, elemXMLNS, types);
		}
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "{0} ({1}), authorization/canHandle: {4}, Request: " + "{2}, conn: {3}",
					new Object[]{this.getClass().getSimpleName(), id(), packet, conn, result});
		}

		return result;
	}

	@Override
	public final int compareTo(XMPPImplIfc proc) {
		return getClass().getName().compareTo(proc.getClass().getName());
	}

	@Override
	public int concurrentQueuesNo() {
		return 1;
	}

	@Override
	@Deprecated
	public void init(Map<String, Object> settings) throws TigaseDBException {
	}

	@Override
	public Element[] supDiscoFeatures(final XMPPResourceConnection session) {
		return null;
	}

	@Override
	public String[][] supElementNamePaths() {
		return null;
	}

	@Override
	public String[] supNamespaces() {
		return null;
	}

	@Override
	public Element[] supStreamFeatures(final XMPPResourceConnection session) {
		return null;
	}

	@Override
	public Set<StanzaType> supTypes() {
		return null;
	}

	public XMPPProcessor getInstance() {
		return this;
	}

	@Override
	public void getStatistics(StatisticsList list) {
	}

	@Override
	public ComponentInfo getComponentInfo() {
		if (cmpInfo == null) {
			cmpInfo = new ComponentInfo(id(), this.getClass());
		}
		return cmpInfo;
	}

	@Override
	public String toString() {
		return String.valueOf(getComponentInfo());
	}

	@Override
	public int getThreadsNo() {
		return threadsNo;
	}

	@Override
	public Integer getQueueSize() {
		return queueSize;
	}

	private Authorization checkPacket(Packet packet, String[][] elemPaths, String[] elemXMLNS, Set<StanzaType> types) {
		Authorization result = null;
		boolean names_ok = elemPaths == ALL_PATHS;

		if (!names_ok) {
			for (int i = 0; i < elemPaths.length; i++) {
				if (packet.isXMLNSStaticStr(elemPaths[i], elemXMLNS[i])) {
					names_ok = true;

					break;
				}
			}
		}
		if (names_ok && ((types == null) || types.contains(packet.getType()))) {
			result = Authorization.AUTHORIZED;
		}

		return result;
	}
}

