package tigase.disteventbus.component;

import tigase.component.modules.AbstractModule;

public abstract class AbstractEventBusModule extends AbstractModule<EventBusContext> {

	private static long id = 0;

	protected String nextStanzaID() {

		String prefix = context.getComponentID().getDomain();

		synchronized (this) {
			return prefix + "-" + (++id);
		}

	}

}
