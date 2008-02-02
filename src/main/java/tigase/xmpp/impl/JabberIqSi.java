/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2007 "Artur Hefczyc" <artur.hefczyc@tigase.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 *
 * $Rev$
 * Last modified by $Author$
 * $Date$
 */
package tigase.xmpp.impl;

import java.util.Arrays;
import java.util.logging.Logger;
import tigase.xmpp.XMPPResourceConnection;
import tigase.xml.Element;

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
public abstract class JabberIqSi extends SimpleForwarder {

  private static final Logger log =
		Logger.getLogger("tigase.xmpp.impl.JabberIqSi");

  private static final String XMLNS = "http://jabber.org/protocol/si";
	private static final String ID = XMLNS;
  private static final String[] ELEMENTS = {"si"};
  private static final String[] XMLNSS = {XMLNS};
  private static final Element[] DISCO_FEATURES =
	{
		new Element("feature",
			new String[] {"var"},
			new String[] {"http://jabber.org/protocol/si"}),
		new Element("feature",
			new String[] {"var"},
			new String[] {"http://jabber.org/protocol/si/profile/file-transfer"})
	};

	public String id() { return ID; }

	public String[] supElements()
	{ return ELEMENTS; }

  public String[] supNamespaces()
	{ return XMLNSS; }

  public Element[] supDiscoFeatures(final XMPPResourceConnection session)
	{ return DISCO_FEATURES; }

}
