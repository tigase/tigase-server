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
import tigase.xmpp.impl.DomainFilter;

import tigase.vhosts.filter.Rule.RuleType;

import java.text.ParseException;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Wojtek
 */
public final class CustomDomainFilter {

	private CustomDomainFilter() {
	}

	private static final Logger log = Logger.getLogger( CustomDomainFilter.class.getName() );

	public static Set<Rule> parseRules( String rules ) throws ParseException {
		String[] rulesArr = rules.split( ";" );
		if ( rulesArr != null ){
			return parseRules( rulesArr );
		}
		return null;
	}

	public static Set<Rule> parseRules( String[] rules ) throws ParseException {

		Set rulesSet = new TreeSet<Rule>();
		for ( String rule : rules ) {

			String[] split = rule.split( "\\|" );
			if ( split != null && ( split.length == 3 || split.length == 4 ) ){

				try {
					Integer id = Integer.valueOf( split[0] );
					boolean allow = false;
					if ( split[1].equalsIgnoreCase( "allow" ) ){
						allow = true;
					} else if ( split[1].equalsIgnoreCase( "deny" ) ){
						allow = false;
					}
					RuleType type = RuleType.valueOf( split[2].toLowerCase() );

					JID jid = null;

					if ( split.length == 4 ){
						jid = JID.jidInstance( split[3] );
					}

					if (type == RuleType.jid && jid == null ) {
						throw new ParseException( "Error while pasing rule (no value for JID provided): " + rule, 0 );
					}

					rulesSet.add( new Rule( id, allow, type, jid ) );
				} catch ( Exception ex ) {
					log.log(Level.FINEST, "Error while pasing rule: " + rule, ex);
					throw new ParseException( "Error while pasing rule: " + rule, 0 );
				}
			} else {
				log.log(Level.FINEST, "Error while pasing rule (wrong number of parameters): " + rule);
				throw new ParseException( "Error while pasing rule: " + rule, 0 );
			}
		}

		return rulesSet;
	}

	public static boolean isAllowed( JID source, JID destination, String rules ) {
		try {
			Set<Rule> parseRules = parseRules( rules );
			if ( parseRules != null ){
				return isAllowed( source, destination, parseRules );
			} else {
				return true;
			}
		} catch ( ParseException e ) {
			return true;
		}
	}

	public static boolean isAllowed( JID source, JID destination, String[] rules ) {
		Set<Rule> parseRules = null;
		try {
			parseRules = parseRules( rules );
		} catch ( ParseException e ) {
			if (log.isLoggable( Level.WARNING)) {
				log.log( Level.WARNING, "Error while parsing rules: " + Arrays.toString( rules ), e);
			}
			return true;
		}
		if ( parseRules != null ){
			return isAllowed( source, destination, parseRules );
		} else {
			return true;
		}
	}

	public static boolean isAllowed( JID source, JID destination, Set<Rule> rules ) {
		if ( rules != null ){
			for ( Rule rule : rules ) {
				log.log(Level.FINEST, "Processing source: {0}, destination: {1}, against rule: {2}",
															new Object[] {source, destination, rule} );
				if ( rule.isMatched( source, destination ) ){
				log.log(Level.FINEST, "Matched source: {0}, destination: {1}, allowed: {2}",
															new Object[] {source, destination, rule.isAllowed()} );
					return rule.isAllowed();
				}
			}
		} else {
			return true;
		}
		return true;
	}
}
