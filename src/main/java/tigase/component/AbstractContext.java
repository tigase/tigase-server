package tigase.component;

import tigase.component.eventbus.EventBus;
import tigase.component.modules.ModuleProvider;

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

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getComponentVersion() {
		return component.getComponentVersion();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getDiscoCategory() {
		return component.getDiscoCategory();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getDiscoCategoryType() {
		return component.getDiscoCategoryType();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getDiscoDescription() {
		return component.getDiscoDescription();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public EventBus getEventBus() {
		return component.getEventBus();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ModuleProvider getModuleProvider() {
		return component.getModuleProvider();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public PacketWriter getWriter() {
		return component.getWriter();
	}

}
