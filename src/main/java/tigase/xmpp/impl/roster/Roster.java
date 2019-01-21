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
package tigase.xmpp.impl.roster;

import tigase.annotations.TigaseDeprecated;
import tigase.db.TigaseDBException;
import tigase.util.stringprep.TigaseStringprepException;
import tigase.xml.Element;
import tigase.xmpp.NotAuthorizedException;
import tigase.xmpp.XMPPResourceConnection;
import tigase.xmpp.jid.JID;

import java.util.function.Function;
import java.util.logging.Logger;

/**
 * Describe class Roster here.
 * <br>
 * Created: Tue Feb 21 18:05:53 2006
 *
 * @deprecated RosterFlat should be used instead
 */
@Deprecated
@TigaseDeprecated(since = "7.1.0", removeIn = "8.1.0")
public class Roster
		extends RosterAbstract {

	private static Logger log = Logger.getLogger("tigase.xmpp.impl.Roster");

	@Override
	public void addBuddy(XMPPResourceConnection session, JID jid, String name, String[] groups, SubscriptionType subscription, String otherData)
			throws NotAuthorizedException, TigaseDBException {
		String nick = name;

		if ((nick == null) || nick.trim().isEmpty()) {
			nick = jid.getLocalpart();

			if ((nick == null) || nick.trim().isEmpty()) {
				nick = jid.toString();
			}
		}

		session.setData(groupNode(jid), NAME, nick);
		String subscriptionTypeStr = subscription != null ? subscription.toString() : SubscriptionType.none.toString();
		session.setData(groupNode(jid), SUBSCRIPTION, subscriptionTypeStr);
		session.setDataList(groupNode(jid), GROUPS, groups);
	}

	@Override
	public boolean addBuddyGroup(XMPPResourceConnection session, JID buddy, String[] groups)
			throws NotAuthorizedException, TigaseDBException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public boolean containsBuddy(XMPPResourceConnection session, JID buddy)
			throws NotAuthorizedException, TigaseDBException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public JID[] getBuddies(final XMPPResourceConnection session) throws NotAuthorizedException, TigaseDBException {
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

	@Override
	public String[] getBuddyGroups(final XMPPResourceConnection session, JID buddy)
			throws NotAuthorizedException, TigaseDBException {
		return session.getDataList(groupNode(buddy), GROUPS);
	}

	@Override
	public String getBuddyName(final XMPPResourceConnection session, JID buddy)
			throws NotAuthorizedException, TigaseDBException {
		return session.getData(groupNode(buddy), NAME, null);
	}

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

	@Override
	public boolean isRosterLoaded(XMPPResourceConnection session) {
		return true;
	}

	@Override
	public boolean isOnline(XMPPResourceConnection session, JID jid) throws NotAuthorizedException, TigaseDBException {
		return true;
	}

	@Override
	public boolean presenceSent(XMPPResourceConnection session, JID jid)
			throws NotAuthorizedException, TigaseDBException {
		return false;
	}

	@Override
	public boolean removeBuddy(final XMPPResourceConnection session, JID jid)
			throws NotAuthorizedException, TigaseDBException {
		session.removeDataGroup(groupNode(jid));

		return true;
	}

	@Override
	public void setBuddyName(final XMPPResourceConnection session, JID buddy, final String name)
			throws NotAuthorizedException, TigaseDBException {
		session.setData(groupNode(buddy), NAME, name);
	}

	@Override
	public void setBuddySubscription(final XMPPResourceConnection session, final SubscriptionType subscription,
									 JID buddy) throws NotAuthorizedException, TigaseDBException {
		session.setData(groupNode(buddy), SUBSCRIPTION, subscription.toString());
	}

	@Override
	public void setOnline(XMPPResourceConnection session, JID jid, boolean online)
			throws NotAuthorizedException, TigaseDBException {
	}

	@Override
	public void setPresenceSent(XMPPResourceConnection session, JID jid, boolean sent)
			throws NotAuthorizedException, TigaseDBException {
	}

	@Override
	public RosterElement getRosterElement(XMPPResourceConnection session, JID jid)
			throws NotAuthorizedException, TigaseDBException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Function<JID, RosterElement> rosterElementProvider(XMPPResourceConnection session) {
		return null;
	}

	@Override
	public void logout(XMPPResourceConnection session) {
	}

	@Override
	public Element getCustomChild(XMPPResourceConnection session, JID buddy) {
		return null;
	}
}
