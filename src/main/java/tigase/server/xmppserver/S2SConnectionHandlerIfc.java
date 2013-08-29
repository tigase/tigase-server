/*
 * S2SConnectionHandlerIfc.java
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



package tigase.server.xmppserver;

//~--- non-JDK imports --------------------------------------------------------

import tigase.io.TLSEventHandler;

import tigase.server.Packet;

import tigase.xml.Element;

import tigase.xmpp.BareJID;
import tigase.xmpp.XMPPIOService;

//~--- JDK imports ------------------------------------------------------------

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.TimerTask;

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
	 *
	 * @return a value of List<Element>
	 */
	public List<Element> getStreamFeatures(S2SIOService serv);

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param packet
	 *
	 * @return a value of boolean
	 */
	boolean addOutPacket(Packet packet);

	/**
	 * Method description
	 *
	 *
	 * @param task
	 * @param delay
	 * @param unit
	 */
	void addTimerTask(tigase.util.TimerTask task, long delay, TimeUnit unit);

	/**
	 * Method description
	 *
	 *
	 * @param port_props
	 */
	void initNewConnection(Map<String, Object> port_props);

	/**
	 * Method description
	 *
	 *
	 * @param elem_name
	 * @param connCid
	 * @param keyCid
	 * @param valid
	 * @param key_sessionId
	 * @param serv_sessionId
	 * @param cdata
	 * @param handshakingOnly
	 *
	 * @return a value of boolean
	 */
	boolean sendVerifyResult(String elem_name, CID connCid, CID keyCid, Boolean valid,
			String key_sessionId, String serv_sessionId, String cdata, boolean handshakingOnly);

	/**
	 * Method description
	 *
	 *
	 * @param serv
	 * @param packets
	 */
	void writePacketsToSocket(IO serv, Queue<Packet> packets);

	/**
	 * Method description
	 *
	 *
	 * @param serv
	 * @param packet
	 *
	 * @return a value of boolean
	 */
	boolean writePacketToSocket(IO serv, Packet packet);

	/**
	 * Method description
	 *
	 *
	 * @param serv
	 * @param strError
	 */
	void writeRawData(IO serv, String strError);

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param cid
	 * @param createNew
	 *
	 * @return a value of CIDConnections
	 *
	 * @throws LocalhostException
	 * @throws NotLocalhostException
	 */
	CIDConnections getCIDConnections(CID cid, boolean createNew)
					throws NotLocalhostException, LocalhostException;

	/**
	 * Method description
	 *
	 *
	 * @return a value of BareJID
	 */
	BareJID getDefHostName();

	/**
	 * Method description
	 *
	 *
	 * @param cid
	 * @param keyCid
	 * @param remote_key
	 * @param stanzaId
	 * @param sessionId
	 *
	 * @return a value of String
	 */
	String getLocalDBKey(CID cid, CID keyCid, String remote_key, String stanzaId,
			String sessionId);

	/**
	 * Method description
	 *
	 *
	 * @return a value of boolean
	 */
	boolean isTlsWantClientAuthEnabled();
}


//~ Formatted in Tigase Code Convention on 13/08/28
