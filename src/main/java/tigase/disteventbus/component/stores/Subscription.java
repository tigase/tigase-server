package tigase.disteventbus.component.stores;

import tigase.xmpp.JID;

public class Subscription {

	private final JID jid;

	private JID serviceJID;

	private boolean inClusterSubscription;

	public Subscription(JID jid) {
		super();
		this.jid = jid;
	}

	public Subscription(JID jid, JID serviceJID) {
		super();
		this.jid = jid;
		this.serviceJID = serviceJID;
	}

	public boolean isInClusterSubscription() {
		return inClusterSubscription;
	}

	public void setInClusterSubscription(boolean inClusterSubscription) {
		this.inClusterSubscription = inClusterSubscription;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Subscription other = (Subscription) obj;
		if (jid == null) {
			if (other.jid != null)
				return false;
		} else if (!jid.equals(other.jid))
			return false;
		return true;
	}

	public JID getJid() {
		return jid;
	}

	public JID getServiceJID() {
		return serviceJID;
	}

	public void setServiceJID(JID serviceJID) {
		this.serviceJID = serviceJID;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((jid == null) ? 0 : jid.hashCode());
		return result;
	}

	@Override
	public String toString() {
		return "Subscription{" + "jid=" + jid + ", serviceJID=" + serviceJID + '}';
	}
}
