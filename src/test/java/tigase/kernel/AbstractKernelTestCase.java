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

import org.junit.After;
import org.junit.Before;
import tigase.kernel.core.Kernel;
import tigase.util.reflection.ClassUtilBean;

/**
 * Class is a base class for tests requiring usage of Kernel instances.
 */
public class AbstractKernelTestCase {

	private Kernel kernel;

	@Before
	public void setupKernel() {
		kernel = new Kernel();
		registerBeans(kernel);
	}

	@After
	public void tearDownKernel() {
		kernel.gc();
		kernel = null;
	}

	protected void registerBeans(Kernel kernel) {
		kernel.registerBean("classUtilBean")
				.asInstance(ClassUtilBean.getInstance())
				.exportable()
				.setActive(true)
				.exec();
	}

	protected <T> T getInstance(Class<T> clazz) {
		return kernel.getInstance(clazz);
	}

	protected <T> T getInstance(String name) {
		return kernel.getInstance(name);
	}

	protected Kernel getKernel() {
		return kernel;
	}

}
