/*
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
package tigase.server.rtbl;

import tigase.db.TigaseDBException;
import tigase.db.UserRepository;
import tigase.eventbus.EventBus;
import tigase.eventbus.EventBusEvent;
import tigase.eventbus.HandleEvent;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Initializable;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.UnregisterAware;
import tigase.kernel.beans.config.ConfigField;
import tigase.kernel.core.Kernel;
import tigase.stats.StatisticsContainerIfc;
import tigase.stats.StatisticsList;
import tigase.xmpp.jid.BareJID;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Bean(name = "rtblRepository", parent = Kernel.class, active = true, exportable = true)
public class RTBLRepository implements Initializable, UnregisterAware, StatisticsContainerIfc {
	
	private static final Logger logger = Logger.getLogger(RTBLRepository.class.getCanonicalName());

	@Inject
	private UserRepository userRepository;
	@Inject(bean = "eventBus")
	private EventBus eventBus;

	@ConfigField(desc = "Reload interval")
	private long reloadInterval = TimeUnit.MINUTES.toMillis(10);

	private BareJID repoUser = BareJID.bareJIDInstanceNS("rtbl");

	private ConcurrentHashMap<Key, RTBL> cache = new ConcurrentHashMap<>();

	private Timer timer;

	private String name = "rtblRepository";
	
	public RTBLRepository() {
	}

	@Override
	public void beforeUnregister() {
		if (timer != null) {
			timer.cancel();
		}
		if (eventBus != null) {
			eventBus.unregisterAll(this);
		}
	}

	@Override
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public void getStatistics(StatisticsList list) {
		list.add(getName(), "No. of lists", cache.size(), Level.FINE);
		cache.values().stream().sorted(Comparator.comparing(RTBL::getJID).thenComparing(RTBL::getNode)).forEachOrdered(rtbl -> {
			list.add(getName(), "No. of items in " + rtbl.getJID() + "/" + rtbl.getNode(), rtbl.getBlocked().size(), Level.FINEST);
		});
	}

	@Override
	public void initialize() {
		eventBus.registerAll(this);
		reload();
		if (reloadInterval > 0) {
			timer = new Timer(true);
			timer.scheduleAtFixedRate(new TimerTask() {
				@Override
				public void run() {
					reload();
				}
			}, reloadInterval, reloadInterval);
		}
	}

	public List<RTBL> getBlockLists() {
		return new ArrayList<>(cache.values());
	}

	public RTBL getBlockList(BareJID jid, String node) {
		return cache.get(new Key(jid, node));
	}

	public boolean isBlocked(BareJID jid) {
		for (RTBL rtbl : cache.values()) {
			if (rtbl.isBlocked(jid)) {
				return true;
			}
		}
		return false;
	}

	public void update(RTBL rtbl) {
		Key key = rtbl.getKey();
		if (!cache.containsKey(key)) {
			return;
		}
		cache.put(key, rtbl);
		try {
			if (!userRepository.userExists(repoUser)) {
				userRepository.addUser(repoUser);
			}
		} catch (TigaseDBException ex) {
			// ignore, possible race condition..
		}
		try {
			RTBL oldList = loadList(key);
			Set<String> prevBlocked = Optional.ofNullable(oldList).map(RTBL::getBlocked).orElse(Collections.emptySet());
			List<String> newToAdd = rtbl.getBlocked().stream().filter(it -> !prevBlocked.contains(it)).toList();

			for (String item : newToAdd) {
				updateStore(key, Action.add, item);
			}
			List<String> oldToRemove = prevBlocked.stream().filter(it -> !rtbl.getBlocked().contains(it)).collect(
					Collectors.toList());
			for (String item : oldToRemove) {
				updateStore(key, Action.remove, item);
			}
		} catch (TigaseDBException ex) {
			logger.log(Level.WARNING, "failed to save updated RTBL " + rtbl);
		}
	}

	public enum Action {
		add,
		remove
	}

	public void add(BareJID pubsubJid, String node, String hash) throws TigaseDBException {
		Key key = new Key(pubsubJid, node);
		if (cache.get(key) == null) {
			userRepository.setData(repoUser, key.getSubnode(), "jid", pubsubJid.toString());
			userRepository.setData(repoUser, key.getSubnode(), "node", node);
			userRepository.setData(repoUser, key.getSubnode(), "hash", hash);
			eventBus.fire(new RTBLAdded(pubsubJid, node, hash));
		}
	}

	@HandleEvent
	public void handleAdded(RTBLAdded event) {
		Key key = event.getKey();
		cache.putIfAbsent(key, new RTBL(key, event.getHash()));
	}

	public void remove(BareJID pubsubJid, String node) throws TigaseDBException {
		Key key = new Key(pubsubJid, node);
		userRepository.removeSubnode(repoUser, key.getSubnode());
		eventBus.fire(new RTBLRemoved(pubsubJid, node));
	}

	@HandleEvent
	public void handleRemoved(RTBLRemoved event) {
		Key key = event.getKey();
		cache.remove(key);
	}

	public void update(BareJID pubsubJid, String node, Action action, String id) {
		Key key = new Key(pubsubJid, node);
		updateStore(key, action, id);
		eventBus.fire(new RTBLChange(pubsubJid, node, action, id));
	}

	@HandleEvent
	public void handleChange(RTBLChange event) {
		Key key = event.getKey();
		RTBL rtbl = cache.get(key);
		if (rtbl != null) {
			switch (event.getAction()) {
				case add -> {
					rtbl.getBlocked().add(event.getId());
				}
				case remove -> {
					rtbl.getBlocked().remove(event.getId());
				}
			}
		}
	}

	public void purge(BareJID pubsubJid, String node) {
		Key key = new Key(pubsubJid, node);
		RTBL rtbl = cache.get(key);
		if (rtbl != null) {
			Set<String> toRemove = new HashSet<>(rtbl.getBlocked());
			rtbl.getBlocked().clear();
			for (String item : toRemove) {
				updateStore(key, Action.remove, item);
			}
			eventBus.fire(new RTBLReload(pubsubJid, node));
		}
	}

	public void reload(BareJID pubsubJid, String node) {
		eventBus.fire(new RTBLRemoved(pubsubJid, node));
	}

	@HandleEvent
	public void handleReload(RTBLReload event) {
		try {
			RTBL rtbl = loadList(event.getKey());
			if (rtbl != null) {
				cache.replace(rtbl.getKey(), rtbl);
			}
		} catch (TigaseDBException e) {
			throw new RuntimeException(e);
		}
	}

	private void updateStore(Key key, Action action, String id) {
		switch (action) {
			case add -> {
				try {
					userRepository.setData(repoUser, key.getSubnode(), id, id);
				} catch (TigaseDBException ex) {
					logger.log(Level.WARNING, "failed to save updated RTBL " + key + ", adding " + id + " failed", ex);
				}
			}
			case remove -> {
				try {
					userRepository.removeData(repoUser, key.getSubnode(), id);
				} catch (TigaseDBException ex) {
					logger.log(Level.WARNING, "failed to save updated RTBL " + key + ", removing " + id + " failed", ex);
				}
			}
		}
	}
	
	private RTBL loadList(Key key) throws TigaseDBException {
		if (userRepository.userExists(repoUser)) {
			try {
				Map<String, String> data = userRepository.getDataMap(repoUser, key.getSubnode());
				BareJID jid = BareJID.bareJIDInstanceNS(data.remove("jid"));
				String node = data.remove("node");
				String hash = data.remove("hash");
				if (jid == null || hash == null || node == null) {
					return null;
				}
				return new RTBL(jid, node, hash, new CopyOnWriteArraySet<>(data.keySet()));
			} catch (TigaseDBException ex) {
				logger.log(Level.WARNING, "could not load RTBL for " + key, ex);
			}
		}
		return null;
	}

	protected void reload() {
		try {
			if (userRepository.userExists(repoUser)) {
				String[] subnodes = userRepository.getSubnodes(repoUser);
				if (subnodes != null) {
					for (String subnode : subnodes) {
						Key key = Key.parse(subnode);
						try {
							if (key != null) {
								RTBL rtbl = loadList(key);
								if (rtbl != null) {
									cache.put(key, rtbl);
								} else {
									cache.remove(key);
								}
							}
						} catch (TigaseDBException ex) {
							logger.log(Level.WARNING, "could not load RTBL " + key, ex);
						}
					}
				}
			}
		} catch (TigaseDBException ex) {
			logger.log(Level.WARNING, "could not load list of RTBLs", ex);
		}
	}

	public static class Key {
		private final BareJID jid;
		private final String node;

		public Key(BareJID jid, String node) {
			this.jid = jid;
			this.node = node;
		}

		public String getNode() {
			return node;
		}

		public BareJID getJid() {
			return jid;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (!(o instanceof Key key)) {
				return false;
			}
			return Objects.equals(jid, key.jid) && Objects.equals(node, key.node);
		}

		@Override
		public int hashCode() {
			return Objects.hash(jid, node);
		}

		public String getSubnode() {
			return jid.toString() + "|" + node.replaceAll("/", "|");
		}

		public static Key parse(String data) {
			String[] parts = data.split("\\|");
			if (parts != null && parts.length > 1) {
				return new Key(BareJID.bareJIDInstanceNS(parts[0]),
							   Arrays.stream(parts).skip(1).collect(Collectors.joining("|")));
			}
			return null;
		}

		@Override
		public String toString() {
			return "jid: " + jid + ", node: " + node;
		}
	}

	public static abstract class RTBLEvent implements Serializable, EventBusEvent {
		private BareJID jid;
		private String node;

		public RTBLEvent() {}

		public RTBLEvent(BareJID jid, String node) {
			this.jid = jid;
			this.node = node;
		}

		public BareJID getJid() {
			return jid;
		}

		public void setJid(BareJID jid) {
			this.jid = jid;
		}

		public String getNode() {
			return node;
		}

		public void setNode(String node) {
			this.node = node;
		}

		public Key getKey() {
			return new Key(jid, node);
		}

	}

	public static class RTBLAdded extends RTBLEvent implements Serializable, EventBusEvent {

		private String hash;

		public RTBLAdded() {}

		public RTBLAdded(BareJID jid, String node, String hash) {
			super(jid, node);
			this.hash = hash;
		}

		public String getHash() {
			return hash;
		}

		public void setHash(String hash) {
			this.hash = hash;
		}
		
	}

	public static class RTBLRemoved extends RTBLEvent implements Serializable, EventBusEvent {

		public RTBLRemoved() {}

		public RTBLRemoved(BareJID jid, String node) {
			super(jid, node);
		}

	}

	public static class RTBLReload extends RTBLEvent implements Serializable, EventBusEvent {

		public RTBLReload() {}

		public RTBLReload(BareJID jid, String node) {
			super(jid, node);
		}

	}

	public static class RTBLChange extends RTBLEvent implements Serializable, EventBusEvent {
		private Action action;
		private String id;

		public RTBLChange() {}

		public RTBLChange(BareJID jid, String node, Action action, String id) {
			super(jid, node);
			this.action = action;
			this.id = id;
		}
		
		public Action getAction() {
			return action;
		}

		public void setAction(Action action) {
			this.action = action;
		}

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}
	}
}
