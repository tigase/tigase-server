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

import tigase.db.*;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.config.ConfigField;
import tigase.kernel.beans.config.ConfigurationChangedAware;
import tigase.kernel.core.Kernel;
import tigase.db.factories.AuthRepositoryMDPoolBean.AuthRepositoryConfigBean;

import java.util.Collection;

/**
 * Created by andrzej on 08.03.2016.
 */
@Bean(name="authRepository", parent = Kernel.class)
public class AuthRepositoryMDPoolBean extends AuthRepositoryMDImpl implements MDPoolBean<AuthRepository, AuthRepositoryConfigBean> {
	@ConfigField(alias = REPO_URI, desc = "URI for UserRepository")
	private String uri;

	@ConfigField(alias = REPO_CLASS, desc = "Class implementing UserRepository")
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
				AuthRepositoryConfigBean defaultBean = kernel.getInstance("default");
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
	public Class<? extends AuthRepositoryConfigBean> getConfigClass() {
		return AuthRepositoryConfigBean.class;
	}

	@Override
	public void unregister(Kernel kernel) {
		this.kernel = null;
	}

	public void setDomains(String[] domains) {
		updateDomains(this.domains, domains);
		this.domains = domains;
	}

	public static class AuthRepositoryConfigBean extends MDPoolConfigBean<AuthRepository,AuthRepositoryConfigBean> implements ConfigurationChangedAware {

		@Override
		protected Class<AuthRepository> getRepositoryIfc() {
			return AuthRepository.class;
		}

		@Override
		protected String getRepositoryPoolClassName() {
			return AuthRepositoryPool.class.getCanonicalName();
		}

	}
}
