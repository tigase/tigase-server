package tigase.disteventbus.component;

import tigase.criteria.Criteria;
import tigase.xml.Element;

public class ElemPathCriteria implements Criteria {

	public static void main(String[] args) {
		// ElemPathCriteria c = new ElemPathCriteria(new String[] { "iq",
		// "pubsub", "subscribe" }, new String[] { null,
		// "http://jabber.org/protocol/pubsub", null });

		ElemPathCriteria c = new ElemPathCriteria(new String[] { "iq", "pubsub", "subscribe" }, new String[] { null,
				"http://jabber.org/protocol/pubsub", null });

		Element e = new Element("iq");
		Element x = new Element("pubsub", new String[] { "xmlns" }, new String[] { "http://jabber.org/protocol/pubsub" });
		x.addChild(new Element("subscribe"));
		e.addChild(x);

		System.out.println(e.getChildrenStaticStr(new String[] { "iq", "pubsub" }));

		System.out.println(c.match(e));

	}

	private final String[] names;

	private final String[] xmlns;

	public ElemPathCriteria(String[] elemNames, String[] namespaces) {
		this.names = elemNames;
		this.xmlns = namespaces;
	}

	@Override
	public Criteria add(Criteria criteria) {
		throw new RuntimeException("UNSUPPORTED!");
	}

	@Override
	public boolean match(Element element) {

		boolean match = element.getName().equals(names[0]);
		if (match && xmlns[0] != null)
			match &= xmlns[0].equals(element.getXMLNS());

		Element child = element;
		int i = 1;
		for (; i < names.length; i++) {
			String n = names[i];
			String x = xmlns[i];

			child = child.getChildStaticStr(n, x);

			match &= child != null;

			if (!match)
				return match;

		}

		// TODO Auto-generated method stub
		return match;
	}
}
