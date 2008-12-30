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

	protected static final String ALLOWED_DOMAINS_KEY = "allowed-domains";

	private static final String ID = "domain-filter";
	private static final String[] ELEMENTS = {ALL};
  private static final String[] XMLNSS = {ALL};

	private enum DOMAINS {
		ALL, LOCAL, OWN, LIST;

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

	@Override
	public void filter(Packet packet, XMPPResourceConnection session,
					NonAuthUserRepository repo, Queue<Packet> results) {
		if (session == null || results == null || results.size() == 0) {
			return;
		}
		try {
			String dbDomains = session.getData(null, ALLOWED_DOMAINS_KEY, null);
			DOMAINS domains = DOMAINS.valueof(dbDomains);
			if (domains == null) {
				if (session.isAnonymous()) {
					domains = DOMAINS.LOCAL;
				} else {
					domains = DOMAINS.ALL;
				}
			}
			Queue<Packet> errors = new LinkedList<Packet>();
			for (Iterator<Packet> it = results.iterator(); it.hasNext();) {
				Packet res = it.next();
				String outDomain = res.getElemTo();
				if (outDomain != null) {
					outDomain = JIDUtils.getNodeHost(outDomain).intern();
				}
				switch (domains) {
					case ALL:
						// Do nothing, allow all packets.
						break;
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
						String[] allowedDomains =
										(String[])session.getCommonSessionData(ALLOWED_DOMAINS_KEY);
						if (allowedDomains == null) {
							allowedDomains = dbDomains.split(",");
							for (int i = 0; i < allowedDomains.length; ++i) {
								allowedDomains[i] = allowedDomains[i].intern();
							}
							session.putCommonSessionData(ALLOWED_DOMAINS_KEY, allowedDomains);
						}
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
			log.log(Level.INFO, "Already error packet, dropping it..: " +
							res.toString(), ex);
		}
	}

	@Override
	public boolean preProcess(Packet packet, XMPPResourceConnection session,
					NonAuthUserRepository repo,	Queue<Packet> results) {
 		boolean stop = false;
		if (session == null ) {
			return stop;
		}
		try {
			String dbDomains = session.getData(null, ALLOWED_DOMAINS_KEY, null);
			DOMAINS domains = DOMAINS.valueof(dbDomains);
			if (domains == null) {
				if (session.isAnonymous()) {
					domains = DOMAINS.LOCAL;
				} else {
					domains = DOMAINS.ALL;
				}
			}
			String outDomain = packet.getElemFrom();
			if (session.getConnectionId().equals(packet.getFrom())) {
				outDomain = packet.getElemTo();
			}
			if (outDomain != null) {
				outDomain = JIDUtils.getNodeHost(outDomain).intern();
			}
			switch (domains) {
				case ALL:
					// Do nothing, allow all packets.
					break;
				case LOCAL:
					if (outDomain != null && !session.isLocalDomain(outDomain)) {
						removePacket(null, packet, results,
										"You can only communicate within the server local domains.");
						stop = true;
					}
					break;
				case OWN:
					if (outDomain != null && !outDomain.equals(session.getDomain())) {
						removePacket(null, packet, results,
										"You can only communicate within your own domain.");
						stop = true;
					}
					break;
				case LIST:
					String[] allowedDomains =
									(String[]) session.getCommonSessionData(ALLOWED_DOMAINS_KEY);
					if (allowedDomains == null) {
						allowedDomains = dbDomains.split(",");
						for (int i = 0; i < allowedDomains.length; ++i) {
							allowedDomains[i] = allowedDomains[i].intern();
						}
						session.putCommonSessionData(ALLOWED_DOMAINS_KEY, allowedDomains);
					}
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
