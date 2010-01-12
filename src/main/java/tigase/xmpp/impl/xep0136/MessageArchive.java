/*  Tigase Jabber/XMPP Server
 *  Copyright (C) 2004-2008 "Artur Hefczyc" <artur.hefczyc@tigase.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 of the License.
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
package tigase.xmpp.impl.xep0136;

import java.util.Map;
import java.util.Queue;
import java.util.logging.Logger;

import tigase.db.NonAuthUserRepository;

import tigase.xmpp.XMPPException;
import tigase.xmpp.XMPPProcessor;
import tigase.xmpp.XMPPProcessorIfc;
import tigase.xmpp.XMPPResourceConnection;

import tigase.server.Packet;

import tigase.xml.Element;

/**
 * Describe class MessageArchive here.
 *
 *
 * Created: Fri Feb 29 22:44:30 2008
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class MessageArchive extends XMPPProcessor	implements XMPPProcessorIfc {

  /**
   * Private logger for class instancess.
   */
  private static Logger log =
		Logger.getLogger("tigase.xmpp.impl.OfflineMessage");

	private static final String ID = "message-archive";
	private static final String[] ELEMENTS =
	{
		"archive",
		"auto",
		"chat",
		"delete",
		"keys",
		"list",
		"modified",
		"pref",
		"remove",
		"retrieve",
		"save"
	};
  private static final String[] XMLNSS =
	{
		"http://www.xmpp.org/extensions/xep-0136.html#ns",
		"http://www.xmpp.org/extensions/xep-0136.html#ns",
		"http://www.xmpp.org/extensions/xep-0136.html#ns",
		"http://www.xmpp.org/extensions/xep-0136.html#ns",
		"http://www.xmpp.org/extensions/xep-0136.html#ns",
		"http://www.xmpp.org/extensions/xep-0136.html#ns",
		"http://www.xmpp.org/extensions/xep-0136.html#ns",
		"http://www.xmpp.org/extensions/xep-0136.html#ns",
		"http://www.xmpp.org/extensions/xep-0136.html#ns",
		"http://www.xmpp.org/extensions/xep-0136.html#ns",
		"http://www.xmpp.org/extensions/xep-0136.html#ns"
	};

//   <feature var='http://www.xmpp.org/extensions/xep-0136.html#ns-auto'/>
//   <feature var='http://www.xmpp.org/extensions/xep-0136.html#ns-encrypt'/>
//   <feature var='http://www.xmpp.org/extensions/xep-0136.html#ns-manage'/>
//   <feature var='http://www.xmpp.org/extensions/xep-0136.html#ns-manual'/>
//   <feature var='http://www.xmpp.org/extensions/xep-0136.html#ns-pref'/>

  private static final Element[] DISCO_FEATURES =
	{
		new Element("feature",	new String[] {"var"},
			new String[] {"http://www.xmpp.org/extensions/xep-0136.html#ns-auto"}),
		new Element("feature",	new String[] {"var"},
			new String[] {"http://www.xmpp.org/extensions/xep-0136.html#ns-encrypt"}),
		new Element("feature",	new String[] {"var"},
			new String[] {"http://www.xmpp.org/extensions/xep-0136.html#ns-manage"}),
		new Element("feature",	new String[] {"var"},
			new String[] {"http://www.xmpp.org/extensions/xep-0136.html#ns-manual"}),
		new Element("feature",	new String[] {"var"},
			new String[] {"http://www.xmpp.org/extensions/xep-0136.html#ns-pref"})
	};

	public String id() { return ID; }

	public String[] supElements()
	{ return ELEMENTS; }

	public String[] supNamespaces()
	{ return XMLNSS; }

  public Element[] supDiscoFeatures(final XMPPResourceConnection session)
	{ return DISCO_FEATURES; }

	public void process(final Packet packet, final XMPPResourceConnection session,
		final NonAuthUserRepository repo, final Queue<Packet> results,
		final Map<String, Object> settings)
		throws XMPPException {

	}


}
