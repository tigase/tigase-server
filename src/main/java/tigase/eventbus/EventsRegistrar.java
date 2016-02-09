package tigase.eventbus;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

public class EventsRegistrar {

	private final ConcurrentHashMap<String, EventInfo> events = new ConcurrentHashMap<>();

	public String getDescription(String eventName) {
		EventInfo info = events.get(eventName);
		return info == null ? null : info.getDescription();
	}

	public Collection<String> getRegisteredEvents() {
		ArrayList<String> result = new ArrayList<>();
		for (EventInfo info : events.values()) {
			if (info.isPrivateEvent())
				continue;
			result.add(info.event);
		}
		return result;
	}

	public boolean isRegistered(String eventName) {
		return this.events.containsKey(eventName);
	}

	public boolean isRegistered(Class<?> eventClass) {
		return this.events.containsKey(eventClass.getName());
	}

	public void register(String event, String description, boolean privateEvent) {
		EventInfo info = new EventInfo(event);
		info.setDescription(description);
		info.setPrivateEvent(privateEvent);

		this.events.put(event, info);
	}

	private static class EventInfo {

		private final String event;
		private String description;
		private boolean privateEvent;

		public EventInfo(String event) {
			this.event = event;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}

		public boolean isPrivateEvent() {
			return privateEvent;
		}

		public void setPrivateEvent(boolean privateEvent) {
			this.privateEvent = privateEvent;
		}
	}

}
