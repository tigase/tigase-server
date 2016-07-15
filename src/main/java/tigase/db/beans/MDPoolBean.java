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

import tigase.db.Repository;
import tigase.db.RepositoryFactory;
import tigase.kernel.beans.RegistrarBean;
import tigase.kernel.beans.config.ConfigField;
import tigase.kernel.core.Kernel;

import java.util.*;

/**
 * Created by andrzej on 08.03.2016.
 */
public abstract class MDPoolBean<S extends Repository,T extends MDPoolConfigBean<S,T>> implements tigase.kernel.beans.config.ConfigurationChangedAware, RegistrarBean {

	public static final String REPO_URI = "repo-uri";
	public static final String REPO_CLASS = "repo-class";
	public static final String POOL_CLASS = "pool-class";
	public static final String POOL_SIZE = "pool-size";

	@ConfigField(alias = REPO_URI, desc = "URI for repository", allowAliasFromParent = false)
	protected String uri;

	@ConfigField(alias = REPO_CLASS, desc = "Class implementing repository", allowAliasFromParent = false)
	private String cls;


	@ConfigField(alias = POOL_SIZE, desc = "Pool size", allowAliasFromParent = false)
	private int poolSize = RepositoryFactory.USER_REPO_POOL_SIZE_PROP_VAL;

	@ConfigField(alias = POOL_CLASS, desc = "Class implementing repository pool", allowAliasFromParent = false)
	protected String poolCls;

	@ConfigField(desc = "Domains")
	private String[] domains = {};

	Kernel kernel;

	@Override
	public void register(Kernel kernel) {
		this.kernel = kernel;
		registerConfigBean("default");

		for (String domain : domains) {
			registerConfigBean(domain);
		}
	}

	@Override
	public void unregister(Kernel kernel) {
		this.kernel = null;
	}

	public String[] getDomains() {
		return this.domains;
	}

	public void setDomains(String[] domains) {
		updateDomains(this.domains, domains);
		this.domains = domains;
	}

	@Override
	public void beanConfigurationChanged(Collection<String> changedFields) {
		if (kernel != null) {
			if (changedFields.contains("uri") || changedFields.contains("cls") || changedFields.contains("poolSize")) {
				T defaultBean = kernel.getInstance("default");
				updateConfigForDefault(defaultBean);
				defaultBean.beanConfigurationChanged(changedFields);
			}
		}
	}

	protected void registerConfigBean(String domain) {
		kernel.registerBean(domain).asClass(getConfigClass()).exec();
		T configBean = kernel.getInstance(domain);
		configBean.setDomain(domain);
		configBean.setMDPool(this);
		if ("default".equals(domain)) {
			updateConfigForDefault(configBean);
		}
		configBean.beanConfigurationChanged(Collections.singleton("domain"));
	}

	protected void updateConfigForDefault(T defaultBean) {
		defaultBean.uri = uri;
		defaultBean.cls = cls;
		defaultBean.poolCls = poolCls;
		defaultBean.poolSize = poolSize;
	}

	protected void updateDomains(String[] oldDomains, String[] newDomains) {
		// in this case register method will take over
		if (kernel == null)
			return;

		Set<String> removed = new HashSet<>(Arrays.asList(oldDomains));
		removed.remove(Arrays.asList(newDomains));
		Set<String> added = new HashSet<>(Arrays.asList(newDomains));
		added.remove(Arrays.asList(oldDomains));

		for (String domain : removed) {
			kernel.unregister(domain);
			this.removeRepo(domain);
		}
		for (String domain : added) {
			registerConfigBean(domain);
		}

	}

	public String getDefaultAlias() {
		return "default";
	}

	protected abstract Class<? extends T> getConfigClass();

	protected abstract void addRepo(String domain, S repo);
	protected abstract S removeRepo(String domain);
	protected abstract void setDefault(S repo);

}
