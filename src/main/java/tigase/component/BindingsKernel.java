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
package tigase.component;

import tigase.kernel.core.Kernel;

import javax.script.SimpleBindings;

public class BindingsKernel
		extends SimpleBindings {

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
