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
package tigase.xmpp.impl;

import tigase.kernel.beans.Initializable;
import tigase.kernel.beans.UnregisterAware;

import java.util.Iterator;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TokenBucketPool
		implements Initializable, UnregisterAware {

	private final static Logger log = Logger.getLogger(TokenBucketPool.class.getName());
	private final ConcurrentHashMap<String, TokenBucket> items = new ConcurrentHashMap<>();
	private boolean autoPurgeEnabled = true;
	private final TimerTask purgerTask = new TimerTask() {
		@Override
		public void run() {
			if (autoPurgeEnabled) {
				purge();
			}
		}
	};
	// unit: events
	private long defaultRate = 100000;
	private TimeUnit timeUnit = TimeUnit.SECONDS;
	// unit: ns
	private long defaultPer = timeUnit.toNanos(1);
	private Timer timer;

	public TokenBucketPool(long rate, long per, TimeUnit timeUnit) {
		this.defaultRate = rate;
		this.defaultPer = timeUnit.toNanos(per);
		this.timeUnit = timeUnit;
	}

	public TokenBucketPool(long rate, long per) {
		this.defaultRate = rate;
		this.defaultPer = timeUnit.toNanos(per);
	}

	public TokenBucketPool() {
	}

	public void setAutoPurgeEnabled(boolean enabled) {
		this.autoPurgeEnabled = enabled;
	}

	public TimeUnit getTimeUnit() {
		return timeUnit;
	}

	public void setTimeUnit(TimeUnit timeUnit) {
		this.timeUnit = timeUnit;
	}

	public long getDefaultRate() {
		return defaultRate;
	}

	public void setDefaultRate(long defaultRate) {
		this.defaultRate = defaultRate;
	}

	public long getDefaultPer() {
		return timeUnit.convert(defaultPer, TimeUnit.NANOSECONDS);
	}

	public void setDefaultPer(long defaultPer) {
		this.defaultPer = timeUnit.toNanos(defaultPer);
	}

	public boolean consume(final String key) {
		TokenBucket item = items.get(key);
		if (item == null) {
			item = new TokenBucket(System.nanoTime(), defaultRate, defaultPer);
			items.put(key, item);
		}

		return consume(item);
	}

	public void purge() {
		if (log.isLoggable(Level.FINE)) {
			log.fine("Purging full TokenBuckets...");
		}
		Iterator<Map.Entry<String, TokenBucket>> iterator = this.items.entrySet().iterator();
		final long current = System.nanoTime();

		while (iterator.hasNext()) {
			Map.Entry<String, TokenBucket> entry = iterator.next();
			if (entry.getValue().estimateAllowance(current) >= entry.getValue().rate) {
				iterator.remove();
			}
		}
	}

	@Override
	public void beforeUnregister() {
		if (timer == null) {
			timer.cancel();
		}
		timer = null;
	}

	@Override
	public void initialize() {
		if (timer != null) {
			beforeUnregister();
		}

		if (log.isLoggable(Level.FINE)) {
			log.fine("TokenBucketPool Created. Auto purge task created.");
		}

		timer = new Timer("TokenBuckerPoolTimerThread", true);

		timer.schedule(purgerTask, TimeUnit.HOURS.toMillis(4));
	}

	int size() {
		return this.items.size();
	}

	private boolean consume(TokenBucket item) {
		return item.consume();
	}

	/**
	 * Single TokenBucket.
	 */
	public static class TokenBucket {

		// unit: ns
		private final float per;
		// unit: events
		private final float rate;
		// unit: events
		private float allowance = 1;
		// unit: ns
		private long lastCheck;

		TokenBucket(long lastCheck, float rate, float per) {
			this.per = per;
			this.rate = rate;
			this.lastCheck = lastCheck;
		}

		/**
		 * Create Token Bucket.
		 *
		 * @param rate amount of available tokens
		 * @param per per nanosecond!
		 */
		public TokenBucket(long rate, long per) {
			this.lastCheck = System.nanoTime();
			this.per = per;
			this.rate = rate;
		}

		/**
		 * Consume token if available.
		 *
		 * @return {@code true} if token was available.
		 */
		public boolean consume() {
			final long current = System.nanoTime();
			updateAllowance(current);
			return consumeNoUpdate();
		}

		final boolean consumeNoUpdate() {
			if (this.allowance < 1.0) {
				return false;
			} else {
				this.allowance -= 1.0;
				return true;
			}
		}

		float getAllowance() {
			return allowance;
		}

		final float estimateAllowance(long current) {
			final float timePassed = current - this.lastCheck;
			this.lastCheck = current;
			return this.allowance + timePassed * (this.rate / this.per);
		}

		final void updateAllowance(long current) {
			final long timePassed = current - this.lastCheck;
			this.lastCheck = current;
			this.allowance += timePassed * (this.rate / this.per);
			if (this.allowance > this.rate) {
				this.allowance = this.rate; // throttle
			}

		}
	}

}
