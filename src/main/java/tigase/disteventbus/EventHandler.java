package tigase.disteventbus;

import tigase.xml.Element;

public interface EventHandler {

	void onEvent(String name, String xmlns, Element event);

}
