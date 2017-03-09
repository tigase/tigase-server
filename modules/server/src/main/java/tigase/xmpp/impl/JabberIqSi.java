/*
 * JabberIqSi.java
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2013 "Tigase, Inc." <office@tigase.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License,
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
 *
 */



package tigase.xmpp.impl;

//~--- non-JDK imports --------------------------------------------------------

import tigase.server.Iq;

import tigase.xml.Element;

import tigase.xmpp.XMPPResourceConnection;

//~--- JDK imports ------------------------------------------------------------

import java.util.logging.Logger;

/**
 * XEP-0096: File Transfer
 * The class is not abstract in fact. Is has been made abstract artificially
 * to prevent from loading the class.
 *
 *
 * Created: Sat Jan 13 18:45:57 2007
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 * @deprecated This class has been deprecated and replaced with
 * <code>tigase.server.xmppsession.PacketFilter</code> code. The class is left
 * for educational purpose only and should not be used. It may be removed in
 * future releases.
 */
@Deprecated
public abstract class JabberIqSi
				extends SimpleForwarder {
	private static final Logger     log = Logger.getLogger(JabberIqSi.class.getName());
	private static final String     XMLNS    = "http://jabber.org/protocol/si";
	private static final String     ID       = XMLNS;
	private static final String[][] ELEMENTS = {
		{ Iq.ELEM_NAME, "si" }
	};
	private static final String[]   XMLNSS   = { XMLNS };
	private static final Element[]  DISCO_FEATURES = { new Element("feature",
			new String[] { "var" }, new String[] { "http://jabber.org/protocol/si" }),
			new Element("feature", new String[] { "var" }, new String[] {
					"http://jabber.org/protocol/si/profile/file-transfer" }) };

	//~--- methods --------------------------------------------------------------

	@Override
	public String id() {
		return ID;
	}

	@Override
	public String[][] supElementNamePaths() {
		return ELEMENTS;
	}

	@Override
	public String[] supNamespaces() {
		return XMLNSS;
	}

	@Override
	public Element[] supDiscoFeatures(final XMPPResourceConnection session) {
		return DISCO_FEATURES;
	}
}

