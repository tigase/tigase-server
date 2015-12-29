package tigase.disteventbus;

import java.util.Set;
import java.util.concurrent.Executor;

import tigase.disteventbus.clustered.DefaultClusteredEventsBus;
import tigase.disteventbus.clustered.EventName;
import tigase.disteventbus.local.DefaultLocalEventsBus;
import tigase.disteventbus.local.Event;
import tigase.disteventbus.local.EventHandler;
import tigase.disteventbus.local.RegistrationException;
import tigase.xml.Element;

/**
 * Created by bmalkow on 17.12.2015.
 */
public class CombinedEventBus implements EventBus {

	private final DefaultLocalEventsBus objectsEventsBus = new DefaultLocalEventsBus();
	private final DefaultClusteredEventsBus xmlEventsBus = new DefaultClusteredEventsBus();

	@Override
	public void addHandler(String name, String xmlns, tigase.disteventbus.clustered.EventHandler handler) {
		xmlEventsBus.addHandler(name, xmlns, handler);
	}

	@Override
	public void addHandler(Class<? extends Event> type, tigase.disteventbus.local.EventHandler handler) {
		objectsEventsBus.addHandler(type, handler);
	}

	@Override
	public void addHandler(EventHandler handler) {
		objectsEventsBus.addHandler(handler);
	}

	public void doFire(String name, String xmlns, Element event) {
		xmlEventsBus.doFire(name, xmlns, event);
	}

	@Override
	public void fire(Event event) {
		objectsEventsBus.fire(event);
	}

	@Override
	public void fire(Element event) {
		xmlEventsBus.fire(event);
	}

	public Set<EventName> getAllListenedEvents() {
		return xmlEventsBus.getAllListenedEvents();
	}

	public Executor getExecutor() {
		return xmlEventsBus.getExecutor();
	}

	public void setExecutor(Executor executor) {
		xmlEventsBus.setExecutor(executor);
	}

	public boolean hasHandlers(String name, String xmlns) {
		return xmlEventsBus.hasHandlers(name, xmlns);
	}

	@Override
	public void registerAll(Object consumer) throws RegistrationException {
		objectsEventsBus.registerAll(consumer);
	}

	@Override
	public void remove(EventHandler handler) {
		objectsEventsBus.remove(handler);
	}

	@Override
	public void remove(Class<? extends Event> type, EventHandler handler) {
		objectsEventsBus.remove(type, handler);
	}

	@Override
	public void removeHandler(String name, String xmlns, tigase.disteventbus.clustered.EventHandler handler) {
		xmlEventsBus.removeHandler(name, xmlns, handler);
	}

	public void setThreadPool(int pool) {
		xmlEventsBus.setThreadPool(pool);
	}

	@Override
	public void unregisterAll(Object consumer) {
		objectsEventsBus.unregisterAll(consumer);
	}
}
