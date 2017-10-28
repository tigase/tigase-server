/*
 * BindingsKernel.java
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2017 "Tigase, Inc." <office@tigase.com>
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

package tigase.component;

import javax.script.SimpleBindings;

import tigase.kernel.core.Kernel;

public class BindingsKernel extends SimpleBindings {

	private Kernel kernel;

	public BindingsKernel(Kernel kernel) {
		this.kernel = kernel;
	}

	public BindingsKernel() {
	}

	@Override
	public boolean containsKey(Object key) {
		boolean v = super.containsKey(key);
		if (!v) {
			v = kernel.isBeanClassRegistered(key.toString());
		}
		return v;
	}

	@Override
	public Object get(Object key) {
		Object v = super.get(key);
		if (v == null && kernel.isBeanClassRegistered(key.toString())) {
			v = kernel.getInstance(key.toString());
		}
		return v;
	}

	public Kernel getKernel() {
		return kernel;
	}

	public void setKernel(Kernel kernel) {
		this.kernel = kernel;
	}
}
