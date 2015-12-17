package tigase.disteventbus;

import tigase.disteventbus.objbus.ObjectsEventsBus;
import tigase.disteventbus.xmlbus.XMLEventsBus;

public interface EventBus extends XMLEventsBus, ObjectsEventsBus {
}
