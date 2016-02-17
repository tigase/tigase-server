/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2016 "Tigase, Inc." <office@tigase.com>
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
 */
package tigase.server.xmppclient;

import tigase.db.DBInitException;
import tigase.db.DataRepository;
import tigase.db.RepositoryFactory;
import tigase.db.UserNotFoundException;

import tigase.xmpp.BareJID;
import tigase.xmpp.JID;

import tigase.util.TigaseStringprepException;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Extended implementation of SeeOtherHost using redirect information from
 * database based on cluster_nodes table.
 *
 */
public class SeeOtherHostDualIP extends SeeOtherHostHashed {

	private static final Logger log = Logger.getLogger( SeeOtherHostDualIP.class.getName() );

	public static final String CLUSTER_NODES_TABLE = "cluster_nodes";
	public static final String SEE_OTHER_HOST_FALLBACK_REDIRECTION_KEY
														 = CM_SEE_OTHER_HOST_CLASS_PROP_KEY + "/" + "fallback-redirection-host";
	public static final String SEE_OTHER_HOST_DB_URL_KEY
														 = CM_SEE_OTHER_HOST_CLASS_PROP_KEY + "/" + "db-url";
	public static final String SEE_OTHER_HOST_DB_QUERY_KEY
														 = CM_SEE_OTHER_HOST_CLASS_PROP_KEY + "/" + "get-host-query";
	public static final String DB_GET_ALL_DATA_DB_QUERY_KEY
														 = CM_SEE_OTHER_HOST_CLASS_PROP_KEY + "/" + "get-all-data-query";
	public static final String GET_ALL_QUERY_TIMEOUT_QUERY_KEY
														 = CM_SEE_OTHER_HOST_CLASS_PROP_KEY + "/" + "get-all-query-timeout";

	public static final String HOSTNAME_ID = "hostname";
	public static final String SECONDARY_HOSTNAME_ID = "secondary";

	public static final String DEF_DB_GET_HOST_QUERY = " select * from " + CLUSTER_NODES_TABLE
																										 + " where " + CLUSTER_NODES_TABLE + "." + HOSTNAME_ID + " = ?";

	private static final String DEF_DB_GET_ALL_DATA_QUERY = "select * from " + CLUSTER_NODES_TABLE;

	private static final int DEF_QUERY_TIME_OUT = 10;

	private String get_host_query = DEF_DB_GET_HOST_QUERY;
	private String get_all_data_query = DEF_DB_GET_ALL_DATA_QUERY;
	private String get_host_DB_url = "";
	private BareJID fallback_host = null;

	private int query_timeout = DEF_QUERY_TIME_OUT;

	private DataRepository data_repo = null;
	private final Map<BareJID, BareJID> redirectsMap = new ConcurrentSkipListMap<BareJID, BareJID>();

	@Override
	public BareJID findHostForJID( BareJID jid, BareJID host ) {

		// get default host identification
		BareJID see_other_host = super.findHostForJID( jid, host );

		// lookup resolutio nin redirection map
		BareJID redirection = redirectsMap.get( see_other_host );

		if ( redirection == null ){
			try {
				// let's try querying the table again
				queryDB( see_other_host );
			} catch ( SQLException | TigaseStringprepException ex ) {
				log.log( Level.SEVERE, "Redirection DB lookup failed", ex );
			}
		}

		if ( redirection == null && fallback_host != null ){
			// let's use default fallback redirection if present
			redirection = fallback_host;
		}

		return redirection;
	}

	@Override
	public void getDefaults( Map<String, Object> defs, Map<String, Object> params ) {
		super.getDefaults( defs, params );

		if ( params.containsKey( "--user-db-uri" ) ){
			get_host_DB_url = (String) params.get( "--user-db-uri" );
		} else if ( params.containsKey( "--" + SEE_OTHER_HOST_DB_URL_KEY ) ){
			get_host_DB_url = (String) params.get( "--" + SEE_OTHER_HOST_DB_URL_KEY );
		}

		if ( params.containsKey( "--" + SEE_OTHER_HOST_DB_QUERY_KEY ) ){
			get_host_query = (String) params.get( "--" + SEE_OTHER_HOST_DB_QUERY_KEY );
		}

		if ( params.containsKey( "--" + DB_GET_ALL_DATA_DB_QUERY_KEY ) ){
			get_all_data_query = (String) params.get( "--" + DB_GET_ALL_DATA_DB_QUERY_KEY );
		}

		if ( params.containsKey( "--" + GET_ALL_QUERY_TIMEOUT_QUERY_KEY ) ){
			query_timeout = Integer.parseInt( (String) params.get( "--" + GET_ALL_QUERY_TIMEOUT_QUERY_KEY ) );
		}

		if ( params.containsKey( "--" + SEE_OTHER_HOST_FALLBACK_REDIRECTION_KEY ) ){
			BareJID bareJIDInstance;
			try {
				bareJIDInstance = BareJID.bareJIDInstance( (String) params.get( "--" + SEE_OTHER_HOST_FALLBACK_REDIRECTION_KEY ) );
				fallback_host = bareJIDInstance;
				defs.put( SEE_OTHER_HOST_FALLBACK_REDIRECTION_KEY, fallback_host );
			} catch ( TigaseStringprepException ex ) {
				log.log( Level.SEVERE, "Problem creating default redirection JID", ex );
			}
		}

		defs.put( SEE_OTHER_HOST_DB_URL_KEY, get_host_DB_url );
		defs.put( SEE_OTHER_HOST_DB_QUERY_KEY, get_host_query );
		defs.put( DB_GET_ALL_DATA_DB_QUERY_KEY, get_all_data_query );
		defs.put( GET_ALL_QUERY_TIMEOUT_QUERY_KEY, query_timeout );
	}

	@Override
	public void setNodes( List<JID> connectedNodes ) {
		super.setNodes( connectedNodes );

		// reload redirections from
		try {
			queryAllDB();
		} catch ( Exception ex ) {
			log.log( Level.SEVERE, "Reloading redirection items failed: ", ex );
		}

	}

	@Override
	public void setProperties( Map<String, Object> props ) {
		super.setProperties( props );

		if ( ( props.containsKey( SEE_OTHER_HOST_DB_URL_KEY ) )
				 && !props.get( SEE_OTHER_HOST_DB_URL_KEY ).toString().trim().isEmpty() ){
			get_host_DB_url = props.get( SEE_OTHER_HOST_DB_URL_KEY ).toString().trim();
		}
		props.put( SEE_OTHER_HOST_DB_URL_KEY, get_host_DB_url );

		if ( ( props.containsKey( SEE_OTHER_HOST_DB_QUERY_KEY ) )
				 && !props.get( SEE_OTHER_HOST_DB_QUERY_KEY ).toString().trim().isEmpty() ){
			get_host_query = props.get( SEE_OTHER_HOST_DB_QUERY_KEY ).toString().trim();
		}
		props.put( SEE_OTHER_HOST_DB_QUERY_KEY, get_host_query );

		if ( ( props.containsKey( DB_GET_ALL_DATA_DB_QUERY_KEY ) )
				 && !props.get( DB_GET_ALL_DATA_DB_QUERY_KEY ).toString().trim().isEmpty() ){
			get_all_data_query = props.get( DB_GET_ALL_DATA_DB_QUERY_KEY ).toString().trim();
		}
		props.put( DB_GET_ALL_DATA_DB_QUERY_KEY, get_all_data_query );

		if ( ( props.containsKey( GET_ALL_QUERY_TIMEOUT_QUERY_KEY ) )
				 && !props.get( GET_ALL_QUERY_TIMEOUT_QUERY_KEY ).toString().trim().isEmpty() ){
			query_timeout = Integer.parseInt( props.get( GET_ALL_QUERY_TIMEOUT_QUERY_KEY ).toString().trim() );
		}
		props.put( GET_ALL_QUERY_TIMEOUT_QUERY_KEY, query_timeout );

		if ( ( props.containsKey( SEE_OTHER_HOST_FALLBACK_REDIRECTION_KEY ) )
				 && !props.get( SEE_OTHER_HOST_FALLBACK_REDIRECTION_KEY ).toString().trim().isEmpty() ){

			BareJID bareJIDInstance;
			try {
				bareJIDInstance = BareJID.bareJIDInstance( (String) props.get( SEE_OTHER_HOST_FALLBACK_REDIRECTION_KEY ).toString().trim() );
				fallback_host = bareJIDInstance;
				props.put( SEE_OTHER_HOST_FALLBACK_REDIRECTION_KEY, fallback_host );
			} catch ( TigaseStringprepException ex ) {
				log.log( Level.SEVERE, "Problem creating default redirection JID", ex );
			}
		}

		try {
			initRepository( get_host_DB_url, null );
		} catch ( Exception ex ) {
			log.log( Level.SEVERE, "Cannot initialize connection to database: ", ex );
		}
	}

	public void initRepository( String conn_str, Map<String, String> map )
			throws SQLException, ClassNotFoundException, IllegalAccessException,
						 InstantiationException, DBInitException {

		log.log( Level.INFO, "Initializing dbAccess for db connection url: {0}", conn_str );
		data_repo = RepositoryFactory.getDataRepository( null, conn_str, map );

		checkDB();

		data_repo.initPreparedStatement( get_host_query, get_host_query );
		data_repo.initPreparedStatement( get_all_data_query, get_all_data_query );
		queryAllDB();
	}

	/**
	 * Performs database check
	 *
	 * @throws SQLException
	 */
	private void checkDB() throws SQLException {
		if ( !data_repo.checkTable( CLUSTER_NODES_TABLE ) ){
			throw new SQLException( "Nodes redirection table doesn't exits!" );
		}
	}

	private BareJID queryDB( BareJID hostname_jid ) throws SQLException,
																												 TigaseStringprepException {

		PreparedStatement get_host = data_repo.getPreparedStatement( hostname_jid, get_host_query );

		ResultSet rs = null;

		synchronized ( get_host ) {
			try {
				get_host.setString( 1, hostname_jid.toString() );

				rs = get_host.executeQuery();

				if ( rs.next() ){
					final BareJID secondary_jid = BareJID.bareJIDInstance( rs.getString( SECONDARY_HOSTNAME_ID ) );
					redirectsMap.put( hostname_jid, secondary_jid );
					return secondary_jid;
				} else {
					return null;
				}
			} finally {
				data_repo.release( null, rs );
			}
		}
	}

	private void queryAllDB() throws SQLException {

		if ( data_repo == null ){
			return;
		}

		PreparedStatement get_all = data_repo.getPreparedStatement( null, get_all_data_query );
		get_all.setQueryTimeout( DEF_QUERY_TIME_OUT );

		ResultSet rs = null;

		synchronized ( get_all ) {
			try {
				rs = get_all.executeQuery();

				while ( rs.next() ) {
					String user_jid = rs.getString( HOSTNAME_ID );
					String node_jid = rs.getString( SECONDARY_HOSTNAME_ID );
					try {
						BareJID hostname_hid = BareJID.bareJIDInstance( user_jid );
						BareJID secondary = BareJID.bareJIDInstance( node_jid );
						redirectsMap.put( hostname_hid, secondary );
					} catch ( TigaseStringprepException ex ) {
						log.warning( "Invalid host or secondary hostname JID: " + user_jid + ", " + node_jid );
					}
				}
			} finally {
				data_repo.release( null, rs );
			}
		}

		log.info( "Loaded " + redirectsMap.size() + " redirect definitions from database." );
	}

	@Override
	public boolean isRedirectionRequired( BareJID defaultHost, BareJID redirectionHost ) {
		return redirectsMap.get( defaultHost ) != null
					 ? !redirectsMap.get( defaultHost ).equals( redirectionHost )
					 : false;
	}

}
