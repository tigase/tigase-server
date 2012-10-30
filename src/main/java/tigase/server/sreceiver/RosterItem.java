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
package tigase.server.sreceiver;

import tigase.xmpp.JID;

/**
 * Describe class RosterItem here.
 *
 *
 * Created: Fri May 11 22:46:58 2007
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class RosterItem {

	private JID jid = null;
	private boolean online = false;
	private boolean subscribed = false;
	private boolean moderation_accepted = false;
	private boolean owner = false;
	private boolean admin = false;

	/**
	 * Creates a new <code>RosterItem</code> instance.
	 *
	 */
	public RosterItem(JID jid) {
		this.jid = jid;
	}

	/**
	 * Gets the value of jid
	 *
	 * @return the value of jid
	 */
	public JID getJid() {
		return this.jid;
	}

	/**
	 * Gets the value of online
	 *
	 * @return the value of online
	 */
	public boolean isOnline() {
		return this.online;
	}

	/**
	 * Sets the value of online
	 *
	 * @param argOnline Value to assign to this.online
	 */
	public void setOnline(final boolean argOnline) {
		this.online = argOnline;
	}

	/**
	 * Gets the value of subscribed
	 *
	 * @return the value of subscribed
	 */
	public boolean isSubscribed() {
		return this.subscribed;
	}

	/**
	 * Sets the value of subscribed
	 *
	 * @param argSubscribed Value to assign to this.subscribed
	 */
	public void setSubscribed(final boolean argSubscribed) {
		this.subscribed = argSubscribed;
	}

	/**
	 * Gets the value of owner
	 *
	 * @return the value of owner
	 */
	public boolean isOwner() {
		return this.owner;
	}

	/**
	 * Sets the value of owner
	 *
	 * @param argOwner Value to assign to this.owner
	 */
	public void setOwner(final boolean argOwner) {
		this.owner = argOwner;
	}

	/**
	 * Gets the value of admin
	 *
	 * @return the value of admin
	 */
	public boolean isAdmin() {
		return this.admin;
	}

	/**
	 * Sets the value of admin
	 *
	 * @param argAdmin Value to assign to this.admin
	 */
	public void setAdmin(final boolean argAdmin) {
		this.admin = argAdmin;
	}

	/**
	 * Gets the value of moderation_accepted
	 *
	 * @return the value of moderation_accepted
	 */
	public boolean isModerationAccepted() {
		return this.moderation_accepted;
	}

	/**
	 * Sets the value of moderation_accepted
	 *
	 * @param argModeration_accepted Value to assign to this.moderation_accepted
	 */
	public void setModerationAccepted(final boolean argModeration_accepted) {
		this.moderation_accepted = argModeration_accepted;
	}

} // RosterItem
