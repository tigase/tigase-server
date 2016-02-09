package tigase.eventbus.component.stores;

import org.junit.Assert;
import org.junit.Test;

import tigase.xmpp.JID;

public class AffiliationStoreTest {

	@Test
	public void testGetAffiliation() throws Exception {
		final AffiliationStore store = new AffiliationStore();
		store.setAllowedSubscribers(new JID[] { JID.jidInstance("a@b.c/d"), JID.jidInstance("x@y.z") });

		Assert.assertEquals(Affiliation.member, store.getAffiliation(JID.jidInstance("a@b.c/d")));
		Assert.assertEquals(Affiliation.member, store.getAffiliation(JID.jidInstance("x@y.z")));
		Assert.assertEquals(Affiliation.none, store.getAffiliation(JID.jidInstance("NONE@y.z")));

		Assert.assertEquals(Affiliation.none, store.getAffiliation(JID.jidInstance("a@b.c")));
		Assert.assertEquals(Affiliation.none, store.getAffiliation(JID.jidInstance("a@b.c/DIFFERENT")));
		Assert.assertEquals(Affiliation.member, store.getAffiliation(JID.jidInstance("x@y.z/any")));

	}
}