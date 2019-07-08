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
import tigase.kernel.beans.config.ConfigField;
import tigase.map.ClusterMapFactory;
import tigase.server.Command;
import tigase.server.DataForm;
import tigase.server.Packet;
import tigase.server.xmppsession.SessionManager;
import tigase.stats.ComponentStatisticsProvider;
import tigase.stats.StatisticsList;
import tigase.vhosts.*;
import tigase.xml.Element;
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
		Jid;

		public Mode merge(Mode mode) {
			switch (this) {
				case Ip:
					if (mode == Ip) {
						return Ip;
					}
					return IpJid;
				case Jid:
					if (mode == Jid) {
						return Jid;
					}
					return IpJid;
				case IpJid:
					return IpJid;
			}
			return this;
		}
	}
	
	private final Logger log = Logger.getLogger(this.getClass().getName());
	private final Map<String, StatHolder> otherStatHolders = new ConcurrentHashMap<>();
	private final StatHolder statHolder = new StatHolder();
	@ConfigField(desc = "Allows storing detailed, per IP/JID statistics of blocked attempts")
	private boolean detailedStatistics = false;
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

		BruteForceLockerVHostExtension extension = session != null ? session.getDomain().getExtension(BruteForceLockerVHostExtension.class) : null;

		final long lockAfterFails = extension == null ? 3 : extension.getLockAccountAfterFailedAttempt();
		if (value.getBadLoginCounter() <= lockAfterFails) {
			final long periodTime =
					(extension == null ? 10 : extension.getPeriodTime()) * 1000;
			value.setInvalidateAtTime(currentTime + periodTime);
		} else {
			final long lockTime = (extension == null ? 10 : extension.getLockTime()) * 1000;
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

		BruteForceLockerVHostExtension extension = session != null ? session.getDomain().getExtension(BruteForceLockerVHostExtension.class) : null;
		final long disableAfterFails =
				extension == null ? 20 : extension.getDisableAccountAfterFailedAttempts();

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

		list.add(keyName, "Blocked IPs", tmp.ips.size(), Level.INFO);
		list.add(keyName, "Blocked JIDs", tmp.jids.size(), Level.INFO);

		list.add(keyName, "Total blocked IP attempts", tmp.ips.values().stream().mapToInt(Integer::intValue).sum(),
				 Level.FINE);
		list.add(keyName, "Total blocked JID attempts", tmp.jids.values().stream().mapToInt(Integer::intValue).sum(),
				 Level.FINE);
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
		BruteForceLockerVHostExtension extension = session != null ? session.getDomain().getExtension(BruteForceLockerVHostExtension.class) : null;
		return extension == null || extension.isEnabled();
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
		BruteForceLockerVHostExtension extension = session != null ? session.getDomain().getExtension(BruteForceLockerVHostExtension.class) : null;
		final Mode mode = extension == null ? Mode.IpJid : extension.getMode();
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
		if (v.ip != null) {
			this.statHolder.addIP(v.ip);
		}
		if (v.jid != null) {
			this.statHolder.addJID(v.jid);
		}
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
			BruteForceLockerVHostExtension extension = session != null ? session.getDomain().getExtension(BruteForceLockerVHostExtension.class) : null;
			long lockAfterFails = extension == null ? 3 : extension.getLockAccountAfterFailedAttempt();
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

	@Bean(name = BruteForceLockerVHostExtension.ID, parent = VHostItemExtensionManager.class, active = true)
	public static class BruteForceLockerVHostExtensionProvider
			implements VHostItemExtensionProvider<BruteForceLockerVHostExtension> {

		@Override
		public String getId() {
			return BruteForceLockerVHostExtension.ID;
		}

		@Override
		public Class<BruteForceLockerVHostExtension> getExtensionClazz() {
			return BruteForceLockerVHostExtension.class;
		}
	}

	public static class BruteForceLockerVHostExtension
			extends AbstractVHostItemExtension<BruteForceLockerVHostExtension>
			implements VHostItemExtensionBackwardCompatible<BruteForceLockerVHostExtension> {

		public static final String ID = "brute-force-locker";
		private static final long DEF_lockAccountAfterFailedAttempt = 3l;
		private static final long DEF_disableAccountAfterFailedAttempts = 20l;
		private static final long DEF_periodTime = 60l;
		private static final long DEF_lockTime = 60l;
		private static final Mode DEF_mode = IpJid;

		private boolean enabled = true;
		private long lockAccountAfterFailedAttempt = DEF_lockAccountAfterFailedAttempt;
		private long disableAccountAfterFailedAttempts = DEF_disableAccountAfterFailedAttempts;
		private long periodTime = DEF_periodTime;
		private long lockTime = DEF_lockTime;
		private Mode mode = DEF_mode;

		public boolean isEnabled() {
			return enabled;
		}

		public long getLockAccountAfterFailedAttempt() {
			return lockAccountAfterFailedAttempt;
		}

		public long getDisableAccountAfterFailedAttempts() {
			return disableAccountAfterFailedAttempts;
		}

		public long getPeriodTime() {
			return periodTime;
		}

		public long getLockTime() {
			return lockTime;
		}

		public Mode getMode() {
			return mode;
		}

		@Override
		public String getId() {
			return ID;
		}

		@Override
		public void initFromElement(Element item) {
			enabled = !"false".equals(item.getAttributeStaticStr("enabled"));
			String tmp = item.getAttributeStaticStr("lock-after-fails");
			if (tmp != null) {
				lockAccountAfterFailedAttempt = Long.parseLong(tmp);
			}
			tmp = item.getAttributeStaticStr("disable-after-fails");
			if (tmp != null) {
				disableAccountAfterFailedAttempts = Long.parseLong(tmp);
			}
			tmp = item.getAttributeStaticStr("period-time");
			if (tmp != null) {
				periodTime = Long.parseLong(tmp);
			}
			tmp = item.getAttributeStaticStr("lock-time");
			if (tmp != null) {
				lockTime = Long.parseLong(tmp);
			}
			tmp = item.getAttributeStaticStr("mode");
			if (tmp != null) {
				mode = Mode.valueOf(tmp);
			}
		}

		@Override
		public void initFromCommand(String prefix, Packet packet) throws IllegalArgumentException {
			enabled = Command.getCheckBoxFieldValue(packet, prefix + "-enabled");
			lockAccountAfterFailedAttempt = Long.parseLong(Command.getFieldValue(packet, prefix + "-lock-after-fails"));
			disableAccountAfterFailedAttempts = Long.parseLong(Command.getFieldValue(packet, prefix + "-disable-after-fails"));
			periodTime = Long.parseLong(Command.getFieldValue(packet, prefix + "-period-time"));
			lockTime = Long.parseLong(Command.getFieldValue(packet, prefix + "-lock-time"));
			mode = Mode.valueOf(Command.getFieldValue(packet, prefix + "-mode"));
		}

		@Override
		public Element toElement() {
			if (enabled && lockAccountAfterFailedAttempt == DEF_lockAccountAfterFailedAttempt &&
					disableAccountAfterFailedAttempts == DEF_disableAccountAfterFailedAttempts &&
					periodTime == DEF_periodTime && lockTime == DEF_lockTime && mode == DEF_mode) {
				return null;
			}

			Element el = new Element(ID);
			if (!enabled) {
				el.setAttribute("enabled", String.valueOf(enabled));
			}
			if (lockAccountAfterFailedAttempt != DEF_lockAccountAfterFailedAttempt) {
				el.setAttribute("lock-after-fails", String.valueOf(lockAccountAfterFailedAttempt));
			}
			if (disableAccountAfterFailedAttempts != DEF_disableAccountAfterFailedAttempts) {
				el.setAttribute("disable-after-fails", String.valueOf(disableAccountAfterFailedAttempts));
			}
			if (periodTime != DEF_periodTime) {
				el.setAttribute("period-time", String.valueOf(periodTime));
			}
			if (lockTime != DEF_lockTime) {
				el.setAttribute("lock-time", String.valueOf(lockTime));
			}
			if (mode != DEF_mode) {
				el.setAttribute("mode", mode.name());
			}
			return el;
		}
		
		@Override
		public void addCommandFields(String prefix, Packet packet, boolean forDefault) {
			Element commandEl = packet.getElemChild(Command.COMMAND_EL, Command.XMLNS);
			DataForm.addFieldValue(commandEl, prefix + "-enabled", String.valueOf(enabled), "boolean",
								   "Brute Force Prevention Enabled");
			DataForm.addFieldValue(commandEl, prefix + "-lock-after-fails",
								   String.valueOf(lockAccountAfterFailedAttempt), "text-single",
								   "Number of allowed invalid login");

			DataForm.addFieldValue(commandEl, prefix + "-disable-after-fails",
								   String.valueOf(disableAccountAfterFailedAttempts), "text-single",
								   "Disable account after failed login");
			DataForm.addFieldValue(commandEl, prefix + "-period-time", String.valueOf(periodTime),
								   "text-single", "Failed login in period of time [sec]");
			DataForm.addFieldValue(commandEl, prefix + "-lock-time", String.valueOf(lockTime),
								   "text-single", "Lock time [sec]");
			DataForm.addFieldValue(commandEl, prefix + "-mode", mode.name(), "Brute Force Prevention Mode",
								   new String[]{Mode.Ip.name(), Mode.Jid.name(), IpJid.name()},
								   new String[]{Mode.Ip.name(), Mode.Jid.name(), IpJid.name()});
		}


		@Override
		public void initFromData(Map<String, Object> data) {
			Boolean enabled = (Boolean) data.remove(LOCK_ENABLED_KEY);
			if (enabled != null) {
				this.enabled = enabled;
			}
			Long tmp = (Long) data.remove(LOCK_AFTER_FAILS_KEY);
			if (tmp != null) {
				lockAccountAfterFailedAttempt = tmp;
			}
			tmp = (Long) data.remove(LOCK_DISABLE_ACCOUNT_FAILS_KEY);
			if (tmp != null) {
				disableAccountAfterFailedAttempts = tmp;
			}
			tmp = (Long) data.remove(LOCK_PERIOD_TIME_KEY);
			if (tmp != null) {
				periodTime = tmp;
			}
			tmp = (Long) data.remove(LOCK_TIME_KEY);
			if (tmp != null) {
				lockTime = tmp;
			}
			String mode = (String) data.remove(LOCK_MODE_KEY);
			if (mode != null) {
				this.mode = Mode.valueOf(mode);
			}
		}

		@Override
		public BruteForceLockerVHostExtension mergeWithDefaults(BruteForceLockerVHostExtension defaults) {
			BruteForceLockerVHostExtension merged = new BruteForceLockerVHostExtension();

			merged.enabled = this.enabled || defaults.enabled;
			merged.mode = this.mode.merge(defaults.mode);
			merged.lockTime = Math.max(this.lockTime,  defaults.lockTime);
			merged.periodTime = Math.max(this.periodTime, defaults.periodTime);
			merged.disableAccountAfterFailedAttempts = Math.min(this.disableAccountAfterFailedAttempts, defaults.disableAccountAfterFailedAttempts);
			merged.lockAccountAfterFailedAttempt = Math.min(this.lockAccountAfterFailedAttempt, defaults.lockAccountAfterFailedAttempt);

			return merged;
		}

	}
}
