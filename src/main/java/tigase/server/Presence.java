/*
 * Presence.java
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2012 "Artur Hefczyc" <artur.hefczyc@tigase.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 *
 */



package tigase.server;

//~--- non-JDK imports --------------------------------------------------------

import tigase.util.TigaseStringprepException;

import tigase.xml.Element;

import tigase.xmpp.JID;

/**
 * Created: Dec 31, 2009 8:42:05 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class Presence
				extends Packet {
	/** Field description */
	public static final String ELEM_NAME = "presence";

	/** Field description */
	public static final String[] PRESENCE_ERROR_PATH = { ELEM_NAME, "error" };

	/** Field description */
	public static final String[] PRESENCE_PRIORITY_PATH = { ELEM_NAME, "priority" };
	public static final String[] PRESENCE_SHOW_PATH = { ELEM_NAME, "show" };

	//~--- constructors ---------------------------------------------------------

	/**
	 * Constructs ...
	 *
	 *
	 * @param elem
	 *
	 * @throws TigaseStringprepException
	 */
	public Presence(Element elem) throws TigaseStringprepException {
		super(elem);
	}

	/**
	 * Constructs ...
	 *
	 *
	 * @param elem
	 * @param stanzaFrom
	 * @param stanzaTo
	 */
	public Presence(Element elem, JID stanzaFrom, JID stanzaTo) {
		super(elem, stanzaFrom, stanzaTo);
	}

	//~--- get methods ----------------------------------------------------------

	@Override
	protected String[] getElNameErrorPath() {
		return PRESENCE_ERROR_PATH;
	}
}


//~ Formatted in Tigase Code Convention on 13/02/15
