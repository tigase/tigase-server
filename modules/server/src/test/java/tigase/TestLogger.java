package tigase;

import java.util.logging.Logger;

public class TestLogger {

	public static Logger getLogger(Class clazz) {
		return Logger.getLogger("test." + clazz.getName());
	}
}
