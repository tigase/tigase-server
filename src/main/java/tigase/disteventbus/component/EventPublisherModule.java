package tigase.disteventbus.component;

import java.util.Collection;
import java.util.logging.Level;

import tigase.component.exceptions.ComponentException;
import tigase.criteria.Criteria;
import tigase.disteventbus.EventHandler;
import tigase.disteventbus.LocalEventBus.LocalEventBusListener;
import tigase.server.Packet;
import tigase.server.Permissions;
import tigase.util.TigaseStringprepException;
import tigase.xml.Element;
import tigase.xmpp.JID;

public class EventPublisherModule extends AbstractEventBusModule {

	public final static String ID = "publisher";

	private final LocalEventBusListener eventBusListener = new LocalEventBusListener() {

		@Override
		public void onAddHandler(String name, String xmlns, EventHandler handler) {
		}

		@Override
		public void onFire(String name, String xmlns, Element event) {
			publishEvent(name, xmlns, event);
		}

		@Override
		public void onRemoveHandler(String name, String xmlns, EventHandler handler) {
		}
	};

	@Override
	public void afterRegistration() {
		super.afterRegistration();

		context.getEventBusInstance().addListener(eventBusListener);
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
		Packet message = Packet.packetInstance(new Element("message", new String[] { "to", "from", "id" }, new String[] {
				toJID.toString(), from, nextStanzaID() }));
		message.getElement().addChild(pubsubEventElem);
		message.setXMLNS(Packet.CLIENT_XMLNS);

		message.setPermissions(Permissions.ADMIN);
		write(message);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void publishEvent(String name, String xmlns, Element event) {
		final Collection subscribers = context.getSubscriptionStore().getSubscribersJIDs(name, xmlns);
		subscribers.addAll(context.getNonClusterSubscriptionStore().getSubscribersJIDs(name, xmlns));

		publishEvent(name, xmlns, event, subscribers);
	}

	public void publishEvent(String name, String xmlns, Element event, Collection<?> subscribers) {
		try {
			final Element eventElem = new Element("event", new String[] { "xmlns" },
					new String[] { "http://jabber.org/protocol/pubsub#event" });
			final Element itemsElem = new Element("items", new String[] { "node" }, new String[] { NodeNameUtil.createNodeName(
					name, xmlns) });
			eventElem.addChild(itemsElem);
			final Element itemElem = new Element("item");
			itemElem.addChild(event);
			itemsElem.addChild(itemElem);

			if (log.isLoggable(Level.FINER))
				log.finer("Sending event (" + name + ", " + xmlns + ") to " + subscribers);

			for (Object subscriber : subscribers) {
				String from;
				JID toJID;
				if (subscriber instanceof JID) {
					from = context.getComponentID().toString();
					toJID = (JID) subscriber;
				} else if (subscriber instanceof NonClusterSubscription) {
					from = ((NonClusterSubscription) subscriber).getServiceJID().toString();
					toJID = ((NonClusterSubscription) subscriber).getJid();
				} else
					throw new RuntimeException("Unknown Subscriber object");

				publishEvent(eventElem, from, toJID);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void unregisterModule() {
		context.getEventBusInstance().removeListener(eventBusListener);
		super.unregisterModule();
	}

}
