package tigase.auth;

import java.util.Map;

public class MechanismSelectorFactory {

	private static final String MECHANISM_SELECTOR_KEY = "mechanism-selector";

	@SuppressWarnings("unchecked")
	public MechanismSelector create(Map<String, Object> settings) throws ClassNotFoundException, InstantiationException,
			IllegalAccessException {
		Class<? extends MechanismSelector> selectorClass;
		if (settings.containsKey(MECHANISM_SELECTOR_KEY)) {
			selectorClass = (Class<MechanismSelector>) Class.forName((String) settings.get(MECHANISM_SELECTOR_KEY));
		} else
			selectorClass = DefaultMechanismSelector.class;

		final MechanismSelector selector = selectorClass.newInstance();
		selector.init(settings);

		return selector;
	}

}
