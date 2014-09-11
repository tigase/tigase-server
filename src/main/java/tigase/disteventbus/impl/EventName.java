package tigase.disteventbus.impl;

import tigase.disteventbus.component.NodeNameUtil;

public class EventName {

	private final String name;

	private final String xmlns;

	public EventName(String name, String xmlns) {
		super();
		this.name = name;
		this.xmlns = xmlns;
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
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (xmlns == null) {
			if (other.xmlns != null)
				return false;
		} else if (!xmlns.equals(other.xmlns))
			return false;
		return true;
	}

	public String getName() {
		return name;
	}

	public String getXmlns() {
		return xmlns;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((xmlns == null) ? 0 : xmlns.hashCode());
		return result;
	}

	public String toEventBusNode() {
		return NodeNameUtil.createNodeName(name, xmlns);
	}

	@Override
	public String toString() {
		return "(" + name + ", " + xmlns + ")";
	}

}