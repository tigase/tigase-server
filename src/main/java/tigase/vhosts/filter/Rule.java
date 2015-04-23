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

import java.util.Objects;

/**
 *
 * @author Wojtek
 */
public class Rule implements Comparable<Rule> {

	int id;
	boolean allow;
	RuleType type;
	JID value;

	public Rule( int id, boolean allow, RuleType type, JID value ) {
		this.id = id;
		this.allow = allow;
		this.type = type;
		this.value = value;
	}

	@Override
	public int compareTo( Rule o ) {
		return id > o.getId() ? +1 : id < o.getId() ? -1 : 0;
	}

	@Override
	public boolean equals( Object obj ) {
		if ( obj == null ){
			return false;
		}
		if ( getClass() != obj.getClass() ){
			return false;
		}
		final Rule other = (Rule) obj;
		if ( this.id != other.id ){
			return false;
		}
		if ( this.allow != other.allow ){
			return false;
		}
		if ( this.type != other.type ){
			return false;
		}
		if ( !Objects.equals( this.value, other.value ) ){
			return false;
		}
		return true;
	}

	public boolean isAllowed() {
		return allow;
	}

	protected int getId() {
		return id;
	}

	@Override
	public int hashCode() {
		int hash = 5;
		hash = 11 * hash + this.id;
		hash = 11 * hash + ( this.allow ? 1 : 0 );
		hash = 11 * hash + Objects.hashCode( this.type );
		if ( value != null ){
			hash = 11 * hash + value.toString().hashCode();
		}
		return hash;
	}

	@Override
	public String toString() {
		return "Rule{" + "id=" + id + ", allow=" + allow + ", type=" + type + ", value=" + value + '}';
	}

	public String toConfigurationString() {
		return id + "|" + (allow ? "allow" : "deny") + "|" + type +
					 (type == RuleType.domain || type == RuleType.jid ? "|" + value :  "") + ";";
	}

	boolean isMatched( JID source, JID destination ) {
		boolean result = false;
		switch ( type ) {
			case all:
				result = true;
				break;
			case self:
				if ( source != null && destination != null ){
					result = source.getBareJID().equals( destination.getBareJID() );
				} else if (source == null ) {
					result = true;
				}
				break;
			case domain:
				if ( value != null && destination != null ){
					result = value.getDomain().equals( destination.getDomain() );
				}
				if ( value != null && destination != null ){
					result |= value.getDomain().equals( source.getDomain() );
				}
				break;
			case jid:
				if ( value != null && destination != null ){
					result = value.getBareJID().equals( destination.getBareJID() );
				}
				if ( value != null && destination != null ){
					result |= value.getBareJID().equals( source.getBareJID() );
				}
				break;
		}

		return result;
	}

	public static enum RuleType {

		jid, self, domain, all
	};

}
