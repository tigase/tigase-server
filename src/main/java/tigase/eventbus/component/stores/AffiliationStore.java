package tigase.eventbus.component.stores;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import tigase.kernel.beans.Bean;
import tigase.kernel.beans.config.ConfigField;
import tigase.xmpp.JID;

@Bean(name = "affiliations")
public class AffiliationStore {

	private final Map<JID, Affiliation> affiliations = new ConcurrentHashMap<JID, Affiliation>();

	@ConfigField(desc = "List of JIDs what can subscribe for events")
	private JID[] allowedSubscribers;

	public Affiliation getAffiliation(final JID jid) {
		Affiliation a = affiliations.get(jid);
		if (a == null && allowedSubscribers != null) {
			for (JID j : allowedSubscribers) {
				if (j.getResource() != null && j.equals(jid)) {
					return Affiliation.member;
				} else if (j.getResource() == null && j.getBareJID().equals(jid.getBareJID())) {
					return Affiliation.member;
				}
			}
		}
		return a == null ? Affiliation.none : a;
	}

	public JID[] getAllowedSubscribers() {
		return allowedSubscribers;
	}

	public void setAllowedSubscribers(JID[] allowedSubscribers) {
		this.allowedSubscribers = allowedSubscribers;
	}

	public void putAffiliation(JID jid, Affiliation affiliation) {
		this.affiliations.put(jid, affiliation);
	}

	public void removeAffiliation(JID jid) {
		this.affiliations.remove(jid);
	}
}
