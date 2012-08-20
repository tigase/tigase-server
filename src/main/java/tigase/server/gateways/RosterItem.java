/*  Tigase Jabber/XMPP Server
 *  Copyright (C) 2004-2012 "Artur Hefczyc" <artur.hefczyc@tigase.org>
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
 *
 * $Rev$
 * Last modified by $Author$
 * $Date$
 */
package tigase.server.gateways;

import java.util.List;

/**
 * Describe class RosterItem here.
 *
 *
 * Created: Tue Nov 13 18:41:27 2007
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class RosterItem {

	private String buddy_id = null;

	/**
	 * Describe name here.
	 */
	private String name;

	/**
	 * Describe subscription here.
	 */
	private String subscription;

	/**
	 * Describe status here.
	 */
	private UserStatus status;

	/**
	 * Describe groups here.
	 */
	private List<String> groups;

	/**
	 * Get the <code>Groups</code> value.
	 *
	 * @return a <code>List<String></code> value
	 */
	public List<String> getGroups() {
		return groups;
	}

	/**
	 * Set the <code>Groups</code> value.
	 *
	 * @param newGroups The new Groups value.
	 */
	public void setGroups(final List<String> newGroups) {
		this.groups = newGroups;
	}

	/**
	 * Get the <code>Status</code> value.
	 *
	 * @return an <code>UserStatus</code> value
	 */
	public UserStatus getStatus() {
		return status;
	}

	/**
	 * Set the <code>Status</code> value.
	 *
	 * @param newStatus The new Status value.
	 */
	public void setStatus(final UserStatus newStatus) {
		this.status = newStatus;
	}

	/**
	 * Get the <code>Subscription</code> value.
	 *
	 * @return a <code>String</code> value
	 */
	public String getSubscription() {
		return subscription;
	}

	/**
	 * Set the <code>Subscription</code> value.
	 *
	 * @param newSubscription The new Subscription value.
	 */
	public void setSubscription(final String newSubscription) {
		this.subscription = newSubscription;
	}
	/**
	 * Get the <code>Name</code> value.
	 *
	 * @return a <code>String</code> value
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set the <code>Name</code> value.
	 *
	 * @param newName The new Name value.
	 */
	public void setName(final String newName) {
		this.name = newName;
	}
	/**
	 * Creates a new <code>RosterItem</code> instance.
	 *
	 */
	public RosterItem(String buddy_id) {
		this.buddy_id = buddy_id;
	}

	public String getBuddyId() {
		return buddy_id;
	}

}
