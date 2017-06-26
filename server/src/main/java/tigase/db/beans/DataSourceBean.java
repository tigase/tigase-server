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

import tigase.component.exceptions.RepositoryException;
import tigase.db.DBInitException;
import tigase.db.DataSource;
import tigase.db.DataSourceHelper;
import tigase.db.DataSourcePool;
import tigase.eventbus.EventBus;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.UnregisterAware;
import tigase.kernel.beans.config.ConfigField;
import tigase.kernel.beans.selector.ConfigType;
import tigase.kernel.beans.selector.ConfigTypeEnum;
import tigase.kernel.core.Kernel;
import tigase.server.BasicComponent;
import tigase.stats.ComponentStatisticsProvider;
import tigase.stats.StatisticsCollector;
import tigase.stats.StatisticsList;
import tigase.stats.StatisticsProviderIfc;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static tigase.db.beans.DataSourceBean.DataSourceMDConfigBean;

/**
 * This is main bean responsible for managing and initialization of data sources.
 * Created by andrzej on 09.03.2016.
 */
@Bean(name="dataSource", parent = Kernel.class, active = true, exportable = true)
@ConfigType({ConfigTypeEnum.DefaultMode, ConfigTypeEnum.SessionManagerMode, ConfigTypeEnum.ConnectionManagersMode,
			 ConfigTypeEnum.ComponentMode})
public class DataSourceBean extends MDPoolBean<DataSource, DataSourceMDConfigBean> implements
																				   ComponentStatisticsProvider {

	private static final Logger log = Logger.getLogger(DataSourceBean.class.getCanonicalName());

	private final Map<String, DataSource> repositories = new ConcurrentHashMap<>();

	private ScheduledExecutorService executorService = null;
	private int watchdogs = 0;

	@Inject
	private EventBus eventBus;

	/**
	 * Retrieves data source for provided name
	 * @param name of data source to retrieve
	 * @return instance of data source for name or default instance of data source
	 */
	public DataSource getRepository(String name) {
		if (name == null)
			return repositories.get("default");
		return repositories.get(name);
	}

	@Override
	public Class<? extends DataSourceMDConfigBean> getConfigClass() {
		return DataSourceMDConfigBean.class;
	}

	/**
	 * Add data source instance to the pool
	 * @param domain name of data source
	 * @param repo instance of data source
	 */
	@Override
	public void addRepo(String domain, DataSource repo) {
		DataSource oldRepo = this.repositories.put(domain, repo);
		fire(new DataSourceChangedEvent(this, domain, repo, oldRepo));
	}

	/**
	 * Remove data source from the pool
	 * @param domain name of data source
	 * @return removed instance of data source
	 */
	@Override
	public DataSource removeRepo(String domain) {
		DataSource oldRepo = repositories.remove(domain);
		fire(new DataSourceChangedEvent(this, domain, null, oldRepo));
		return oldRepo;
	}

	/**
	 * Retrieve list of all available data source names
	 * @return list of names
	 */
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

	protected ScheduledFuture addWatchdogTask(Runnable task, Duration frequency) {
		synchronized (this) {
			if (executorService == null) {
				executorService = Executors.newSingleThreadScheduledExecutor();
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "created watchdog executor");
				}
			}
			watchdogs++;
			return executorService.scheduleAtFixedRate(task, frequency.toMillis(), frequency.toMillis(), TimeUnit.MILLISECONDS);
		}
	}

	protected void removeWatchdogTask(ScheduledFuture scheduledFuture) {
		synchronized (this) {
			scheduledFuture.cancel(true);
			watchdogs--;
			if (watchdogs == 0) {
				if (log.isLoggable(Level.FINEST)) {
					log.log(Level.FINEST, "destroying watchdog executor");
				}
				executorService.shutdown();
				executorService = null;
			}
		}
	}

	public static class DataSourceMDConfigBean
			extends MDPoolConfigBean<DataSource, DataSourceMDConfigBean>
			implements UnregisterAware {

		private ScheduledFuture future = null;

		@ConfigField(desc = "Watchdog data source frequency", alias = "watchdog-frequency")
		private Duration watchdogFrequency = Duration.ofHours(1);

		/**
		 * Get interface to which all instances in this pool must conform.
		 * @return interface 
		 */
		@Override
		protected Class<? extends DataSource> getRepositoryIfc() {
			return DataSource.class;
		}

		/**
		 * Finds and retrieves repository pool class name for data source defined in this config bean.
		 * <br/>
		 * Name of a pool class will be retrieved from <code>poolCls</code> field or looked for
		 * instances of <code>DataSourcePool</code> class annotated with {@link tigase.db.Repository.Meta}
		 * which supported uri matches (regexp) of data source URI.
		 *
		 * @return name of a class
		 * @throws DBInitException
		 */
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

		public void setWatchdogFrequency(Duration watchdogFrequency) {
			this.watchdogFrequency = watchdogFrequency;
			updateWatchdogTask();
		}

		@Override
		public void initialize() {
			super.initialize();
			updateWatchdogTask();
		}

		/**
		 * Initializes instances of provided data source.
		 * @param repo instance of data source
		 * @throws RepositoryException
		 */
		@Override
		protected void initRepository(DataSource repo) throws RepositoryException {
			repo.initialize(getUri());
		}

		@Override
		public void beforeUnregister() {
			if (future != null) {
				if (mdPool != null) {
					if (log.isLoggable(Level.FINEST)) {
						log.log(Level.FINEST, "unregistering watchdog for data source {0}", name);
					}
					((DataSourceBean) mdPool).removeWatchdogTask(future);
				}
			}
		}

		private void executeWatchdog() {
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "execution of watchdog for data source {0}", name);
			}
			this.getRepository().checkConnectivity(watchdogFrequency);
		}

		private void updateWatchdogTask() {
			if (mdPool != null) {
				DataSourceBean dataSourceBean = (DataSourceBean) mdPool;
				if (future != null) {
					if (log.isLoggable(Level.FINEST)) {
						log.log(Level.FINEST, "unregistering watchdog for data source {0}", name);
					}
					dataSourceBean.removeWatchdogTask(future);
				}

				if (!watchdogFrequency.isZero()) {
					if (log.isLoggable(Level.FINEST)) {
						log.log(Level.FINEST, "registering watchdog for data source {0} with frequency {1}",
								new Object[]{name, watchdogFrequency});
					}
					dataSourceBean.addWatchdogTask(this::executeWatchdog, watchdogFrequency);
				}
			}
		}
	}

	/**
	 * Event emitted by {@link tigase.db.beans.DataSourceBean} using {@link tigase.eventbus.EventBus}
	 * when instance of {@link tigase.db.DataSource} for some name changes.
	 */
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

		/**
		 * Check if event was emitted by provided instance of <code>DataSourceBean</code>
		 * @param bean
		 * @return
		 */
		public boolean isCorrectSender(DataSourceBean bean) {
			return this.bean == bean;
		}

		/**
		 * Get name for which data source instance was changed
		 * @return
		 */
		public String getDomain() {
			return domain;
		}

		/**
		 * Get old instance of data source
		 * @return
		 */
		public DataSource getOldDataSource() {
			return oldDataSource;
		}

		/**
		 * Get new instance of data source
		 * @return
		 */
		public DataSource getNewDataSource() {
			return newDataSource;
		}

	}
}
