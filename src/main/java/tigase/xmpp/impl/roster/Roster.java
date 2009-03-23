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
package tigase.xmpp.impl.roster;

import java.util.logging.Logger;
import tigase.xmpp.NotAuthorizedException;
import tigase.xmpp.XMPPResourceConnection;
import tigase.util.JIDUtils;
import tigase.db.TigaseDBException;

/**
 * Describe class Roster here.
 *
 *
 * Created: Tue Feb 21 18:05:53 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class Roster extends RosterAbstract {

	/**
   * Private logger for class instancess.
   */
  private static Logger log =	Logger.getLogger("tigase.xmpp.impl.Roster");

  public String[] getBuddies(final XMPPResourceConnection session)
    throws NotAuthorizedException, TigaseDBException {
    return session.getDataGroups(ROSTER);
  }

  public String getBuddyName(final XMPPResourceConnection session,
		final String buddy)
    throws NotAuthorizedException, TigaseDBException {
    return session.getData(groupNode(buddy), NAME, null);
  }

  public void setBuddyName(final XMPPResourceConnection session,
		final String buddy, final String name)
    throws NotAuthorizedException, TigaseDBException {
    session.setData(groupNode(buddy), NAME, name);
  }

  public void setBuddySubscription(final XMPPResourceConnection session,
    final SubscriptionType subscription, final String buddy)
		throws NotAuthorizedException, TigaseDBException {
    session.setData(groupNode(buddy), SUBSCRIPTION, subscription.toString());
  }

  public SubscriptionType getBuddySubscription(
		final XMPPResourceConnection session,
    final String buddy) throws NotAuthorizedException, TigaseDBException {
		//		return SubscriptionType.both;
		String subscr = session.getData(groupNode(buddy),	SUBSCRIPTION, null);
		if (subscr != null) {
			return SubscriptionType.valueOf(subscr);
		}
		return null;
  }

	public boolean removeBuddy(final XMPPResourceConnection session,
		final String jid) throws NotAuthorizedException, TigaseDBException {
		session.removeDataGroup(groupNode(jid));
		return true;
	}

	public void addBuddy(XMPPResourceConnection session,
		String jid, String name, String[] groups)
    throws NotAuthorizedException, TigaseDBException {
		String nick = JIDUtils.getNodeNick(jid);
		if (nick == null) {
			nick = jid;
		}
		session.setData(groupNode(jid), NAME, nick);
    session.setData(groupNode(jid), SUBSCRIPTION, SubscriptionType.none.toString());
    session.setDataList(groupNode(jid), GROUPS, groups);
	}

  public String[] getBuddyGroups(final XMPPResourceConnection session,
		final String buddy)
    throws NotAuthorizedException, TigaseDBException {
    return session.getDataList(groupNode(buddy), GROUPS);
  }

	@Override
	public boolean containsBuddy(XMPPResourceConnection session, String buddy)
					throws NotAuthorizedException, TigaseDBException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public boolean addBuddyGroup(XMPPResourceConnection session, String buddy,
					String[] groups)
					throws NotAuthorizedException, TigaseDBException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public String[] getBuddies(XMPPResourceConnection session, boolean onlineOnly)
					throws NotAuthorizedException, TigaseDBException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void setBuddyOnline(XMPPResourceConnection session, String buddy,
					boolean online)
					throws NotAuthorizedException, TigaseDBException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public boolean isBuddyOnline(XMPPResourceConnection session, String buddy)
					throws NotAuthorizedException, TigaseDBException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

} // Roster
