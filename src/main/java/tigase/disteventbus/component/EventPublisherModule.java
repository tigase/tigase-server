package tigase.disteventbus.component;

import java.util.Collection;
import java.util.logging.Level;

import tigase.component.exceptions.ComponentException;
import tigase.criteria.Criteria;
import tigase.disteventbus.EventHandler;
import tigase.disteventbus.component.stores.Subscription;
import tigase.server.Packet;
import tigase.server.Permissions;
import tigase.util.TigaseStringprepException;
import tigase.xml.Element;
import tigase.xmpp.JID;

public class EventPublisherModule extends AbstractEventBusModule {

	public final static String ID = "publisher";

	private final EventHandler eventBusEventFiredHandler = new EventHandler() {
		@Override
		public void onEvent(String name, String xmlns, Element event) {
			publishEvent(name, xmlns, event);
		}
	};

	@Override
	public void afterRegistration() {
		super.afterRegistration();

		context.getEventBusInstance().addHandler(null, null, eventBusEventFiredHandler);
	}

	@Override
	public String[] getFeatures() {
		return null;
	}

	@Override
	public Criteria getModuleCriteria() {
		return null;
	}

	@Override
	public void process(Packet packet) throws ComponentException, TigaseStringprepException {
	}

	public void publishEvent(Element event) {
		publishEvent(event.getName(), event.getXMLNS(), event);
	}

	private void publishEvent(Element pubsubEventElem, String from, JID toJID) throws TigaseStringprepException {
		Packet message = Packet.packetInstance(new Element("message", new String[] { "to", "from", "id" },
				new String[] { toJID.toString(), from, nextStanzaID() }));
		message.getElement().addChild(pubsubEventElem);
		message.setXMLNS(Packet.CLIENT_XMLNS);

		message.setPermissions(Permissions.ADMIN);

		write(message);
	}

	public void publishEvent(final String name, final String xmlns, Element event) {
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
		final Collection<Subscription> subscribers = context.getSubscriptionStore().getSubscribersJIDs(name, xmlns);
		publishEvent(name, xmlns, event, subscribers);
	}

	public void publishEvent(String name, String xmlns, Element event, Collection<Subscription> subscribers) {
		try {
			final Element eventElem = new Element("event", new String[] { "xmlns" },
					new String[] { "http://jabber.org/protocol/pubsub#event" });
			final Element itemsElem = new Element("items", new String[] { "node" },
					new String[] { NodeNameUtil.createNodeName(name, xmlns) });
			eventElem.addChild(itemsElem);
			final Element itemElem = new Element("item");
			itemElem.addChild(event);
			itemsElem.addChild(itemElem);

			if (log.isLoggable(Level.FINER)) {
				log.log(Level.FINER, "Sending event ({0}, {1}, {2}) to {3}", new Object[] { name, xmlns, event, subscribers });
			}

			for (Subscription subscriber : subscribers) {

				String from;
				if (subscriber.getServiceJID() == null) {
					from = context.getComponentID().toString();
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

	@Override
	public void unregisterModule() {
		context.getEventBusInstance().removeHandler(null, null, eventBusEventFiredHandler);
		super.unregisterModule();
	}

}
