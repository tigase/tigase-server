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
package tigase.kernel.modular;

import org.junit.Assert;
import org.junit.Test;
import tigase.TestLogger;
import tigase.component.DSLBeanConfigurator;
import tigase.kernel.DefaultTypesConverter;
import tigase.kernel.core.RegistrarKernel;
import tigase.kernel.modular.c1.Component1Registrar;
import tigase.kernel.modular.c2.Component2Registrar;

import java.util.Collection;
import java.util.logging.Logger;

public class ApplicationTest {

	private static final Logger log = TestLogger.getLogger(ApplicationTest.class);

	public ApplicationTest() {
	}

	@Test
	public void testBootstrapModules() {
		final RegistrarKernel krnl = new RegistrarKernel();
		krnl.setName("root");
		krnl.registerBean(DefaultTypesConverter.class).exportable().exec();
		krnl.registerBean(DSLBeanConfigurator.class).exportable().exec();

		// registering our main class of application
		krnl.registerBean(ComponentsManager.class).exec();

		// registering components
		krnl.registerBean(Component1Registrar.class).exec();
		krnl.registerBean(Component2Registrar.class).exec();

		// starting application
		final ComponentsManager componentsManager = krnl.getInstance(ComponentsManager.class);

		// Two components should be injected to ComponentsManager.
		Assert.assertNotNull(componentsManager.getComponents());
		Assert.assertEquals(2, componentsManager.getComponents().length);

		Collection<String> response = componentsManager.process("call1");
		Assert.assertTrue(response.contains("response:Component1(h:call1)"));
		Assert.assertTrue(response.contains("response:Component2(call1)"));
	}

}
