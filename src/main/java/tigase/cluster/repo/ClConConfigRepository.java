/*
 * ClConConfigRepository.java
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



package tigase.cluster.repo;

//~--- non-JDK imports --------------------------------------------------------

import tigase.db.DBInitException;
import tigase.db.comp.ConfigRepository;

import tigase.disteventbus.EventBusFactory;
import tigase.sys.ShutdownHook;
import tigase.sys.TigaseRuntime;
import tigase.util.DNSResolverFactory;
import tigase.xml.Element;

import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import static tigase.cluster.ClusterConnectionManager.CLUSTER_INITIATED_EVENT;

/**
 * Class description
 *
 *
 * @version        5.2.0, 13/03/09
 * @author         <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 */
public class ClConConfigRepository
		extends ConfigRepository<ClusterRepoItem>
		implements ShutdownHook {

	private static final Logger log = Logger.getLogger(ClConConfigRepository.class.getName());

	public static final String AUTORELOAD_INTERVAL_PROP_KEY = "repo-autoreload-interval";
	public static final String AUTO_REMOVE_OBSOLETE_ITEMS_PROP_KEY = "repo-auto-remove-obsolete-items";
	public static final long AUTORELOAD_INTERVAL_PROP_VAL = 15;

	protected long autoreload_interval = AUTORELOAD_INTERVAL_PROP_VAL;
	protected boolean auto_remove_obsolete_items = true;
	protected long lastReloadTime = 0;
	protected long lastReloadTimeFactor = 10;

    protected boolean firstLoadDone = false;
	@Override
	public void destroy() {
		// Nothing to do
	}
	
	//~--- get methods ----------------------------------------------------------

	@Override
	public String[] getDefaultPropetyItems() {
		return ClConRepoDefaults.getDefaultPropetyItems();
	}

	@Override
	public String getName() {
		return "Cluster repository clean-up";
	}

	@Override
	public String getPropertyKey() {
		return ClConRepoDefaults.getPropertyKey();
	}

	@Override
	public String getConfigKey() {
		return ClConRepoDefaults.getConfigKey();
	}

	@Override
	public ClusterRepoItem getItemInstance() {
		return ClConRepoDefaults.getItemInstance();
	}

	//~--- methods --------------------------------------------------------------

	@Override
	public void initRepository(String resource_uri, Map<String, String> params) throws DBInitException {
		// Nothing to do
	}

	@Override
	public void reload() {
		super.reload();

        String host = DNSResolverFactory.getInstance().getDefaultHost();

        // we check if we already realoded repo from repository and have all items (own item will have
        // correct update time), if so we set flag that first load was made and if there was only one item
        // we send even that cluster was initiated
        if (!firstLoadDone) {
            if (log.isLoggable(Level.FINEST)) {
                log.log(Level.FINEST, "First Cluster repository reload done: {0}, items size: {1}, last updated own item: {2}",
                        new Object[]{firstLoadDone, items.size(), items.get(host).getLastUpdate()});
            }

            if (items.get(host) != null && items.get(host).getLastUpdate() > 0 ) {
                firstLoadDone = true;

                if (items.size() == 1) {
                    Element event = new Element(CLUSTER_INITIATED_EVENT);
                    event.setXMLNS(CLUSTER_INITIATED_EVENT);
                    event.setAttribute( "local", "true" );
                    EventBusFactory.getInstance().fire(event);
                }

            }
        }

		ClusterRepoItem item = getItem(host);
		try {
			item = ( item != null ) ? (ClusterRepoItem)(item.clone()) : null;
		} catch ( CloneNotSupportedException ex ) {
			if ( log.isLoggable( Level.FINEST ) ){
				log.log( Level.SEVERE, "Cloning of ClusterRepoItem has failed", ex );
			}
		}

		if (item == null) {
			item = getItemInstance();
			item.setHostname(host);
		}
		item.setSecondaryHostname( DNSResolverFactory.getInstance().getSecondaryHost() );
		item.setLastUpdate(System.currentTimeMillis());
		item.setCpuUsage(TigaseRuntime.getTigaseRuntime().getCPUUsage());
		item.setMemUsage(TigaseRuntime.getTigaseRuntime().getHeapMemUsage());
		storeItem(item);


		if (auto_remove_obsolete_items) {
			Iterator<ClusterRepoItem> iterator = iterator();
			while(iterator.hasNext()) {
				ClusterRepoItem next = iterator.next();
				if ( ( next.getLastUpdate() > 0 ) && System.currentTimeMillis() - next.getLastUpdate() > 5000 * autoreload_interval ){
					removeItem( next.getHostname() );
				}
			}
		}

	}

	public void itemLoaded(ClusterRepoItem item) {
		if ( log.isLoggable( Level.FINEST ) ){
			log.log( Level.FINEST, "Item loaded: {0}", item );
		}
		if (System.currentTimeMillis() - item.getLastUpdate() <= 5000 * autoreload_interval && clusterRecordValid(item)) {
			addItem(item);
		} else {
			if ( log.isLoggable( Level.FINEST ) ){
				log.log( Level.FINEST,
								 "Removing stale item: {0}; current time: {1}, last update: {2} ({3}), diff: {4}, autoreload {5}",
								 new Object[] { item, System.currentTimeMillis(), item.getLastUpdate(),
																new Date( item.getLastUpdate() ), System.currentTimeMillis() - item.getLastUpdate(),
																5000 * autoreload_interval } );
			}
			if ( auto_remove_obsolete_items ){
				removeItem( item.getHostname() );
			}
		}
	}

	@Override
	public boolean itemChanged(ClusterRepoItem oldItem, ClusterRepoItem newItem) {
		return !oldItem.getPassword().equals( newItem.getPassword() )
					 || ( oldItem.getPortNo() != newItem.getPortNo() )
					 || !Objects.equals( oldItem.getSecondaryHostname(), newItem.getSecondaryHostname() );
	}

	//~--- get methods ----------------------------------------------------------

	@Override
	public void getDefaults(Map<String, Object> defs, Map<String, Object> params) {
		super.getDefaults(defs, params);
		defs.put(AUTORELOAD_INTERVAL_PROP_KEY, AUTORELOAD_INTERVAL_PROP_VAL);
		defs.put(AUTO_REMOVE_OBSOLETE_ITEMS_PROP_KEY, true);

		String[] items_arr = (String[]) defs.get(getConfigKey());

		for (String it : items_arr) {
			ClusterRepoItem item = getItemInstance();

			item.initFromPropertyString(it);
			addItem(item);
		}
		if (getItem(DNSResolverFactory.getInstance().getDefaultHost()) == null) {
			ClusterRepoItem item = getItemInstance();

			item.initFromPropertyString(DNSResolverFactory.getInstance().getDefaultHost());
			addItem(item);
		}
	}

	//~--- set methods ----------------------------------------------------------

	@Override
	public void setProperties(Map<String, Object> props) {
		super.setProperties(props);
		autoreload_interval = (Long) props.get(AUTORELOAD_INTERVAL_PROP_KEY);
		auto_remove_obsolete_items = (boolean) props.get(AUTO_REMOVE_OBSOLETE_ITEMS_PROP_KEY);

		setAutoloadTimer(autoreload_interval);
		TigaseRuntime.getTigaseRuntime().addShutdownHook(this);
	}

	@Override
	public String shutdown() {
		String host = DNSResolverFactory.getInstance().getDefaultHost();
		removeItem( host );
		return "== " + "Removing cluster_nodes item: " + host + "\n";
	}

	//~--- methods --------------------------------------------------------------

	public void storeItem(ClusterRepoItem item) {}

	private boolean clusterRecordValid( ClusterRepoItem item ) {

		// we ignore faulty addresses
		boolean isCorrect = !item.getHostname().equalsIgnoreCase( "localhost" );

		if ( !isCorrect && log.isLoggable( Level.FINE ) ){
			log.log( Level.FINE, "Incorrect entry in cluster table, skipping: {0}", item );
		}
		return isCorrect;
	}
}
