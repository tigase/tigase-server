package tigase.auth;

import org.junit.Assert;
import org.junit.Test;
import tigase.xmpp.jid.BareJID;

public class BruteForceLockerBeanTest {

	@Test
	public void test3InvalidLoginsAndWait() {
		BruteForceLockerBean bean = new BruteForceLockerBean();
		bean.initialize();
		bean.clearAll();

		bean.addInvalidLogin(null, "1.2.3.4", BareJID.bareJIDInstanceNS("a@bc.d"), 100000);
		Assert.assertTrue(bean.isLoginAllowed(null, "1.2.3.4", BareJID.bareJIDInstanceNS("a@bc.d"), 100001));
		bean.addInvalidLogin(null, "1.2.3.4", BareJID.bareJIDInstanceNS("a@bc.d"), 100002);
		Assert.assertTrue(bean.isLoginAllowed(null, "1.2.3.4", BareJID.bareJIDInstanceNS("a@bc.d"), 100003));
		bean.addInvalidLogin(null, "1.2.3.4", BareJID.bareJIDInstanceNS("a@bc.d"), 100004);
		Assert.assertTrue(bean.isLoginAllowed(null, "1.2.3.4", BareJID.bareJIDInstanceNS("a@bc.d"), 100005));

		//  invalid login after lock time
		bean.addInvalidLogin(null, "1.2.3.4", BareJID.bareJIDInstanceNS("a@bc.d"), 100006 + 10_000);
		Assert.assertTrue(bean.isLoginAllowed(null, "1.2.3.4", BareJID.bareJIDInstanceNS("a@bc.d"), 100007));
	}

	@Test
	public void test4InvalidLogins() {
		BruteForceLockerBean bean = new BruteForceLockerBean();
		bean.initialize();
		bean.clearAll();

		Assert.assertTrue(bean.isLoginAllowed(null, "1.2.3.4", BareJID.bareJIDInstanceNS("a@bc.d"), 99999));
		bean.addInvalidLogin(null, "1.2.3.4", BareJID.bareJIDInstanceNS("a@bc.d"), 100000);
		Assert.assertTrue(bean.isLoginAllowed(null, "1.2.3.4", BareJID.bareJIDInstanceNS("a@bc.d"), 100001));
		bean.addInvalidLogin(null, "1.2.3.4", BareJID.bareJIDInstanceNS("a@bc.d"), 100002);
		Assert.assertTrue(bean.isLoginAllowed(null, "1.2.3.4", BareJID.bareJIDInstanceNS("a@bc.d"), 100003));
		bean.addInvalidLogin(null, "1.2.3.4", BareJID.bareJIDInstanceNS("a@bc.d"), 100004);
		Assert.assertTrue(bean.isLoginAllowed(null, "1.2.3.4", BareJID.bareJIDInstanceNS("a@bc.d"), 100005));

		// one invalid login too much
		bean.addInvalidLogin(null, "1.2.3.4", BareJID.bareJIDInstanceNS("a@bc.d"), 100006);
		Assert.assertFalse(bean.isLoginAllowed(null, "1.2.3.4", BareJID.bareJIDInstanceNS("a@bc.d"), 100007));
		// allowed from different IP
		Assert.assertTrue(bean.isLoginAllowed(null, "1.2.3.5", BareJID.bareJIDInstanceNS("a@bc.d"), 100007));
		// allowed for different JID
		Assert.assertTrue(bean.isLoginAllowed(null, "1.2.3.4", BareJID.bareJIDInstanceNS("b@bc.d"), 100007));

		// try after lock time
		Assert.assertTrue(bean.isLoginAllowed(null, "1.2.3.4", BareJID.bareJIDInstanceNS("a@bc.d"), 100007 + 10_000));

	}

}