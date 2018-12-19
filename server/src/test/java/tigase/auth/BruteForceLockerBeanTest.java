package tigase.auth;

import org.junit.Assert;
import org.junit.Test;
import tigase.eventbus.impl.EventBusSerializer;
import tigase.xml.Element;
import tigase.xmpp.jid.BareJID;

import java.util.HashMap;

public class BruteForceLockerBeanTest {

	@Test
	public void testNestedMaps() {
		EventBusSerializer converter = new EventBusSerializer();
		Event event = new Event();
		event.getHolder().addIP("1.2.3.4");

		event.getHolder().addIP("1.2.3.4");
		event.getHolder().addIP("1.2.3.4");
		event.getHolder().addIP("1.2.3.4");
		event.getHolder().addIP("1.2.3.5");

		event.getHolder().addJID(BareJID.bareJIDInstanceNS("a@b.c"));
		event.getHolder().addJID(BareJID.bareJIDInstanceNS("b@b.c"));
		event.getHolder().addJID(BareJID.bareJIDInstanceNS("c@b.c"));
		event.getHolder().addJID(BareJID.bareJIDInstanceNS("c@b.c"));
		event.getHolder().addJID(BareJID.bareJIDInstanceNS("c@b.c"));

		Element s = converter.serialize(event);

		System.out.println(s.toString());

	}

	@Test
	public void testStatsSerializer() {
		BruteForceLockerBean.StatHolder sh1 = new BruteForceLockerBean.StatHolder();

		sh1.addIP("1.2.3.4");
		sh1.addIP("1.2.3.4");
		sh1.addIP("1.2.3.4");
		sh1.addIP("1.2.3.5");

		sh1.addJID(BareJID.bareJIDInstanceNS("a@b.c"));
		sh1.addJID(BareJID.bareJIDInstanceNS("b@b.c"));
		sh1.addJID(BareJID.bareJIDInstanceNS("c@b.c"));
		sh1.addJID(BareJID.bareJIDInstanceNS("c@b.c"));
		sh1.addJID(BareJID.bareJIDInstanceNS("c@b.c"));

		String[] parcel = sh1.encodeToStrings();

		BruteForceLockerBean.StatHolder sh2 = new BruteForceLockerBean.StatHolder();

		sh2.fillFromString(parcel);

		Assert.assertEquals(sh1.getIps().size(), sh2.getIps().size());
		Assert.assertEquals(sh1.getJids().size(), sh2.getJids().size());

		sh1.getIps().forEach((ip, count) -> Assert.assertEquals(count, sh2.getIps().get(ip)));
		sh1.getJids().forEach((jid, count) -> Assert.assertEquals(count, sh2.getJids().get(jid)));
	}

	@Test
	public void test3InvalidLoginsAndWait() {
		BruteForceLockerBean bean = new BruteForceLockerBean();
		bean.setMap(new HashMap<>());
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

	public class Event {

		private BruteForceLockerBean.StatHolder holder = new BruteForceLockerBean.StatHolder();
		private String name;

		public BruteForceLockerBean.StatHolder getHolder() {
			return holder;
		}

		public void setHolder(BruteForceLockerBean.StatHolder holder) {
			this.holder = holder;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

}