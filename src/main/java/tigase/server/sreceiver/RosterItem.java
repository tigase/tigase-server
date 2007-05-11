/*  Tigase Project
 *  Copyright (C) 2001-2007
 *  "Artur Hefczyc" <artur.hefczyc@tigase.org>
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
package tigase.server.sreceiver;

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

	private String jid = null;
	private boolean online = false;
	private boolean subscribed = false;
	private boolean owner = false;
	private boolean admin = false;

	/**
	 * Creates a new <code>RosterItem</code> instance.
	 *
	 */
	public RosterItem(String jid) {
		this.jid = jid;
	}

	/**
	 * Gets the value of jid
	 *
	 * @return the value of jid
	 */
	public String getJid() {
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

} // RosterItem
