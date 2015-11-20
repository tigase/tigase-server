package tigase.disteventbus.component;

import tigase.component.modules.AbstractModule;
import tigase.xmpp.JID;

public abstract class AbstractEventBusModule extends AbstractModule {

	private static long id = 0;

	private EventBusComponent component;

	protected boolean isClusteredEventBus(final JID jid) {
		return jid.getLocalpart().equals("eventbus") && component.getNodesConnected().contains(jid);
	}

	protected String nextStanzaID() {

		String prefix = component.getComponentId().getDomain();

		synchronized (this) {
			return prefix + "-" + (++id);
		}

	}

}
