/*
 * Bean10.java
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2016 "Tigase, Inc." <office@tigase.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
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

package tigase.kernel;

import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;

import java.util.Set;

@Bean(name = "bean10", active = true)
public class Bean10 {

	@Inject(type = Special.class, nullAllowed = true)
	private Set<Special> collectionOfSpecial;

	@Inject(nullAllowed = true)
	private Special[] tableOfSpecial;

	public Set<Special> getCollectionOfSpecial() {
		return collectionOfSpecial;
	}

	public void setCollectionOfSpecial(Set<Special> collectionOfSpecial) {
		this.collectionOfSpecial = collectionOfSpecial;
	}

	public Special[] getTableOfSpecial() {
		return tableOfSpecial;
	}

	public void setTableOfSpecial(Special[] tableOfSpecial) {
		this.tableOfSpecial = tableOfSpecial;
	}
}
