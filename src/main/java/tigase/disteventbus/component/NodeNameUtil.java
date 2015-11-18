package tigase.disteventbus.component;

import tigase.disteventbus.impl.EventName;

public class NodeNameUtil {

	private NodeNameUtil() {
	}

	public static String createNodeName(String eventName, String xmlns) {
		return (eventName == null ? "*" : eventName) + "|" + (xmlns == null ? "*" : xmlns);
	}

	public static EventName parseNodeName(String nodeName) {
		int i = nodeName.indexOf('|');
		String n = nodeName.substring(0, i);
		String x = nodeName.substring(i + 1);
		return new EventName(n.equals("*") ? null : n, x.equals("*") ? null : x);
	}

	// public static EventName parseNodeName(String nodeName) {
	// String[] x = nodeName.split("\\|", 2);
	// return new EventName(x[0].equals("*") ? null : x[0], x[1].equals("*") ?
	// null : x[1]);
	// }

}
