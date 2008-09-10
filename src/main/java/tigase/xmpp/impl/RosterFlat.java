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

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Queue;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.logging.Logger;
import tigase.server.Packet;
import tigase.xml.Element;
import tigase.xml.XMLUtils;
import tigase.xmpp.NotAuthorizedException;
import tigase.xmpp.StanzaType;
import tigase.xmpp.XMPPResourceConnection;
import tigase.util.JIDUtils;

/**
 * Describe class RosterFlat here.
 *
 *
 * Created: Tue Feb 21 18:05:53 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class RosterFlat extends RosterAbstract {

	/**
   * Private logger for class instancess.
   */
  private static Logger log =	Logger.getLogger("tigase.xmpp.impl.RosterFlat");

	

	public String[] getBuddies(final XMPPResourceConnection session)
    throws NotAuthorizedException {
    return session.getDataGroups(ROSTER);
  }

  public String getBuddyName(final XMPPResourceConnection session,
		final String buddy)
    throws NotAuthorizedException {
    return session.getData(groupNode(buddy), NAME, null);
  }

  public void setBuddyName(final XMPPResourceConnection session,
		final String buddy, final String name) throws NotAuthorizedException {
    session.setData(groupNode(buddy), NAME, name);
  }

  public void setBuddySubscription(final XMPPResourceConnection session,
    final SubscriptionType subscription, final String buddy)
		throws NotAuthorizedException {
    session.setData(groupNode(buddy), SUBSCRIPTION, subscription.toString());
  }

  public SubscriptionType getBuddySubscription(
		final XMPPResourceConnection session,
    final String buddy) throws NotAuthorizedException {
		//		return SubscriptionType.both;
		String subscr = session.getData(groupNode(buddy),	SUBSCRIPTION, null);
		if (subscr != null) {
			return SubscriptionType.valueOf(subscr);
		}
		return null;
  }

	public boolean removeBuddy(final XMPPResourceConnection session,
		final String jid) throws NotAuthorizedException {
		session.removeDataGroup(groupNode(jid));
		return true;
	}

	public void addBuddy(final XMPPResourceConnection session,
		final String jid) throws NotAuthorizedException {
		String nick = JIDUtils.getNodeNick(jid);
		if (nick == null) {
			nick = jid;
		}
		session.setData(groupNode(jid), NAME, nick);
    session.setData(groupNode(jid), SUBSCRIPTION,
			SubscriptionType.none.toString());
	}

  public String[] getBuddyGroups(final XMPPResourceConnection session,
		final String buddy)
    throws NotAuthorizedException {
    return session.getDataList(groupNode(buddy), GROUPS);
  }

} // RosterFlat
