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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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

	@ConfigField(desc = "Map of classes for data sources")
	ConcurrentHashMap<String,String> customClasses = new ConcurrentHashMap<>();

	private final Map<String,T> repositories = new ConcurrentHashMap<>();
	private Kernel kernel;

	protected abstract Class<? extends T> findClassForDataSource(DataSource dataSource) throws DBInitException;

	protected Collection<T> getRepositories() {
		return repositories.values();
	}

	protected T getRepository(String domain) {
		T repo = repositories.get(domain);
		if (repo == null) {
			repo = repositories.get(dataSourceBean.getDefaultAlias());
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
		this.dataSourceBean = dataSourceBean;
		for (String name : dataSourceBean.getDataSourceNames()) {
			updateDataSource(name, dataSourceBean.getRepository(name), null);
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

}
