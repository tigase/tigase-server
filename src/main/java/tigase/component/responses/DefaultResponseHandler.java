package tigase.component.responses;

import tigase.component.responses.ResponseManager.Entry;
import tigase.server.Packet;

public class DefaultResponseHandler implements Runnable {

	private final Entry entry;

	private final Packet packet;

	public DefaultResponseHandler(Packet packet, ResponseManager.Entry entry) {
		this.packet = packet;
		this.entry = entry;
	}

	@Override
	public void run() {
		final String type = this.packet.getElement().getAttributeStaticStr("type");

		if (type != null && type.equals("result")) {
			entry.getCallback().onSuccess(packet);
		} else if (type != null && type.equals("error")) {
			String condition = packet.getErrorCondition();
			entry.getCallback().onError(packet, condition);
		}
	}

}
