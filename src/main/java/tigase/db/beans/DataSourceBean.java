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
 * $Rev$
 * Last modified by $Author$
 * $Date$
 */
package tigase.db.beans;

import tigase.db.DBInitException;
import tigase.db.DataSource;
import tigase.db.DataSourceHelper;
import tigase.db.DataSourcePool;
import tigase.eventbus.EventBus;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;
import tigase.kernel.core.Kernel;
import tigase.server.BasicComponent;
import tigase.stats.*;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import static tigase.db.beans.DataSourceBean.DataSourceMDConfigBean;

/**
 * Created by andrzej on 09.03.2016.
 */
@Bean(name="dataSource", parent = Kernel.class, exportable = true)
public class DataSourceBean extends MDPoolBean<DataSource, DataSourceMDConfigBean> implements
																				   ComponentStatisticsProvider {

	private final Map<String, DataSource> repositories = new ConcurrentHashMap<>();

	@Inject
	private EventBus eventBus;

	public DataSource getRepository(String name) {
		if (name == null)
			return repositories.get("default");
		return repositories.get(name);
	}

	@Override
	public Class<? extends DataSourceMDConfigBean> getConfigClass() {
		return DataSourceMDConfigBean.class;
	}

	@Override
	public void addRepo(String domain, DataSource repo) {
		DataSource oldRepo = this.repositories.put(domain, repo);
		fire(new DataSourceChangedEvent(this, domain, repo, oldRepo));
	}

	@Override
	public DataSource removeRepo(String domain) {
		DataSource oldRepo = repositories.remove(domain);
		fire(new DataSourceChangedEvent(this, domain, null, oldRepo));
		return oldRepo;
	}

	public Set<String> getDataSourceNames() {
		return Collections.unmodifiableSet(repositories.keySet());
	}

	@Override
	public void setDefault(DataSource repo) {
		// here we do nothing
	}

	@Override
	public boolean belongsTo(Class<? extends BasicComponent> component) {
		return StatisticsCollector.class.isAssignableFrom(component);
	}

	@Override
	public void everyHour() {

	}

	@Override
	public void everyMinute() {

	}

	@Override
	public void everySecond() {

	}

	@Override
	public void getStatistics(String compName, StatisticsList list) {
		String name = getName();
		list.add(name, "Number of data sources", repositories.size(), Level.FINE);
		repositories.entrySet()
				.stream()
				.filter(e -> e.getValue() instanceof StatisticsProviderIfc)
				.forEach(e -> ((StatisticsProviderIfc) e.getValue()).getStatistics(name + "/" + e.getKey(), list));
	}

	private void fire(Object event) {
		if (eventBus != null)
			eventBus.fire(event);
	}

	@Override
	public Class<?> getDefaultBeanClass() {
		return DataSourceMDConfigBean.class;
	}

	public static class DataSourceMDConfigBean extends MDPoolConfigBean<DataSource, DataSourceMDConfigBean> {

		@Override
		protected Class<? extends DataSource> getRepositoryIfc() {
			return DataSource.class;
		}

		@Override
		protected String getRepositoryPoolClassName() throws DBInitException {
			if (poolCls != null)
				return poolCls;

			Class<? extends DataSourcePool> poolClass = null;
			try {
				poolClass = DataSourceHelper.getDefaultClass(DataSourcePool.class, uri);
			} catch (DBInitException ex) {
				// ok, no problem - it maybe a data source without a pool
			}
			return poolClass == null ? null : poolClass.getCanonicalName();
		}

	}

	public static class DataSourceChangedEvent {

		private final DataSourceBean bean;
		private final String domain;
		private final DataSource newDataSource;
		private final DataSource oldDataSource;

		public DataSourceChangedEvent(DataSourceBean bean, String domain, DataSource newDataSource, DataSource oldDataSource) {
			this.bean = bean;
			this.domain = domain;
			this.newDataSource = newDataSource;
			this.oldDataSource = oldDataSource;
		}

		public boolean isCorrectSender(DataSourceBean bean) {
			return this.bean == bean;
		}

		public String getDomain() {
			return domain;
		}

		public DataSource getOldDataSource() {
			return oldDataSource;
		}

		public DataSource getNewDataSource() {
			return newDataSource;
		}

	}
}
