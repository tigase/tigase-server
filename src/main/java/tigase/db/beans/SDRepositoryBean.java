/**
 * Tigase XMPP Server - The instant messaging server
 * Copyright (C) 2004 Tigase, Inc. (office@tigase.com)
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
 */
package tigase.db.beans;

import tigase.component.exceptions.RepositoryException;
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
import tigase.kernel.beans.config.ConfigurationChangedAware;
import tigase.kernel.core.Kernel;
import tigase.osgi.ModulesManagerImpl;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import static tigase.db.beans.MDPoolBean.REPO_CLASS;

/**
 * Abstract class implementing bean to which can be used to create name unaware repository pool. This class is
 * resposible for creation of correct repository instance for single specified data source.
 * <br>
 * Created by andrzej on 17.08.2016.
 */
public abstract class SDRepositoryBean<A extends DataSourceAware>
		implements Initializable, UnregisterAware, ConfigurationChangedAware, RegistrarBean {

	private static final Logger log = Logger.getLogger(SDRepositoryBean.class.getCanonicalName());

	@ConfigField(alias = REPO_CLASS, desc = "Class implementing repository", allowAliasFromParent = false)
	private String cls;
	private DataSource dataSource;
	@Inject
	private DataSourceBean dataSourceBean;
	@ConfigField(desc = "Name of data source", alias = "data-source")
	private String dataSourceName;
	@Inject
	private EventBus eventBus;
	private Kernel kernel;
	@ConfigField(desc = "Bean name")
	private String name;
	@Inject(bean = "instance", nullAllowed = true)
	private A repository;

	public String getDataSourceName() {
		if (dataSourceName == null) {
			return "default";
		}
		return dataSourceName;
	}

	public String getName() {
		return name;
	}

	@Override
	public void beanConfigurationChanged(Collection<String> changedFields) {
		if (dataSourceBean == null) {
			return;
		}

		String name = "default";
		if (dataSourceName != null && !dataSourceName.isEmpty()) {
			name = dataSourceName;
		}

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
			if (log.isLoggable(Level.WARNING)) {
				log.log(Level.WARNING, "There is no data source named '" + Optional.ofNullable(dataSourceName)
						.orElse(name) +  "'");
			}
		}
	}

	@Override
	public void register(Kernel kernel) {
		this.kernel = kernel;
	}

	@Override
	public void unregister(Kernel kernel) {
		kernel.unregister("instance");
	}

	@Override
	public void initialize() {
		eventBus.registerAll(this);
		beanConfigurationChanged(Collections.singleton("uri"));
		if (dataSource == null) {
			throw new RuntimeException("There is no data source named '" + Optional.ofNullable(dataSourceName)
					.orElse("default") + "'");
		}
		setRepository(kernel.getInstance("instance"));
	}

	@Override
	public void beforeUnregister() {
		eventBus.unregisterAll(this);
		kernel.unregister("instance");
	}

	protected abstract Class<?> findClassForDataSource(DataSource dataSource) throws DBInitException;

	protected void initializeRepository(A repository) {
	}

	protected A getRepository() {
		return repository;
	}

	public void setRepository(A repository) {
		if (repository != null) {
			initializeRepository(repository);
			try {
				dataSource.checkSchemaVersion(repository, true);
				repository.setDataSource(dataSource);
			} catch (RepositoryException ex) {
				throw new RuntimeException("Failed to initialize repository", ex);
			}
		}
		this.repository = repository;
	}

	protected Class<?> getRepositoryClassName() throws DBInitException, ClassNotFoundException {
		if (cls == null) {
			return findClassForDataSource(dataSource);
		}
		return ModulesManagerImpl.getInstance().forName(cls);
	}

	@HandleEvent
	protected void onDataSourceChange(DataSourceBean.DataSourceChangedEvent event) {
		if (!event.isCorrectSender(dataSourceBean)) {
			return;
		}

		if (!event.getDomain().equals("default") && !event.getDomain().equals(dataSourceName)) {
			return;
		}

		beanConfigurationChanged(Collections.singleton("uri"));
	}

}
