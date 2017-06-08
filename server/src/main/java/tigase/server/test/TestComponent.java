/*
 * TestComponent.java
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2012 "Artur Hefczyc" <artur.hefczyc@tigase.org>
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
 *
 */

package tigase.server.test;

import tigase.component.AbstractKernelBasedComponent;
import tigase.component.modules.impl.DiscoveryModule;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.config.ConfigField;
import tigase.kernel.core.Kernel;
import tigase.server.Packet;
import tigase.stats.StatisticsList;

import javax.script.Bindings;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A test component used to demonstrate API and for running different kinds of
 * tests on the Tigase server - generate local traffic for performance and load tests.
 * Created: Nov 28, 2009 9:22:36 PM
 */
@Bean(name = "test", parent = Kernel.class, active = false)
public class TestComponent
		extends AbstractKernelBasedComponent {

	private static final Logger log = Logger.getLogger(TestComponent.class.getName());

	@Inject(nullAllowed = true)
	private TestSpamModule spamTestModule;

	@ConfigField(desc = "Number of processing threads", alias = "processing-threads-count")
	private int threadsNumber = Runtime.getRuntime().availableProcessors();

	@Override
	public String getDiscoCategoryType() {
		return "spam";
	}

	@Override
	public String getDiscoDescription() {
		return "Spam filtering";
	}

	@Override
	public void getStatistics(StatisticsList list) {
		super.getStatistics(list);
		if (null != spamTestModule) {
			list.add(getName(), "Spam messages found", spamTestModule.getTotalSpamCounter(), Level.INFO);
			list.add(getName(), "All messages processed", spamTestModule.getMessagesCounter(), Level.FINE);
		}
		if (list.checkLevel(Level.FINEST)) {
			// Some very expensive statistics generation code...
		}
	}

	@Override
	public int hashCodeForPacket(Packet packet) {
		if (packet.getElemTo() != null) {
			return packet.getElemTo().hashCode();
		}
		// This should not happen, every packet must have a destination
		// address, but maybe our SPAM checker is used for checking
		// strange kind of packets too....
		if (packet.getElemFrom() != null) {
			return packet.getElemFrom().hashCode();
		}
		// If this really happens on your system you should look carefully
		// at packets arriving to your component and decide a better way
		// to calculate hashCode
		return 1;
	}

	@Override
	public void initBindings(Bindings binds) {
		super.initBindings(binds);
		if (null != spamTestModule) {
			spamTestModule.initBindings(binds);
		}
	}

	@Override
	public boolean isDiscoNonAdmin() {
		return true;
	}

	@Override
	public int processingInThreads() {
		return threadsNumber;
	}

	@Override
	public int processingOutThreads() {
		return threadsNumber;
	}

	@Override
	protected void registerModules(Kernel kernel) {
		kernel.registerBean("disco").asClass(DiscoveryModule.class).exec();
	}
}
