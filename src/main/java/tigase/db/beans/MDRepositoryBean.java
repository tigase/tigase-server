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
import tigase.db.DataSourceAware;
import tigase.eventbus.EventBus;
import tigase.eventbus.HandleEvent;
import tigase.kernel.beans.Initializable;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.RegistrarBean;
import tigase.kernel.beans.UnregisterAware;
import tigase.kernel.beans.config.ConfigField;
import tigase.kernel.core.Kernel;
import tigase.osgi.ModulesManagerImpl;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Abstract class implementing bean to which should be used to create domain aware repository pool.
 * This class is resposible for creation of correct repository instances for every DataSource configured.
 *
 * Created by andrzej on 15.03.2016.
 */
public abstract class MDRepositoryBean<T extends DataSourceAware> implements Initializable, UnregisterAware, RegistrarBean {

	private static final Logger log = Logger.getLogger(MDRepositoryBean.class.getCanonicalName());

	@Inject
	protected DataSourceBean dataSourceBean;

	@Inject
	private EventBus eventBus;

	@ConfigField(desc = "Map of aliases for data sources to use")
	protected ConcurrentHashMap<String, String> aliases = new ConcurrentHashMap<>();

	@ConfigField(desc = "Map of classes for data sources")
	protected ConcurrentHashMap<String,String> customClasses = new ConcurrentHashMap<>();

	@ConfigField(desc = "Create repositories for: Main only data source, every UserRepository, every data source, listed data sources")
	protected SelectorType dataSourceSelection = SelectorType.MainOnly;

	@ConfigField(desc = "Default data source to use")
	protected String defaultDataSourceName = "default";

	@ConfigField(desc = "List of domains for which we create separate instances")
	protected CopyOnWriteArrayList<String> domains = new CopyOnWriteArrayList<>();

	private final Map<String,T> repositories = new ConcurrentHashMap<>();
	private Kernel kernel;

	protected abstract Class<? extends T> findClassForDataSource(DataSource dataSource) throws DBInitException;

	protected Collection<T> getRepositories() {
		return repositories.values();
	}

	protected T getRepository(String domain) {
		T repo = repositories.get(aliases.getOrDefault(domain, domain));
		if (repo == null) {
			repo = repositories.get(defaultDataSourceName);
		}
		return repo;
	}

	@HandleEvent
	protected void onDataSourceChange(DataSourceBean.DataSourceChangedEvent event) {
		if (!event.isCorrectSender(dataSourceBean))
			return;

		updateDataSource(event.getDomain(), event.getNewDataSource(), event.getOldDataSource());
	}

	public void setDataSourceBean(DataSourceBean dataSourceBean) {
		Map<String, DataSource> oldDataSources = new HashMap<>();
		String defAlias = defaultDataSourceName;
		if (this.dataSourceBean != null) {
			oldDataSources.put(defAlias, this.dataSourceBean.getRepository(defAlias));
			for (String domain : this.dataSourceBean.getDomains()) {
				oldDataSources.put(domain, this.dataSourceBean.getRepository(domain));
			}
		}

		this.dataSourceBean = dataSourceBean;

		if (this.dataSourceBean != null) {
			switch (dataSourceSelection) {
				case MainOnly:
					updateDataSource(defAlias, dataSourceBean.getRepository(defAlias), oldDataSources.get(defAlias));
					break;
				case EveryDataSource:
					for (String name : dataSourceBean.getDataSourceNames()) {
						updateDataSource(name, dataSourceBean.getRepository(name), oldDataSources.get(name));
					}
					break;
				case EveryUserRepository:
					updateDataSource(defAlias, dataSourceBean.getRepository(defAlias), oldDataSources.get(defAlias));

					UserRepositoryMDPoolBean userRepositoryPool = kernel.getInstance(UserRepositoryMDPoolBean.class);
					for (String name : userRepositoryPool.getDomains()) {
						updateDataSource(defAlias, dataSourceBean.getRepository(name), oldDataSources.get(name));
					}
					break;
				case List:
					for (String name : domains) {
						updateDataSource(name, dataSourceBean.getRepository(name), oldDataSources.get(name));
					}
					break;
			}
		}
	}

	@Override
	public void register(Kernel kernel) {
		this.kernel = kernel;
	}

	@Override
	public void unregister(Kernel kernel) {
		this.kernel = null;
	}

	protected void updateDataSource(String domain, DataSource newDS, DataSource oldDS) {
		T repo = null;
		if (newDS != null) {
			Class<? extends T> repoClass = null;
			try {
				repoClass = getClassForDomain(domain);
				if (repoClass == null)
					repoClass = findClassForDataSource(newDS);

				kernel.registerBean(domain).asClass(repoClass).exec();
				repo = kernel.getInstance(domain);
				initializeRepository(domain, repo);
				repo.setDataSource(newDS);
			} catch (DBInitException|ClassNotFoundException e) {
//				} catch (DBInitException|InstantiationException|IllegalAccessException|ClassNotFoundException e) {
				log.log(Level.SEVERE, "could not initialize instance of MsgPository = " + repoClass + " for dataSource " + newDS);
				repo = null;
			}
		} else {
			kernel.unregister(domain);
		}
		if (repo == null)
			repositories.remove(domain);
		else
			repositories.put(domain, repo);
	}

	protected Class<? extends T> getClassForDomain(String domain) throws ClassNotFoundException {
		String className = customClasses.get(domain);
		if (className == null)
			return null;
		return (Class<? extends T>) ModulesManagerImpl.getInstance().forName(className);
	}

	@Override
	public void initialize() {
		eventBus.registerAll(this);
	}

	protected void initializeRepository(String domain, T repo) {

	}

	@Override
	public void beforeUnregister() {
		eventBus.unregisterAll(this);
	}

	public static enum SelectorType {
		MainOnly,
		EveryDataSource,
		EveryUserRepository,
		List
	}
}
