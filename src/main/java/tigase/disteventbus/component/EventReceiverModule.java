package tigase.disteventbus.component;

import java.util.Collection;
import java.util.Iterator;
import java.util.logging.Level;

import tigase.component.exceptions.ComponentException;
import tigase.criteria.Criteria;
import tigase.disteventbus.component.stores.Affiliation;
import tigase.disteventbus.component.stores.Subscription;
import tigase.server.Packet;
import tigase.util.TigaseStringprepException;
import tigase.xml.Element;
import tigase.xmpp.Authorization;

public class EventReceiverModule extends AbstractEventBusModule {

	public final static String ID = "receiver";
	private static final Criteria CRIT = new ElemPathCriteria(new String[] { "message", "event" },
			new String[] { null, "http://jabber.org/protocol/pubsub#event" });
	private EventPublisherModule eventPublisherModule;

	@Override
	public void afterRegistration() {
		super.afterRegistration();

		eventPublisherModule = context.getModuleProvider().getModule(EventPublisherModule.ID);
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
		final Affiliation affiliation = context.getAffiliationStore().getAffiliation(packet.getStanzaFrom());
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
				String eventName = event.getName();
				String eventXmlns = event.getXMLNS();

				event.setAttribute("remote", "true");

				if (log.isLoggable(Level.FINER))
					log.finer("Received event (" + eventName + ", " + eventXmlns + "): " + event);

				context.getEventBusInstance().doFire(eventName, eventXmlns, event);

				// forwarding event to _non cluster_ subscribers.
				final Collection<Subscription> subscribers = context.getSubscriptionStore().getSubscribersJIDs(eventName,
						eventXmlns);
				Iterator<Subscription> it = subscribers.iterator();
				while (it.hasNext()) {
					Subscription subscription = it.next();
					if (subscription.isInClusterSubscription())
						it.remove();
				}
				eventPublisherModule.publishEvent(eventName, eventXmlns, event, subscribers);
			}
		}

	}

}
