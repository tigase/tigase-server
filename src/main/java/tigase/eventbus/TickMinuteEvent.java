package tigase.eventbus;

public class TickMinuteEvent {

	private final long timestamp;

	public TickMinuteEvent(long timestamp) {
		this.timestamp = timestamp;
	}

	public long getTimestamp() {
		return timestamp;
	}

	@Override
	public String toString() {
		final StringBuffer sb = new StringBuffer("TickMinuteEvent{");
		sb.append("timestamp=").append(timestamp);
		sb.append('}');
		return sb.toString();
	}
}
