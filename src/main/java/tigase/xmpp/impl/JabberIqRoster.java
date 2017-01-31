/*
 * JabberIqRoster.java
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
import java.util.*;
import java.util.logging.*;
import tigase.db.*;
import tigase.server.*;

import tigase.util.TigaseStringprepException;
import tigase.xml.Element;
import tigase.xmpp.*;
import static tigase.xmpp.impl.PresenceSubscription.AUTO_AUTHORIZE_PROP_KEY;
import tigase.xmpp.impl.roster.RosterAbstract.SubscriptionType;
import tigase.xmpp.impl.roster.*;

/**
 * Class
 * <code>JabberIqRoster</code> implements part of <em>RFC-3921</em> -
 * <em>XMPP Instant Messaging</em> specification describing roster management.
 * 7. Roster Management
 *
 *
 * Created: Tue Feb 21 17:42:53 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class JabberIqRoster
		extends XMPPProcessor
		implements XMPPProcessorIfc {

	/** Field description */
	public static final String ANON = "anon";
	private static final String[][] ELEMENTS = {
		{ Iq.ELEM_NAME, Iq.QUERY_NAME }, { Iq.ELEM_NAME, Iq.QUERY_NAME }, { Iq.ELEM_NAME, Iq.QUERY_NAME }
	};
	/** Private logger for class instance. */
	private static final Logger log = Logger.getLogger( JabberIqRoster.class.getName() );
	private static final String[] XMLNSS = { RosterAbstract.XMLNS,
																					 RosterAbstract.XMLNS_DYNAMIC, RosterAbstract.XMLNS_LOAD };
	private static final String[] IQ_QUERY_ITEM_PATH = { Iq.ELEM_NAME, Iq.QUERY_NAME,
																											 "item" };
	/** unique ID of the plugin */
	private static final String ID = RosterAbstract.XMLNS;
	/** variable holding setting regarding auto authorisation of items added to
	 * user roset */
	private static boolean autoAuthorize = false;
	//~--- fields ---------------------------------------------------------------
	/** instance of class implementing {@link RosterAbstract} */
	protected RosterAbstract roster_util = getRosterUtil();

	//~--- methods --------------------------------------------------------------
	@Override
	public int concurrentQueuesNo() {
		return super.concurrentQueuesNo() * 4;
	}

	@Override
	public String id() {
		return ID;
	}

	@Override
	public void init( Map<String, Object> settings ) throws TigaseDBException {
		autoAuthorize = Boolean.parseBoolean( (String) settings.get( AUTO_AUTHORIZE_PROP_KEY ) );
		if ( autoAuthorize ){
			log.config( "Automatic presence subscription of new roster items enabled,"
									+ "results in less strict XMPP specs compatibility " );
		}
		if ( roster_util != null ){
			roster_util.setProperties( settings );
		}
	}

	/**
	 * {@inheritDoc}
	 *
	 * <br><br>
	 *
	 * Performs processing of <em>IQ</em> packets with <em>jabber:iq:roster</em>
	 * xmlns with the regard whether it's roster <em>set</em> or <em>get</em>
	 * request or possibly dynamic-roster is involved. request.
	 */
	@Override
	public void process( Packet packet, XMPPResourceConnection session,
											 NonAuthUserRepository repo, Queue<Packet> results, Map<String, Object> settings )
			throws XMPPException {
		if ( session == null ){
			if ( log.isLoggable( Level.FINE ) ){
				log.log( Level.FINE, "Session is null, ignoring packet: {0}", packet );
			}

			return;
		}    // end of if (session == null)
		if ( !session.isAuthorized() ){
			if ( log.isLoggable( Level.FINE ) ){
				log.log( Level.FINE, "Session is not authorized, ignoring packet: {0}", packet );
			}

			return;
		}

		// The roster request can be between the user and the server or between the
		// user and some other entity like transport
		JID connectionId = session.getConnectionId();

		if ( connectionId.equals( packet.getPacketFrom() ) ){

			// Packet from the user, let's check where it should go
			if ( ( packet.getStanzaTo() != null ) && !session.isLocalDomain( packet.getStanzaTo()
					.toString(), false ) && !session.isUserId( packet.getStanzaTo().getBareJID() ) ){

				// This is most likely a roster reaquest sent to a transport or some other
				// roster resource.
				results.offer( packet.copyElementOnly() );

				return;
			}
		} else {
			
			// Packet probably to the user, let's check where it came from
			if ( session.isUserId( packet.getStanzaTo().getBareJID() ) ){
				if ( packet.getStanzaTo().getResource() != null ){
					Packet result = packet.copyElementOnly();

					result.setPacketTo( session.getConnectionId( packet.getStanzaTo() ) );
					result.setPacketFrom( packet.getTo() );
					results.offer( result );
				} else {
					processRemoteRosterManagementRequest( packet, session, results, settings );
				}

				return;
			} else {
				// Hm, I do not know what to do here, should not happen
			}
		}
		try {
			if ( ( packet.getStanzaFrom() != null ) && !session.isUserId( packet.getStanzaFrom()
					.getBareJID() ) ){

				// RFC says: ignore such request
				log.log( Level.WARNING, "Roster request ''from'' attribute doesn't match "
																+ "session: {0}, request: {1}", new Object[] { session,
																																							 packet } );
				return;
			}    // end of if (packet.getElemFrom() != null
			StanzaType type = packet.getType();
			String xmlns = packet.getElement().getXMLNSStaticStr( Iq.IQ_QUERY_PATH );

			if ( xmlns == RosterAbstract.XMLNS ){
				switch ( type ) {
					case get:
						processGetRequest( packet, session, results, settings );
						break;

					case set:
						processSetRequest( packet, session, results, settings );
						break;

					case result:
						// Ignore
						break;

					default:
						results.offer( Authorization.BAD_REQUEST.getResponseMessage( packet,
																																				 "Request type is incorrect", false ) );
						break;
				}    // end of switch (type)
			} else {
				if ( xmlns == RosterAbstract.XMLNS_DYNAMIC ){
					switch ( type ) {
						case get:
							dynamicGetRequest( packet, session, results, settings );
							break;

						case set:
							dynamicSetRequest( packet, session, results, settings );
							break;

						case result:
							// Ignore
							break;

						default:
							results.offer( Authorization.BAD_REQUEST.getResponseMessage( packet,
																																					 "Request type is incorrect", false ) );
							break;
					}    // end of switch (type)
				} else if (xmlns == RosterAbstract.XMLNS_LOAD) {
					switch ( type ) {
						case set:
							if (!roster_util.isRosterLoaded(session)) {
								// here we do not care about result as we only trigger 
								// loading of roster from database
								roster_util.getRosterElement(session, session.getJID());
							}
							Element resultEl = packet.getElement().clone();
							resultEl.setAttribute("type", StanzaType.result.name());
							Packet result = Packet.packetInstance(resultEl, packet.getStanzaFrom(), packet.getStanzaTo());
							result.setPacketFrom(packet.getPacketFrom());
							result.setPacketTo(packet.getPacketTo());
							results.add(result);
							break;
						default:
							break;
					}
				} else {
					// Hm, don't know what to do, unexpected name space, let's record it
					log.log( Level.WARNING, "Unknown XMLNS for the roster plugin: {0}", packet );
				}
			}
		} catch ( RosterRetrievingException e ) {
			log.log( Level.WARNING, "Unknown roster retrieving exception: {0} for packet: {1}",
							 new Object[] { e, packet } );
			results.offer( Authorization.UNDEFINED_CONDITION.getResponseMessage( packet, e
					.getMessage(), true ) );
		} catch ( RepositoryAccessException e ) {
			log.log( Level.WARNING,
							 "Problem with roster repository access: {0} for packet: {1}",
							 new Object[] { e, packet } );
			results.offer( packet.okResult( (String) null, 0 ) );
		} catch ( NotAuthorizedException e ) {
			log.log( Level.WARNING,
							 "Received roster request but user session is not authorized yet: {0}", packet );
			results.offer( Authorization.NOT_AUTHORIZED.getResponseMessage( packet,
																																			"You must authorize session first.", true ) );
		} catch ( PolicyViolationException e ) {
			log.log( Level.FINE,
							 "Roster set request violated items number policy: {0}", packet );
			results.offer( Authorization.POLICY_VIOLATION.getResponseMessage( packet,
																																			e.getLocalizedMessage(), true ) );
		} catch ( TigaseDBException e ) {
			log.log( Level.WARNING, "Database problem, please contact admin:", e );
			results.offer( Authorization.INTERNAL_SERVER_ERROR.getResponseMessage( packet,
																																						 "Database access problem, please contact administrator.", true ) );
		}    // end of try-catch
	}

	@Override
	public Element[] supDiscoFeatures( final XMPPResourceConnection session ) {
		return RosterAbstract.DISCO_FEATURES;
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
	public Element[] supStreamFeatures( final XMPPResourceConnection session ) {
		return RosterAbstract.FEATURES;
	}

	//~--- get methods ----------------------------------------------------------
	/**
	 * Returns an array of group names retrieved from Element item
	 *
	 * @param item from which groups should be obtained
	 * @return an array of group names retrieved from Element item
	 */
	public static String[] getItemGroups( Element item ) {
		List<Element> elgr = item.getChildren();

		if ( ( elgr != null ) && ( elgr.size() > 0 ) ){
			ArrayList<String> groups = new ArrayList<String>( 1 );

			for ( Element grp : elgr ) {
				if ( grp.getName() == RosterAbstract.GROUP ){
					groups.add( grp.getCData() );
				}
			}
			if ( groups.size() > 0 ){
				return groups.toArray( new String[ groups.size() ] );
			}
		}

		return null;
	}

	//~--- methods --------------------------------------------------------------
	/**
	 * Method processes roster
	 * <code>get</code> request related to dynamic roster. Generates output packet
	 * with data from the DynamicRoster implementation for every
	 * <em>item</em>element from processed packet or error otherwise.
	 *
	 * @param packet   packet is which being processed.
	 * @param session  user session which keeps all the user session data and also
	 *                 gives an access to the user's repository data.
	 * @param results  this a collection with packets which have been generated as
	 *                 input packet processing results.
	 * @param settings this map keeps plugin specific settings loaded from the
	 *                 Tigase server configuration.
	 *
	 * @throws NotAuthorizedException
	 */
	protected static void dynamicGetRequest( Packet packet, XMPPResourceConnection session,
																					 Queue<Packet> results, Map<String, Object> settings )
			throws NotAuthorizedException {
		Element request = packet.getElement();
		Element item = request.findChildStaticStr( IQ_QUERY_ITEM_PATH );

		if ( item != null ){
			Element new_item = DynamicRoster.getItemExtraData( session, settings, item );

			if ( new_item == null ){
				new_item = item;
			}
			results.offer( packet.okResult( new_item, 1 ) );
		} else {
			try {
				results.offer( Authorization.BAD_REQUEST.getResponseMessage( packet,
																																		 "Missing 'item' element, request can not be processed.", true ) );
			} catch ( PacketErrorTypeException ex ) {
				log.log( Level.SEVERE, "Received error packet? not possible.", ex );
			}
		}
	}

	/**
	 * Method processes roster
	 * <code>set</code> request related to dynamic roster. Sets extra data for
	 * every
	 * <em>item</em>element from processed packet with the DynamicRoster
	 * implementation and generates success packet or, in case of failure
	 * generates error.
	 *
	 * @param packet   packet is which being processed.
	 * @param session  user session which keeps all the user session data and also
	 *                 gives an access to the user's repository data.
	 * @param results  this a collection with packets which have been generated as
	 *                 input packet processing results.
	 * @param settings this map keeps plugin specific settings loaded from the
	 *                 Tigase server configuration.
	 */
	protected static void dynamicSetRequest( Packet packet, XMPPResourceConnection session,
																					 Queue<Packet> results, Map<String, Object> settings ) {
		Element request = packet.getElement();
		List<Element> items = request.getChildrenStaticStr( Iq.IQ_QUERY_PATH );

		if ( ( items != null ) && ( items.size() > 0 ) ){
			for ( Element item : items ) {
				DynamicRoster.setItemExtraData( session, settings, item );
			}
			results.offer( packet.okResult( (String) null, 0 ) );
		} else {
			try {
				results.offer( Authorization.BAD_REQUEST.getResponseMessage( packet,
																																		 "Missing 'item' element, request can not be processed.", true ) );
			} catch ( PacketErrorTypeException ex ) {
				log.log( Level.SEVERE, "Received error packet? not possible.", ex );
			}
		}
	}

	/**
	 * Method processes roster
	 * <code>get</code> request. Generates output result packet containing user
	 * roster items also considering roster version attribute to minimise traffic.
	 *
	 * @param packet   packet is which being processed.
	 * @param session  user session which keeps all the user session data and also
	 *                 gives an access to the user's repository data.
	 * @param results  this a collection with packets which have been generated as
	 *                 input packet processing results.
	 * @param settings this map keeps plugin specific settings loaded from the
	 *                 Tigase server configuration.
	 *
	 * @throws NotAuthorizedException
	 * @throws RepositoryAccessException
	 * @throws RosterRetrievingException
	 * @throws TigaseDBException
	 */
	protected void processGetRequest( Packet packet, XMPPResourceConnection session,
																		Queue<Packet> results, Map<String, Object> settings )
			throws NotAuthorizedException, TigaseDBException, RosterRetrievingException,
						 RepositoryAccessException {

		// Retrieve all Dynamic roster elements from the roster repository
		List<Element> its = DynamicRoster.getRosterItems( session, settings );

		// If the dynamic roster exists, we have to always recalculate hash, as the
		// part of the roster could have changed outside of the Tigase server.
		if ( ( its != null ) && ( its.size() > 0 ) ){
			updateHash( session, settings );
		}

		// Check roster version hash.
		String incomingHash = packet.getAttributeStaticStr( Iq.IQ_QUERY_PATH, RosterAbstract.VER_ATT );
		String storedHash = "";

		// If client provided hash and the server calculated hash are the same
		// return the success result and abort further roster processing.
		// No need to send the whole roster to the client.
		if ( incomingHash != null ){
			storedHash = roster_util.getBuddiesHash( session );
			if ( ( storedHash == null ) || storedHash.isEmpty() ){
				updateHash( session, settings );
				storedHash = roster_util.getBuddiesHash( session );
			}
			if ( incomingHash.equals( storedHash ) ){
				results.offer( packet.okResult( (String) null, 0 ) );

				return;
			}
		}

		// Retrieve standard roster items.
		List<Element> ritems = roster_util.getRosterItems( session );

		// Send the user's standard roster first
		if ( ( ritems != null ) && ( ritems.size() > 0 ) ){
			Element query = new Element( "query" );

			query.setXMLNS( RosterAbstract.XMLNS );
			if ( incomingHash != null ){
				query.setAttribute( RosterAbstract.VER_ATT, storedHash );
			}
			query.addChildren( ritems );
			results.offer( packet.okResult( query, 0 ) );
		} else {
			results.offer( packet.okResult( (String) null, 1 ) );
		}

		// Push the dynamic roster items now
		try {
			if ( ( its != null ) && ( its.size() > 0 ) ){
				ArrayDeque<Element> items = new ArrayDeque<Element>( its );

				while ( items.size() > 0 ) {
					Element iq = new Element( "iq", new String[] { "type", "id", "to" },
																		new String[] { "set",
																									 session.nextStanzaId(), session.getJID().toString() } );

					iq.setXMLNS( CLIENT_XMLNS );

					Element query = new Element( "query" );

					query.setXMLNS( RosterAbstract.XMLNS );
					iq.addChild( query );
					query.addChild( items.poll() );
					while ( ( query.getChildren().size() < 20 ) && ( items.size() > 0 ) ) {
						query.addChild( items.poll() );
					}

					Packet rost_res = Packet.packetInstance( iq, null, session.getJID() );

					rost_res.setPacketTo( session.getConnectionId() );
					rost_res.setPacketFrom( packet.getTo() );
					results.offer( rost_res );
				}
			}
		} catch ( NoConnectionIdException ex ) {
			log.log( Level.WARNING,
							 "Problem with roster request, no connection ID for session: {0}, request: {1}",
							 new Object[] { session,
															packet } );
		}
	}

	/**
	 * Method processes roster
	 * <code>set</code> request. Performs modifications of user roster.
	 *
	 * @param packet   packet is which being processed.
	 * @param session  user session which keeps all the user session data and also
	 *                 gives an access to the user's repository data.
	 * @param results  this a collection with packets which have been generated as
	 *                 input packet processing results.
	 * @param settings this map keeps plugin specific settings loaded from the
	 *                 Tigase server configuration.
	 *
	 * @throws NotAuthorizedException
	 * @throws TigaseDBException
	 * @throws XMPPException
	 */
	protected void processSetRequest( Packet packet, XMPPResourceConnection session,
																		Queue<Packet> results, final Map<String, Object> settings )
			throws XMPPException, NotAuthorizedException, TigaseDBException, PolicyViolationException {

		// Element request = packet.getElement();
		List<Element> items = packet.getElemChildrenStaticStr( Iq.IQ_QUERY_PATH );

		if ( items != null ){
			try {

				// RFC-3921 draft bis-03 forbids multiple items in one request
				// This however seems to make no much sense and actually was
				// requested by many users to allow for multiple items
				for ( Element item : items ) {
					JID buddy = JID.jidInstance( item.getAttributeStaticStr( "jid" ) );

					if ( DynamicRoster.getBuddyItem( session, settings, buddy ) != null ){

						// Let's return an error. Dynamic roster cannot be modified via
						// XMPP.
						results.offer( Authorization.FEATURE_NOT_IMPLEMENTED.getResponseMessage(
								packet, "You cannot modify this contact. It is controlled by an "
												+ "external service.", true ) );
						return;
					}
					if ( session.isUserId( buddy.getBareJID() ) ){
						results.offer( Authorization.NOT_ALLOWED.getResponseMessage( packet,
													 "User can't add himself to the roster, RFC says NO.", true ) );
						return;
					}

					String subscription = item.getAttributeStaticStr( "subscription" );

					if ( ( subscription != null ) && subscription.equals( "remove" ) ){
						SubscriptionType sub = roster_util.getBuddySubscription( session, buddy );

						if ( sub == null ){
							sub = SubscriptionType.none;
						}

						String type = item.getAttributeStaticStr( Packet.TYPE_ATT );

						if ( ( sub != SubscriptionType.none ) && ( ( type == null ) || !type.equals( ANON ) ) ){

							// Unavailable presence should be sent first, otherwise it will be
							// blocked by the server after the subscription is canceled
							Element pres = new Element( PresenceAbstract.PRESENCE_ELEMENT_NAME );

							pres.setXMLNS( CLIENT_XMLNS );
							pres.setAttribute( Packet.TO_ATT, buddy.toString() );
							pres.setAttribute( Packet.FROM_ATT, session.getJID().toString() );
							pres.setAttribute( Packet.TYPE_ATT, "unavailable" );

							Packet pres_packet = Packet.packetInstance( pres, session.getJID(), buddy );

							// We have to set a higher priority for this particular
							// unavailable packet
							// to make sure it is delivered before subscription cancellation
							pres_packet.setPriority( Priority.HIGH );
							results.offer( pres_packet );
							pres = new Element( PresenceAbstract.PRESENCE_ELEMENT_NAME );
							pres.setXMLNS( CLIENT_XMLNS );
							pres.setAttribute( Packet.TO_ATT, buddy.toString() );
							pres.setAttribute( Packet.FROM_ATT, session.getBareJID().toString() );
							pres.setAttribute( Packet.TYPE_ATT, "unsubscribe" );
							results.offer( Packet.packetInstance( pres, session.getJID()
									.copyWithoutResource(), buddy ) );
							pres = new Element( PresenceAbstract.PRESENCE_ELEMENT_NAME );
							pres.setXMLNS( CLIENT_XMLNS );
							pres.setAttribute( Packet.TO_ATT, buddy.toString() );
							pres.setAttribute( Packet.FROM_ATT, session.getBareJID().toString() );
							pres.setAttribute( Packet.TYPE_ATT, "unsubscribed" );
							results.offer( Packet.packetInstance( pres, session.getJID()
									.copyWithoutResource(), buddy ) );
						}    // is in the roster while he isn't. In such a case just ensure the

						// client that the buddy has been removed for sure
						Element it = new Element( "item" );

						it.setAttribute( "jid", buddy.toString() );
						it.setAttribute( "subscription", "remove" );
						roster_util.removeBuddy( session, buddy );
						roster_util.updateBuddyChange( session, results, it );
					} else {

						// We are adding a new roster element here
						String name = item.getAttributeStaticStr( "name" );
						List<Element> groups = item.getChildren();
						String[] gr = null;

						if ( ( groups != null ) && ( groups.size() > 0 ) ){
							gr = new String[ groups.size() ];

							int cnt = 0;

							for ( Element group : groups ) {
								gr[cnt++] = ( ( group.getCData() == null )
															? ""
															: group.getCData() );
							}    // end of for (ElementData group : groups)

							// end of for (ElementData group : groups)
						}
						roster_util.addBuddy( session, buddy, name, gr, null );

						String type = item.getAttributeStaticStr( Packet.TYPE_ATT );

						if ( ( type != null ) && type.equals( ANON ) || autoAuthorize ){
							roster_util.setBuddySubscription( session, SubscriptionType.both, buddy );

							Element pres = (Element) session.getSessionData( XMPPResourceConnection.PRESENCE_KEY );

							if ( pres == null ){
								pres = new Element( PresenceAbstract.PRESENCE_ELEMENT_NAME );
								pres.setXMLNS( CLIENT_XMLNS );
							} else {
								pres = pres.clone();
							}
							pres.setAttribute( Packet.TO_ATT, buddy.toString() );
							pres.setAttribute( Packet.FROM_ATT, session.getJID().toString() );
							results.offer( Packet.packetInstance( pres, session.getJID(), buddy ) );

							if ( autoAuthorize ){
								PresenceAbstract.sendPresence( StanzaType.subscribe, session.getJID().copyWithoutResource(),
																			 buddy.copyWithoutResource(), results, null );
							}
						}

						Element new_buddy = roster_util.getBuddyItem( session, buddy );

						if ( log.isLoggable( Level.FINEST ) ){
							log.log( Level.FINEST, "1. New Buddy: {0}", new_buddy.toString() );
						}
						if ( roster_util.getBuddySubscription( session, buddy ) == null ){
							roster_util.setBuddySubscription( session, SubscriptionType.none, buddy );
						}    // end of if (getBuddySubscription(session, buddy) == null)
						new_buddy = roster_util.getBuddyItem( session, buddy );
						if ( log.isLoggable( Level.FINEST ) ){
							log.log( Level.FINEST, "2. New Buddy: {0}", new_buddy.toString() );
						}
						roster_util.updateBuddyChange( session, results, new_buddy );
					}      // end of else

					// end of else
				}
				results.offer( packet.okResult( (String) null, 0 ) );
			} catch ( TigaseStringprepException ex ) {
				results.offer( Authorization.BAD_REQUEST.getResponseMessage( packet,
																																		 "Buddy JID is incorrct, stringprep failed.", true ) );
			}
		} else {
			log.log( Level.WARNING, "No items found in roster set request: {0}", packet );
			results.offer( Authorization.BAD_REQUEST.getResponseMessage( packet,
																																	 "No items found in the roster set request", true ) );
		}
	}

	/**
	 * Calculates hash value based on the user roster items and saves it to user's
	 * session data.
	 *
	 * @param session  user session which keeps all the user session data and also
	 *                 gives an access to the user's repository data.
	 * @param settings this map keeps plugin specific settings loaded from the
	 *                 Tigase server configuration.
	 *
	 * @throws NotAuthorizedException
	 * @throws RepositoryAccessException
	 * @throws RosterRetrievingException
	 * @throws TigaseDBException
	 */
	protected void updateHash( XMPPResourceConnection session, Map<String, Object> settings )
			throws NotAuthorizedException, TigaseDBException, RosterRetrievingException,
						 RepositoryAccessException {

		// Retrieve standard roster items.
		List<Element> ritems = roster_util.getRosterItems( session );

		// Recalculate the roster hash again with dynamic roster content
		StringBuilder roster_str = new StringBuilder( 5000 );

		// Retrieve all Dynamic roster elements from the roster repository
		List<Element> its = DynamicRoster.getRosterItems( session, settings );

		// There is always a chance that the same elements exist in a dynamic roster
		// and the standard user roster. Moreover, the items in the standard roster
		// may have a different presence subscription set.
		// Here we make sure they are both in sync, that is for each entry which
		// exists in both rosters we enforce 'both' subscription type for element in
		// standard roster and remove it from the dynamic roster list.
		if ( ( its != null ) && ( its.size() > 0 ) ){
			for ( Iterator<Element> it = its.iterator() ; it.hasNext() ; ) {
				Element element = it.next();

				try {
					JID jid = JID.jidInstance( element.getAttributeStaticStr( "jid" ) );

					if ( roster_util.containsBuddy( session, jid ) ){
						roster_util.setBuddySubscription( session, SubscriptionType.both, jid );

						String[] itemGroups = getItemGroups( element );

						if ( itemGroups != null ){
							roster_util.addBuddyGroup( session, jid, itemGroups );
						}
						it.remove();
					}
				} catch ( TigaseStringprepException ex ) {
					log.log( Level.INFO,
									 "JID from dynamic roster is incorrect, stringprep failed for: {0}", element
							.getAttributeStaticStr( "jid" ) );
					it.remove();
				}
			}

			// This may seem to be redundant as this call has already been made
			// but the roster could have been changed during above dynamic roster
			// merge
			ritems = roster_util.getRosterItems( session );
			for ( Element ritem : its ) {
				roster_str.append( ritem.toString() );
			}
		}
		for ( Element ritem : ritems ) {
			roster_str.append( ritem.toString() );
		}
		roster_util.updateRosterHash( roster_str.toString(), session );
	}

	//~--- get methods ----------------------------------------------------------
	/**
	 * Returns shared instance of class implementing {@link RosterAbstract} -
	 * either default one ({@link RosterFlat}) or the one configured with
	 * <em>"roster-implementation"</em> property.
	 *
	 * @return a shared instance of class implementing {@link RosterAbstract}
	 */
	protected RosterAbstract getRosterUtil() {
		return RosterFactory.getRosterImplementation( true );
	}

	//~--- methods --------------------------------------------------------------
	/**
	 * Performs processing of remote roster management requests as described in <a
	 * href="http://xmpp.org/extensions/xep-0321.html">XEP-0321: Remote Roster
	 * Management</a> for the purpose of, for example, gateways.
	 *
	 * @param packet   packet is which being processed.
	 * @param session  user session which keeps all the user session data and also
	 *                 gives an access to the user's repository data.
	 * @param results  this a collection with packets which have been generated as
	 *                 input packet processing results.
	 * @param settings this map keeps plugin specific settings loaded from the
	 *                 Tigase server configuration.
	 * @throws PacketErrorTypeException
	 */
	private void processRemoteRosterManagementRequest( Packet packet,
																										 XMPPResourceConnection session,
																										 Queue<Packet> results,
																										 final Map<String, Object> settings )
			throws PacketErrorTypeException {
		if ( !RemoteRosterManagement.isRemoteAllowed( packet.getStanzaFrom(), session ) ){
			results.offer( Authorization.NOT_ALLOWED.getResponseMessage( packet,
																																	 "Not authorized for remote roster management", true ) );

			return;
		}
		try {
			switch ( packet.getType() ) {
				case get:
					List<Element> ritems = roster_util.getRosterItems( session );

					if ( ( ritems != null ) && !ritems.isEmpty() ){
						Element query = new Element( "query" );

						query.setXMLNS( RosterAbstract.XMLNS );

						String jidStr = "@" + packet.getStanzaFrom().getBareJID().toString();

						for ( Element ritem : ritems ) {
							if ( ritem.getAttributeStaticStr( "jid" ).endsWith( jidStr ) ){
								query.addChild( ritem );
							}
						}
						results.offer( packet.okResult( query, 0 ) );
					} else {
						results.offer( packet.okResult( (String) null, 1 ) );
					}

					break;

				case set:
					processSetRequest( packet, session, results, settings );

					break;

				default:
					results.offer( Authorization.BAD_REQUEST.getResponseMessage( packet,
																																			 "Bad stanza type", true ) );

					break;
			}
		} catch ( PolicyViolationException e ) {
			log.log( Level.WARNING,
							 "Roster set request violated items number policy: {0}", packet );
			results.offer( Authorization.POLICY_VIOLATION.getResponseMessage( packet,
																																			e.getLocalizedMessage(), true ) );
		} catch ( Throwable ex ) {
			log.log( Level.WARNING, "Reflection execution exception", ex );
			results.offer( Authorization.INTERNAL_SERVER_ERROR.getResponseMessage( packet,
																																						 "Internal server error", true ) );
		}
	}
}    // JabberIqRoster
