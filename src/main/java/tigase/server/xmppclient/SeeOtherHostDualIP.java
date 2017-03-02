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
import tigase.cluster.repo.ClusterRepoItem;
import tigase.cluster.repo.ClusterRepoItemEvent;
import tigase.db.DBInitException;
import tigase.db.DataSource;
import tigase.db.DataSourceAware;
import tigase.db.DataSourceHelper;
import tigase.db.beans.MDRepositoryBeanWithStatistics;
import tigase.eventbus.EventBus;
import tigase.eventbus.HandleEvent;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Initializable;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.UnregisterAware;
import tigase.kernel.beans.config.ConfigField;
import tigase.xmpp.BareJID;
import tigase.xmpp.JID;

import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Extended implementation of SeeOtherHost using redirect information from
 * database based on cluster_nodes table.
 *
 */
public class SeeOtherHostDualIP
		extends SeeOtherHostHashed implements Initializable, UnregisterAware {

	private static final Logger log = Logger.getLogger( SeeOtherHostDualIP.class.getName() );

	public static final String SEE_OTHER_HOST_FALLBACK_REDIRECTION_KEY
														 = CM_SEE_OTHER_HOST_CLASS_PROP_KEY + "/" + "fallback-redirection-host";

	public static final String SEE_OTHER_HOST_DATA_SOURCE_KEY
														 = CM_SEE_OTHER_HOST_CLASS_PROP_KEY + "/" + "data-source";
	public static final String SEE_OTHER_HOST_DATA_SOURCE_VALUE
														 = SeeOtherHostDualIPSQLRepository.class.getName();

	public static final String SEE_OTHER_HOST_DB_URL_KEY
														 = CM_SEE_OTHER_HOST_CLASS_PROP_KEY + "/" + "db-url";

	@ConfigField(desc = "Failback host", alias = "failbackHost")
	private BareJID fallback_host = null;

	@Inject
	private EventBus eventBus;

	@Inject(bean = "dualIPRepository")
	private DualIPRepository repo = null;

	private final Map<BareJID, BareJID> redirectsMap = Collections.synchronizedMap(new HashMap());

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

	@HandleEvent
	public void clusterRepoItemEvent( ClusterRepoItemEvent event ) {

		if ( log.isLoggable( Level.FINE ) ){
			log.log( Level.FINE, "Procesing ClusterRepoItemEvent: {0}", new Object[] { event } );
		}

		REPO_ITEM_UPDATE_TYPE action = event.getAction();

		ClusterRepoItem item = event.getItem();

		BareJID hostname;
		String hostnameStr = item.getHostname();
		if ( null != hostnameStr ){
			hostname = BareJID.bareJIDInstanceNS( hostnameStr );
		} else {
			return;
		}

		BareJID secondary = null;
		String secondaryStr = item.getSecondaryHostname();
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

	@Override
	public void initialize() {
		super.initialize();

		eventBus.registerAll(this);

		reloadRedirection();
	}

	@Override
	public void beforeUnregister() {
		eventBus.unregisterAll(this);
	}

	public interface DualIPRepository<T extends DataSource> extends DataSourceAware<T> {

		public static final String HOSTNAME_ID = "hostname";
		public static final String SECONDARY_HOSTNAME_ID = "secondary";

		Map<BareJID, BareJID> queryAllDB() throws SQLException;

	}

	@Bean(name = "dualIPRepository", parent = SeeOtherHostDualIP.class, active = true)
	public static class DualIPRepositoryWrapper extends MDRepositoryBeanWithStatistics<DualIPRepository>
			implements DualIPRepository<DataSource> {

		public DualIPRepositoryWrapper() {
			super(DualIPRepository.class);
		}

		public Map<BareJID, BareJID> queryAllDB() throws SQLException {
			return getRepository("").queryAllDB();
		}

		@Override
		protected Class<? extends DualIPRepository> findClassForDataSource(DataSource dataSource) throws DBInitException {
			return DataSourceHelper.getDefaultClass(DualIPRepository.class, dataSource.getResourceUri());
		}

		@Override
		public void setDataSource(DataSource dataSource) {
			// nothing to do
		}

		@Override
		public Class<?> getDefaultBeanClass() {
			return DualIPRepositoryWrapperConfigBean.class;
		}

		public static class DualIPRepositoryWrapperConfigBean extends MDRepositoryConfigBean<DualIPRepository> {

		}
	}

}
