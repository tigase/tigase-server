package tigase.component;

import tigase.component.modules.ModuleProvider;
import tigase.disteventbus.EventBus;
import tigase.xmpp.JID;

/**
 * Abstract basic implementation of {@linkplain Context} as delegator of
 * {@linkplain AbstractComponent}.
 * 
 * @author bmalkow
 * 
 */
public abstract class AbstractContext implements Context {

	private final AbstractComponent<?> component;

	public AbstractContext(AbstractComponent<?> component) {
		this.component = component;
	}

	@Override
	public JID getComponentID() {
		return component.getComponentId();
	}

	@Override
	public String getComponentVersion() {
		return component.getComponentVersion();
	}

	@Override
	public String getDiscoCategory() {
		return component.getDiscoCategory();
	}

	@Override
	public String getDiscoCategoryType() {
		return component.getDiscoCategoryType();
	}

	@Override
	public String getDiscoDescription() {
		return component.getDiscoDescription();
	}

	@Override
	public EventBus getEventBus() {
		return component.getEventBus();
	}

	@Override
	public ModuleProvider getModuleProvider() {
		return component.getModuleProvider();
	}

	@Override
	public PacketWriter getWriter() {
		return component.getWriter();
	}

}
