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
package tigase.db.factories;

import tigase.db.RepositoryFactory;
import tigase.db.UserRepository;
import tigase.db.UserRepositoryMDImpl;
import tigase.db.UserRepositoryPool;
import tigase.db.factories.UserRepositoryMDPoolBean.UserRepositoryConfigBean;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.config.ConfigField;
import tigase.kernel.beans.config.ConfigurationChangedAware;
import tigase.kernel.core.Kernel;

import java.util.Collection;

/**
 * Created by andrzej on 07.03.2016.
 */
@Bean(name="userRepository", parent = Kernel.class)
public class UserRepositoryMDPoolBean extends UserRepositoryMDImpl implements MDPoolBean<UserRepository, UserRepositoryConfigBean> {

	@ConfigField(alias = REPO_URI, desc = "URI for repository")
	private String uri;

	@ConfigField(alias = REPO_CLASS, desc = "Class implementing repository")
	private String cls;

	@ConfigField(alias = POOL_SIZE, desc = "Pool size")
	private int poolSize = RepositoryFactory.USER_REPO_POOL_SIZE_PROP_VAL;

	@ConfigField(desc = "Domains")
	private String[] domains = {};

	Kernel kernel;

	@Override
	public void beanConfigurationChanged(Collection<String> changedFields) {
		if (kernel != null) {
			if (changedFields.contains("uri") || changedFields.contains("cls") || changedFields.contains("poolSize")) {
				UserRepositoryConfigBean defaultBean = kernel.getInstance("default");
				updateConfigForDefault(defaultBean);
				defaultBean.beanConfigurationChanged(changedFields);
			}
		}
	}

	@Override
	public void register(Kernel kernel) {
		this.kernel = kernel;

		registerConfigBean("default");

		for (String domain : domains) {
			registerConfigBean(domain);
		}
	}

	@Override
	public String getDefUri() {
		return uri;
	}

	@Override
	public String getDefClass() {
		return cls;
	}

	@Override
	public int getDefPoolSize() {
		return poolSize;
	}

	@Override
	public String[] getDomains() {
		return domains;
	}

	@Override
	public Kernel getKernel() {
		return kernel;
	}

	@Override
	public Class<? extends UserRepositoryConfigBean> getConfigClass() {
		return UserRepositoryConfigBean.class;
	}

	@Override
	public void unregister(Kernel kernel) {
		this.kernel = null;
	}

	public void setDomains(String[] domains) {
		updateDomains(this.domains, domains);
		this.domains = domains;
	}

	public static class UserRepositoryConfigBean extends MDPoolConfigBean<UserRepository,UserRepositoryConfigBean> implements ConfigurationChangedAware {

		@Override
		protected Class<UserRepository> getRepositoryIfc() {
			return UserRepository.class;
		}

		@Override
		protected String getRepositoryPoolClassName() {
			return UserRepositoryPool.class.getCanonicalName();
		}

	}
}
