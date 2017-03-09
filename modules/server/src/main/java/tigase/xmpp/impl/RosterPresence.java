/*
 * RosterPresence.java
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

import tigase.db.NonAuthUserRepository;

import tigase.server.Iq;
import tigase.server.Packet;

import tigase.xml.Element;

import tigase.xmpp.Authorization;
import tigase.xmpp.impl.roster.RosterAbstract;
import tigase.xmpp.impl.roster.RosterFactory;
import tigase.xmpp.XMPPException;
import tigase.xmpp.XMPPProcessor;
import tigase.xmpp.XMPPProcessorIfc;
import tigase.xmpp.XMPPResourceConnection;
import tigase.xmpp.XMPPStopListenerIfc;

//~--- JDK imports ------------------------------------------------------------

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Map;
import java.util.Queue;

/**
 * Describe class RosterPresence here.
 *
 *
 * Created: Wed Jan 30 19:25:25 2008
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
@Deprecated
public class RosterPresence
				extends XMPPProcessor
				implements XMPPProcessorIfc, XMPPStopListenerIfc {
	private static final String ID = "roster-presence";

	/**
	 * Private logger for class instance.
	 */
	private static Logger           log = Logger.getLogger(RosterPresence.class.getName());
	private static final String     PRESENCE = "presence";
	private static final String[][] ELEMENTS = {
		{ PRESENCE }, Iq.IQ_QUERY_PATH, Iq.IQ_QUERY_PATH
	};
	private static final String[]   XMLNSS = { PresenceAbstract.XMLNS, RosterAbstract.XMLNS,
			RosterAbstract.XMLNS_DYNAMIC };
	private static final RosterAbstract roster_util = RosterFactory.getRosterImplementation(
			true);
	private static final Element[] FEATURES       = RosterAbstract.FEATURES;
	private static final Element[] DISCO_FEATURES = RosterAbstract.DISCO_FEATURES;

	//~--- fields ---------------------------------------------------------------

	private JabberIqRoster rosterProc   = new JabberIqRoster();
	private PresenceState       presenceProc = new PresenceState();

	//~--- methods --------------------------------------------------------------

	@Override
	public int concurrentQueuesNo() {
		return super.concurrentQueuesNo() * 2;
	}

	@Override
	public String id() {
		return ID;
	}

	@Override
	public void process(final Packet packet, final XMPPResourceConnection session,
			final NonAuthUserRepository repo, final Queue<Packet> results, final Map<String,
			Object> settings)
					throws XMPPException {

		if (!session.isAuthorized()) {
			if (log.isLoggable(Level.FINE)) {
				log.log(Level.FINE, "Session is not authorized, ignoring packet: {0}", packet);
			}

			return;
		}
		if (packet.getElemName().equals(PRESENCE)) {
			presenceProc.process(packet, session, repo, results, settings);
		} else {
			if ((packet.getStanzaTo() != null) && (packet.getStanzaFrom() != null) && session
					.isUserId(packet.getStanzaTo().getBareJID()) &&!session.isUserId(packet
					.getStanzaFrom().getBareJID())) {
				if (!RemoteRosterManagement.isRemoteAllowed(packet.getStanzaFrom(), session)) {
					results.offer(Authorization.NOT_ALLOWED.getResponseMessage(packet,
							"Not authorized for remote roster management", true));

					return;
				}
				try {
					switch (packet.getType()) {
					case get :
						List<Element> ritems = roster_util.getRosterItems(session);

						if ((ritems != null) &&!ritems.isEmpty()) {
							Element query = new Element("query");

							query.setXMLNS(RosterAbstract.XMLNS);

							String jidStr = "@" + packet.getStanzaFrom().getBareJID().toString();

							for (Element ritem : ritems) {
								if (ritem.getAttributeStaticStr("jid").endsWith(jidStr)) {
									query.addChild(ritem);
								}
							}
							results.offer(packet.okResult(query, 0));
						} else {
							results.offer(packet.okResult((String) null, 1));
						}

						break;

					case set :

//          processSetMethod.invoke(rosterProc, new Object[]{
//                          packet, session, results, settings
//                  });
//          List<Element> nitems = packet.getElemChildren("/iq/query");
//          if (nitems != null) {
//                  for (Element nitem : nitems) {
//                          JID buddy = JID.jidInstanceNS(nitem.getAttribute("jid"));
//                          String name = nitem.getAttribute("name");
//                          String subscrStr = nitem.getAttribute("subscription");
//                          RosterAbstract.SubscriptionType subscr = subscrStr == null ? null : RosterAbstract.SubscriptionType.valueOf(subscrStr);
//                          String[] groups = null;
//                          List<Element> ngroups = nitem.getChildren();
//                          if (ngroups != null && !ngroups.isEmpty()) {
//                                  int i = 0;
//                                  groups = new String[ngroups.size()];
//                                  for (Element group : nitem.getChildren()) {
//                                          groups[i++] = group.getCData() == null ? "" : group.getCData();
//                                  }
//                          }
//                          roster_util.addBuddy(session, buddy, name, groups, null);
//                          roster_util.setBuddySubscription(session, subscr, buddy);
//
//                          Element item = roster_util.getBuddyItem(session, buddy);
//                          roster_util.updateBuddyChange(session, results, item);
//                  }
//          }
//
//          results.offer(packet.okResult((String) null, 0));
						rosterProc.processSetRequest(packet, session, results, settings);

						break;

					default :
						results.offer(Authorization.BAD_REQUEST.getResponseMessage(packet,
								"Bad stanza type", true));

						break;
					}
				} catch (Throwable ex) {
					log.log(Level.WARNING, "Reflection execution exception", ex);
					results.offer(Authorization.INTERNAL_SERVER_ERROR.getResponseMessage(packet,
							"Internal server error", true));
				}
			} else {
				rosterProc.process(packet, session, repo, results, settings);
			}
		}
	}

	@Override
	public void stopped(final XMPPResourceConnection session, final Queue<Packet> results,
			final Map<String, Object> settings) {
		presenceProc.stopped(session, results, settings);
	}

	@Override
	public Element[] supDiscoFeatures(final XMPPResourceConnection session) {
		return DISCO_FEATURES;
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
	public Element[] supStreamFeatures(final XMPPResourceConnection session) {
		return FEATURES;
	}
}

