package tigase.map;

import java.util.Map;

import tigase.disteventbus.objbus.Event;
import tigase.disteventbus.objbus.ObjEventHandler;

public interface MapDestroyedEventHandler extends ObjEventHandler {

	void onMapDestroyed(Map map, String type);

	class MapDestroyedEvent extends Event<MapDestroyedEventHandler> {

		private Map map;
		private String type;

		public MapDestroyedEvent(Map map, String type) {
			this.map = map;
			this.type = type;
		}

		@Override
		protected void dispatch(MapDestroyedEventHandler handler) throws Exception {
			handler.onMapDestroyed(this.map, this.type);
		}
	}

}
