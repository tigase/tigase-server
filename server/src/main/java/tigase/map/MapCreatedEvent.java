package tigase.map;

import java.util.Map;

public class MapCreatedEvent {

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
