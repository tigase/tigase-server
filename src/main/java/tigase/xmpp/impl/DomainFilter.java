/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2014 "Tigase, Inc." <office@tigase.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3 of the License,
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
 */

package tigase.xmpp.impl;

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

import tigase.util.DNSResolverFactory;
import tigase.util.StringUtilities;
import tigase.vhosts.VHostItem;
import tigase.vhosts.filter.CustomDomainFilter;
import tigase.vhosts.filter.DomainFilterPolicy;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created: Dec 30, 2008 12:43:28 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 */
public class DomainFilter
				extends XMPPProcessor
				implements XMPPPacketFilterIfc, XMPPPreprocessorIfc {
	/** constant domain key name */
	public static final String ALLOWED_DOMAINS_KEY = "allowed-domains";

	/** constant domain list key name */
	public static final String  ALLOWED_DOMAINS_LIST_KEY = "allowed-domains-list";
	/** id of the plugin */
	private static final String ID                       = "domain-filter";

	/** Private logger for class instances. */
	private static final Logger     log = Logger.getLogger(DomainFilter.class.getName());
	/** paths for which plugin should be enabled */
	private static final String[][] ELEMENTS = ALL_PATHS;

	/** xmlns for which plugin should be enabled */
	private static final String[]   XMLNSS   = { ALL_NAMES };

	/** default local hostname */
	private static String local_hostname;
	
	private static final String[] EMPTY_STRING_ARRAY = new String[0];

	@Override
	public void filter(Packet packet, XMPPResourceConnection session,
			NonAuthUserRepository repo, Queue<Packet> results) {
		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "Filtering (packet): {0}", packet);
		}
		if ((session == null) || (results == null) || (results.size() == 0)) {
			return;
		}
		try {
			DomainFilterPolicy domains = getDomains(session);

			// Fast return when user is authorized to send packets to any domain
			if (domains == DomainFilterPolicy.ALL) {
				return;
			}

			Queue<Packet> errors = new ArrayDeque<Packet>(1);

			for (Iterator<Packet> it = results.iterator(); it.hasNext(); ) {
				Packet res = it.next();

				if ( log.isLoggable( Level.FINEST ) ){
					log.log( Level.FINEST, "Filtering (result): {0}", res );
				}
				
				if (domains == DomainFilterPolicy.BLOCK) {
					if ((res.getType() != StanzaType.error) && ((((res.getStanzaFrom() != null) &&
							!session.isUserId(res.getStanzaFrom().getBareJID())) || ((res
							.getStanzaTo() != null) &&!session.isUserId(res.getStanzaTo()
							.getBareJID()))))) {
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
					if ((outDomain != null) &&!session.isLocalDomain(outDomain, true) && !outDomain.equals( local_hostname )) {
						removePacket( it, res, errors,
													"You can only communicate within the server local domains." );
						if ( log.isLoggable( Level.FINEST ) ){
							log.log( Level.FINEST, "LOCAL Domains only, blocking packet (filter): {0}", res );
						}
					} else {
						if ( log.isLoggable( Level.FINEST ) ){
							log.log( Level.FINEST, "LOCAL Domains only, packet not blocked (filter): {0}", res );
						}
					}

					break;

				case OWN :
					if ((outDomain != null) && !outDomain.equals( local_hostname )
							&&!outDomain.endsWith(session.getDomain().getVhost().getDomain())) {

						removePacket( it, res, errors,
													"You can only communicate within your own domain." );
						if ( log.isLoggable( Level.FINEST ) ){
							log.log( Level.FINEST, "OWN Domain only, blocking packet (filter): {0}", res );
						}
					} else {
						if ( log.isLoggable( Level.FINEST ) ){
							log.log( Level.FINEST, "OWN Domain only, packet not blocked (filter): {0}", res );
						}
					}

					break;

				case CUSTOM :
					String[] customRules = getDomainsList(session);

					if ( ( outDomain == null ) || outDomain.equals( local_hostname )
							 || ( res.getType() != null && res.getType().equals( StanzaType.error ) )
							 || ( res.getStanzaFrom() == null && res.getStanzaTo() != null
										&& session.isUserId( res.getStanzaTo().getBareJID() ) ) ){
						// don't filter system packets, breaks things
						break;
					}

					boolean isAlowed = CustomDomainFilter.isAllowed( res.getStanzaFrom(), res.getStanzaTo(), customRules );

					if ( !isAlowed ){
						removePacket( it, res, errors,
													"Your packet was blocked by server filtering rules - FORBIDDEN" );
						if ( log.isLoggable( Level.FINEST ) ){
							log.log( Level.FINEST, "CUSTOM filtering rules for domain {0}, blocking packet (filter): {1}, rules: {2}",
											 new Object[] { outDomain, res, Arrays.asList( customRules) } );
						}
					} else {
						if ( log.isLoggable( Level.FINEST ) ){
							log.log( Level.FINEST, "CUSTOM filtering rules for domain {0}, packet not blocked (filter): {1}, rules: {2}",
											 new Object[] { outDomain, res, Arrays.asList( customRules) } );
						}
					}

					break;

				case BLACKLIST :
					String[] blacklistedDomains = getDomainsList(session);
					boolean  blacklist_match          = false;

					if ( ( outDomain == null ) || outDomain.equals( local_hostname ) ){
						// don't filter system packets, breaks things
						break;
					}
					for (String domain : blacklistedDomains) {

						// Intentionally comparing domains by reference instead of value
						// domain is processed through the String.intern() method
						if (domain == outDomain) {
							blacklist_match = true;

							break;
						}
					}
					if (blacklist_match) {
						removePacket(it, res, errors,
								"You attempted to communicate with the blacklisted domain - FORBIDDEN");
						if ( log.isLoggable( Level.FINEST ) ){
							log.log( Level.FINEST, "BLACKLIST domain {1}, blocking packet (filter): {0}",
																		 new Object [] {res,outDomain} );
						}
					} else {
						if ( log.isLoggable( Level.FINEST ) ){
							log.log( Level.FINEST, "BLACKLIST domain {1], packet not blocked (filter): {0}",
																		 new Object [] {res,outDomain} );
						}
					}


					break;

				case LIST :

					String[] allowedDomains = getDomainsList(session);
					boolean  found          = false;

					if ( ( outDomain == null ) || outDomain.equals( local_hostname ) ){
						// don't filter system packets, breaks things
						break;
					}
					for (String domain : allowedDomains) {

						// Intentionally comparing domains by reference instead of value
						// domain is processed through the String.intern() method
						if (domain == outDomain) {
							found = true;

							break;
						}
					}
					if (!found) {
						removePacket( it, res, errors,
													"You can only communicate within selected list of domains." );
						if ( log.isLoggable( Level.FINEST ) ){
							log.log( Level.FINEST, "LIST Domain only {1}, blocking packet (filter): {0}",
											 new Object[] { res, outDomain } );
						}
					} else {
						if ( log.isLoggable( Level.FINEST ) ){
							log.log( Level.FINEST, "LIST Domain only {1}, packet not blocked (filter): {0}",
											 new Object[] { res, outDomain } );
						}
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

	@Override
	public String id() {
		return ID;
	}

	@Override
	public void init( Map<String, Object> settings ) throws TigaseDBException {
		super.init( settings );
		local_hostname = DNSResolverFactory.getInstance().getDefaultHost();
	}

	@Override
	public boolean preProcess( Packet packet, XMPPResourceConnection session,
														 NonAuthUserRepository repo, Queue<Packet> results, Map<String, Object> settings ) {
		if ( log.isLoggable( Level.FINEST ) ){
			log.log( Level.FINEST, "Processing: {0}", packet );
		}

		boolean stop = false;

		if ( ( session == null ) || session.isServerSession() ){
			return stop;
		}
		try {
			DomainFilterPolicy domains = getDomains( session );

			if ( log.isLoggable( Level.FINEST ) ){
				log.log( Level.FINEST, "DOMAINS setting is: {0}", domains.name() );
			}

			// Fast return when user is authorized to send packets to any domain
			if ( domains == DomainFilterPolicy.ALL ){
				return stop;
			}

			String outDomain = ( packet.getStanzaFrom() != null )
												 ? packet.getStanzaFrom().getDomain()
												 : null;

			try {
				if ( session.getConnectionId().equals( packet.getPacketFrom() ) ){
					outDomain = ( packet.getStanzaTo() != null )
											? packet.getStanzaTo().getDomain()
											: null;
				}
			} catch ( NoConnectionIdException ex ) {
				log.log( Level.WARNING,
								 "No connection id for session, even though this is not a server "
								 + "session: {0}, request: {1}", new Object[] { session,
																																packet } );
			}
			switch ( domains ) {
				case BLOCK:
				if ((packet.getType() == StanzaType.error) || ((packet.getStanzaFrom() ==
						null) || (session.isUserId(packet.getStanzaFrom().getBareJID())
											&& ((packet.getStanzaTo() == null )
													|| session.isUserId( packet.getStanzaTo().getBareJID() ) ) ) ) ){
						return stop;
					} else {
						removePacket( null, packet, results, "Communication blocked." );
						stop = true;
					}
					if ( log.isLoggable( Level.FINEST ) ){
						log.log( Level.FINEST, "BLOCK, blocking packet: {0}", packet );
					}

					break;

				case LOCAL:
					if ( ( outDomain != null ) && !session.isLocalDomain( outDomain, true ) ){
						removePacket( null, packet, results,
													"You can only communicate within the server local domains." );
						stop = true;
						if ( log.isLoggable( Level.FINEST ) ){
							log.log( Level.FINEST, "LOCAL Domains only {1}, blocking packet: {0}",
											 new Object[] { packet, outDomain } );
						}
					} else {
						if ( log.isLoggable( Level.FINEST ) ){
							log.log( Level.FINEST, "LOCAL Domains only {1}, packet not blocked: {0}",
											 new Object[] { packet, outDomain } );
						}
					}

					break;

				case OWN:
					if ( ( outDomain != null ) && !outDomain.equals( local_hostname )
							 && !outDomain.endsWith( session.getDomain().getVhost().getDomain() ) ){
						removePacket( null, packet, results,
													"You can only communicate within your own domain." );
						stop = true;
						if ( log.isLoggable( Level.FINEST ) ){
							log.log( Level.FINEST, "OWN Domain only {1}, blocking packet: {0}",
											 new Object[] { packet, outDomain } );
						}
					} else {
						if ( log.isLoggable( Level.FINEST ) ){
							log.log( Level.FINEST, "OWN Domain only {1}, packet not blocked: {0}",
											 new Object[] { packet, outDomain } );
						}
					}

					break;

				case CUSTOM:
					String[] customRules = getDomainsList( session );

					if ( ( outDomain == null ) || outDomain.equals( local_hostname )
							 || (packet.getType() == StanzaType.error)
							 || ( packet.getStanzaFrom() == null && packet.getStanzaTo() != null
										&& session.isUserId( packet.getStanzaTo().getBareJID() ) ) ){
						// don't filter system packets, breaks things
						break;
					}

					boolean isAlowed = CustomDomainFilter.isAllowed( packet.getStanzaFrom(), packet.getStanzaTo(), customRules );

					if ( !isAlowed ){
						removePacket( null, packet, results,
													"Your packet was blocked by server filtering rules - FORBIDDEN" );
						stop = true;
						if ( log.isLoggable( Level.FINEST ) ){
							log.log( Level.FINEST, "CUSTOM filtering rules {0}, blocking packet (filter): {1}",
											 new Object[] { outDomain, packet } );
						}
					} else {
						if ( log.isLoggable( Level.FINEST ) ){
							log.log( Level.FINEST, "CUSTOM filtering rules {0}, packet not blocked (filter): {1}",
											 new Object[] { outDomain, packet } );
						}
					}

					break;

				case BLACKLIST:
					String[] disallowedDomains = getDomainsList( session );
					boolean blacklist_match = false;
					if ( ( outDomain == null ) || outDomain.equals( local_hostname ) ){
						// don't filter system packets, breaks things
						break;
					}

					for ( String domain : disallowedDomains ) {

					// Intentionally comparing domains by reference instead of value
						// domain is processed through the String.intern() method
						if ( domain == outDomain ){
							blacklist_match = true;

							break;
						}
					}
					if ( blacklist_match ){
						removePacket( null, packet, results,
													"You attempted to communicate with the blacklisted domain - FORBIDDEN" );
						stop = true;
						if ( log.isLoggable( Level.FINEST ) ){
							log.log( Level.FINEST, "Packet to blacklisted domain {1}, blocking packet: {0}",
											 new Object[] { packet, outDomain } );
						}
					} else {
						if ( log.isLoggable( Level.FINEST ) ){
							log.log( Level.FINEST, "Packet NOT TO blacklisted domain {1}, NOT blocking packet: {0}",
											 new Object[] { packet, outDomain } );
						}
					}

					break;

				case LIST:
					String[] allowedDomains = getDomainsList( session );
					boolean found = false;

					if ( ( outDomain == null ) || outDomain.equals( local_hostname ) ){
						// don't filter system packets, breaks things
						break;
					}
					for ( String domain : allowedDomains ) {

					// Intentionally comparing domains by reference instead of value
						// domain is processed through the String.intern() method
						if ( domain == outDomain ){
							found = true;

							break;
						}
					}
					if ( !found ){
						removePacket( null, packet, results,
													"You can only communicate within selected list of domains." );
						stop = true;
						if ( log.isLoggable( Level.FINEST ) ){
							log.log( Level.FINEST, "LISTED Domains only {1}, blocking packet: {0}",
											 new Object[] { packet, outDomain } );
						}
					} else {
						if ( log.isLoggable( Level.FINEST ) ){
							log.log( Level.FINEST, "LISTED Domain only {1}, packet not blocked: {0}",
											 new Object[] { packet, outDomain } );
						}
					}

					break;
			}
		} catch ( NotAuthorizedException ex ) {

			// This can happen actually easily when filtering initial handshaking
			// packets. I am not sure what to do yet.....
			// Assuming this an initial, and authentication traffic, allowing for
			// everything, which means, just ignore.
		} catch ( TigaseDBException ex ) {
			log.log( Level.WARNING, "Can't access user repository.", ex );
		}

		return stop;
	}

	@Override
	public String[][] supElementNamePaths() {
		return ELEMENTS;
	}

	@Override
	public String[] supNamespaces() {
		return XMLNSS;
	}

	/**
	 * Method retrieves filtering policy based on user session, from most specific to most general,
	 * i.e.: first user session is checked, if that fails then user repository and if there is no
	 * rules configured then domain filtering policy from VHost is being returned (if present).
	 *
	 * @param session  user session which keeps all the user session data and also
	 *                 gives an access to the user's repository data. It allows
	 *                 for storing information in a permanent storage or in memory
	 *                 only during the live of the online session. This parameter
	 *                 can be null if there is no online user session at the time
	 *                 of the packet processing.
	 *
	 * @return relevant domain filtering policy
	 * 
	 * @throws NotAuthorizedException
	 * @throws TigaseDBException
	 */
	public DomainFilterPolicy getDomains(XMPPResourceConnection session)
					throws NotAuthorizedException, TigaseDBException {
		VHostItem domain = session.getDomain();
		DomainFilterPolicy domainFilterPolicy
											 = (DomainFilterPolicy) session.getCommonSessionData(ALLOWED_DOMAINS_KEY );

		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "Domains read from user session: {0} for VHost: {1}",
														new Object[] {domainFilterPolicy, domain} );
		}
		if (domainFilterPolicy == null) {
			String dbDomains = session.getData(null, ALLOWED_DOMAINS_KEY, null);

			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "Domains read from database: {0} for VHost: {1}",
															new Object[] {dbDomains, domain});
			}
			domainFilterPolicy = DomainFilterPolicy.valueof(dbDomains);
			if (domainFilterPolicy == null) {
				if (session.isAnonymous()) {
					domainFilterPolicy = DomainFilterPolicy.LOCAL;
				} else {
					// by default ALL
					domainFilterPolicy = domain.getDomainFilter();
				}
			}
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "Domains read from VHost item: {0} for VHost: {1}",
															new Object[] {domainFilterPolicy, domain});
			}
			session.putCommonSessionData( ALLOWED_DOMAINS_KEY, domainFilterPolicy );
		}

		return domainFilterPolicy;
	}

	/**
	 * Method retrieves list of domains to be applied to {@code LIST} and
	 * {@code BLACKLIST} filtering policies based on user session, from most
	 * specific to most general, i.e.: first user session is checked, if that
	 * fails then user repository and if there is no rules configured then list of
	 * domains from VHost is being returned (if present).
	 *
	 * @param session user session which keeps all the user session data and also
	 *                gives an access to the user's repository data. It allows for
	 *                storing information in a permanent storage or in memory only
	 *                during the live of the online session. This parameter can be
	 *                null if there is no online user session at the time of the
	 *                packet processing.
		 *
	 * @return list of domains to be whitelisted/blacklisted
	 *
	 * @throws NotAuthorizedException
	 * @throws TigaseDBException
	 */
	public String[] getDomainsList(XMPPResourceConnection session)
					throws NotAuthorizedException, TigaseDBException {
		VHostItem domain = session.getDomain();

		String[] domainsList = (String[]) session.getCommonSessionData( ALLOWED_DOMAINS_LIST_KEY );

		if ( log.isLoggable( Level.FINEST ) ){
			log.log( Level.FINEST, "Getting list of domains from user session: {0} for VHost: {1}",
														 new Object[] { domainsList != null? Arrays.asList( domainsList) : "", domain } );
		}

		if ( domainsList == null ){
			String dbDomains = session.getData( null, ALLOWED_DOMAINS_LIST_KEY, null );

			if ( log.isLoggable( Level.FINEST ) ){
				log.log( Level.FINEST, "Domains list read from database: {0} for VHost: {1}",
															 new Object[] { dbDomains, domain } );
			}

			if ( dbDomains != null ){
				domainsList = StringUtilities.stringToArrayOfString( dbDomains, ";" );
			} else {
				domainsList = domain.getDomainFilterDomains();
				if ( log.isLoggable( Level.FINEST ) ){
					log.log( Level.FINEST, "Domains list read from VHost: {0} for VHost: {1}",
																 new Object[] { domainsList != null? Arrays.asList( domainsList) : "", domain } );
				}
			}
			if (domainsList == null)
				domainsList = EMPTY_STRING_ARRAY;
			session.putCommonSessionData( ALLOWED_DOMAINS_LIST_KEY, domainsList );
		}

		return domainsList;
	}

	/**
	 * Helper method removing packets from processing queue and generating
	 * appropriate error packet to be send back to client
	 *
	 * @param it iterator over collection of packets being filtered
	 * @param res currently processed packet
	 * @param errors collection of outgoing errors
	 * @param msg human readable error message
	 */
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
