package tigase.map;

import java.util.Map;

public class MapDestroyedEvent {

	private final Map map;
	private final String type;

	public MapDestroyedEvent(Map map, String type) {
		this.map = map;
		this.type = type;
	}

	public Map getMap() {
		return map;
	}

	public String getType() {
		return type;
	}

}
