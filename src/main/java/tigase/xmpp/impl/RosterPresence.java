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
package tigase.xmpp.impl;

import java.util.Queue;
import java.util.Map;
import java.util.logging.Logger;
import java.util.logging.Level;
import tigase.server.Packet;
import tigase.xml.Element;
import tigase.xmpp.XMPPProcessor;
import tigase.xmpp.XMPPProcessorIfc;
import tigase.xmpp.XMPPStopListenerIfc;
import tigase.xmpp.XMPPResourceConnection;
import tigase.xmpp.XMPPException;
import tigase.db.NonAuthUserRepository;

/**
 * Describe class RosterPresence here.
 *
 *
 * Created: Wed Jan 30 19:25:25 2008
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class RosterPresence  extends XMPPProcessor
	implements XMPPProcessorIfc, XMPPStopListenerIfc {

	/**
   * Private logger for class instancess.
   */
  private static Logger log =	Logger.getLogger("tigase.xmpp.impl.Presence");

	private static final String ID = "roster-presence";

	private static final String PRESENCE = "presence";
	private static final String[] ELEMENTS = {PRESENCE, "query", "query"};
  private static final String[] XMLNSS = {
		Presence.XMLNS, JabberIqRoster.XMLNS, JabberIqRoster.XMLNS_DYNAMIC};
  private static final Element[] DISCO_FEATURES =	JabberIqRoster.DISCO_FEATURES;
	private static final Element[] FEATURES =	JabberIqRoster.FEATURES;

	@Override
	public String id() { return ID; }

	@Override
	public String[] supElements()
	{ return ELEMENTS; }

	@Override
	public Element[] supStreamFeatures(final XMPPResourceConnection session)
	{ return FEATURES; }

	@Override
  public String[] supNamespaces()
	{ return XMLNSS; }

	@Override
  public Element[] supDiscoFeatures(final XMPPResourceConnection session)
	{ return DISCO_FEATURES; }

	@Override
	public void stopped(final XMPPResourceConnection session,
		final Queue<Packet> results, final Map<String, Object> settings) {
		Presence.stopped(session, results, settings);
		JabberIqRoster.stopped(session, results, settings);
	}

	@Override
	public int concurrentQueuesNo() {
		return Runtime.getRuntime().availableProcessors() * 2;
	}

	@Override
	public int concurrentThreadsPerQueue() {
		// Packet processing order does matter for roster/presence therefore
		// we need a single thread for each queue.
		return 1;
	}

	@Override
  public void process(final Packet packet,
		final XMPPResourceConnection session,
		final NonAuthUserRepository repo, final Queue<Packet> results,
		final Map<String, Object> settings)
		throws XMPPException {

		if (session == null) {
			if (log.isLoggable(Level.FINE)) {
				log.fine("Session is null, ignoring packet: " + packet.toString());
			}
			return;
		} // end of if (session == null)

		if (packet.getElemName().equals(PRESENCE)) {
			Presence.process(packet, session, repo, results, settings);
		} else {
			JabberIqRoster.process(packet, session, repo, results, settings);
		}
	}

}
