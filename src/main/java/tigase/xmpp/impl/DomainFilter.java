/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2008 "Artur Hefczyc" <artur.hefczyc@tigase.org>
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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;
import tigase.db.NonAuthUserRepository;
import tigase.db.TigaseDBException;
import tigase.server.Packet;
import tigase.util.JIDUtils;
import tigase.xmpp.Authorization;
import tigase.xmpp.NotAuthorizedException;
import tigase.xmpp.PacketErrorTypeException;
import tigase.xmpp.XMPPPacketFilterIfc;
import tigase.xmpp.XMPPPreprocessorIfc;
import tigase.xmpp.XMPPProcessor;
import tigase.xmpp.XMPPResourceConnection;

/**
 * Created: Dec 30, 2008 12:43:28 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class DomainFilter extends XMPPProcessor
				implements XMPPPacketFilterIfc, XMPPPreprocessorIfc {

  /**
   * Private logger for class instancess.
   */
  private static Logger log =
		Logger.getLogger("tigase.xmpp.impl.DomainFilter");

	public static final String ALLOWED_DOMAINS_LIST_KEY =
					"allowed-domains-list";
	public static final String ALLOWED_DOMAINS_KEY = "allowed-domains";

	private static final String ID = "domain-filter";
	private static final String[] ELEMENTS = {ALL};
  private static final String[] XMLNSS = {ALL};

	public enum DOMAINS {
		ALL, LOCAL, OWN, BLOCK, LIST;

		public static DOMAINS valueof(String domains) {
			if (domains == null) {
				return null;
			}
			try {
				return DOMAINS.valueOf(domains);
			} catch (Exception e) {
				return LIST;
			} // end of try-catch
		}

	}

	/**
	 * Describe <code>supElements</code> method here.
	 *
	 * @return a <code>String[]</code> value
	 */
	@Override
	public String[] supElements()
	{ return ELEMENTS; }

	/**
	 * Describe <code>supNamespaces</code> method here.
	 *
	 * @return a <code>String[]</code> value
	 */
	@Override
  public String[] supNamespaces()
	{ return XMLNSS; }

	/**
	 * Describe <code>id</code> method here.
	 *
	 * @return a <code>String</code> value
	 */
	@Override
	public String id() { return ID; }

	private DOMAINS getDomains(XMPPResourceConnection session) 
					throws NotAuthorizedException, TigaseDBException {
		DOMAINS domains =
						(DOMAINS)session.getCommonSessionData(ALLOWED_DOMAINS_KEY);
		log.finest("domains read from user session: " + domains);
		if (domains == null) {
			String dbDomains = session.getData(null, ALLOWED_DOMAINS_KEY, null);
			log.finest("Domains read from database: " + dbDomains);
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
	
	private String[] getDomainsList(XMPPResourceConnection session)
					throws NotAuthorizedException, TigaseDBException {
		String[] allowedDomains =
						(String[]) session.getCommonSessionData(ALLOWED_DOMAINS_LIST_KEY);
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

	@Override
	public void filter(Packet packet, XMPPResourceConnection session,
					NonAuthUserRepository repo, Queue<Packet> results) {
		if (session == null || results == null || results.size() == 0) {
			return;
		}
		try {
			DOMAINS domains = getDomains(session);
			// Fast return when user is authorized to send packets to any domain
			if (domains == DOMAINS.ALL) {
				return;
			}
			Queue<Packet> errors = new LinkedList<Packet>();
			for (Iterator<Packet> it = results.iterator(); it.hasNext();) {
				Packet res = it.next();
				if (domains == DOMAINS.BLOCK) {
					removePacket(it, res, errors, "Communication blocked.");
					continue;
				}
				String outDomain = res.getElemTo();
				if (outDomain != null) {
					outDomain = JIDUtils.getNodeHost(outDomain).intern();
				}
				switch (domains) {
					case LOCAL:
						if (outDomain != null && !session.isLocalDomain(outDomain)) {
							removePacket(it, res, errors,
											"You can only communicate within the server local domains.");
						}
						break;
					case OWN:
						if (outDomain != null && !outDomain.equals(session.getDomain())) {
							removePacket(it, res, errors, 
											"You can only communicate within your own domain.");
						}
						break;
					case LIST:
						String[] allowedDomains = getDomainsList(session);
						boolean found = false;
						for (String domain : allowedDomains) {
							if (domain == outDomain) {
								found = true;
								break;
							}
						}
						if (!found) {
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

	private void removePacket(Iterator<Packet> it, Packet res,
					Queue<Packet> errors, String msg) {
		if (it != null) {
			it.remove();
		}
		try {
			errors.add(Authorization.FORBIDDEN.getResponseMessage(res, msg,	true));
		} catch (PacketErrorTypeException ex) {
			log.fine("Already error packet, dropping it..: " +
							res.toString());
		}
	}

	@Override
	public boolean preProcess(Packet packet, XMPPResourceConnection session,
					NonAuthUserRepository repo,	Queue<Packet> results) {
		if (log.isLoggable(Level.FINEST)) {
			log.finest("Processing: " + packet.toString());
		}
		boolean stop = false;
		if (session == null) {
			return stop;
		}
		try {
			DOMAINS domains = getDomains(session);
			if (log.isLoggable(Level.FINEST)) {
				log.finest("DOMAINS setting is: " + domains.name());
			}
			// Fast return when user is authorized to send packets to any domain
			if (domains == DOMAINS.ALL) {
				return stop;
			}
			String outDomain = packet.getElemFrom();
			if (session.getConnectionId().equals(packet.getFrom())) {
				outDomain = packet.getElemTo();
			}
			if (outDomain != null) {
				outDomain = JIDUtils.getNodeHost(outDomain).intern();
			}
			switch (domains) {
				case BLOCK:
					if ((packet.getElemFrom() == null ||
									JIDUtils.getNodeID(packet.getElemFrom()).equals(session.getUserId())) &&
									(packet.getElemTo() == null ||
									JIDUtils.getNodeID(packet.getElemTo()).equals(session.getUserId()))) {
						return stop;
					} else {
						removePacket(null, packet, results, "Communication blocked.");
						stop = true;
					}
					if (log.isLoggable(Level.FINEST)) {
						log.finest("BLOCK, blocking packet: " +	packet.toString());
					}
					break;
				case LOCAL:
					if (outDomain != null && !session.isLocalDomain(outDomain)) {
						removePacket(null, packet, results,
										"You can only communicate within the server local domains.");
						stop = true;
						if (log.isLoggable(Level.FINEST)) {
							log.finest("LOCAL Domains only, blocking packet: " +
											packet.toString());
						}
					} else {
						if (log.isLoggable(Level.FINEST)) {
							log.finest("LOCAL Domains only, packet not blocked: " +
											packet.toString());
						}
					}
					break;
				case OWN:
					if (outDomain != null && !outDomain.equals(session.getDomain())) {
						removePacket(null, packet, results,
										"You can only communicate within your own domain.");
						stop = true;
						if (log.isLoggable(Level.FINEST)) {
							log.finest("OWN Domain only, blocking packet: " +
											packet.toString());
						}
					} else {
						if (log.isLoggable(Level.FINEST)) {
							log.finest("OWN Domain only, packet not blocked: " +
											packet.toString());
						}
					}
					break;
				case LIST:
					String[] allowedDomains = getDomainsList(session);
					boolean found = false;
					for (String domain : allowedDomains) {
						if (domain == outDomain) {
							found = true;
							break;
						}
					}
					if (!found) {
						removePacket(null, packet, results,
										"You can only communicate within selected list of domains.");
						stop = true;
						if (log.isLoggable(Level.FINEST)) {
							log.finest("LISTED Domains only, blocking packet: " +
											packet.toString());
						}
					} else {
						if (log.isLoggable(Level.FINEST)) {
							log.finest("LISTED Domain only, packet not blocked: " +
											packet.toString());
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

}
