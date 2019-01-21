/**
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

import tigase.server.Packet;
import tigase.util.common.TimerTask;
import tigase.xml.Element;
import tigase.xmpp.XMPPIOService;
import tigase.xmpp.jid.BareJID;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

/**
 * Created: Dec 9, 2010 11:40:28 PM
 *
 * @param <IO>
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public interface S2SConnectionHandlerIfc<IO extends XMPPIOService<?>> {

	/**
	 * Returns stream features available for particular S2S connection.
	 *
	 * @param serv {@link S2SIOService} for which stream features should be retrieved
	 *
	 * @return list of stream features
	 */
	public List<Element> getStreamFeatures(S2SIOService serv);

	boolean addOutPacket(Packet packet);

	void addTimerTask(TimerTask task, long delay, TimeUnit unit);

	CIDConnections getCIDConnections(CID cid, boolean createNew) throws NotLocalhostException, LocalhostException;

	CIDConnections.CIDConnectionsOpenerService getConnectionOpenerService();

	BareJID getDefHostName();

	/**
	 * Returns secret used for particular domain
	 *
	 * @param domain for which secret should be returned
	 *
	 * @return for particular domain
	 *
	 * @throws NotLocalhostException if the domain is not local
	 */
	String getSecretForDomain(String domain) throws NotLocalhostException;

	String getServerNameForDomain(String domain);

	void initNewConnection(Map<String, Object> port_props);

	/**
	 * Checks if TLS is required for particular domain
	 *
	 * @param domain for which secret should be returned
	 *
	 * @return boolean indicating whether TLS is required
	 */
	boolean isTlsRequired(String domain);

	boolean isTlsWantClientAuthEnabled();

	boolean isTlsNeedClientAuthEnabled();

	boolean sendVerifyResult(String elem_name, CID connCid, CID keyCid, Boolean valid, String key_sessionId,
							 String serv_sessionId, String cdata, boolean handshakingOnly);

	boolean sendVerifyResult(String elem_name, CID connCid, CID keyCid, Boolean valid, String key_sessionId,
							 String serv_sessionId, String cdata, boolean handshakingOnly, Element errorElem);

	boolean writePacketToSocket(IO serv, Packet packet);

	void writePacketsToSocket(IO serv, Queue<Packet> packets);

	void writeRawData(IO serv, String strError);

}

