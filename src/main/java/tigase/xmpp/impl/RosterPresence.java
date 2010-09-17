/*
 *   Tigase Jabber/XMPP Server
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

//~--- non-JDK imports --------------------------------------------------------

import tigase.db.NonAuthUserRepository;

import tigase.server.Packet;

import tigase.xml.Element;

import tigase.xmpp.XMPPException;
import tigase.xmpp.XMPPProcessor;
import tigase.xmpp.XMPPProcessorIfc;
import tigase.xmpp.XMPPResourceConnection;
import tigase.xmpp.XMPPStopListenerIfc;
import tigase.xmpp.impl.roster.RosterAbstract;

//~--- JDK imports ------------------------------------------------------------

import java.util.Map;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

/**
 * Describe class RosterPresence here.
 *
 *
 * Created: Wed Jan 30 19:25:25 2008
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class RosterPresence extends XMPPProcessor implements XMPPProcessorIfc, XMPPStopListenerIfc {

	/**
	 * Private logger for class instance.
	 */
	private static Logger log = Logger.getLogger(RosterPresence.class.getName());
	private static final String ID = "roster-presence";
	private static final String PRESENCE = "presence";
	private static final String[] ELEMENTS = { PRESENCE, "query", "query" };
	private static final String[] XMLNSS = { Presence.XMLNS, RosterAbstract.XMLNS,
			RosterAbstract.XMLNS_DYNAMIC };
	private static final Element[] DISCO_FEATURES = RosterAbstract.DISCO_FEATURES;
	private static final Element[] FEATURES = RosterAbstract.FEATURES;

	//~--- fields ---------------------------------------------------------------

	private JabberIqRoster rosterProc = new JabberIqRoster();
	private Presence presenceProc = new Presence();

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public int concurrentQueuesNo() {
		return Runtime.getRuntime().availableProcessors() * 2;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public String id() {
		return ID;
	}

	/**
	 * Method description
	 *
	 *
	 * @param packet
	 * @param session
	 * @param repo
	 * @param results
	 * @param settings
	 *
	 * @throws XMPPException
	 */
	@Override
	public void process(final Packet packet, final XMPPResourceConnection session,
			final NonAuthUserRepository repo, final Queue<Packet> results,
				final Map<String, Object> settings)
			throws XMPPException {

//  if (session == null) {
//    if (log.isLoggable(Level.FINE)) {
//      log.fine("Session is null, ignoring packet: " + packet.toString());
//    }
//
//    return;
//  }    // end of if (session == null)
		if ( !session.isAuthorized()) {
			if (log.isLoggable(Level.FINE)) {
				log.log(Level.FINE, "Session is not authorized, ignoring packet: {0}", packet);
			}

			return;
		}

		if (packet.getElemName().equals(PRESENCE)) {
			presenceProc.process(packet, session, repo, results, settings);
		} else {
			rosterProc.process(packet, session, repo, results, settings);
		}
	}

	/**
	 * Method description
	 *
	 *
	 * @param session
	 * @param results
	 * @param settings
	 */
	@Override
	public void stopped(final XMPPResourceConnection session, final Queue<Packet> results,
			final Map<String, Object> settings) {
		presenceProc.stopped(session, results, settings);
		rosterProc.stopped(session, results, settings);
	}

	/**
	 * Method description
	 *
	 *
	 * @param session
	 *
	 * @return
	 */
	@Override
	public Element[] supDiscoFeatures(final XMPPResourceConnection session) {
		return DISCO_FEATURES;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public String[] supElements() {
		return ELEMENTS;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public String[] supNamespaces() {
		return XMLNSS;
	}

	/**
	 * Method description
	 *
	 *
	 * @param session
	 *
	 * @return
	 */
	@Override
	public Element[] supStreamFeatures(final XMPPResourceConnection session) {
		return FEATURES;
	}
}


//~ Formatted in Sun Code Convention


//~ Formatted by Jindent --- http://www.jindent.com
