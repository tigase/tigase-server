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

import java.util.logging.Logger;
import tigase.xmpp.XMPPResourceConnection;

/**
 * XEP-0096: File Transfer
 *
 *
 * Created: Sat Jan 13 18:45:57 2007
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class JabberIqSi extends SimpleForwarder {

  private static final Logger log =
		Logger.getLogger("tigase.xmpp.impl.JabberIqSi");

  protected static final String XMLNS = "http://jabber.org/protocol/si";
	protected static final String ID = XMLNS;
  protected static final String[] ELEMENTS = {"si"};
  protected static final String[] XMLNSS = {XMLNS};
  protected static final String[] DISCO_FEATURES =
	{"http://jabber.org/protocol/si",
	 "http://jabber.org/protocol/si/profile/file-transfer"};

	public String id() { return ID; }

	public String[] supElements() { return ELEMENTS; }

  public String[] supNamespaces() { return XMLNSS; }

  public String[] supDiscoFeatures(final XMPPResourceConnection session)
	{ return DISCO_FEATURES; }

}
