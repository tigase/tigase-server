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

import tigase.db.*;
import tigase.eventbus.EventBus;
import tigase.eventbus.HandleEvent;
import tigase.kernel.beans.Initializable;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.UnregisterAware;
import tigase.kernel.beans.config.ConfigurationChangedAware;

import java.util.Collection;
import java.util.Collections;

/**
 * Created by andrzej on 09.03.2016.
 */
public abstract class AuthUserRepositoryConfigBean<T extends Repository, U extends AuthUserRepositoryConfigBean<T,U>> extends MDPoolConfigBean<T,U> implements ConfigurationChangedAware, Initializable, UnregisterAware {

	@Inject
	private EventBus eventBus;
	@Inject
	private DataSourceBean dataSourceBean;

	private DataSource dataSource;
	private String repositoryUri;

	@Override
	protected String getUri() {
		return repositoryUri;
	}

	@Override
	protected String getRepositoryClassName() throws DBInitException {
		if (cls != null)
			return cls;
		return RepositoryFactory.getRepoClassName(getRepositoryIfc(), repositoryUri);
	}

	@Override
	protected T newRepositoryInstance(String cls) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
		T repository = super.newRepositoryInstance(cls);
		if (repository instanceof DataSourceAware)
			((DataSourceAware) repository).setDataSource(dataSource);
		return repository;
	}

	@Override
	public void beanConfigurationChanged(Collection<String> changedFields) {
		if (dataSourceBean != null) {
			if (uri == null) {
				repositoryUri = domain;
				dataSource = dataSourceBean.getRepository(domain);
				if (dataSource != null)
					repositoryUri = dataSource.getResourceUri();
			} else {
				repositoryUri = uri;

				if (repositoryUri.startsWith("dataSource:")) {
					String dataSourceName = repositoryUri.substring("dataSource:".length());
					dataSource = dataSourceBean.getRepository(dataSourceName);
					if (dataSource != null)
						repositoryUri = dataSource.getResourceUri();
				}
			}
		}

		super.beanConfigurationChanged(changedFields);
	}

	@Override
	protected String getRepositoryPoolClassName() {
		return null;
	}

	@HandleEvent
	protected void onDataSourceChange(DataSourceBean.DataSourceChangedEvent event) {
		if (!event.isCorrectSender(dataSourceBean))
			return;

		if (uri != null || !event.getDomain().equals(domain))
			return;

		beanConfigurationChanged(Collections.singleton("uri"));
	}

	@Override
	public void initialize() {
		eventBus.registerAll(this);
	}

	@Override
	public void beforeUnregister() {
		eventBus.unregisterAll(this);
	}
}
