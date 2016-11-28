/*
 * RegistrationThrottling.java
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
 *
 */
package tigase.server.xmppclient;

import tigase.kernel.beans.Bean;
import tigase.kernel.beans.UnregisterAware;
import tigase.kernel.beans.config.ConfigField;
import tigase.kernel.core.Kernel;
import tigase.server.Packet;
import tigase.xmpp.XMPPIOService;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by andrzej on 19.11.2016.
 */
@Bean(name = RegistrationThrottling.ID, parent = Kernel.class, active = false, exportable = true)
public class RegistrationThrottling implements UnregisterAware {

	public static final String ID = "registration-throttling";
	@ConfigField(desc = "Limit of allowed account registrations for IP in specified period")
	protected Integer limit = 4;
	@ConfigField(desc = "Period for which limit is set")
	protected Duration period = Duration.ofDays(1);
	private AtomicBoolean cleanUpScheduled = new AtomicBoolean(false);
	private ConcurrentHashMap<String, List<Long>> registrations = new ConcurrentHashMap<>();
	private Timer timer = new Timer("registration-timer", true);

	public void startFor(Kernel kernel) {
		kernel.registerBean(RegistrationThrottlingProcessor.class).exec();
	}

	public void stopFor(Kernel kernel) {
		kernel.unregister(RegistrationThrottlingProcessor.ID);
	}

	@Override
	public void beforeUnregister() {
		timer.cancel();
	}

	protected boolean checkLimits(XMPPIOService service) {
		List<Long> registrationTimes = registrations.computeIfAbsent(service.getRemoteAddress(),
																	 (k) -> new ArrayList<Long>());
		synchronized (registrationTimes) {
			cleanUp(registrationTimes);

			if (registrationTimes.size() <= limit) {
				registrationTimes.add(System.currentTimeMillis());
			}

			return registrationTimes.size() <= limit;
		}
	}

	protected boolean checkLimits(XMPPIOService service, Packet packet) {
		boolean result = checkLimits(service);
		scheduleCleanUpIfNeeded();
		return result;
	}

	protected void cleanUp(List<Long> registrationTimes) {
		// Five seconds added to improve performance
		long oldestAllowed = (System.currentTimeMillis() - period.toMillis()) + 5000;
		registrationTimes.removeIf((ts) -> ts < oldestAllowed);
	}

	protected void cleanUpFromTimer() {
		Iterator<Map.Entry<String, List<Long>>> it = registrations.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<String, List<Long>> e = it.next();
			List<Long> registrationTimes = e.getValue();
			synchronized (registrationTimes) {
				cleanUp(registrationTimes);
				if (registrationTimes.isEmpty()) {
					it.remove();
				}
			}
		}
		Optional<Long> earliest = registrations.values().stream().flatMap(times -> times.stream()).min(Long::compare);
		if (earliest.isPresent()) {
			timer.schedule(new CleanUpTask(), System.currentTimeMillis() - earliest.get());
		} else {
			cleanUpScheduled.compareAndSet(true, false);
		}
	}

	protected void scheduleCleanUpIfNeeded() {
		if (cleanUpScheduled.compareAndSet(false, true)) {
			timer.schedule(new CleanUpTask(), period.toMillis());
		}
	}

	protected class CleanUpTask
			extends TimerTask {

		@Override
		public void run() {
			RegistrationThrottling.this.cleanUpFromTimer();
		}
	}
}

