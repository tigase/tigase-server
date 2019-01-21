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
package tigase.server.bosh;

import tigase.server.Packet;
import tigase.server.xmppclient.SeeOtherHostIfc;
import tigase.xml.Element;
import tigase.xmpp.jid.BareJID;
import tigase.xmpp.jid.JID;

/**
 * Describe interface BoshSessionTaskHandler here.
 * <br>
 * Created: Sat Aug  4 10:39:21 2007
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public interface BoshSessionTaskHandler {

	JID getJidForBoshSession(BoshSession bs);

	BoshTask scheduleTask(BoshSession bs, long delay);

	BoshSendQueueTask scheduleSendQueueTask(BoshSession tt, long delay);

	void cancelTask(BoshTask bs);

	void cancelSendQueueTask(BoshSendQueueTask bt);

	void writeRawData(BoshIOService ios, String data);

	boolean addOutStreamOpen(Packet packet, BoshSession bs);

	boolean addOutStreamClosed(Packet packet, BoshSession bs, boolean withTimeout);

	BareJID getDefHostName();

	BareJID getSeeOtherHostForJID(Packet packet, BareJID userId, SeeOtherHostIfc.Phase ph);

	Element getSeeOtherHostError(Packet packet, BareJID destination);

	boolean processUndeliveredPacket(Packet packet, Long stamp, String errorMessage);
}
