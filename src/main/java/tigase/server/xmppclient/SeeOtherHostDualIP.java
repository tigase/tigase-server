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

import tigase.db.Repository;

import tigase.xmpp.BareJID;
import tigase.xmpp.JID;

import tigase.osgi.ModulesManagerImpl;
import tigase.util.TigaseStringprepException;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import static tigase.server.xmppclient.SeeOtherHostIfc.CM_SEE_OTHER_HOST_CLASS_PROP_KEY;

/**
 * Extended implementation of SeeOtherHost using redirect information from
 * database based on cluster_nodes table.
 *
 */
public class SeeOtherHostDualIP extends SeeOtherHostHashed {

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

	private final Map<BareJID, BareJID> redirectsMap = new ConcurrentSkipListMap<BareJID, BareJID>();

	@Override
	public BareJID findHostForJID( BareJID jid, BareJID host ) {

		// get default host identification
		BareJID see_other_host = super.findHostForJID( jid, host );

		// lookup resolutio nin redirection map
		BareJID redirection = redirectsMap.get( see_other_host );

		if ( redirection == null ){
			// let's try querying the table again
			reloadRedirection();
		}

		if ( redirection == null && fallback_host != null ){
			// let's use default fallback redirection if present
			redirection = fallback_host;
		}

		return redirection;
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
		} else {
			defs.put( SEE_OTHER_HOST_DATA_SOURCE_KEY, SEE_OTHER_HOST_DATA_SOURCE_VALUE );
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

		String repo_class = (String) props.getOrDefault( SEE_OTHER_HOST_DATA_SOURCE_KEY, SEE_OTHER_HOST_DATA_SOURCE_VALUE );

		try {
			Class<?> cls = null;
			if ( repo_class != null && "eventbus".equals( repo_class.trim().toLowerCase() ) ){
			} else {
				cls = ModulesManagerImpl.getInstance().forName( repo_class );
			}

			if ( null != cls ){

				DualIPRepository repoTmp = (DualIPRepository) cls.newInstance();

				if ( repo == null ){
					repo = repoTmp;
				}

				repo.setProperties( props );

				repo.initRepository( get_host_DB_url, null );

				reloadRedirection();
			} else {

				// we are going to use eventbus!!!!
//				register handler
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
				log.log( Level.ALL, "Reloaded redirection items: " + Arrays.asList( redirectsMap ) );
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
