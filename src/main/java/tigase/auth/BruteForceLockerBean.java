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
package tigase.auth;

import tigase.eventbus.EventBus;
import tigase.eventbus.HandleEvent;
import tigase.kernel.DefaultTypesConverter;
import tigase.kernel.TypesConverter;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Initializable;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.UnregisterAware;
import tigase.map.ClusterMapFactory;
import tigase.server.xmppsession.SessionManager;
import tigase.stats.ComponentStatisticsProvider;
import tigase.stats.StatisticsList;
import tigase.vhosts.VHostItem;
import tigase.xmpp.XMPPResourceConnection;
import tigase.xmpp.jid.BareJID;
import tigase.xmpp.jid.JID;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import static tigase.auth.BruteForceLockerBean.Mode.IpJid;
import static tigase.auth.BruteForceLockerBean.Mode.valueOf;

@Bean(name = "brute-force-locker", parent = SessionManager.class, active = true)
public class BruteForceLockerBean
		implements Initializable, UnregisterAware, ComponentStatisticsProvider {

	private final static String ANY = "*";

	private static final String LOCK_ENABLED_KEY = "brute-force-lock-enabled";
	private static final String LOCK_AFTER_FAILS_KEY = "brute-force-lock-after-fails";
	private static final String LOCK_DISABLE_ACCOUNT_FAILS_KEY = "brute-force-disable-after-fails";
	private static final String LOCK_TIME_KEY = "brute-force-lock-time";
	private static final String LOCK_PERIOD_TIME_KEY = "brute-force-period-time";
	private static final String LOCK_MODE_KEY = "brute-force-mode";
	private static final String MAP_TYPE = "brute-force-invalid-logins";

	public enum Mode {
		Ip,
		IpJid,
		Jid
	}

	static {
		List<VHostItem.DataType> types = new ArrayList<>();
		types.add(new VHostItem.DataType(LOCK_ENABLED_KEY, "Brute Force Prevention Enabled", Boolean.class,
										 Boolean.TRUE));
		types.add(new VHostItem.DataType(LOCK_AFTER_FAILS_KEY, "Number of allowed invalid login", Long.class, 3L));
		types.add(
				new VHostItem.DataType(LOCK_DISABLE_ACCOUNT_FAILS_KEY, "Disable account after failed login", Long.class,
									   20L));
		types.add(
				new VHostItem.DataType(LOCK_PERIOD_TIME_KEY, "Failed login in period of time [sec]", Long.class, 60L));
		types.add(new VHostItem.DataType(LOCK_TIME_KEY, "Lock time [sec]", Long.class, 60L));
		types.add(new VHostItem.DataType(LOCK_MODE_KEY, "Brute Force Prevention Mode", Mode.class, IpJid));
		VHostItem.registerData(types);
	}

	private final Logger log = Logger.getLogger(this.getClass().getName());
	private final Map<String, StatHolder> otherStatHolders = new ConcurrentHashMap<>();
	private final StatHolder statHolder = new StatHolder();
	@Inject
	private EventBus eventBus;
	private Map<Key, Value> map;
	@Inject
	private SessionManager sessionManager;

	public static String getClientIp(XMPPResourceConnection session) {
		try {
			return Optional.ofNullable(session.getConnectionId())
					.map(JID::getResource)
					.map(res -> res.split("_")[2])
					.orElse(null);
		} catch (Exception e) {
			return null;
		}
	}

	public void addInvalidLogin(XMPPResourceConnection session, String ip, BareJID jid) {
		addInvalidLogin(session, ip, jid, System.currentTimeMillis());
	}

	public void addInvalidLogin(XMPPResourceConnection session, String ip, BareJID jid, final long currentTime) {
		if (ip == null) {
			if (log.isLoggable(Level.FINEST)) {
				log.finest("IP is null. Skip adding entry.");
			}
			return;
		}
		if (map == null) {
			log.warning("Brute Force Locker is no initialized yet!");
			return;
		}

		final Key key = createKey(session, ip, jid);
		Value value = map.get(key);

		if (value == null) {
			value = new Value(session != null ? session.getDomain().getVhost().toString() : null, ip, jid);
			value.setBadLoginCounter(0);

			if (log.isLoggable(Level.FINEST)) {
				log.finest("Entry didn't exists. Create new one.");
			}
		}

		if (value.getInvalidateAtTime() < currentTime) {
			if (log.isLoggable(Level.FINEST)) {
				log.finest("Entry exists and is old, reset counter.");
			}
			value.setBadLoginCounter(0);
		}

		value.setBadLoginCounter(value.getBadLoginCounter() + 1);

		final long lockAfterFails = session == null ? 3 : (long) session.getDomain().getData(LOCK_AFTER_FAILS_KEY);
		if (value.getBadLoginCounter() <= lockAfterFails) {
			final long periodTime =
					(session == null ? 10 : (long) session.getDomain().getData(LOCK_PERIOD_TIME_KEY)) * 1000;
			value.setInvalidateAtTime(currentTime + periodTime);
		} else {
			final long lockTime = (session == null ? 10 : (long) session.getDomain().getData(LOCK_TIME_KEY)) * 1000;
			value.setInvalidateAtTime(currentTime + lockTime);
		}

		if (log.isLoggable(Level.FINEST)) {
			log.finest(
					"New invalidate time for " + key + " == " + value.getInvalidateAtTime() + "; getBadLoginCounter " +
							"== " + value.getBadLoginCounter());
		}

		map.put(key, value);

		addToStatistic(value);
	}

	public boolean canUserBeDisabled(XMPPResourceConnection session, String ip, BareJID jid) {
		final Key key = createKey(session, ip, jid);

		if (!key.isJIDPresent()) {
			return false;
		}

		Value value = map.get(key);

		if (value == null) {
			return false;
		}

		final long disableAfterFails =
				session == null ? 20 : (long) session.getDomain().getData(LOCK_DISABLE_ACCOUNT_FAILS_KEY);

		if (disableAfterFails == 0) {
			return false;
		} else {
			return value.getBadLoginCounter() > disableAfterFails;
		}
	}

	public void clearAll() {
		if (map == null) {
			log.warning("Brute Force Locker is no initialized yet!");
			return;
		}
		map.clear();
	}

	public void clearOutdated() {
		clearOutdated(System.currentTimeMillis());
	}

	public void clearOutdated(final long currentTime) {
		if (map == null) {
			log.warning("Brute Force Locker is no initialized yet!");
			return;
		}

		final HashSet<Key> toRemove = new HashSet<>();

		map.forEach((key, value) -> {
			if (value.getInvalidateAtTime() < currentTime) {
				toRemove.add(key);
			}
		});
		toRemove.forEach(key -> map.remove(key));
	}

	@Override
	public void getStatistics(String compName, StatisticsList list) {
		clearOutdated();
		final String keyName = compName + "/BruteForceLocker";
		ArrayList<Value> l = new ArrayList<>(this.map.values());
		for (Value value : l) {
			list.add(keyName, "Present locks: " + value.jid + " from " + value.ip, value.badLoginCounter, Level.FINER);
		}

		final StatHolder tmp = new StatHolder();

		this.statHolder.ips.forEach((ip, count) -> tmp.addIP(ip, count));
		this.statHolder.jids.forEach((jid, count) -> tmp.addJID(jid, count));

		this.otherStatHolders.values().forEach(otherSH -> {
			otherSH.ips.forEach((ip, count) -> tmp.addIP(ip, count));
			otherSH.jids.forEach((jid, count) -> tmp.addJID(jid, count));
		});

		tmp.ips.forEach((ip, count) -> list.add(keyName, "From IP: " + ip, count, Level.INFO));
		tmp.jids.forEach((jid, count) -> list.add(keyName, "For JID: " + jid, count, Level.INFO));
	}

	@Override
	public void initialize() {
		this.map = ClusterMapFactory.get().createMap(MAP_TYPE, Key.class, Value.class);
		assert this.map != null : "Distributed Map is NULL!";
		assert this.sessionManager != null : "SessionManager is NULL!";

		if (eventBus != null) {
			eventBus.registerAll(this);
		}
	}

	public boolean isEnabled(XMPPResourceConnection session) {
		return session == null || (boolean) session.getDomain().getData(LOCK_ENABLED_KEY);
	}

	@HandleEvent(filter = HandleEvent.Type.remote)
	public void handleStatisticsEmitEvent(StatisticsEmitEvent event) {
		if (event.getNodeName() == null) {
			return;
		}

		this.otherStatHolders.put(event.getNodeName(), event.getStatHolder());
	}

	public boolean isLoginAllowed(XMPPResourceConnection session, final String ip, final BareJID jid) {
		return isLoginAllowed(session, ip, jid, System.currentTimeMillis());
	}

	public boolean isLoginAllowed(XMPPResourceConnection session, final String ip, final BareJID jid,
								  final long currentTime) {
		if (ip == null) {
			if (log.isLoggable(Level.FINEST)) {
				log.finest("IP is null. Return true.");
			}
			return true;
		}
		if (map == null) {
			log.warning("Brute Force Locker is no initialized yet!");
			return false;
		}

		final Key key = createKey(session, ip, jid);
		Value value = map.get(key);

		if (value == null) {
			if (log.isLoggable(Level.FINEST)) {
				log.finest("No entry for " + key + ". Return true.");
			}
			return true;
		}

		return isLoginAllowed(session, key, value, currentTime);
	}

	@Override
	public void beforeUnregister() {
		eventBus.unregisterAll(this);
	}

	@Override
	public void everyHour() {

	}

	@Override
	public void everyMinute() {
//		String clusterNode = DNSResolverFactory.getInstance().getDefaultHost();
		String clusterNode = sessionManager.getComponentId().getDomain();
		eventBus.fire(new StatisticsEmitEvent(clusterNode, this.statHolder));
	}

	@Override
	public void everySecond() {

	}

	void setMap(HashMap<Key, Value> map) {
		this.map = map;
	}

	final Key createKey(XMPPResourceConnection session, String ip, BareJID jid) {
		final Mode mode = session == null ? Mode.IpJid : valueOf(session.getDomain().getData(LOCK_MODE_KEY));
		return createKey(mode, session, ip, jid);
	}

	final Key createKey(final Mode mode, XMPPResourceConnection session, String ip, BareJID jid) {
		final String domain = session == null ? ANY : session.getDomain().getVhost().toString();
		switch (mode) {
			case Jid:
				return new Key(ANY, jid == null ? ANY : jid.toString(), domain);
			case Ip:
				return new Key(ip == null ? ANY : ip, ANY, domain);
			case IpJid:
				return new Key(ip == null ? ANY : ip, jid == null ? ANY : jid.toString(), domain);
			default:
				throw new RuntimeException("Unknown mode " + mode);
		}
	}

	private void addToStatistic(Value v) {
		this.statHolder.addIP(v.ip);
		this.statHolder.addJID(v.jid);
	}

	private boolean isLoginAllowed(final XMPPResourceConnection session, final Key key, final Value value,
								   final long currentTime) {
		if (value.getInvalidateAtTime() < currentTime) {
			map.remove(key);
			if (log.isLoggable(Level.FINEST)) {
				log.finest("Entry existed, but was too old. Entry removed. Return true.");
			}
			return true;
		} else {
			long lockAfterFails = session == null ? 3 : (long) session.getDomain().getData(LOCK_AFTER_FAILS_KEY);
			boolean r = value.badLoginCounter <= lockAfterFails;
			if (log.isLoggable(Level.FINEST)) {
				log.finest("Entry exist. lockAfterFails=" + lockAfterFails + ", value" + ".badLoginCounter=" +
								   value.badLoginCounter + ", result=" + r);
			}
			return r;
		}
	}

	public static class Key
			implements TypesConverter.Parcelable {

		private String domain;
		private String ip;
		private String jid;

		public Key() {
		}

		public Key(String ip, String jid, String domain) {
			this.ip = ip;
			this.jid = jid;
			this.domain = domain;
		}

		@Override
		public String[] encodeToStrings() {
			return new String[]{jid, ip, domain};
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}

			Key key = (Key) o;

			if (!domain.equals(key.domain)) {
				return false;
			}
			if (!ip.equals(key.ip)) {
				return false;
			}
			return jid.equals(key.jid);
		}

		@Override
		public void fillFromString(String[] encoded) {
			this.jid = encoded[0];
			this.ip = encoded[1];
			this.domain = encoded[2];
		}

		public String getIp() {
			return ip;
		}

		public void setIp(String ip) {
			this.ip = ip;
		}

		public String getJid() {
			return jid;
		}

		public void setJid(String jid) {
			this.jid = jid;
		}

		@Override
		public int hashCode() {
			int result = domain.hashCode();
			result = 31 * result + ip.hashCode();
			result = 31 * result + jid.hashCode();
			return result;
		}

		public boolean isJIDPresent() {
			return jid != null && !jid.equals(ANY);
		}

		@Override
		public String toString() {
			return "Key[ip=" + ip + ", jid=" + jid + ", domain=" + domain + "]";
		}

	}

	public static class LoginLockedException
			extends Exception {

	}

	public static class StatHolder
			implements TypesConverter.Parcelable {

		private final Map<String, Integer> ips = new ConcurrentHashMap<>();
		private final Map<BareJID, Integer> jids = new ConcurrentHashMap<>();
		private final DefaultTypesConverter typesConverter = new DefaultTypesConverter();

		public Map<String, Integer> getIps() {
			return ips;
		}

		public Map<BareJID, Integer> getJids() {
			return jids;
		}

		public void clear() {
			ips.clear();
			jids.clear();
		}

		public int addIP(String ip) {
			return add(ips, ip, 1);
		}

		public int addJID(BareJID jid) {
			return add(jids, jid, 1);
		}

		public int addIP(String ip, int value) {
			return add(ips, ip, value);
		}

		public int addJID(BareJID jid, int value) {
			return add(jids, jid, value);
		}

		@Override
		public String[] encodeToStrings() {
			String[] r = new String[2 + ips.size() * 2 + jids.size() * 2];
			r[0] = String.valueOf(ips.size());
			r[1] = String.valueOf(jids.size());

			fillTab(ips, r, 2);
			fillTab(jids, r, 2 + ips.size() * 2);

			return r;
		}

		@Override
		public void fillFromString(String[] encoded) {
			try {
				final int lenIps = Integer.parseInt(encoded[0]);
				final int lenJids = Integer.parseInt(encoded[1]);

				ips.clear();
				ips.putAll(read(encoded, 2, lenIps, key -> key));

				jids.clear();
				jids.putAll(read(encoded, 2 + lenIps * 2, lenJids, BareJID::bareJIDInstanceNS));

			} catch (Exception e) {
				throw new RuntimeException("Cannot decode parcel: " + Arrays.toString(encoded), e);
			}
		}

		private <T> HashMap<T, Integer> read(final String[] src, final int offset, final int len,
											 Function<String, T> keyCnv) {
			HashMap<T, Integer> r = new HashMap<>();
			for (int i = 0; i < len; i++) {
				r.put(keyCnv.apply(src[offset + 2 * i]), Integer.parseInt(src[offset + 2 * i + 1]));
			}
			return r;
		}

		private <T> void fillTab(Map<T, Integer> src, String[] dst, final int offset) {
			int idx = offset;
			for (Map.Entry<T, Integer> x : src.entrySet()) {
				String key = x.getKey().toString();
				Integer value = x.getValue();
				dst[idx] = key;
				dst[idx + 1] = String.valueOf(value);
				idx = idx + 2;
			}
		}

		private <T> int add(Map<T, Integer> map, T key, int value) {
			synchronized (map) {
				Integer v = map.get(key);
				v = (v == null ? 0 : v) + value;
				map.put(key, v);
				return v;
			}
		}

	}

	public static class StatisticsEmitEvent
			implements Serializable {

		private String nodeName;

		private StatHolder statHolder;

		public StatisticsEmitEvent() {
		}

		public StatisticsEmitEvent(String nodeName, StatHolder statHolder) {
			this.nodeName = nodeName;
			this.statHolder = statHolder;
		}

		public String getNodeName() {
			return nodeName;
		}

		public void setNodeName(String nodeName) {
			this.nodeName = nodeName;
		}

		public StatHolder getStatHolder() {
			return statHolder;
		}

		public void setStatHolder(StatHolder statHolder) {
			this.statHolder = statHolder;
		}
	}

	public static class Value
			implements TypesConverter.Parcelable {

		private int badLoginCounter;
		private String domain;
		/** Invalidate this value at specific time */
		private long invalidateAtTime;
		private String ip;
		private BareJID jid;

		public Value() {
		}

		public Value(String domain, String ip, BareJID jid) {
			this.domain = domain;
			this.ip = ip;
			this.jid = jid;
		}

		@Override
		public String[] encodeToStrings() {
			return new String[]{Integer.toString(badLoginCounter), Long.toString(invalidateAtTime)};
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}

			Value value = (Value) o;

			if (badLoginCounter != value.badLoginCounter) {
				return false;
			}
			return invalidateAtTime == value.invalidateAtTime;
		}

		@Override
		public void fillFromString(String[] encoded) {
			this.badLoginCounter = Integer.valueOf(encoded[0]);
			this.invalidateAtTime = Long.valueOf(encoded[1]);
		}

		public int getBadLoginCounter() {
			return badLoginCounter;
		}

		public void setBadLoginCounter(int badLoginCounter) {
			this.badLoginCounter = badLoginCounter;
		}

		public long getInvalidateAtTime() {
			return invalidateAtTime;
		}

		public void setInvalidateAtTime(long invalidateAtTime) {
			this.invalidateAtTime = invalidateAtTime;
		}

		@Override
		public int hashCode() {
			int result = badLoginCounter;
			result = 31 * result + (int) (invalidateAtTime ^ (invalidateAtTime >>> 32));
			return result;
		}

	}

}
