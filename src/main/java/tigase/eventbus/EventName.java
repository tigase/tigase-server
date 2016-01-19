package tigase.eventbus;

public class EventName {

	private final String eventName;

	private final String eventPackage;

	public EventName(String eventName) {
		int i = eventName.lastIndexOf(".");
		String tmp = i >= 0 ? eventName.substring(0, i) : "";
		this.eventPackage = tmp.equals("*") ? null : tmp;

		tmp = eventName.substring(i + 1);
		this.eventName = tmp.equals("*") ? null : tmp;
	}

	public EventName(String eventName, String eventPackage) {
		super();
		this.eventName = eventName;
		this.eventPackage = eventPackage;
	}

	public final static String toString(final String eventName, final String eventPackage) {
		String result = "";
		if (eventPackage == null)
			result += "*";
		else
			result += eventPackage;

		if (!result.isEmpty())
			result += ".";

		if (eventName == null)
			result += "*";
		else
			result += eventName;

		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		EventName other = (EventName) obj;
		if (eventName == null) {
			if (other.eventName != null)
				return false;
		} else if (!eventName.equals(other.eventName))
			return false;
		if (eventPackage == null) {
			if (other.eventPackage != null)
				return false;
		} else if (!eventPackage.equals(other.eventPackage))
			return false;
		return true;
	}

	public String getName() {
		return eventName;
	}

	public String getPackage() {
		return eventPackage;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((eventName == null) ? 0 : eventName.hashCode());
		result = prime * result + ((eventPackage == null) ? 0 : eventPackage.hashCode());
		return result;
	}

	@Override
	public String toString() {
		return toString(eventName, eventPackage);
	}

}