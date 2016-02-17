/*
 * EventPublisherModule.java
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

package tigase.eventbus.component;

import java.io.Serializable;
import java.util.Collection;
import java.util.logging.Level;

import tigase.component.exceptions.ComponentException;
import tigase.criteria.Criteria;
import tigase.eventbus.EventRoutingSelector;
import tigase.eventbus.component.stores.Subscription;
import tigase.eventbus.component.stores.SubscriptionStore;
import tigase.eventbus.impl.AbstractHandler;
import tigase.eventbus.impl.EventBusImplementation;
import tigase.eventbus.impl.EventBusSerializer;
import tigase.eventbus.impl.EventName;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Initializable;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.UnregisterAware;
import tigase.server.Packet;
import tigase.server.Permissions;
import tigase.util.TigaseStringprepException;
import tigase.xml.Element;
import tigase.xmpp.JID;

@Bean(name = EventPublisherModule.ID)
public class EventPublisherModule extends AbstractEventBusModule implements Initializable, UnregisterAware {

	public final static String ID = "publisher";
	@Inject
	private EventBusComponent component;
	@Inject(nullAllowed = false, bean = "localEventBus")
	private EventBusImplementation localEventBus;
	@Inject
	private SubscriptionStore subscriptionStore;
	private EventBusSerializer serializer = new EventBusSerializer();
	private final AbstractHandler firedEventHandler = new AbstractHandler(null, null) {
		@Override
		public void dispatch(Object event, Object source, boolean remotelyGeneratedEvent) {
			if (remotelyGeneratedEvent)
				return;

			if (event instanceof Element) {
				publishEvent((Element) event);
			} else if (!(event instanceof EventBusImplementation.InternalEventbusEvent)) {
				publishObjectEvent(event);
			}
		}

		@Override
		public AbstractHandler.Type getRequiredEventType() {
			return Type.asIs;
		}
	};

	@Override
	public void beforeUnregister() {
		localEventBus.removeHandler(firedEventHandler);
	}

	@Override
	public String[] getFeatures() {
		return null;
	}

	@Override
	public Criteria getModuleCriteria() {
		return null;
	}

	protected Collection<Subscription> getSubscribers(String packageName, String eventName, Object event) {
		Collection<Subscription> subscribers = subscriptionStore.getSubscribersJIDs(packageName, eventName);

		EventRoutingSelector selector = localEventBus.getEventRoutingSelector(event.getClass());
		if (selector != null) {
			subscribers = selector.getSubscriptions(event, subscribers);
		}

		return subscribers;
	}

	@Override
	public void initialize() {
		localEventBus.addHandler(firedEventHandler);

	}

	@Override
	public void process(Packet packet) throws ComponentException, TigaseStringprepException {
	}

	private void publishEvent(Element pubsubEventElem, String from, JID toJID) throws TigaseStringprepException {
		Packet message = Packet.packetInstance(new Element("message", new String[] { "to", "from", "id" },
				new String[] { toJID.toString(), from, nextStanzaID() }));
		message.getElement().addChild(pubsubEventElem);
		message.setXMLNS(Packet.CLIENT_XMLNS);

		message.setPermissions(Permissions.ADMIN);

		write(message);
	}

	public void publishEvent(final Element event) {
		EventName en = new EventName(event.getName());

		final String isRemote = event.getAttributeStaticStr("remote");
		if (isRemote != null && ("true".equals(isRemote) || "1".equals(isRemote))) {
			if (log.isLoggable(Level.FINEST)) {
				log.finest("Remote event. No need to redistribute this way. " + event.toString());
			}
			return;
		}
		final String isLocal = event.getAttributeStaticStr("local");
		if (isLocal != null && ("true".equals(isLocal) || "1".equals(isLocal))) {
			if (log.isLoggable(Level.FINEST)) {
				log.finest("Event for local subscribers only. Skipping. " + event.toString());
			}
			return;
		}
		final Collection<Subscription> subscribers = subscriptionStore.getSubscribersJIDs(en.getPackage(), en.getName());
		publishEvent(en.getPackage(), en.getName(), event, subscribers);
	}

	public void publishEvent(String eventPackage, String name, Element event, Collection<Subscription> subscribers) {
		try {
			final Element eventElem = new Element("event", new String[] { "xmlns" },
					new String[] { "http://jabber.org/protocol/pubsub#event" });
			final Element itemsElem = new Element("items", new String[] { "node" },
					new String[] { EventName.toString(eventPackage, name) });
			eventElem.addChild(itemsElem);
			final Element itemElem = new Element("item");
			itemElem.addChild(event);
			itemsElem.addChild(itemElem);

			if (log.isLoggable(Level.FINER)) {
				log.log(Level.FINER, "Sending event ({0}, {1}, {2}) to {3}",
						new Object[] { name, eventPackage, event, subscribers });
			}

			for (Subscription subscriber : subscribers) {

				String from;
				if (subscriber.getServiceJID() == null) {
					from = component.getComponentId().toString();
				} else {
					from = subscriber.getServiceJID().toString();
				}
				JID toJID = subscriber.getJid();

				publishEvent(eventElem, from, toJID);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void publishObjectEvent(Object event) {
		if (!(event instanceof Serializable))
			return;
		Class<?> eventClass = event.getClass();
		final String packageName = eventClass.getPackage().getName();
		final String eventName = eventClass.getSimpleName();

		final Collection<Subscription> subscribers = getSubscribers(packageName, eventName, event);
		if (subscribers.isEmpty())
			return;

		Element eventElement = serializer.serialize(event);

		publishEvent(packageName, eventName, eventElement, subscribers);
	}
}
