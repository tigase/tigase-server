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
package tigase.xmpp.impl;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.logging.Logger;
import tigase.db.NonAuthUserRepository;
import tigase.db.UserNotFoundException;
import tigase.server.Packet;
import tigase.util.JIDUtils;
import tigase.xml.DomBuilderHandler;
import tigase.xml.Element;
import tigase.xml.SimpleParser;
import tigase.xml.SingletonFactory;
import tigase.xmpp.NotAuthorizedException;
import tigase.xmpp.StanzaType;
import tigase.xmpp.XMPPImplIfc;
import tigase.xmpp.XMPPPostprocessorIfc;
import tigase.xmpp.XMPPProcessor;
import tigase.xmpp.XMPPProcessorIfc;
import tigase.xmpp.XMPPResourceConnection;

/**
 * Describe class OfflineMessages here.
 *
 *
 * Created: Mon Oct 16 13:28:53 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class OfflineMessages extends XMPPProcessor
	implements XMPPPostprocessorIfc, XMPPProcessorIfc {

  /**
   * Private logger for class instancess.
   */
  private static Logger log =
		Logger.getLogger("tigase.xmpp.impl.OfflineMessage");

	private static final String ID = "msgoffline";
  private static final String XMLNS = "jabber:client";
	private static final String[] ELEMENTS = {"presence"};
  private static final String[] XMLNSS = {XMLNS};
  private static final Element[] DISCO_FEATURES =
	{new Element("feature",	new String[] {"var"},	new String[] {"msgoffline"})};

	private SimpleParser parser = SingletonFactory.getParserInstance();
	private SimpleDateFormat formater =
		new SimpleDateFormat("yyyyMMdd'T'HH:mm:ss");

	// Implementation of tigase.xmpp.XMPPImplIfc

	/**
	 * Describe <code>supElements</code> method here.
	 *
	 * @return a <code>String[]</code> value
	 */
	public String[] supElements()
	{ return ELEMENTS; }

	/**
	 * Describe <code>supNamespaces</code> method here.
	 *
	 * @return a <code>String[]</code> value
	 */
  public String[] supNamespaces()
	{ return XMLNSS; }

	/**
	 * Describe <code>supDiscoFeatures</code> method here.
	 *
	 * @param session a <code>XMPPResourceConnection</code> value
	 * @return a <code>String[]</code> value
	 */
  public Element[] supDiscoFeatures(final XMPPResourceConnection session)
	{ return DISCO_FEATURES; }

	/**
	 * Describe <code>id</code> method here.
	 *
	 * @return a <code>String</code> value
	 */
	public String id() { return ID; }

	// Implementation of tigase.xmpp.XMPPProcessorIfc

	/**
	 * Describe <code>process</code> method here.
	 *
	 * @param packet a <code>Packet</code> value
	 * @param conn a <code>XMPPResourceConnection</code> value
	 * @param repo a <code>NonAuthUserRepository</code> value
	 * @param results a <code>Queue</code> value
	 */
	public void process(final Packet packet, final XMPPResourceConnection conn,
		final NonAuthUserRepository repo, final Queue<Packet> results,
		final Map<String, Object> settings) {

		if (conn == null) {
			return;
		} // end of if (session == null)

		StanzaType type = packet.getType();
		if (type == null || type == StanzaType.available) {
			// Should we send off-line messages now?
			// Let's try to do it here and maybe later I find better place.
			String priority_str = packet.getElemCData("/presence/priority");
			int priority = 0;
			if (priority_str != null) {
				try {
					priority = Integer.decode(priority_str);
				} catch (NumberFormatException e) {
					priority = 0;
				} // end of try-catch
			} // end of if (priority != null)
			if (priority >= 0) {
				try {
					Queue<Packet> packets =	restorePacketForOffLineUser(conn);
					if (packets != null) {
						log.finer("Sending off-line messages: " + packets.size());
						results.addAll(packets);
					} // end of if (packets != null)
				} catch (NotAuthorizedException e) {	} // end of try-catch
			} // end of if (priority >= 0)
		} // end of if (type == null || type == StanzaType.available)

	}

	// Implementation of tigase.xmpp.XMPPPostprocessorIfc

	/**
	 * Describe <code>postProcess</code> method here.
	 *
	 * @param packet a <code>Packet</code> value
	 * @param conn a <code>XMPPResourceConnection</code> value
	 * @param repo a <code>NonAuthUserRepository</code> value
	 * @param queue a <code>Queue</code> value
	 */
	public void postProcess(final Packet packet,
		final XMPPResourceConnection conn,	final NonAuthUserRepository repo,
		final Queue<Packet> queue) {
		if (conn == null) {
			try {
				if (savePacketForOffLineUser(packet, repo)) {
					packet.processedBy(ID);
				}
			} catch (UserNotFoundException e) {
				log.finest("UserNotFoundException at trying to save packet for off-line user."
					+ packet.getStringData());
			} // end of try-catch
		} // end of if (conn == null)
	}

	public boolean savePacketForOffLineUser(Packet pac,
		NonAuthUserRepository repo) throws UserNotFoundException {

		StanzaType type = pac.getType();

		if ((pac.getElemName().equals("message") &&
				(pac.getElement().findChild("/message/body") != null)
				(type == null || type == StanzaType.normal || type == StanzaType.chat))
			|| (pac.getElemName().equals("presence") &&
					(type == StanzaType.subscribe || type == StanzaType.subscribed
						|| type == StanzaType.unsubscribe || type == StanzaType.unsubscribed))) {
			Element packet = pac.getElement().clone();
			String stamp = null;
			synchronized (formater) {
				stamp = formater.format(new Date());
			}
			String from = JIDUtils.getNodeHost(pac.getElemTo());
			Element x = new Element("x", "Offline Storage",
				new String[] {"from", "stamp", "xmlns"},
				new String[] {from, stamp, "jabber:x:delay"});
			packet.addChild(x);
			String user_id = JIDUtils.getNodeID(pac.getElemTo());
			repo.addOfflineDataList(user_id, ID,
				"messages", new String[] {packet.toString()});
			return true;
		} // end of if (pac.getElemName().equals("message"))
		return false;
	}

	public Queue<Packet> restorePacketForOffLineUser(XMPPResourceConnection conn)
		throws NotAuthorizedException {
		DomBuilderHandler domHandler = new DomBuilderHandler();
		String[] msgs = conn.getOfflineDataList(ID, "messages");
		if (msgs != null && msgs.length > 0) {
			conn.removeOfflineDataGroup(ID);
			LinkedList<Packet> pacs = new LinkedList<Packet>();
			StringBuilder sb = new StringBuilder();
			for (String msg: msgs) {
				sb.append(msg);
			}
			char[] data = sb.toString().toCharArray();
			parser.parse(domHandler, data, 0, data.length);
			Queue<Element> elems = domHandler.getParsedElements();
			Element elem = null;
			while ((elem = elems.poll()) != null) {
				pacs.offer(new Packet(elem));
			} // end of while (elem = elems.poll() != null)
			try {
				Collections.sort(pacs, new StampComparator());
			} catch (NullPointerException e) {
				log.warning("Can not sort off line messages, " + e);
			}
			return pacs;
		} // end of if (msgs != null)
		else {
			return null;
		} // end of if (msgs != null) else
	}

	private class StampComparator implements Comparator<Packet> {
		public int compare(Packet p1, Packet p2) {
			String stamp1 = p1.getElement().getAttribute("/message/x", "stamp");
			String stamp2 = p2.getElement().getAttribute("/message/x", "stamp");
			return stamp1.compareTo(stamp2);
		}
	}

} // OfflineMessages
