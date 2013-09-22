/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2012 "Artur Hefczyc" <artur.hefczyc@tigase.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License.
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

package tigase.xmpp.impl.roster;

//~--- non-JDK imports --------------------------------------------------------

import tigase.db.TigaseDBException;

import tigase.util.TigaseStringprepException;

import tigase.xmpp.JID;
import tigase.xmpp.NotAuthorizedException;
import tigase.xmpp.XMPPResourceConnection;

//~--- JDK imports ------------------------------------------------------------

import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

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
	 * Private logger for class instances.
	 */
	private static Logger log = Logger.getLogger("tigase.xmpp.impl.Roster");

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param session
	 * @param jid
	 * @param name
	 * @param groups
	 * @param otherData
	 *
	 * @throws NotAuthorizedException
	 * @throws TigaseDBException
	 */
	@Override
	public void addBuddy(XMPPResourceConnection session, JID jid, String name, String[] groups,
			String otherData)
			throws NotAuthorizedException, TigaseDBException {
		String nick = name;

		if ((nick == null) || nick.trim().isEmpty()) {
			nick = jid.getLocalpart();

			if ((nick == null) || nick.trim().isEmpty()) {
				nick = jid.toString();
			}
		}

		session.setData(groupNode(jid), NAME, nick);
		session.setData(groupNode(jid), SUBSCRIPTION, SubscriptionType.none.toString());
		session.setDataList(groupNode(jid), GROUPS, groups);
	}

	/**
	 * Method description
	 *
	 *
	 * @param session
	 * @param buddy
	 * @param groups
	 *
	 * 
	 *
	 * @throws NotAuthorizedException
	 * @throws TigaseDBException
	 */
	@Override
	public boolean addBuddyGroup(XMPPResourceConnection session, JID buddy, String[] groups)
			throws NotAuthorizedException, TigaseDBException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	/**
	 * Method description
	 *
	 *
	 * @param session
	 * @param buddy
	 *
	 * 
	 *
	 * @throws NotAuthorizedException
	 * @throws TigaseDBException
	 */
	@Override
	public boolean containsBuddy(XMPPResourceConnection session, JID buddy)
			throws NotAuthorizedException, TigaseDBException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param session
	 *
	 * 
	 *
	 * @throws NotAuthorizedException
	 * @throws TigaseDBException
	 */
	@Override
	public JID[] getBuddies(final XMPPResourceConnection session)
			throws NotAuthorizedException, TigaseDBException {
		String[] jids = session.getDataGroups(ROSTER);

		if ((jids != null) && (jids.length > 0)) {
			JID[] result = new JID[jids.length];
			int idx = 0;

			for (String jid : jids) {
				try {
					result[idx++] = JID.jidInstance(jid);
				} catch (TigaseStringprepException ex) {
					log.warning("Can't load user jid from database, stringprep problem: " + jid);
				}
			}

			return result;
		} else {
			return null;
		}
	}

	/**
	 * Method description
	 *
	 *
	 * @param session
	 * @param buddy
	 *
	 * 
	 *
	 * @throws NotAuthorizedException
	 * @throws TigaseDBException
	 */
	@Override
	public String[] getBuddyGroups(final XMPPResourceConnection session, JID buddy)
			throws NotAuthorizedException, TigaseDBException {
		return session.getDataList(groupNode(buddy), GROUPS);
	}

	/**
	 * Method description
	 *
	 *
	 * @param session
	 * @param buddy
	 *
	 * 
	 *
	 * @throws NotAuthorizedException
	 * @throws TigaseDBException
	 */
	@Override
	public String getBuddyName(final XMPPResourceConnection session, JID buddy)
			throws NotAuthorizedException, TigaseDBException {
		return session.getData(groupNode(buddy), NAME, null);
	}

	/**
	 * Method description
	 *
	 *
	 * @param session
	 * @param buddy
	 *
	 * 
	 *
	 * @throws NotAuthorizedException
	 * @throws TigaseDBException
	 */
	@Override
	public SubscriptionType getBuddySubscription(final XMPPResourceConnection session, JID buddy)
			throws NotAuthorizedException, TigaseDBException {

		// return SubscriptionType.both;
		String subscr = session.getData(groupNode(buddy), SUBSCRIPTION, null);

		if (subscr != null) {
			return SubscriptionType.valueOf(subscr);
		}

		return null;
	}

	/**
	 * Method description
	 *
	 *
	 * @param session
	 * @param jid
	 *
	 * 
	 *
	 * @throws NotAuthorizedException
	 * @throws TigaseDBException
	 */
	@Override
	public boolean isOnline(XMPPResourceConnection session, JID jid)
			throws NotAuthorizedException, TigaseDBException {
		return true;
	}

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param session
	 * @param jid
	 *
	 * 
	 *
	 * @throws NotAuthorizedException
	 * @throws TigaseDBException
	 */
	@Override
	public boolean presenceSent(XMPPResourceConnection session, JID jid)
			throws NotAuthorizedException, TigaseDBException {
		return false;
	}

	/**
	 * Method description
	 *
	 *
	 * @param session
	 * @param jid
	 *
	 * 
	 *
	 * @throws NotAuthorizedException
	 * @throws TigaseDBException
	 */
	@Override
	public boolean removeBuddy(final XMPPResourceConnection session, JID jid)
			throws NotAuthorizedException, TigaseDBException {
		session.removeDataGroup(groupNode(jid));

		return true;
	}

	//~--- set methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param session
	 * @param buddy
	 * @param name
	 *
	 * @throws NotAuthorizedException
	 * @throws TigaseDBException
	 */
	@Override
	public void setBuddyName(final XMPPResourceConnection session, JID buddy, final String name)
			throws NotAuthorizedException, TigaseDBException {
		session.setData(groupNode(buddy), NAME, name);
	}

	/**
	 * Method description
	 *
	 *
	 * @param session
	 * @param subscription
	 * @param buddy
	 *
	 * @throws NotAuthorizedException
	 * @throws TigaseDBException
	 */
	@Override
	public void setBuddySubscription(final XMPPResourceConnection session,
			final SubscriptionType subscription, JID buddy)
			throws NotAuthorizedException, TigaseDBException {
		session.setData(groupNode(buddy), SUBSCRIPTION, subscription.toString());
	}

	/**
	 * Method description
	 *
	 *
	 * @param session
	 * @param jid
	 * @param online
	 *
	 * @throws NotAuthorizedException
	 * @throws TigaseDBException
	 */
	@Override
	public void setOnline(XMPPResourceConnection session, JID jid, boolean online)
			throws NotAuthorizedException, TigaseDBException {}

	/**
	 * Method description
	 *
	 *
	 * @param session
	 * @param jid
	 * @param sent
	 *
	 * @throws NotAuthorizedException
	 * @throws TigaseDBException
	 */
	@Override
	public void setPresenceSent(XMPPResourceConnection session, JID jid, boolean sent)
			throws NotAuthorizedException, TigaseDBException {}

	/* (non-Javadoc)
	 * @see tigase.xmpp.impl.roster.RosterAbstract#getRosterElement(tigase.xmpp.XMPPResourceConnection, tigase.xmpp.JID)
	 */
	@Override
	public RosterElementIfc getRosterElement(XMPPResourceConnection session, JID jid)
			throws NotAuthorizedException, TigaseDBException {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see tigase.xmpp.impl.roster.RosterAbstract#logout()
	 */
	@Override
	public void logout(XMPPResourceConnection session) {
		// TODO Auto-generated method stub
		
	}
	
	public String getCustomStatus(XMPPResourceConnection session, JID buddy) {
		return null;
	}


//@Override
//public String[] getBuddies(XMPPResourceConnection session, boolean onlineOnly)
//        throws NotAuthorizedException, TigaseDBException {
//  throw new UnsupportedOperationException("Not supported yet.");
//}
//@Override
//public void setBuddyOnline(XMPPResourceConnection session, String buddy,
//        boolean online)
//        throws NotAuthorizedException, TigaseDBException {
//  throw new UnsupportedOperationException("Not supported yet.");
//}
//
//@Override
//public boolean isBuddyOnline(XMPPResourceConnection session, String buddy)
//        throws NotAuthorizedException, TigaseDBException {
//  throw new UnsupportedOperationException("Not supported yet.");
//}
}    // Roster


//~ Formatted in Sun Code Convention


//~ Formatted by Jindent --- http://www.jindent.com
