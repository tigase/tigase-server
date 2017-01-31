package tigase.disteventbus.component;

import tigase.component.modules.AbstractModule;
import tigase.xmpp.JID;

public abstract class AbstractEventBusModule extends AbstractModule<EventBusContext> {

	private static long id = 0;

	protected boolean isClusteredEventBus(final JID jid) {
		return jid.getLocalpart().equals("eventbus") && context.getConnectedNodes().contains(jid);
	}

	protected String nextStanzaID() {

		String prefix = context.getComponentID().getDomain();

		synchronized (this) {
			return prefix + "-" + (++id);
		}

	}

}
