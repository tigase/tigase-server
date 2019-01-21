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

import tigase.kernel.beans.Inject;

public class Bean2 {

	@Inject
	private Bean3 bean3;

	@Inject(bean = "bean4_1", nullAllowed = true)
	private Bean4 bean4;

	public Bean3 getBean3() {
		return bean3;
	}

	public void setBean3(Bean3 bean3) {
		this.bean3 = bean3;
	}

	public Bean4 getBean4() {
		return bean4;
	}

	public void setBean4(Bean4 bean4) {
		this.bean4 = bean4;
	}

}
