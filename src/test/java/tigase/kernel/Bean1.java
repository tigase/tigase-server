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
package tigase.kernel;

import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;

import java.util.Set;

@Bean(name = "bean1", active = true)
public class Bean1 {

	@Inject(nullAllowed = true)
	private Bean2 bean2;

	@Inject(nullAllowed = true)
	private Bean3 bean3;

	@Inject(type = Special.class, nullAllowed = true)
	private Set<Special> collectionOfSpecial;

	@Inject(nullAllowed = true)
	private Special[] tableOfSpecial;

	public Bean2 getBean2() {
		return bean2;
	}

	public void setBean2(Bean2 bean2) {
		this.bean2 = bean2;
	}

	public Bean3 getBean3() {
		return bean3;
	}

	public void setBean3(Bean3 bean3) {
		this.bean3 = bean3;
	}

	public Set<Special> getCollectionOfSpecial() {
		return collectionOfSpecial;
	}

	public void setCollectionOfSpecial(Set<Special> xxx) {
		this.collectionOfSpecial = xxx;
	}

	public Special[] getTableOfSpecial() {
		return tableOfSpecial;
	}

	public void setTableOfSpecial(Special[] ss) {
		this.tableOfSpecial = ss;
	}

}
