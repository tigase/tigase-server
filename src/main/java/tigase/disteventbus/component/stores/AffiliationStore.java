package tigase.disteventbus.component.stores;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import tigase.xmpp.JID;

public class AffiliationStore {

	private final Map<JID, Affiliation> affiliations = new ConcurrentHashMap<JID, Affiliation>();

	public Affiliation getAffiliation(final JID jid) {
		Affiliation a = affiliations.get(jid);
		return a == null ? Affiliation.none : a;
	}

}
