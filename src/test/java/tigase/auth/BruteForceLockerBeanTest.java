/**
 * Tigase XMPP Server - The instant messaging server
 * Copyright (C) 2004 Tigase, Inc. (office@tigase.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 */
package tigase.auth;

import org.junit.Assert;
import org.junit.Test;
import tigase.eventbus.impl.EventBusSerializer;
import tigase.kernel.DefaultTypesConverter;
import tigase.xml.Element;
import tigase.xmpp.jid.BareJID;

import java.util.HashMap;

public class BruteForceLockerBeanTest {

	@Test
	public void testKeyValueSerialization() {
		final BruteForceLockerBean.Key k1 = new BruteForceLockerBean.Key("1.2.3.4", "a@b.c", "c.d");
		final BruteForceLockerBean.Value v1 = new BruteForceLockerBean.Value("c.d", "1.2.3.4",
																			 BareJID.bareJIDInstanceNS("a@b.c"));
		v1.setBadLoginCounter(412);
		v1.setInvalidateAtTime(8352);

		DefaultTypesConverter converter = new DefaultTypesConverter();

		String k1s = converter.toString(k1);
		String v1s = converter.toString(v1);

		final BruteForceLockerBean.Key k2 = converter.convert(k1s, k1.getClass());
		final BruteForceLockerBean.Value v2 = converter.convert(v1s, v1.getClass());

		Assert.assertEquals("1.2.3.4", k2.getIp());
		Assert.assertEquals("a@b.c", k2.getJid());
		Assert.assertEquals(k1, k2);

		Assert.assertEquals(412, v2.getBadLoginCounter());
		Assert.assertEquals(v1.getBadLoginCounter(), v2.getBadLoginCounter());

		Assert.assertEquals(8352, v2.getInvalidateAtTime());
		Assert.assertEquals(v1.getInvalidateAtTime(), v2.getInvalidateAtTime());

	}

	@Test
	public void testStatsSerializer() {
		BruteForceLockerBean.StatHolder sh1 = new BruteForceLockerBean.StatHolder();
		sh1.addIP("1.2.3.4");
		sh1.addIP("1.2.3.4");
		sh1.addIP("1.2.3.5");
		sh1.addIP("2.2.3.4");
		sh1.addIP("3.2.3.4");
		sh1.addIP("4.2.3.4");
		sh1.addIP("5.2.3.4");
		sh1.addIP("6.2.3.4");
		sh1.addIP("7.2.3.4");
		sh1.addIP("8.2.3.4");
		sh1.addIP("9.2.3.4");
		sh1.addIP("10.2.3.4");
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

		Assert.assertEquals(11, sh2.getIps().size());
		Assert.assertEquals(3, sh2.getJids().size());

		sh1.getIps().forEach((ip, count) -> Assert.assertEquals(count, sh2.getIps().get(ip)));
		sh1.getJids().forEach((jid, count) -> Assert.assertEquals(count, sh2.getJids().get(jid)));
	}

	@Test
	public void testStatsSerializer_empty() {
		BruteForceLockerBean.StatHolder sh1 = new BruteForceLockerBean.StatHolder();
		String[] parcel = sh1.encodeToStrings();

		BruteForceLockerBean.StatHolder sh2 = new BruteForceLockerBean.StatHolder();
		sh2.fillFromString(parcel);

		Assert.assertEquals(sh1.getIps().size(), sh2.getIps().size());
		Assert.assertEquals(sh1.getJids().size(), sh2.getJids().size());

		Assert.assertEquals(0, sh2.getIps().size());
		Assert.assertEquals(0, sh2.getJids().size());

		sh1.getIps().forEach((ip, count) -> Assert.assertEquals(count, sh2.getIps().get(ip)));
		sh1.getJids().forEach((jid, count) -> Assert.assertEquals(count, sh2.getJids().get(jid)));
	}

	@Test
	public void testStatsSerializer_noips() {
		BruteForceLockerBean.StatHolder sh1 = new BruteForceLockerBean.StatHolder();
		sh1.addJID(BareJID.bareJIDInstanceNS("a@b.c"));
		sh1.addJID(BareJID.bareJIDInstanceNS("b@b.c"));
		String[] parcel = sh1.encodeToStrings();

		BruteForceLockerBean.StatHolder sh2 = new BruteForceLockerBean.StatHolder();
		sh2.fillFromString(parcel);

		Assert.assertEquals(sh1.getIps().size(), sh2.getIps().size());
		Assert.assertEquals(sh1.getJids().size(), sh2.getJids().size());

		Assert.assertEquals(0, sh2.getIps().size());
		Assert.assertEquals(2, sh2.getJids().size());

		sh1.getIps().forEach((ip, count) -> Assert.assertEquals(count, sh2.getIps().get(ip)));
		sh1.getJids().forEach((jid, count) -> Assert.assertEquals(count, sh2.getJids().get(jid)));
	}

	@Test
	public void testStatsSerializer_nojids() {
		BruteForceLockerBean.StatHolder sh1 = new BruteForceLockerBean.StatHolder();
		sh1.addIP("1.2.3.4");
		sh1.addIP("1.2.3.4");
		sh1.addIP("1.2.3.5");
		sh1.addIP("2.2.3.4");
		sh1.addIP("3.2.3.4");
		sh1.addIP("4.2.3.4");
		String[] parcel = sh1.encodeToStrings();

		BruteForceLockerBean.StatHolder sh2 = new BruteForceLockerBean.StatHolder();
		sh2.fillFromString(parcel);

		Assert.assertEquals(sh1.getIps().size(), sh2.getIps().size());
		Assert.assertEquals(sh1.getJids().size(), sh2.getJids().size());

		Assert.assertEquals(5, sh2.getIps().size());
		Assert.assertEquals(0, sh2.getJids().size());

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
	public void testEventSerialization() {
		BruteForceLockerBean.StatHolder holder = new BruteForceLockerBean.StatHolder();
		holder.addIP("1.2.3.4");
		holder.addIP("1.2.3.4");
		holder.addIP("1.2.3.5");
		holder.addIP("2.2.3.4");
		holder.addIP("3.2.3.4");
		holder.addIP("4.2.3.4");
		holder.addIP("5.2.3.4");
		holder.addIP("6.2.3.4");
		holder.addIP("7.2.3.4");
		holder.addIP("8.2.3.4");
		holder.addIP("9.2.3.4");
		holder.addIP("10.2.3.4");
		holder.addJID(BareJID.bareJIDInstanceNS("a@b.c"));
		holder.addJID(BareJID.bareJIDInstanceNS("b@b.c"));
		holder.addJID(BareJID.bareJIDInstanceNS("c@b.c"));
		holder.addJID(BareJID.bareJIDInstanceNS("c@b.c"));
		holder.addJID(BareJID.bareJIDInstanceNS("c@b.c"));

		BruteForceLockerBean.StatisticsEmitEvent event1 = new BruteForceLockerBean.StatisticsEmitEvent();
		event1.setNodeName("name-node-123");
		event1.setStatHolder(holder);

		final EventBusSerializer serializer = new EventBusSerializer();
		Element element = serializer.serialize(event1);
		BruteForceLockerBean.StatisticsEmitEvent event2 = serializer.deserialize(element);

		Assert.assertEquals(event2.getNodeName(), "name-node-123");
		Assert.assertEquals(event1.getNodeName(), event2.getNodeName());

		Assert.assertEquals(event1.getStatHolder().getIps().size(), event2.getStatHolder().getIps().size());
		Assert.assertEquals(event1.getStatHolder().getJids().size(), event2.getStatHolder().getJids().size());

		event1.getStatHolder()
				.getIps()
				.forEach((ip, count) -> Assert.assertEquals(count, event2.getStatHolder().getIps().get(ip)));
		event1.getStatHolder()
				.getJids()
				.forEach((jid, count) -> Assert.assertEquals(count, event2.getStatHolder().getJids().get(jid)));
	}

	@Test
	public void test4InvalidLogins() {
		BruteForceLockerBean bean = new BruteForceLockerBean();
		bean.setMap(new HashMap<>());
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