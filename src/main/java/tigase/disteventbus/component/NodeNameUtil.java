package tigase.disteventbus.component;

public class NodeNameUtil {

	public static String createNodeName(String eventName, String xmlns) {
		return (eventName == null ? "*" : eventName) + "|" + xmlns;
	}

	public static String[] parseNodeName(String nodeName) {
		String[] x = nodeName.split("\\|", 2);
		return new String[] { x[0].equals("*") ? null : x[0], x[1] };
	}

	private NodeNameUtil() {
	}

}
