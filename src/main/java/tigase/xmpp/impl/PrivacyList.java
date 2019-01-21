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

import tigase.db.TigaseDBException;
import tigase.util.stringprep.TigaseStringprepException;
import tigase.xml.Element;
import tigase.xmpp.NotAuthorizedException;
import tigase.xmpp.XMPPResourceConnection;
import tigase.xmpp.impl.roster.RosterAbstract;
import tigase.xmpp.impl.roster.RosterElement;
import tigase.xmpp.jid.BareJID;
import tigase.xmpp.jid.JID;

import java.util.*;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PrivacyList {

	public static final PrivacyList ALLOW_ALL = new PrivacyList(null, new Element("list"));
	private static final Logger log = Logger.getLogger(PrivacyList.class.getCanonicalName());
	private static final Set<Item.Type> ALL_TYPES = EnumSet.allOf(Item.Type.class);
	private static final PrivacyList DENY_ALL = new PrivacyList(null, new Element("list")) {
		@Override
		public boolean isAllowed(JID jid, Item.Type type) {
			return false;
		}
	};
	private final Item[] items;
	private final String name;
	private final Function<JID, RosterElement> rosterElementGetter;

	public static PrivacyList create(final Map<BareJID, RosterElement> roster, Element el) {
		if (el == null) {
			return null;
		}

		return new PrivacyList((jid) -> {
			if (jid == null) {
				return null;
			}
			return roster.get(jid.getBareJID());
		}, el).getSingletonIfPossible();
	}

	public static PrivacyList create(XMPPResourceConnection session, RosterAbstract rosterUtil, Element el)
			throws NotAuthorizedException, TigaseDBException {
		if (el == null) {
			return null;
		}

		return new PrivacyList(rosterUtil.rosterElementProvider(session), el).getSingletonIfPossible();
	}

	public PrivacyList(Function<JID, RosterElement> rosterElementGetter, Element el) {
		this.rosterElementGetter = rosterElementGetter;
		this.name = el.getAttributeStaticStr(Privacy.NAME);
		this.items = Optional.ofNullable(el.getChildren())
				.orElse(Collections.emptyList())
				.stream()
				.sorted(JabberIqPrivacy.compar)
				.map(this::elemToItem)
				.filter(it -> it != null)
				.toArray(x -> new Item[x]);
	}

	public String getName() {
		return name;
	}

	public Stream<JID> getBlockedJids() {
		return Arrays.stream(items).filter(x -> x instanceof ItemJid).map(x -> ((ItemJid) x).jid);
	}

	public boolean isAllowed(JID jid, Item.Type type) {
		for (Item item : items) {
			if (item.matches(jid, type)) {
				return item.isAllowed();
			}
		}

		return true;
	}

	public boolean isEmpty() {
		return items.length == 0;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("PrivacyList[" + name + ", [");
		for (Item item : items) {
			sb.append(item.toString()).append(",");
		}
		sb.append("]]");
		return sb.toString();
	}

	private PrivacyList getSingletonIfPossible() {
		if (isEmpty()) {
			return ALLOW_ALL;
		} else if (items.length == 1) {
			if (items[0] instanceof ItemAll) {
				return items[0].isAllowed() ? ALLOW_ALL : DENY_ALL;
			}
		}
		return this;
	}

	private Item elemToItem(Element el) {
		String type = el.getAttributeStaticStr("type");
		String value = el.getAttributeStaticStr("value");
		String action = el.getAttributeStaticStr("action");
		if (action == null) {
			return null;
		}

		boolean allow;
		switch (action) {
			case "allow":
				allow = true;
				break;
			case "deny":
				allow = false;
				break;
			default:
				return null;
		}

		if (type == null) {
			return new ItemAll(allow);
		}

		Set<Item.Type> types = ALL_TYPES;
		List<Element> children = el.getChildren();
		if (children != null && !children.isEmpty()) {
			types = children.stream().map(child -> {
				switch (child.getName()) {
					case "message":
						return Item.Type.message;
					case "iq":
						return Item.Type.iq;
					case "presence-in":
						return Item.Type.presenceIn;
					case "presence-out":
						return Item.Type.presenceOut;
					default:
						return null;
				}
			}).filter(val -> val != null).collect(Collectors.toSet());
			if (types.isEmpty()) {
				types = ALL_TYPES;
			}
		}

		switch (type) {
			case "jid":
				try {
					JID jid = JID.jidInstance(value);
					return new ItemJid(jid, allow, types);
				} catch (TigaseStringprepException ex) {
					log.log(Level.FINEST, "Exception while creating jid instance for value: " + value, ex);
					return null;
				}
			case "group":
				return new ItemGroup(value, allow, types);
			case "subscription":
				try {
					return new ItemSubscription(RosterAbstract.SubscriptionType.valueOf(value), allow, types);
				} catch (IllegalArgumentException ex) {
					log.log(Level.FINEST, "Exception while parsing subscription value: " + value, ex);
					return null;
				}
			default:
				return null;
		}
	}

	public interface Item {

		enum Type {
			message,
			iq,
			presenceIn,
			presenceOut
		}

		boolean isAllowed();

		boolean matches(JID jid, Type type);
	}

	private abstract class AbstractItem
			implements Item {

		protected final boolean allowed;
		protected final Set<Type> types;

		protected AbstractItem(boolean allowed, Set<Type> types) {
			this.allowed = allowed;
			this.types = types;
		}

		@Override
		public boolean isAllowed() {
			return allowed;
		}

		protected String[] getRosterGroupsForJid(JID jid) {
			RosterElement item = rosterElementGetter.apply(jid);
			return item == null ? null : item.getGroups();
		}

		protected RosterAbstract.SubscriptionType getSubscriptionForJID(JID jid) {
			RosterElement item = rosterElementGetter.apply(jid);
			return item == null ? null : item.getSubscription();
		}
	}

	private class ItemAll
			implements Item {

		private final boolean allowed;

		public ItemAll(boolean allowed) {
			this.allowed = allowed;
		}

		@Override
		public boolean isAllowed() {
			return allowed;
		}

		@Override
		public boolean matches(JID jid, Type type) {
			return true;
		}
	}

	private class ItemGroup
			extends AbstractItem {

		private final String group;

		public ItemGroup(String group, boolean allowed, Set<Type> types) {
			super(allowed, types);
			this.group = group;
		}

		@Override
		public boolean matches(JID jid, Type type) {
			if (!types.contains(type)) {
				return false;
			}

			String[] groups = getRosterGroupsForJid(jid);
			if (groups != null) {
				for (String group : groups) {
					if (group.equals(this.group)) {
						return true;
					}
				}
			}
			return false;
		}
	}

	private class ItemJid
			extends AbstractItem {

		private final JID jid;

		public ItemJid(JID jid, boolean allowed, Set<Type> types) {
			super(allowed, types);
			this.jid = jid;
		}

		@Override
		public boolean matches(JID jid, Type type) {
			return matches(jid) && types.contains(type);
		}

		private boolean matches(JID jid) {
			if (this.jid.getLocalpart() != null) {
				if (this.jid.getResource() != null) {
					return jid.equals(this.jid);
				} else if (this.jid.getResource() == null) {
					return jid.getBareJID().equals(this.jid.getBareJID());
				}
			} else {
				if (this.jid.getResource() != null) {
					return jid.equals(this.jid);
				} else if (this.jid.getResource() == null) {
					return jid.getDomain().equals(this.jid.getDomain());
				}
			}
			return false;
		}
	}

	private class ItemSubscription
			extends AbstractItem {

		private final RosterAbstract.SubscriptionType subscription;

		public ItemSubscription(RosterAbstract.SubscriptionType subscription, boolean allowed, Set<Type> types) {
			super(allowed, types);
			this.subscription = subscription;
		}

		@Override
		public boolean matches(JID jid, Type type) {
			if (!types.contains(type)) {
				return false;
			}

			RosterAbstract.SubscriptionType subscription = getSubscriptionForJID(jid);
			switch (this.subscription) {
				case none:
					return subscription == null || (!RosterAbstract.TO_SUBSCRIBED.contains(subscription) &&
							!RosterAbstract.FROM_SUBSCRIBED.contains(subscription));
				case to:
					return RosterAbstract.TO_SUBSCRIBED.contains(subscription);
				case from:
					return RosterAbstract.FROM_SUBSCRIBED.contains(subscription);
				case both:
					return RosterAbstract.TO_SUBSCRIBED.contains(subscription) &&
							RosterAbstract.FROM_SUBSCRIBED.contains(subscription);
			}
			return false;
		}
	}
}
