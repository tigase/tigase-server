package tigase.disteventbus.component;

import java.util.Collection;
import java.util.logging.Level;

import tigase.component.exceptions.ComponentException;
import tigase.criteria.Criteria;
import tigase.server.Packet;
import tigase.util.TigaseStringprepException;
import tigase.xml.Element;

public class EventReceiverModule extends AbstractEventBusModule {

	private static final Criteria CRIT = new ElemPathCriteria(new String[] { "message", "event" }, new String[] { null,
			"http://jabber.org/protocol/pubsub#event" });

	public final static String ID = "receiver";

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
		final String type = packet.getElement().getAttributeStaticStr("type");

		if (type != null && type.equals("error")) {
			if (log.isLoggable(Level.FINE))
				log.fine("Ignoring error message!");
			return;
		}

		Element eventElem = packet.getElement().getChild("event", "http://jabber.org/protocol/pubsub#event");
		Element itemsElem = eventElem.getChild("items");

		for (Element item : itemsElem.getChildren()) {
			if (!"item".equals(item.getName()))
				continue;
			for (Element event : item.getChildren()) {
				String eventName = event.getName();
				String eventXmlns = event.getXMLNS();

				if (log.isLoggable(Level.FINER))
					log.finer("Received event (" + eventName + ", " + eventXmlns + ").");

				context.getEventBusInstance().doFire(eventName, eventXmlns, event);

				final Collection<NonClusterSubscription> subscribers = context.getNonClusterSubscriptionStore().getSubscribersJIDs(
						eventName, eventXmlns);
				eventPublisherModule.publishEvent(eventName, eventXmlns, event, subscribers);
			}
		}

	}

}
