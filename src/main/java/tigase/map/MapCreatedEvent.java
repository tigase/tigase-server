package tigase.map;

import java.util.Map;

import tigase.disteventbus.local.Event;

public class MapCreatedEvent implements Event {

	private final Map map;
	private final String type;
	private final String[] parameters;

	public MapCreatedEvent(Map map, String type, String... parameters) {
		this.map = map;
		this.type = type;
		this.parameters = parameters;
	}

	public Map getMap() {
		return map;
	}

	public String[] getParameters() {
		return parameters;
	}

	public String getType() {
		return type;
	}
}
