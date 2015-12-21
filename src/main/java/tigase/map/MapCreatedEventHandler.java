package tigase.map;

import java.util.Map;

import tigase.disteventbus.objbus.Event;
import tigase.disteventbus.objbus.ObjEventHandler;

public interface MapCreatedEventHandler extends ObjEventHandler {

	void onMapCreated(Map map, String type, String... parameters);

	class MapCreatedEvent extends Event<MapCreatedEventHandler> {

		private Map map;
		private String type;
		private String[] parameters;

		public MapCreatedEvent(Map map, String type, String... parameters) {
			this.map = map;
			this.type = type;
			this.parameters = parameters;
		}

		@Override
		protected void dispatch(MapCreatedEventHandler handler) throws Exception {
			handler.onMapCreated(this.map, this.type, this.parameters);
		}
	}

}
