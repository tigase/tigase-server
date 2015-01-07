/*
 * OfflineMessages.java
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
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;
import tigase.db.MsgRepositoryIfc;
import tigase.db.NonAuthUserRepository;
import tigase.db.TigaseDBException;
import tigase.db.UserNotFoundException;
import tigase.osgi.ModulesManagerImpl;
import static tigase.server.Message.ELEM_NAME;
import tigase.server.Packet;
import tigase.util.DNSResolver;
import tigase.util.TigaseStringprepException;
import tigase.xml.DomBuilderHandler;
import tigase.xml.Element;
import tigase.xml.SimpleParser;
import tigase.xml.SingletonFactory;
import tigase.xmpp.JID;
import tigase.xmpp.NotAuthorizedException;
import tigase.xmpp.StanzaType;
import tigase.xmpp.XMPPPostprocessorIfc;
import tigase.xmpp.XMPPProcessor;
import tigase.xmpp.XMPPProcessorIfc;
import tigase.xmpp.XMPPResourceConnection;

/**
 * OfflineMessages plugin implementation which follows <a
 * href="http://xmpp.org/extensions/xep-0160.html">XEP-0160: Best Practices for
 * Handling Offline Messages</a> specification. Responsible for storing messages
 * send to offline users - either as a standalone plugin or as a processor for
 * other plugins (e.g. AMP). Is registered to handle packets of type
 * {@code <presence>}.
 *
 *
 * Created: Mon Oct 16 13:28:53 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 */
public class OfflineMessages
		extends XMPPProcessor
		implements XMPPPostprocessorIfc, XMPPProcessorIfc {

	/** Field holds default client namespace for stanzas. In case of
	 * {@code msgoffline} plugin it is <em>jabber:client</em> */
	protected static final String XMLNS = "jabber:client";
	/** Field holds identification string for the plugin. In case of
	 * {@code msgoffline} plugin it is <em>msgoffline</em> */
	private static final String ID = "msgoffline";
	/** Private logger for class instances. */
	private static final Logger log = Logger.getLogger( OfflineMessages.class.getName() );
	/** Field holds an array for element paths for which the plugin offers
	 * processing capabilities. In case of {@code msgoffline} plugin it is
	 * <em>presence</em> stanza */
	private static final String[][] ELEMENTS = {
		{ Presence.PRESENCE_ELEMENT_NAME } };
	/** Field holds an array of name-spaces for stanzas which can be processed by
	 * this plugin. In case of {@code msgoffline} plugin it is
	 * <em>jabber:client</em> */
	private static final String[] XMLNSS = { XMLNS };
	/** Field holds an array of XML Elements with service discovery features which
	 * have to be returned to the client uppon request. In case of
	 * {@code msgoffline} plugin it is the same as plugin name -
	 * <em>msgoffline</em> */
	private static final Element[] DISCO_FEATURES = {
		new Element( "feature", new String[] { "var" }, new String[] { "msgoffline" } ) };
	/** Field holds the default hostname of the machine. */
	private static final String defHost = DNSResolver.getDefaultHostname();
	/** Field holds an array for element paths for which the plugin offers message
	 * saving capabilities. In case of {@code msgoffline} plugin it is
	 * <em>presence</em> stanza */
	public static final String[] MESSAGE_EVENT_PATH = { ELEM_NAME, "event" };
	/** Field holds an array for element paths for which the plugin offers
	 * processing capabilities. In case of {@code msgoffline} plugin it is
	 * <em>presence</em> stanza */
	public static final String[] MESSAGE_HEADER_PATH = { ELEM_NAME, "header" };
	private static final String MSG_REPO_CLASS_KEY = "msg-repo-class";
	//~--- fields ---------------------------------------------------------------
	/** Field holds class for formatting and parsing dates in a locale-sensitive
	 * manner */
	private final SimpleDateFormat formatter;
	private String msgRepoCls = null;
	
	{
		this.formatter = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'" );
		this.formatter.setTimeZone( TimeZone.getTimeZone( "UTC" ) );
	}

	//~--- methods --------------------------------------------------------------
	@Override
	public int concurrentQueuesNo() {
		return Runtime.getRuntime().availableProcessors();
	}

	@Override
	public String id() {
		return ID;
	}

	@Override
	public void init(Map<String, Object> settings) throws TigaseDBException {
		super.init(settings);
		msgRepoCls = (String) settings.get(MSG_REPO_CLASS_KEY);
	}
	
	/**
	 * {@inheritDoc}
	 *
	 * <br><br>
	 *
	 * OfflineMessages postprocessor simply calls {@code savePacketForOffLineUser}
	 * method to store packet to offline repository.
	 */
	@Override
	public void postProcess( final Packet packet, final XMPPResourceConnection conn,
													 final NonAuthUserRepository repo, final Queue<Packet> queue,
													 Map<String, Object> settings ) {
		if ( conn == null ){
			try {
				MsgRepositoryIfc msg_repo = getMsgRepoImpl( repo, conn );

				savePacketForOffLineUser( packet, msg_repo );
			} catch ( UserNotFoundException e ) {
				if ( log.isLoggable( Level.FINEST ) ){
					log.finest(
							"UserNotFoundException at trying to save packet for off-line user."
							+ packet );
				}
			}    // end of try-catch
		}      // end of if (conn == null)
	}

	/**
	 * {@inheritDoc}
	 *
	 * <br><br>
	 *
	 * {@code OfflineMessages} processor is triggered by {@code <presence>}
	 * stanza. Upon receiving it plugin tries to load messages from repository
	 * and, if the result is not empty, sends them to the user
	 */
	@Override
	public void process( final Packet packet, final XMPPResourceConnection conn,
											 final NonAuthUserRepository repo, final Queue<Packet> results,
											 final Map<String, Object> settings )
			throws NotAuthorizedException {
		if ( loadOfflineMessages( packet, conn ) ){
			try {
				MsgRepositoryIfc msg_repo = getMsgRepoImpl( repo, conn );
				Queue<Packet> packets = restorePacketForOffLineUser( conn, msg_repo );

				if ( packets != null ){
					if ( log.isLoggable( Level.FINER ) ){
						log.finer( "Sending off-line messages: " + packets.size() );
					}
					results.addAll( packets );
				}    // end of if (packets != null)
			} catch ( UserNotFoundException e ) {
				log.info( "Something wrong, DB problem, cannot load offline messages. " + e );
			}      // end of try-catch
		}
	}

	/**
	 * Method restores all messages from repository for the JID of the current
	 * session. All retrieved elements are then instantiated as {@code Packet}
	 * objects added to {@code LinkedList} collection and, if possible, sorted by
	 * timestamp.
	 *
	 * @param conn user session which keeps all the user session data and also
	 *             gives an access to the user's repository data.
	 * @param repo an implementation of {@link MsgRepositoryIfc} interface
	 *
	 *
	 * @return a {@link Queue} of {@link Packet} objects based on all stored
	 *         payloads for the JID of the current session.
	 *
	 * @throws UserNotFoundException
	 * @throws NotAuthorizedException
	 */
	public Queue<Packet> restorePacketForOffLineUser( XMPPResourceConnection conn,
																										MsgRepositoryIfc repo )
			throws UserNotFoundException, NotAuthorizedException {
		Queue<Element> elems = repo.loadMessagesToJID( conn.getJID(), true );

		if ( elems != null ){
			LinkedList<Packet> pacs = new LinkedList<Packet>();
			Element elem = null;

			while ( ( elem = elems.poll() ) != null ) {
				try {
					pacs.offer( Packet.packetInstance( elem ) );
				} catch ( TigaseStringprepException ex ) {
					log.warning( "Packet addressing problem, stringprep failed: " + elem );
				}
			}    // end of while (elem = elems.poll() != null)
			try {
				Collections.sort( pacs, new StampComparator() );
			} catch ( NullPointerException e ) {
				try {
					log.warning( "Can not sort off line messages: " + pacs + ",\n" + e );
				} catch ( Exception exc ) {
					log.log( Level.WARNING, "Can not print log message.", exc );
				}
			}

			return pacs;
		}

		return null;
	}

	/**
	 * Method stores messages to offline repository with the following rules
	 * applied, i.e. saves only:
	 * <ul>
	 * <li> message stanza with either nonempty {@code <body>}, {@code <event>} or
	 * {@code <header>} child element and only messages of type normal, chat.</li>
	 * <li> presence stanza of type subscribe, subscribed, unsubscribe and
	 * unsubscribed.</li>
	 * </ul>
	 * <br>
	 * Processed messages are stamped with the {@code delay} element and
	 * appropriate timestamp.
	 * <br>
	 *
	 *
	 * @param pac  a {@link Packet} object containing packet that should be
	 *             verified and saved
	 * @param repo a {@link MsgRepositoryIfc} repository handler responsible for
	 *             storing messages
	 *
	 * @return {@code true} if the packet was correctly saved to repository,
	 *         {@code false} otherwise.
	 *
	 * @throws UserNotFoundException
	 */
	public boolean savePacketForOffLineUser( Packet pac, MsgRepositoryIfc repo )
			throws UserNotFoundException {
		StanzaType type = pac.getType();

		// save only:
		// message stanza with either {@code <body>} or {@code <event>} child element and only of type normal, chat
		// presence stanza of type subscribe, subscribed, unsubscribe and unsubscribed
		if ( ( pac.getElemName().equals( "message" )
					 && ( ( pac.getElemCDataStaticStr( tigase.server.Message.MESSAGE_BODY_PATH ) != null )
								|| ( pac.getElemChildrenStaticStr( MESSAGE_EVENT_PATH ) != null )
								|| ( pac.getElemChildrenStaticStr( MESSAGE_HEADER_PATH ) != null ) )
					 && ( ( type == null ) || ( type == StanzaType.normal ) || ( type == StanzaType.chat ) ) )
				 || ( pac.getElemName().equals( "presence" )
							&& ( ( type == StanzaType.subscribe ) || ( type == StanzaType.subscribed )
									 || ( type == StanzaType.unsubscribe ) || ( type == StanzaType.unsubscribed ) ) ) ){
			if ( log.isLoggable( Level.FINEST ) ){
				log.log( Level.FINEST, "Storing packet for offline user: {0}", pac );
			}

			Element elem = pac.getElement().clone();
			String stamp = null;

			synchronized ( formatter ) {
				stamp = formatter.format( new Date() );
			}

			String from = pac.getStanzaTo().getDomain();
			Element x = new Element( "delay", "Offline Storage - " + defHost, new String[] {
				"from",
				"stamp", "xmlns" }, new String[] { from, stamp, "urn:xmpp:delay" } );

			elem.addChild( x );
			repo.storeMessage( pac.getStanzaFrom(), pac.getStanzaTo(), null, elem );
			pac.processedBy( ID );

			return true;
		} else {
			if ( log.isLoggable( Level.FINEST ) ){
				log.log( Level.FINEST, "Packet for offline user not suitable for storing: {0}",
								 pac );
			}
		}

		return false;
	}

	@Override
	public Element[] supDiscoFeatures( final XMPPResourceConnection session ) {
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

	//~--- get methods ----------------------------------------------------------
	/**
	 * Method allows obtaining instance of {@link MsgRepositoryIfc} interface
	 * implementation.
	 *
	 * @param conn user session which keeps all the user session data and also
	 *             gives an access to the user's repository data.
	 * @param repo an implementation of {@link MsgRepositoryIfc} interface
	 *
	 * @return instance of {@link MsgRepositoryIfc} interface implementation.
	 */
	protected MsgRepositoryIfc getMsgRepoImpl( NonAuthUserRepository repo,
																						 XMPPResourceConnection conn ) {
		if (msgRepoCls == null) {
			return new MsgRepositoryImpl( repo, conn );
		} else {
			try {
				OfflineMsgRepositoryIfc msgRepo = (OfflineMsgRepositoryIfc) ModulesManagerImpl.getInstance().forName(msgRepoCls).newInstance();
				msgRepo.init(repo, conn);
				return msgRepo;
			} catch (Exception ex) {
				return null;
			}
		}
	}

	//~--- methods --------------------------------------------------------------
	/**
	 * Method determines whether offline messages should be loaded - the process
	 * should be run only once per user session and only for available/null
	 * presence with priority greater than 0.
	 *
	 *
	 * @param packet a {@link Packet} object containing packet that should be
	 *               verified and saved
	 * @param conn   user session which keeps all the user session data and also
	 *               gives an access to the user's repository data.
	 *
	 * @return {@code true} if the messages should be loaded, {@code false}
	 *         otherwise.
	 */
	protected boolean loadOfflineMessages( Packet packet, XMPPResourceConnection conn ) {

		// If the user session is null or the user is anonymous just
		// ignore it.
		if ( ( conn == null ) || conn.isAnonymous() ){
			return false;
		}    // end of if (session == null)

		// Try to restore the offline messages only once for the user session
		if ( conn.getSessionData( ID ) != null ){
			return false;
		}

		// if we are using XEP-0013: Flexible offline messages retrieval then we skip loading
		if ( conn.getCommonSessionData(FlexibleOfflineMessageRetrieval.FLEXIBLE_OFFLINE_XMLNS) != null ){
			return false;
		}

		StanzaType type = packet.getType();

		if ( ( type == null ) || ( type == StanzaType.available ) ){

			// Should we send off-line messages now?
			// Let's try to do it here and maybe later I find better place.
			String priority_str = packet.getElemCDataStaticStr( tigase.server.Presence.PRESENCE_PRIORITY_PATH );
			int priority = 0;

			if ( priority_str != null ){
				try {
					priority = Integer.decode( priority_str );
				} catch ( NumberFormatException e ) {
					priority = 0;
				}    // end of try-catch
			}      // end of if (priority != null)
			if ( priority >= 0 ){
				conn.putSessionData( ID, ID );

				return true;
			}      // end of if (priority >= 0)
		}        // end of if (type == null || type == StanzaType.available)

		return false;
	}

	public static interface OfflineMsgRepositoryIfc extends MsgRepositoryIfc {
		
		void init( NonAuthUserRepository repo, XMPPResourceConnection conn);
		
	}
	
	//~--- inner classes --------------------------------------------------------
	/**
	 * Implementation of {@code MsgRepositoryIfc} interface providing basic
	 * support for storing and loading of Elements from repository.
	 */
	private class MsgRepositoryImpl implements OfflineMsgRepositoryIfc {

		/** Field holds user session which keeps all the user session data and also
		 * gives an access to the user's repository data. */
		private XMPPResourceConnection conn = null;
		/** Field holds a reference to user session which keeps all the user session
		 * data and also gives an access to the user's repository data. */
		private SimpleParser parser = SingletonFactory.getParserInstance();
		/** Field holds reference to an implementation of {@link MsgRepositoryIfc}
		 * interface */
		private NonAuthUserRepository repo = null;

		//~--- constructors -------------------------------------------------------
		/**
		 * Constructs {@code MsgRepositoryImpl} object referencing user session and
		 * having handle to user repository.
		 *
		 * @param repo an implementation of {@link MsgRepositoryIfc} interface
		 * @param conn user session which keeps all the user session data and also
		 *             gives an access to the user's repository data.
		 */
		private MsgRepositoryImpl(NonAuthUserRepository repo, XMPPResourceConnection conn) {
			init(repo, conn);
		}
		
		@Override
		public void init(NonAuthUserRepository repo, XMPPResourceConnection conn) {
			this.repo = repo;
			this.conn = conn;
		}

		@Override
		public void initRepository(String conn_str, Map<String, String> map) {
			// nothing to do here as we base on UserRepository which is already initialized
		}
		
		//~--- get methods --------------------------------------------------------
		@Override
		public Element getMessageExpired( long time, boolean delete ) {
			throw new UnsupportedOperationException( "Not supported yet." );
		}

		//~--- methods ------------------------------------------------------------
		@Override
		public Queue<Element> loadMessagesToJID( JID to, boolean delete )
				throws UserNotFoundException {
			try {
				DomBuilderHandler domHandler = new DomBuilderHandler();
				String[] msgs = conn.getOfflineDataList( ID, "messages" );

				if ( ( msgs != null ) && ( msgs.length > 0 ) ){
					conn.removeOfflineData( ID, "messages" );

					StringBuilder sb = new StringBuilder();

					for ( String msg : msgs ) {
						sb.append( msg );
					}

					char[] data = sb.toString().toCharArray();

					parser.parse( domHandler, data, 0, data.length );

					return domHandler.getParsedElements();
				}    // end of while (elem = elems.poll() != null)
			} catch ( NotAuthorizedException ex ) {
				log.info( "User not authrized to retrieve offline messages, "
									+ "this happens quite often on some installations where there"
									+ " are a very short living client connections. They can "
									+ "disconnect at any time. " + ex );
			} catch ( TigaseDBException ex ) {
				log.warning( "Error accessing database for offline message: " + ex );
			}

			return null;
		}

		@Override
		public void storeMessage( JID from, JID to, Date expired, Element msg )
				throws UserNotFoundException {
			repo.addOfflineDataList( to.getBareJID(), ID, "messages",
															 new String[] { msg.toString() } );
		}
	}

	/**
	 * {@link Comparator} interface implementation for the purpose of sorting
	 * Elements retrieved from the repository by the timestamp stored in
	 * {@code delay} element.
	 */
	public class StampComparator
			implements Comparator<Packet> {

		@Override
		public int compare( Packet p1, Packet p2 ) {
			String stamp1 = "";
			String stamp2 = "";

			// Try XEP-0203 - the new XEP...
			Element stamp_el1 = p1.getElement().getChild( "delay", "urn:xmpp:delay" );

			if ( stamp_el1 == null ){

				// XEP-0091 support - the old one...
				stamp_el1 = p1.getElement().getChild( "x", "jabber:x:delay" );
			}
			stamp1 = stamp_el1.getAttributeStaticStr( "stamp" );

			// Try XEP-0203 - the new XEP...
			Element stamp_el2 = p2.getElement().getChild( "delay", "urn:xmpp:delay" );

			if ( stamp_el2 == null ){

				// XEP-0091 support - the old one...
				stamp_el2 = p2.getElement().getChild( "x", "jabber:x:delay" );
			}
			stamp2 = stamp_el2.getAttributeStaticStr( "stamp" );

			return stamp1.compareTo( stamp2 );
		}
	}
}    // OfflineMessages
