/*
 * RegistratBeanCyclicTest.java
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

import org.junit.Assert;
import org.junit.Test;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.RegistrarBean;
import tigase.kernel.core.Kernel;
import tigase.kernel.core.PlantUMLGrapher;
import tigase.kernel.core.RegistrarKernel;

import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RegistratBeanCyclicTest {

	@Test
	public void test01() {
		Logger logger = Logger.getLogger("tigase");
		Handler handler = new ConsoleHandler();
		handler.setLevel(Level.ALL);
		logger.addHandler(handler);
		logger.setLevel(Level.ALL);

		Kernel k = new RegistrarKernel();
		k.setName("root");
		k.registerBean(A.class).exec();

		PlantUMLGrapher gr = new PlantUMLGrapher(k);

		A a = k.getInstance(A.class);
		B b = a.b;
		C c = a.b.c;
		Assert.assertSame(a, b.a);
		Assert.assertSame(a, c.a);
		Assert.assertSame(b, ((Kernel) k.getInstance("a#KERNEL")).getInstance("b"));
		Assert.assertSame(c, ((Kernel) ((Kernel) k.getInstance("a#KERNEL")).getInstance("b#KERNEL")).getInstance("c"));

		System.out.println(gr.getDependencyGraph());
	}

	@Bean(name = "a")
	public static class A
			implements RegistrarBean {

		@Inject
		B b;

		public void register(Kernel kernel) {
			kernel.registerBean(B.class).exec();
		}

		@Override
		public void unregister(Kernel kernel) {
		}
	}

	@Bean(name = "b")
	public static class B
			implements RegistrarBean {

		@Inject
		A a;

		@Inject
		C c;

		public void register(Kernel kernel) {
			kernel.registerBean(C.class).exec();
			kernel.getParent().ln("service", kernel, "a");
		}

		@Override
		public void unregister(Kernel kernel) {
		}
	}

	@Bean(name = "c")
	public static class C {

		@Inject
		A a;
	}
}


