
/*
* Tigase Jabber/XMPP Server
* Copyright (C) 2004-2012 "Artur Hefczyc" <artur.hefczyc@tigase.org>
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
*
* $Rev$
* Last modified by $Author$
* $Date$
 */
package tigase.server.xmppserver;

//~--- non-JDK imports --------------------------------------------------------

import tigase.io.TLSEventHandler;

import tigase.server.Packet;

import tigase.xml.Element;

import tigase.xmpp.BareJID;
import tigase.xmpp.XMPPIOService;

//~--- JDK imports ------------------------------------------------------------

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.TimerTask;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

//~--- interfaces -------------------------------------------------------------

/**
 * Created: Dec 9, 2010 11:40:28 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 *
 * @param <IO>
 */
public interface S2SConnectionHandlerIfc<IO extends XMPPIOService<?>> {

	/**
	 * Method description
	 *
	 *
	 *
	 * @param serv
	 * 
	 */
	public List<Element> getStreamFeatures(S2SIOService serv);

	//~--- methods --------------------------------------------------------------

	boolean addOutPacket(Packet packet);

	void addTimerTask(tigase.util.TimerTask task, long delay, TimeUnit unit);

	//~--- get methods ----------------------------------------------------------

	CIDConnections getCIDConnections(CID cid, boolean createNew)
			throws NotLocalhostException, LocalhostException;

	BareJID getDefHostName();
       
    String getSecretForDomain(String domain) throws NotLocalhostException;
        
	//~--- methods --------------------------------------------------------------

	void initNewConnection(Map<String, Object> port_props);

	boolean isTlsWantClientAuthEnabled();
	
	boolean sendVerifyResult(String elem_name, CID connCid, CID keyCid, Boolean valid,
			String key_sessionId, String serv_sessionId, String cdata, boolean handshakingOnly);

	boolean writePacketToSocket(IO serv, Packet packet);

	void writePacketsToSocket(IO serv, Queue<Packet> packets);

	void writeRawData(IO serv, String strError);
}


//~ Formatted in Sun Code Convention


//~ Formatted by Jindent --- http://www.jindent.com
