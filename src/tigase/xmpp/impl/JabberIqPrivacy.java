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
package tigase.xmpp.impl;

import java.util.List;
import java.util.Queue;
import java.util.logging.Logger;
import tigase.server.Packet;
import tigase.xmpp.Authorization;
import tigase.xmpp.NotAuthorizedException;
import tigase.xmpp.StanzaType;
import tigase.xmpp.XMPPProcessor;
import tigase.xmpp.XMPPResourceConnection;
import tigase.xml.Element;

import static tigase.xmpp.impl.Privacy.*;

/**
 * Describe class JabberIqPrivacy here.
 *
 *
 * Created: Mon Oct  9 18:18:11 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class JabberIqPrivacy extends XMPPProcessor {

  /**
   * Private logger for class instancess.
   */
  private static Logger log =
		Logger.getLogger("tigase.xmpp.impl.JabberIqPrivacy");

  protected static final String XMLNS = "jabber:iq:privacy";
	protected static final String ID = XMLNS;
	protected static final String[] ELEMENTS = {"query"};
  protected static final String[] XMLNSS = {XMLNS};

	public String id() { return ID; }

	public String[] supElements() { return ELEMENTS; }

  public String[] supNamespaces() { return XMLNSS; }

  public void process(final Packet packet, final XMPPResourceConnection session,
		final Queue<Packet> results) {
		try {

			StanzaType type = packet.getType();
			switch (type) {
			case get:
				processGetRequest(packet, session, results);
				break;
			case set:
				processSetRequest(packet, session, results);
				break;
			case result:
				// Ignore
				break;
			default:
				results.offer(Authorization.BAD_REQUEST.getResponseMessage(packet,
						"Request type is incorrect", false));
				break;
			} // end of switch (type)

		} catch (NotAuthorizedException e) {
      log.warning(
				"Received privacy request but user session is not authorized yet: " +
        packet.getStringData());
			results.offer(Authorization.NOT_AUTHORIZED.getResponseMessage(packet,
					"You must authorize session first.", true));
		} // end of try-catch
	}

	private void processSetRequest(final Packet packet,
		final XMPPResourceConnection session,	final Queue<Packet> results)
    throws NotAuthorizedException {
		List<Element> children = packet.getElemChildren("/iq/query");
		if (children != null || children.size() == 1) {
			Element child = children.get(0);
			if (child.getName().equals("list")) {
				// Broken privacy implementation sends list without name set
				// instead of sending BAD_REQUEST error I can just assign
				// 'default' name here.
				String name = child.getAttribute(NAME);
				if (name == null || name.length() == 0) {
					child.setAttribute(NAME, "default");
				} // end of if (name == null || name.length() == 0)
				Privacy.addList(session, child);
				results.offer(packet.okResult((String)null, 0));
			} // end of if (child.getName().equals("list))
			if (child.getName().equals("default")) {
				Privacy.setDefaultList(session, child);
			} // end of if (child.getName().equals("list))
			if (child.getName().equals("active")) {
				Privacy.setActiveList(session, child);
			} // end of if (child.getName().equals("list))
		} else {
			results.offer(Authorization.BAD_REQUEST.getResponseMessage(packet,
					"Only 1 element is allowed in privacy set request.", true));
		} // end of else
	}

	private void processGetRequest(final Packet packet,
		final XMPPResourceConnection session,	final Queue<Packet> results)
    throws NotAuthorizedException {
		List<Element> children = packet.getElemChildren("/iq/query");
		if (children == null || children.size() == 0) {
			String[] lists = Privacy.getLists(session);
			if (lists != null) {
				StringBuilder sblists = new StringBuilder();
				for (String list : lists) {
					sblists.append("<list name=\"" + list + "\"/>");
				}
				String list = Privacy.getDefaultList(session);
				if (list != null) {
					sblists.append("<default name=\"" + list + "\"/>");
				} // end of if (defList != null)
				list = Privacy.getActiveList(session);
				if (list != null) {
					sblists.append("<active name=\"" + list + "\"/>");
				} // end of if (defList != null)
				results.offer(packet.okResult(sblists.toString(), 1));
			} else {
				results.offer(packet.okResult((String)null, 1));
			} // end of if (buddies != null) else
		} else {
			if (children.size() > 1) {
				results.offer(Authorization.BAD_REQUEST.getResponseMessage(packet,
					"You can retrieve only one list at a time.", true));
			} else {
				Element eList = Privacy.getList(session,
					children.get(0).getAttribute("name"));
				if (eList != null) {
					results.offer(packet.okResult(eList, 1));
				} else {
					results.offer(Authorization.ITEM_NOT_FOUND.getResponseMessage(packet,
							"Requested list not found.", true));
				} // end of if (eList != null) else
			} // end of else
		} // end of else
  }

} // JabberIqPrivacy
