package org.tigase;

import tigase.server.Packet;
import tigase.util.TigaseStringprepException;
import tigase.xml.Element;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.UUID;

public class PseudoRandomPacketGenerator {

	public long sum = 0;
	private long counter = 0;

	public static Packet generatePacket(String element, String xmlns, String queryXmlns)
			throws TigaseStringprepException {
		final Element elem = new Element(element);
		elem.setAttribute("id", String.valueOf(UUID.randomUUID()));
		if ("iq".equals(element) && null != queryXmlns) {
			final Element query = new Element("query", new String[]{"xmlns"}, new String[]{queryXmlns});
			elem.addChild(query);
		} else if (null != xmlns) {
			elem.setAttribute("xmlns", xmlns);
		}

		return Packet.packetInstance(elem);
	}

	public static void main(String[] args) {

		final PseudoRandomPacketGenerator generator = new PseudoRandomPacketGenerator();
//		final Queue<Packet> packets = generator.generateSemiRandomPacketQueue(10, 2);
		final Queue<Packet> packets = generator.generateSemiRandomPacketQueue(new String[]{"message"}, new String[]{},
		                                                                      10, 2);

		packets.forEach(System.out::println);
//		System.out.println(generator.sum);

	}

	public Queue<Packet> generateSemiRandomPacketQueue(long limit, int step) {
		return generateSemiRandomPacketQueue(null, null, limit, step);
	}

	public Queue<Packet> generateSemiRandomPacketQueue(String[] elements, String[] xmlns, long limit, int step) {

		Queue<Packet> packets = new ArrayDeque<>();

		if (null == xmlns) {
			xmlns = new String[]{null, "http://jabber.org/protocol/disco#items", "bind", "jabber:iq:roster", "session",
			                     "jabber:iq:register", "command:xmlns", "pubsub:xmlns", "http://jabber.org/protocol/disco#info"};
		}
		if (null == elements) {
			elements = new String[]{"message", "presence", "cluster", "iq", "auth", "abort", "failure", "success"};
		}

		for (int i = 0; i <= limit; i++) {
			try {
				packets.add(generatePacket(getNextItem(elements, step), getNextItem(xmlns, step),
				                           getNextItem(xmlns, step)));
				counter++;
			} catch (TigaseStringprepException e) {
			}
		}

		return packets;
	}

	private String getNextItem(String[] elements, int step) {
		if (null != elements && elements.length > 0) {
			final int idx = (int) ((step + counter) % elements.length);
			sum += idx;
			final String element = elements[idx];
			return element;
		} else {
			return null;
		}
	}
}

