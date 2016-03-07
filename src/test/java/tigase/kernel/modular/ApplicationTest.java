/*
 * ApplicationTest.java
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

package tigase.kernel.modular;

import org.junit.Assert;
import org.junit.Test;
import tigase.component.PropertiesBeanConfigurator;
import tigase.kernel.core.RegistrarKernel;
import tigase.kernel.modular.c1.Component1Registrar;
import tigase.kernel.modular.c2.Component2Registrar;

import java.util.Collection;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ApplicationTest {

	public ApplicationTest() {
		Logger logger = Logger.getLogger("tigase.kernel");

		// create a ConsoleHandler
		Handler handler = new ConsoleHandler();
		handler.setLevel(Level.ALL);
		logger.addHandler(handler);
		logger.setLevel(Level.ALL);

		if (logger.isLoggable(Level.CONFIG))
			logger.config("Logger successfully initialized");
	}

	@Test
	public void testBootstrapModules() {
		final RegistrarKernel krnl = new RegistrarKernel();
		krnl.setName("root");
		krnl.registerBean(PropertiesBeanConfigurator.class).exec();

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
		Assert.assertTrue(response.contains("response:Component1(call1)"));
		Assert.assertTrue(response.contains("response:Component2(call1)"));
	}

}
