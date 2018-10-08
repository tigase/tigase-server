package tigase.auth;

import tigase.kernel.TypesConverter;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Initializable;
import tigase.map.ClusterMapFactory;
import tigase.server.xmppsession.SessionManager;
import tigase.vhosts.VHostItem;
import tigase.xmpp.XMPPResourceConnection;
import tigase.xmpp.jid.BareJID;
import tigase.xmpp.jid.JID;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static tigase.auth.BruteForceLockerBean.Mode.IpJid;
import static tigase.auth.BruteForceLockerBean.Mode.valueOf;

@Bean(name = "brute-force-locker", parent = SessionManager.class, active = true)
public class BruteForceLockerBean
		implements Initializable {

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
	private Map<Key, Value> map;

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
			value = new Value();
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

			if (log.isLoggable(Level.FINEST)) {
				log.finest("IP is null. Skip adding entry.");
			}
		} else {
			final long lockTime = (session == null ? 10 : (long) session.getDomain().getData(LOCK_TIME_KEY)) * 1000;
			value.setInvalidateAtTime(currentTime + lockTime);
		}

		map.put(key, value);
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

		return value.getBadLoginCounter() > disableAfterFails;
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

	@Override
	public void initialize() {
		this.map = ClusterMapFactory.get().createMap(MAP_TYPE, Key.class, Value.class);
		assert this.map != null : "Distributed Map is NULL!";
	}

	public boolean isEnabled(XMPPResourceConnection session) {
		return session == null || (boolean) session.getDomain().getData(LOCK_ENABLED_KEY);
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

	public static class Value
			implements TypesConverter.Parcelable {

		private int badLoginCounter;
		/** Invalidate this value at specific time */
		private long invalidateAtTime;

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
