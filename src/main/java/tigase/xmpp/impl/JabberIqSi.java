/*  Package Jabber Server
 *  Copyright (C) 2001, 2002, 2003, 2004, 2005
 *  "Artur Hefczyc" <artur.hefczyc@tigase.org>
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software Foundation,
 *  Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
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
	{ return Arrays.copyOf(ELEMENTS, ELEMENTS.length); }

  public String[] supNamespaces()
	{ return Arrays.copyOf(XMLNSS, XMLNSS.length); }

  public Element[] supDiscoFeatures(final XMPPResourceConnection session)
	{ return Arrays.copyOf(DISCO_FEATURES, DISCO_FEATURES.length); }

}
