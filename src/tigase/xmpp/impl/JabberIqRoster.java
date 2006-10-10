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
import tigase.util.JID;
import tigase.xml.Element;
import tigase.xmpp.Authorization;
import tigase.xmpp.NotAuthorizedException;
import tigase.xmpp.StanzaType;
import tigase.xmpp.XMPPProcessor;
import tigase.xmpp.XMPPResourceConnection;

import static tigase.xmpp.impl.Roster.SubscriptionType;

/**
 * Class <code>JabberIqRoster</code> implements part of <em>RFC-3921</em> -
 * <em>XMPP Instant Messaging</em> specification describing roster management.
 * 7.  Roster Management
 *
 *
 * Created: Tue Feb 21 17:42:53 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class JabberIqRoster extends XMPPProcessor {

  /**
   * Private logger for class instancess.
   */
  private static Logger log =
		Logger.getLogger("tigase.xmpp.impl.JabberIqRoster");

  protected static final String XMLNS = "jabber:iq:roster";
	protected static final String ID = XMLNS;
	protected static final String[] ELEMENTS = {"query"};
  protected static final String[] XMLNSS = {XMLNS};

	public String id() { return ID; }

	public String[] supElements() { return ELEMENTS; }

  public String[] supNamespaces() { return XMLNSS; }

	private void processSetRequest(final Packet packet,
		final XMPPResourceConnection session,	final Queue<Packet> results)
    throws NotAuthorizedException {

		Element request = packet.getElement();

    String buddy = request.getAttribute("/iq/query/item", "jid");
    Element item =  request.findChild("/iq/query/item");
    String subscription = item.getAttribute("subscription");
    if (subscription != null && subscription.equals("remove")) {
			Roster.removeBuddy(session, buddy);
      results.offer(packet.okResult((String)null, 0));
			Element it = new Element("item");
			it.setAttribute("jid", buddy);
			it.setAttribute("subscription", "remove");
			Roster.updateBuddyChange(session, results, it);
    } else {
      String name = request.getAttribute("/iq/query/item", "name");
      if (name == null) {
        name = buddy;
      } // end of if (name == null)
      Roster.setBuddyName(session, buddy, name);
      if (Roster.getBuddySubscription(session, buddy) == null) {
        Roster.setBuddySubscription(session, SubscriptionType.none, buddy);
      } // end of if (getBuddySubscription(session, buddy) == null)
      List<Element> groups = item.getChildren();
      if (groups != null) {
        String[] gr = new String[groups.size()];
        int cnt = 0;
        for (Element group : groups) {
          gr[cnt++] = group.getCData();
        } // end of for (ElementData group : groups)
        session.setDataList(Roster.groupNode(buddy), Roster.GROUPS, gr);
      } // end of if (groups != null)
      results.offer(packet.okResult((String)null, 0));
      Roster.updateBuddyChange(session, results,
				Roster.getBuddyItem(session, buddy));
    } // end of else
  }

	private void processGetRequest(final Packet packet,
		final XMPPResourceConnection session,	final Queue<Packet> results)
    throws NotAuthorizedException {
    String[] buddies = Roster.getBuddies(session);
    if (buddies != null) {
      StringBuilder items = new StringBuilder();
      for (String buddy : buddies) {
        items.append(Roster.getBuddyItem(session, buddy));
      }
      results.offer(packet.okResult(items.toString(), 1));
    } // end of if (buddies != null)
    else {
      results.offer(packet.okResult((String)null, 1));
    } // end of if (buddies != null) else
  }

  public void process(final Packet packet, final XMPPResourceConnection session,
		final Queue<Packet> results) {

		try {
			if (packet.getElemFrom() != null
				&& !session.getUserId().equals(JID.getNodeID(packet.getElemFrom()))) {
				// RFC says: ignore such request
				log.warning(
					"Roster request 'from' attribute doesn't match session userid: "
					+ session.getUserId()
					+ ", request: " + packet.getStringData());
				return;
			} // end of if (packet.getElemFrom() != null
				// && !session.getUserId().equals(JID.getNodeID(packet.getElemFrom())))

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
				"Received roster request but user session is not authorized yet: " +
        packet.getStringData());
			results.offer(Authorization.NOT_AUTHORIZED.getResponseMessage(packet,
					"You must authorize session first.", true));
		} // end of try-catch
	}


} // JabberIqRoster
