/*
 * EventReceiverModule.java
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
package tigase.eventbus.component;

import java.util.Collection;
import java.util.Iterator;
import java.util.logging.Level;

import tigase.component.exceptions.ComponentException;
import tigase.criteria.Criteria;
import tigase.eventbus.EventBusImplementation;
import tigase.eventbus.EventName;
import tigase.eventbus.EventRoutedTransientFiller;
import tigase.eventbus.Serializer;
import tigase.eventbus.component.stores.Affiliation;
import tigase.eventbus.component.stores.AffiliationStore;
import tigase.eventbus.component.stores.Subscription;
import tigase.eventbus.component.stores.SubscriptionStore;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;
import tigase.server.Packet;
import tigase.util.TigaseStringprepException;
import tigase.xml.Element;
import tigase.xmpp.Authorization;

@Bean(name = EventReceiverModule.ID)
public class EventReceiverModule extends AbstractEventBusModule {

	public final static String ID = "receiver";
	private static final Criteria CRIT = new ElemPathCriteria(new String[] { "message", "event" },
			new String[] { null, "http://jabber.org/protocol/pubsub#event" });
	@Inject
	private AffiliationStore affiliationStore;

	@Inject
	private EventPublisherModule eventPublisherModule;

	@Inject(nullAllowed = false, bean = "localEventBus")
	private EventBusImplementation localEventBus;

	@Inject
	private SubscriptionStore subscriptionStore;
	private Serializer serializer = new Serializer();

	private void fireEventLocally(final EventName name, final Element event) {
		Object obj = serializer.deserialize(event);
		if (obj == null)
			obj = event;
		else {
			Collection<EventRoutedTransientFiller> fillers = localEventBus.getEventRoutedTransientFillers(event.getClass());
			if (fillers != null) {
				for (EventRoutedTransientFiller f : fillers) {
					f.fillEvent(event);
				}
			}
		}

		localEventBus.fire(obj, this, true);

	}

	@Override
	public String[] getFeatures() {
		return null;
	}

	@Override
	public Criteria getModuleCriteria() {
		return CRIT;
	}

	@Override
	public void process(Packet packet) throws ComponentException, TigaseStringprepException {
		final Affiliation affiliation = affiliationStore.getAffiliation(packet.getStanzaFrom());
		if (!affiliation.isPublishItem())
			throw new ComponentException(Authorization.FORBIDDEN);

		final String type = packet.getElement().getAttributeStaticStr("type");

		if (type != null && type.equals("error")) {
			if (log.isLoggable(Level.FINE))
				log.fine("Ignoring error message! " + packet);
			return;
		}

		if (log.isLoggable(Level.FINER))
			log.finer("Received event stanza: " + packet.toStringFull());

		Element eventElem = packet.getElement().getChild("event", "http://jabber.org/protocol/pubsub#event");
		Element itemsElem = eventElem.getChild("items");

		for (Element item : itemsElem.getChildren()) {
			if (!"item".equals(item.getName()))
				continue;
			for (Element event : item.getChildren()) {
				EventName eventName = new EventName(event.getName());

				event.setAttribute("remote", "true");

				if (log.isLoggable(Level.FINER))
					log.finer("Received event " + eventName + ": " + event);

				fireEventLocally(eventName, event);

				// forwarding event to _non cluster_ subscribers.
				final Collection<Subscription> subscribers = subscriptionStore.getSubscribersJIDs(eventName.getPackage(), eventName.getName()
				);
				Iterator<Subscription> it = subscribers.iterator();
				while (it.hasNext()) {
					Subscription subscription = it.next();
					if (subscription.isInClusterSubscription())
						it.remove();
				}
				eventPublisherModule.publishEvent(eventName.getPackage(), eventName.getName(), event, subscribers);
			}
		}

	}

}
