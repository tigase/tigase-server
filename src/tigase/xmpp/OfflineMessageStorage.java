/*  Package Jabber Server
 *  Copyright (C) 2001, 2002, 2003, 2004, 2005
 *  "Artur Hefczyc" <kobit@users.sourceforge.net>
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
package tigase.xmpp;

import java.util.LinkedList;
import java.util.Queue;
import java.text.SimpleDateFormat;
import java.util.Date;
import tigase.util.JID;
import tigase.db.UserNotFoundException;
import tigase.db.UserRepository;
import tigase.server.Packet;
import tigase.xml.Element;
import tigase.xml.SimpleParser;
import tigase.xml.SingletonFactory;
import tigase.xml.DomBuilderHandler;

/**
 * Describe class OfflineMessageStorage here.
 *
 *
 * Created: Sat Sep 16 19:47:11 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class OfflineMessageStorage {

  private DomBuilderHandler domHandler = null;
	private static SimpleParser parser = SingletonFactory.getParserInstance();
	private UserRepository repository = null;
	private SimpleDateFormat formater = null;

	/**
	 * Creates a new <code>OfflineMessageStorage</code> instance.
	 *
	 */
	public OfflineMessageStorage(UserRepository repository) {
		domHandler = new DomBuilderHandler();
		this.repository = repository;
		formater = new SimpleDateFormat("yyyyMMdd'T'HH:mm:ss");
	}

	public boolean savePacketForOffLineUser(Packet packet)
		throws UserNotFoundException {
		if (packet.getElemName().equals("message")) {
			StanzaType type = packet.getType();
			if (type == null || type == StanzaType.normal || type == StanzaType.chat) {

// 	<x from='capulet.com' stamp='20020910T23:08:25' xmlns='jabber:x:delay'>
//     Offline Storage
//   </x>
				String stamp = null;
				synchronized (formater) {
					stamp = formater.format(new Date());
				}
				String from = JID.getNodeHost(packet.getElemTo());
				Element x = new Element("x", "Offline Storage",
					new String[] {"from", "stamp", "xmlns"},
					new String[] {from, stamp, "jabber:x:delay"});
				Element msg = packet.getElement();
				msg.addChild(x);
				String user_id = JID.getNodeID(packet.getElemTo());
				repository.addDataList(user_id, "off-line",
					"messages", new String[] {packet.getStringData()});
			return true;
			}
		} // end of if (packet.getElemName().equals("message"))
		return false;
	}

	public Queue<Packet> restorePacketForOffLineUser(String userId)
		throws UserNotFoundException {
		String[] msgs = repository.getDataList(userId, "off-line", "messages");
		if (msgs != null && msgs.length > 0) {
			repository.removeData(userId, "off-line", "messages");
			Queue<Packet> pacs = new LinkedList<Packet>();
			synchronized (domHandler) {
				for (String msg: msgs) {
					char[] data = msg.toCharArray();
					parser.parse(domHandler, data, 0, data.length);
					Queue<Element> elems = domHandler.getParsedElements();
					Element elem = null;
					while ((elem = elems.poll()) != null) {
						pacs.offer(new Packet(elem));
					} // end of while (elem = elems.poll() != null)
				} // end of for (int i = 0; i < msgs.length; i++)
			}
			return pacs;
		} // end of if (msgs != null)
		else {
			return null;
		} // end of if (msgs != null) else
	}

} // OfflineMessageStorage
