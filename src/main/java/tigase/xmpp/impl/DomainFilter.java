/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2012 "Artur Hefczyc" <artur.hefczyc@tigase.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3 of the License.
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
 * $Rev$
 * Last modified by $Author$
 * $Date$
 */

package tigase.xmpp.impl;

//~--- non-JDK imports --------------------------------------------------------

import tigase.db.NonAuthUserRepository;
import tigase.db.TigaseDBException;

import tigase.server.Packet;

import tigase.xmpp.Authorization;
import tigase.xmpp.NoConnectionIdException;
import tigase.xmpp.NotAuthorizedException;
import tigase.xmpp.PacketErrorTypeException;
import tigase.xmpp.StanzaType;
import tigase.xmpp.XMPPPacketFilterIfc;
import tigase.xmpp.XMPPPreprocessorIfc;
import tigase.xmpp.XMPPProcessor;
import tigase.xmpp.XMPPResourceConnection;

//~--- JDK imports ------------------------------------------------------------

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

/**
 * Created: Dec 30, 2008 12:43:28 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class DomainFilter extends XMPPProcessor
		implements XMPPPacketFilterIfc, XMPPPreprocessorIfc {

	/**
	 * Enum description
	 *
	 */
	public enum DOMAINS {
		ALL, LOCAL, OWN, BLOCK,
		LIST;

		/**
		 * Method description
		 *
		 *
		 * @param domains
		 *
		 * @return
		 */
		public static DOMAINS valueof(String domains) {
			if (domains == null) {
				return null;
			}

			try {
				return DOMAINS.valueOf(domains);
			} catch (Exception e) {
				return LIST;
			}    // end of try-catch
		}
	}

	//~--- static fields --------------------------------------------------------

	/**
	 * Private logger for class instances.
	 */
	private static final Logger log = Logger.getLogger(DomainFilter.class.getName());

	/** Field description */
	public static final String ALLOWED_DOMAINS_LIST_KEY = "allowed-domains-list";

	/** Field description */
	public static final String ALLOWED_DOMAINS_KEY = "allowed-domains";
	private static final String ID = "domain-filter";
	private static final String[] ELEMENTS = { ALL };
	private static final String[] XMLNSS = { ALL };

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param packet
	 * @param session
	 * @param repo
	 * @param results
	 */
	@Override
	public void filter(Packet packet, XMPPResourceConnection session, NonAuthUserRepository repo,
			Queue<Packet> results) {
		if ((session == null) || (results == null) || (results.size() == 0)) {
			return;
		}

		try {
			DOMAINS domains = getDomains(session);

			// Fast return when user is authorized to send packets to any domain
			if (domains == DOMAINS.ALL) {
				return;
			}

			Queue<Packet> errors = new ArrayDeque<Packet>(1);

			for (Iterator<Packet> it = results.iterator(); it.hasNext(); ) {
				Packet res = it.next();

				if (domains == DOMAINS.BLOCK) {
					if ((res.getType() != StanzaType.error)
							&& ((((res.getStanzaFrom() != null)
								&&!session.isUserId(res.getStanzaFrom().getBareJID())) || ((res.getStanzaTo()
									!= null) &&!session.isUserId(res.getStanzaTo().getBareJID()))))) {
						removePacket(it, res, errors, "Communication blocked.");
					}

					continue;
				}

				String outDomain = null;

				if (res.getStanzaTo() != null) {
					outDomain = res.getStanzaTo().getDomain();
				}

				switch (domains) {
					case LOCAL :
						if ((outDomain != null) &&!session.isLocalDomain(outDomain, true)) {
							removePacket(it, res, errors,
									"You can only communicate within the server local domains.");
						}

						break;

					case OWN :
						if ((outDomain != null)
								&&!outDomain.equals(session.getDomain().getVhost().getDomain())) {
							removePacket(it, res, errors, "You can only communicate within your own domain.");
						}

						break;

					case LIST :
						String[] allowedDomains = getDomainsList(session);
						boolean found = false;

						for (String domain : allowedDomains) {

							// Intentionally comparing domains by reference instead of value
							// domain is processed through the String.intern() method
							if (domain == outDomain) {
								found = true;

								break;
							}
						}

						if ( !found) {
							removePacket(it, res, errors,
									"You can only communicate within selected list of domains.");
						}

						break;
				}
			}

			results.addAll(errors);
		} catch (NotAuthorizedException ex) {

			// This can happen actually easily when filtering initial handshaking
			// packets. I am not sure what to do yet.....
			// Assuming this an initial, and authentication traffic, allowing for
			// everything, which means, just ignore.
		} catch (TigaseDBException ex) {
			log.log(Level.WARNING, "Can't access user repository.", ex);
		}
	}

	/**
	 * Describe <code>id</code> method here.
	 *
	 * @return a <code>String</code> value
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
	 *
	 * @param settings
	 * @return
	 */
	@Override
	public boolean preProcess(Packet packet, XMPPResourceConnection session,
			NonAuthUserRepository repo, Queue<Packet> results, Map<String, Object> settings) {
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "Processing: {0}", packet);
		}

		boolean stop = false;

		if (session == null) {
			return stop;
		}

		if (session.isServerSession()) {
			return stop;
		}

		try {
			DOMAINS domains = getDomains(session);

			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "DOMAINS setting is: {0}", domains.name());
			}

			// Fast return when user is authorized to send packets to any domain
			if (domains == DOMAINS.ALL) {
				return stop;
			}

			String outDomain = (packet.getStanzaFrom() != null)
				? packet.getStanzaFrom().getDomain() : null;

			try {
				if (session.getConnectionId().equals(packet.getPacketFrom())) {
					outDomain = (packet.getStanzaTo() != null) ? packet.getStanzaTo().getDomain() : null;
				}
			} catch (NoConnectionIdException ex) {
				log.log(Level.WARNING,
						"No connection id for session, even though this is not a server "
							+ "session: {0}, request: {1}", new Object[] { session,
						packet });
			}

			switch (domains) {
				case BLOCK :
					if ((packet.getType() == StanzaType.error)
							|| ((packet.getStanzaFrom() == null)
								|| (session.isUserId(packet.getStanzaFrom().getBareJID())
									&& ((packet.getStanzaTo() == null)
										|| session.isUserId(packet.getStanzaTo().getBareJID()))))) {
						return stop;
					} else {
						removePacket(null, packet, results, "Communication blocked.");
						stop = true;
					}

					if (log.isLoggable(Level.FINEST)) {
						log.log(Level.FINEST, "BLOCK, blocking packet: {0}", packet);
					}

					break;

				case LOCAL :
					if ((outDomain != null) &&!session.isLocalDomain(outDomain, true)) {
						removePacket(null, packet, results,
								"You can only communicate within the server local domains.");
						stop = true;

						if (log.isLoggable(Level.FINEST)) {
							log.log(Level.FINEST, "LOCAL Domains only, blocking packet: {0}", packet);
						}
					} else {
						if (log.isLoggable(Level.FINEST)) {
							log.log(Level.FINEST, "LOCAL Domains only, packet not blocked: {0}", packet);
						}
					}

					break;

				case OWN :
					if ((outDomain != null) &&!outDomain.equals(session.getDomain().getVhost().getDomain())) {
						removePacket(null, packet, results, "You can only communicate within your own domain.");
						stop = true;

						if (log.isLoggable(Level.FINEST)) {
							log.log(Level.FINEST, "OWN Domain only, blocking packet: {0}", packet);
						}
					} else {
						if (log.isLoggable(Level.FINEST)) {
							log.log(Level.FINEST, "OWN Domain only, packet not blocked: {0}", packet);
						}
					}

					break;

				case LIST :
					String[] allowedDomains = getDomainsList(session);
					boolean found = false;

					for (String domain : allowedDomains) {
						if (domain == outDomain) {
							found = true;

							break;
						}
					}

					if ( !found) {
						removePacket(null, packet, results,
								"You can only communicate within selected list of domains.");
						stop = true;

						if (log.isLoggable(Level.FINEST)) {
							log.log(Level.FINEST, "LISTED Domains only, blocking packet: {0}", packet);
						}
					} else {
						if (log.isLoggable(Level.FINEST)) {
							log.log(Level.FINEST, "LISTED Domain only, packet not blocked: {0}", packet);
						}
					}

					break;
			}
		} catch (NotAuthorizedException ex) {

			// This can happen actually easily when filtering initial handshaking
			// packets. I am not sure what to do yet.....
			// Assuming this an initial, and authentication traffic, allowing for
			// everything, which means, just ignore.
		} catch (TigaseDBException ex) {
			log.log(Level.WARNING, "Can't access user repository.", ex);
		}

		return stop;
	}

	/**
	 * Describe <code>supElements</code> method here.
	 *
	 * @return a <code>String[]</code> value
	 */
	@Override
	public String[] supElements() {
		return ELEMENTS;
	}

	/**
	 * Describe <code>supNamespaces</code> method here.
	 *
	 * @return a <code>String[]</code> value
	 */
	@Override
	public String[] supNamespaces() {
		return XMLNSS;
	}

	//~--- get methods ----------------------------------------------------------

	public DOMAINS getDomains(XMPPResourceConnection session)
			throws NotAuthorizedException, TigaseDBException {
		DOMAINS domains = (DOMAINS) session.getCommonSessionData(ALLOWED_DOMAINS_KEY);

		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "domains read from user session: {0}", domains);
		}

		if (domains == null) {
			String dbDomains = session.getData(null, ALLOWED_DOMAINS_KEY, null);

			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "Domains read from database: {0}", dbDomains);
			}

			domains = DOMAINS.valueof(dbDomains);

			if (domains == null) {
				if (session.isAnonymous()) {
					domains = DOMAINS.LOCAL;
				} else {
					domains = DOMAINS.ALL;
				}
			}

			session.putCommonSessionData(ALLOWED_DOMAINS_KEY, domains);
		}

		return domains;
	}

	public String[] getDomainsList(XMPPResourceConnection session)
			throws NotAuthorizedException, TigaseDBException {
		String[] allowedDomains = (String[]) session.getCommonSessionData(ALLOWED_DOMAINS_LIST_KEY);

		if (allowedDomains == null) {
			String dbDomains = session.getData(null, ALLOWED_DOMAINS_KEY, null);

			allowedDomains = dbDomains.split(",");

			for (int i = 0; i < allowedDomains.length; ++i) {
				allowedDomains[i] = allowedDomains[i].intern();
			}

			session.putCommonSessionData(ALLOWED_DOMAINS_LIST_KEY, allowedDomains);
		}

		return allowedDomains;
	}

	//~--- methods --------------------------------------------------------------

	private void removePacket(Iterator<Packet> it, Packet res, Queue<Packet> errors, String msg) {
		if (it != null) {
			it.remove();
		}

		try {
			errors.add(Authorization.FORBIDDEN.getResponseMessage(res, msg, true));
		} catch (PacketErrorTypeException ex) {
			log.log(Level.FINE, "Already error packet, dropping it..: {0}", res);
		}
	}
}


//~ Formatted in Sun Code Convention


//~ Formatted by Jindent --- http://www.jindent.com
