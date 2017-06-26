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
import tigase.db.DataSourceAware;
import tigase.eventbus.EventBus;
import tigase.eventbus.HandleEvent;
import tigase.kernel.beans.*;
import tigase.kernel.beans.config.ConfigField;
import tigase.kernel.beans.config.ConfigurationChangedAware;
import tigase.kernel.core.Kernel;
import tigase.osgi.ModulesManagerImpl;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static tigase.db.beans.MDPoolBean.REPO_CLASS;

/**
 * Abstract class implementing bean to which should be used to create name aware repository pool.
 * This class is responsible for creation of correct repository instances for every DataSource configured.
 *
 * Created by andrzej on 15.03.2016.
 */
public abstract class MDRepositoryBean<T extends DataSourceAware> implements Initializable, UnregisterAware, RegistrarBeanWithDefaultBeanClass {

	private static final Logger log = Logger.getLogger(MDRepositoryBean.class.getCanonicalName());

	@Inject(nullAllowed = true)
	private MDRepositoryConfigBean[] configBeans;

	@Inject
	private DataSourceBean dataSourceBean;

	@Inject
	private EventBus eventBus;

	@ConfigField(desc = "Map of aliases for data sources to use")
	protected ConcurrentHashMap<String, String> aliases = new ConcurrentHashMap<>();

	@ConfigField(desc = "Bean name")
	private String name;

	@ConfigField(desc = "Create repositories for: every UserRepository, every data source, listed data sources")
	protected SelectorType dataSourceSelection = SelectorType.List;
	
	private final Map<String,T> repositories = new ConcurrentHashMap<>();
	private Kernel kernel;

	/**
	 * Method returns class implementing repository which supports data source instance provided in parameter.
	 * @param dataSource
	 * @return repository class
	 * @throws DBInitException
	 */
	protected abstract Class<? extends T> findClassForDataSource(DataSource dataSource) throws DBInitException;

	/**
	 * Provides access to all available repository instances
	 * @return stream of repository instances
	 */
	protected Stream<T> repositoriesStream() {
		return getRepositories().values().stream();
	}

	/**
	 * Returns name of a bean
	 * @return name of a bean
	 */
	public String getName() {
		return name;
	}

	/**
	 * Provides access to unmodifiable map domain to repository instance
	 * @return map of domain to repository instance
	 */
	protected Map<String,T> getRepositories() {
		return Collections.unmodifiableMap(repositories);
	}

	/**
	 * Provides repository instance for passed domain name
	 * @param domain
	 * @return instance of repository
	 */
	protected T getRepository(String domain) {
		T repo = repositories.get(aliases.getOrDefault(domain, domain));
		if (repo == null) {
			repo = repositories.get("default");
		}
		return repo;
	}

	@HandleEvent
	protected void onDataSourceChange(DataSourceBean.DataSourceChangedEvent event) {
		if (!event.isCorrectSender(dataSourceBean))
			return;

		if (dataSourceSelection == SelectorType.EveryDataSource) {
			if (event.getNewDataSource() == null) {
				kernel.unregister(event.getDomain());
			} else {
				registerIfNotExists(event.getDomain());
			}
		}
	}

	public void setDataSourceBean(DataSourceBean dataSourceBean) {
		Map<String, DataSource> oldDataSources = new HashMap<>();
		String defAlias = "default";
		if (this.dataSourceBean != null) {
			oldDataSources.put(defAlias, this.dataSourceBean.getRepository(defAlias));
			for (String domain : this.dataSourceBean.getDataSourceNames()) {
				oldDataSources.put(domain, this.dataSourceBean.getRepository(domain));
			}
		}

		this.dataSourceBean = dataSourceBean;

		if (this.dataSourceBean != null) {
			switch (dataSourceSelection) {
				case EveryDataSource:
					for (String name : dataSourceBean.getDataSourceNames()) {
						registerIfNotExists(name);
					}
					break;
				case EveryUserRepository:
					registerIfNotExists("default");

					UserRepositoryMDPoolBean userRepositoryPool = kernel.getInstance(UserRepositoryMDPoolBean.class);
					for (String name : userRepositoryPool.getDomainsList()) {
						registerIfNotExists(name);
					}
					break;
				case List:
					registerIfNotExists("default");

					// manual registration is already possible by configuring new subbeans
					break;
			}
		}
	}

	public void registerIfNotExists(String name) {
		if (!kernel.isBeanClassRegistered(name)) {
			Class<?> cls = getDefaultBeanClass();
			kernel.registerBean(name).asClass(cls).exec();
		}
	}

	@Override
	public void register(Kernel kernel) {
		this.kernel = kernel;
		registerIfNotExists("default");
	}

	@Override
	public void unregister(Kernel kernel) {
		this.kernel = null;
	}

	/**
	 * Method called to initialized passed repository instance for passed domain.
	 * <br/>
	 * Should be empty if no custom initialization is required.
	 * @param domain
	 * @param repo
	 */
	protected void initializeRepository(String domain, T repo) {

	}

	public void initialize() {
		eventBus.registerAll(this);
	}

	@Override
	public void beforeUnregister() {
		eventBus.unregisterAll(this);
	}

	/**
	 * Method called when repository instance for domain changes.
	 * @param domain name of domain
	 * @param newRepo new instance of repository
	 * @param oldRepo old instance of repository
	 */
	protected void updateDataSourceAware(String domain, T newRepo, T oldRepo) {
		if (newRepo != null) {
			this.repositories.put(domain, newRepo);
		} else {
			this.repositories.remove(domain, oldRepo);
		}
	}

	/**
	 * Determines behaviour of MDRepositoryBean.
	 */
	public static enum SelectorType {
		/**
		 * Repository instances will be created for default data source and for data sources listed in configuration.
		 */
		List,
		/**
		 * Repository instances will be created for every data source.
		 */
		EveryDataSource,
		/**
		 * Repository instances will be created for every data source for which user repository exists.
		 */
		EveryUserRepository,
	}

	/**
	 * MDRepositoryConfigBean is bean responsible for basic management and initialization of repository for domain.
	 * @param <A>
	 */
	public abstract static class MDRepositoryConfigBean<A extends DataSourceAware> implements Initializable, UnregisterAware, ConfigurationChangedAware, RegistrarBean {

		@Inject
		protected DataSourceBean dataSourceBean;

		@Inject
		private EventBus eventBus;

		@Inject
		private MDRepositoryBean<A> mdRepositoryBean;

		private Kernel kernel;

		@Inject(bean = "instance", nullAllowed = true)
		private A dataSourceAware;

		@ConfigField(desc = "Name (ie. domain)")
		private String name;

		@ConfigField(alias = REPO_CLASS, desc = "Class implementing repository", allowAliasFromParent = false)
		private String cls;

		@ConfigField(desc = "Name of data source", alias = "data-source")
		private String dataSourceName;

		private DataSource dataSource;

		/**
		 * Method returns class for instance repository matching data source or returning class
		 * specified in <code>cls</code> config field.
		 * @return
		 * @throws DBInitException
		 * @throws ClassNotFoundException
		 */
		protected Class<?> getRepositoryClassName() throws DBInitException, ClassNotFoundException {
			if (cls == null) {
				return mdRepositoryBean.findClassForDataSource(dataSource);
			}
			return ModulesManagerImpl.getInstance().forName(cls);
		}

		protected String getCls() {
			return cls;
		}

		@Override
		public void beanConfigurationChanged(Collection<String> changedFields) {
			if (name == null || mdRepositoryBean == null || dataSourceBean == null)
				return;

			String name = this.name;
			if (dataSourceName != null && !dataSourceName.isEmpty())
				name = dataSourceName;

			dataSource = dataSourceBean.getRepository(name);

			if (dataSource != null) {
				try {
					Class<?> repoClass = getRepositoryClassName();

					kernel.registerBean("instance").asClass(repoClass).exec();
				} catch (DBInitException | ClassNotFoundException ex) {
					throw new RuntimeException("Could not initialize bean '" + name + "'", ex);
				}
			} else {
				if (kernel.isBeanClassRegistered("instance")) {
					kernel.unregister("instance");
				}
			}
		}

		@HandleEvent
		protected void onDataSourceChange(DataSourceBean.DataSourceChangedEvent event) {
			if (!event.isCorrectSender(dataSourceBean))
				return;

			if (!event.getDomain().equals(name) && !event.getDomain().equals(dataSourceName))
				return;

			beanConfigurationChanged(Collections.singleton("uri"));
		}

		public void setDataSourceAware(A dataSourceAware) {
			if (mdRepositoryBean == null)// && dataSourceAware == null)
				return;

			if (dataSourceAware != null) {
				this.mdRepositoryBean.initializeRepository(name, dataSourceAware);
				try {
					dataSourceAware.setDataSource(dataSource);
				} catch (RepositoryException ex) {
					throw new RuntimeException("Failed to initialize repository", ex);
				}
			}
			mdRepositoryBean.updateDataSourceAware(name, dataSourceAware, this.dataSourceAware);
			this.dataSourceAware = dataSourceAware;
		}

		@Override
		public void register(Kernel kernel) {
			this.kernel = kernel;
			String rootBean = kernel.getParent().getName();
			this.kernel.getParent().ln("service", kernel, rootBean);
		}

		@Override
		public void unregister(Kernel kernel) {
			kernel.unregister("instance");
		}

		@Override
		public void initialize() {
			eventBus.registerAll(this);
			beanConfigurationChanged(Collections.singleton("uri"));
			setDataSourceAware(kernel.getInstance("instance"));
		}

		@Override
		public void beforeUnregister() {
			eventBus.unregisterAll(this);
			kernel.unregister("instance");
		}
	}
}
