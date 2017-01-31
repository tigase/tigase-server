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

import tigase.cluster.ClusterConnectionManager.REPO_ITEM_UPDATE_TYPE;

import tigase.db.Repository;
import tigase.db.RepositoryFactory;

import tigase.xmpp.BareJID;
import tigase.xmpp.JID;

import tigase.disteventbus.EventBus;
import tigase.disteventbus.EventBusFactory;
import tigase.disteventbus.EventHandler;
import tigase.osgi.ModulesManagerImpl;
import tigase.util.TigaseStringprepException;
import tigase.xml.Element;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static tigase.cluster.ClusterConnectionManager.EVENTBUS_REPO_ITEM_EVENT_XMLNS;
import static tigase.cluster.ClusterConnectionManager.REPO_ITEM_EVENT_NAME;
import static tigase.server.xmppclient.SeeOtherHostIfc.CM_SEE_OTHER_HOST_CLASS_PROP_KEY;

/**
 * Extended implementation of SeeOtherHost using redirect information from
 * database based on cluster_nodes table.
 *
 */
public class SeeOtherHostDualIP
		extends SeeOtherHostHashed
		implements EventHandler {

	private static final Logger log = Logger.getLogger( SeeOtherHostDualIP.class.getName() );

	public static final String SEE_OTHER_HOST_FALLBACK_REDIRECTION_KEY
														 = CM_SEE_OTHER_HOST_CLASS_PROP_KEY + "/" + "fallback-redirection-host";

	private BareJID fallback_host = null;

	public static final String SEE_OTHER_HOST_DATA_SOURCE_KEY
														 = CM_SEE_OTHER_HOST_CLASS_PROP_KEY + "/" + "data-source";
	public static final String SEE_OTHER_HOST_DATA_SOURCE_VALUE
														 = SeeOtherHostDualIPSQLRepository.class.getName();

	public static final String SEE_OTHER_HOST_DB_URL_KEY
														 = CM_SEE_OTHER_HOST_CLASS_PROP_KEY + "/" + "db-url";

	protected String get_host_DB_url = "";

	private DualIPRepository repo = null;

	private final Map<BareJID, BareJID> redirectsMap = Collections.synchronizedMap(new HashMap());

	private final EventBus eventBus = EventBusFactory.getInstance();

	@Override
	public BareJID findHostForJID( BareJID jid, BareJID host ) {

		// get default host identification
		BareJID see_other_host = super.findHostForJID( jid, host );

		// lookup resolution in redirection map
		BareJID redirection;
		redirection = redirectsMap.get( see_other_host );

		if ( redirection == null ){
			// let's try querying the table again
			reloadRedirection();
			redirection = redirectsMap.get( see_other_host );
		}

		if ( redirection == null && fallback_host != null ){
			// let's use default fallback redirection if present
			redirection = fallback_host;
		}

		return redirection;
	}

	@Override
	public void onEvent( String name, String xmlns, Element event ) {

		Element child = event.getChild( "repo-item" );

		String actionAttr = child.getAttributeStaticStr( "action" );
		String hostnameStr = child.getAttributeStaticStr( "hostname" );
		String secondaryStr = child.getAttributeStaticStr( "secondary" );

		if ( log.isLoggable( Level.FINE ) ){
			log.log( Level.FINE, "Procesing clusterItem event: {0} with action: {1}, hostname: {2}, secondary: {3}",
							 new Object[] { event, actionAttr, hostnameStr, secondaryStr } );
		}

		REPO_ITEM_UPDATE_TYPE action;
		if ( null != actionAttr ){
			action = REPO_ITEM_UPDATE_TYPE.valueOf( actionAttr );
		} else {
			return;
		}

		BareJID hostname;
		if ( null != hostnameStr ){
			hostname = BareJID.bareJIDInstanceNS( hostnameStr );
		} else {
			return;
		}

		BareJID secondary = null;
		if ( null != secondaryStr && !secondaryStr.trim().isEmpty() ){
			secondary = BareJID.bareJIDInstanceNS( secondaryStr );
		}

		BareJID oldItem;
		switch ( action ) {
			case ADDED:
			case UPDATED:
				oldItem = redirectsMap.put( hostname, secondary );
				if ( log.isLoggable( Level.FINE ) ){
					log.log( Level.FINE, "Redirection item :: hostname: {0}, secondary: {1}, added/updated! Replaced: {2}",
									 new Object[] { hostname, secondary, oldItem } );
				}

				break;

			case REMOVED:
				oldItem = redirectsMap.remove( hostname );
				if ( log.isLoggable( Level.FINE ) ){
					log.log( Level.FINE, "Redirection item :: hostname: {0}, {1}",
									 new Object[] { hostname, ( oldItem != null ? "removed" : "was not present in redirection map" ) } );
				}
				break;
		}
	}

	@Override
	public void setNodes( List<JID> connectedNodes ) {
		super.setNodes( connectedNodes );

		reloadRedirection();
	}

	@Override
	public void getDefaults( Map<String, Object> defs, Map<String, Object> params ) {
		super.getDefaults( defs, params );

		if ( params.containsKey( "--user-db-uri" ) ){
			get_host_DB_url = (String) params.get( "--user-db-uri" );
		} else if ( params.containsKey( "--" + SEE_OTHER_HOST_DB_URL_KEY ) ){
			get_host_DB_url = (String) params.get( "--" + SEE_OTHER_HOST_DB_URL_KEY );
		}
		defs.put( SEE_OTHER_HOST_DB_URL_KEY, get_host_DB_url );

		if ( params.containsKey( "--" + SEE_OTHER_HOST_DATA_SOURCE_KEY ) ){
			defs.put( SEE_OTHER_HOST_DATA_SOURCE_KEY, params.get( "--" + SEE_OTHER_HOST_DATA_SOURCE_KEY ) );
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

	}

	@Override
	public void setProperties( Map<String, Object> props ) {
		super.setProperties( props );

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

		if ( ( props.containsKey( SEE_OTHER_HOST_DB_URL_KEY ) )
				 && !props.get( SEE_OTHER_HOST_DB_URL_KEY ).toString().trim().isEmpty() ){
			get_host_DB_url = props.get( SEE_OTHER_HOST_DB_URL_KEY ).toString().trim();
		}
		props.put( SEE_OTHER_HOST_DB_URL_KEY, get_host_DB_url );

		String repo_class = (String) props.get( SEE_OTHER_HOST_DATA_SOURCE_KEY );

		try {
			Class<?> cls = null;
			if ( null == repo_class ){
				cls = RepositoryFactory.getRepoClass( DualIPRepository.class, get_host_DB_url );
			} else if ( "eventbus".equals( repo_class.trim().toLowerCase() ) ){
				if ( log.isLoggable( Level.CONFIG ) ){
					log.log( Level.CONFIG, "Using Evenbus as a source of DualIP data" );
				}
				eventBus.addHandler( REPO_ITEM_EVENT_NAME, EVENTBUS_REPO_ITEM_EVENT_XMLNS, this );
			} else {
				cls = ModulesManagerImpl.getInstance().forName( repo_class );
			}

			if ( null != cls ){

				if ( log.isLoggable( Level.CONFIG ) ){
					log.log( Level.CONFIG, "Using {0} class for DualIP repository", cls );
				}
				DualIPRepository repoTmp = (DualIPRepository) cls.newInstance();

				if ( repo == null ){
					repo = repoTmp;
				}

				repo.setProperties( props );

				repo.initRepository( get_host_DB_url, null );

				reloadRedirection();
			}

		} catch ( Exception ex ) {
			log.log( Level.SEVERE, "Cannot initialize connection to database: ", ex );
		}
	}

	@Override
	public boolean isRedirectionRequired( BareJID defaultHost, BareJID redirectionHost ) {
		return redirectsMap.get( defaultHost ) != null
					 ? !redirectsMap.get( defaultHost ).equals( redirectionHost )
					 : false;
	}

	protected void reloadRedirection() {
		// reload redirections from
		if ( null == repo ){
			return;
		}
		try {
			final Map<BareJID, BareJID> queryAllDB = repo.queryAllDB();
			if ( null != queryAllDB ){
				redirectsMap.clear();
				redirectsMap.putAll( queryAllDB );
				if ( log.isLoggable( Level.FINE ) ){
					log.log( Level.FINE, "Reloaded redirection items: " + Arrays.asList( redirectsMap ) );
				}
			}
		} catch ( Exception ex ) {
			log.log( Level.SEVERE, "Reloading redirection items failed: ", ex );
		}
	}

	public interface DualIPRepository extends Repository {

		public static final String HOSTNAME_ID = "hostname";
		public static final String SECONDARY_HOSTNAME_ID = "secondary";

		Map<BareJID, BareJID> queryAllDB() throws SQLException;

		void setProperties( Map<String, Object> props );

		void getDefaults( Map<String, Object> defs, Map<String, Object> params );

	}

}
