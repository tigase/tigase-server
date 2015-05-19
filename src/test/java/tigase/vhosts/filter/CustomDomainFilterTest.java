/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2015 "Tigase, Inc." <office@tigase.com>
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3 of the License,
 * or (at your option) any later version.
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
package tigase.vhosts.filter;

import tigase.xmpp.JID;

import tigase.util.TigaseStringprepException;
import tigase.vhosts.filter.Rule.RuleType;

import java.text.ParseException;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Wojtek
 */
public class CustomDomainFilterTest {

	public CustomDomainFilterTest() {
	}

	String[] rules = {
		"4|deny|all",
		"1|allow|self",
		"3|allow|jid|pubsub@test.com",
		"2|allow|jid|admin@test2.com", };


	@BeforeClass
	public static void setUpClass() {
	}

	@Before
	public void setUp() {
	}

	@Test
	public void testParseRules() throws TigaseStringprepException, ParseException {
		System.out.println( "parseRules" );

		Set<Rule> expResult = new TreeSet<>();
		Rule rule = new Rule( 1, true, RuleType.self, null );

		if ( rule != null ){
			expResult.add( rule );
		}
		rule = new Rule( 2, true, RuleType.jid, JID.jidInstance( "admin@test2.com" ) );
		if ( rule != null ){
			expResult.add( rule );
		}
		rule = new Rule( 3, true, RuleType.jid, JID.jidInstance( "pubsub@test.com" ) );
		if ( rule != null ){
			expResult.add( rule );
		}
		rule = new Rule( 4, false, RuleType.all, null );
		if ( rule != null ){
			expResult.add( rule );
		}

		Set<Rule> result = CustomDomainFilter.parseRules( rules );
		assertEquals( expResult, result );
	}

	@Test
	public void testParseRulesString() throws TigaseStringprepException, ParseException {
		System.out.println( "parseRules" );
		String rulseString = "4|deny|all;1|allow|self;3|allow|jid|pubsub@test.com;2|allow|jid|admin@test2.com";

		Set<Rule> expResult = new TreeSet<>();
		Rule rule = new Rule( 1, true, RuleType.self, null );

		if ( rule != null ){
			expResult.add( rule );
		}
		rule = new Rule( 2, true, RuleType.jid, JID.jidInstance( "admin@test2.com" ) );
		if ( rule != null ){
			expResult.add( rule );
		}
		rule = new Rule( 3, true, RuleType.jid, JID.jidInstance( "pubsub@test.com" ) );
		if ( rule != null ){
			expResult.add( rule );
		}
		rule = new Rule( 4, false, RuleType.all, null );
		if ( rule != null ){
			expResult.add( rule );
		}

		Set<Rule> result = CustomDomainFilter.parseRules( rulseString );
		assertEquals( expResult, result );

		rulseString = "1|allow|self;2|allow|jid|admin@test2.com;3|allow|jid|pubsub@test.com;4|deny|all;";
		String resultString = new String();
		for ( Rule res : result) {
			resultString += res.toConfigurationString();
		}
		assertEquals( rulseString, resultString );

	}

	@Test(expected = ParseException.class)
	public void testParseRulesException() throws TigaseStringprepException, ParseException {
		String[] rules_fail = {
			"8|deny|||self,",
			"|||18|||deny,self::::"
		};
		Set<Rule> result = CustomDomainFilter.parseRules( rules_fail );
	}

	@Test
	public void testIsAllowed() throws TigaseStringprepException, ParseException {

		JID jid1_r1 = JID.jidInstance( "user1", "domain1", "resource1" );
		JID jid1_r2 = JID.jidInstance( "user1", "domain1", "resource2" );
		JID jid2_r1 = JID.jidInstance( "user2", "domain1", "resource1" );
		JID jid3_r1 = JID.jidInstance( "user3", "domain1", "resource1" );
		JID admin = JID.jidInstance( "admin", "test2.com" );
		JID pubsub = JID.jidInstance( "pubsub", "test.com" );

		boolean allowed = CustomDomainFilter.isAllowed( jid1_r1, jid1_r2, rules );
		assertTrue( "should be allowed / self / permitted jid", allowed );

		allowed = CustomDomainFilter.isAllowed( jid1_r1, admin, rules );
		assertTrue( "should be allowed / permitted jid", allowed );

		allowed = CustomDomainFilter.isAllowed( jid1_r1, pubsub, rules );
		assertTrue( "should be allowed / permitted jid", allowed );

		allowed = CustomDomainFilter.isAllowed( jid1_r1, jid2_r1, rules );
		assertFalse( "should be denyed / permitted jid", allowed );

		allowed = CustomDomainFilter.isAllowed( jid2_r1, jid2_r1, rules );
		assertTrue( "should be allowed / self", allowed );

		allowed = CustomDomainFilter.isAllowed( jid3_r1, jid2_r1, rules );
		assertFalse( "should be denied / not permitted jids", allowed );

	}

}
