package tigase.disteventbus.xmlbus;

import tigase.xml.Element;

public interface EventHandler {

	void onEvent(String name, String xmlns, Element event);

}
