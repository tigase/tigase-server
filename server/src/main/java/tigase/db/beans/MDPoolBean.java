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

import tigase.kernel.beans.Inject;
import tigase.kernel.beans.RegistrarBeanWithDefaultBeanClass;
import tigase.kernel.beans.config.ConfigField;
import tigase.kernel.core.Kernel;

/**
 * Abstract class providing base part for implementation of pool for multiple domains.
 *
 * Created by andrzej on 08.03.2016.
 */
public abstract class MDPoolBean<S,T extends MDPoolConfigBean<S,T>> implements RegistrarBeanWithDefaultBeanClass {

	public static final String REPO_URI = "repo-uri";
	public static final String REPO_CLASS = "repo-class";
	public static final String POOL_CLASS = "pool-class";
	public static final String POOL_SIZE = "pool-size";
	
	@ConfigField(desc = "Bean name")
	private String name;

	private Kernel kernel;

	@Inject(nullAllowed = true)
	private MDPoolConfigBean[] configBeans;

	public String getName() {
		return name;
	}

	@Override
	public void register(Kernel kernel) {
		this.kernel = kernel;
		if (!kernel.isBeanClassRegistered("default")){
			registerConfigBean("default");
		}
	}

	@Override
	public void unregister(Kernel kernel) {
		this.kernel = null;
	}

	protected void registerConfigBean(String domain) {
		kernel.registerBean(domain).asClass(getConfigClass()).exec();
	}

	/**
	 * Default alias used if for provided domain then is no repo instance.
	 * @return default alias
	 */
	public String getDefaultAlias() {
		return "default";
	}

	/**
	 * Returns per domain configuration class
	 * @return class
	 */
	protected abstract Class<? extends T> getConfigClass();

	/**
	 * Method called to add repo instance for domain
	 * @param domain
	 * @param repo
	 */
	protected abstract void addRepo(String domain, S repo);

	/**
	 * Method called to remove repo instance for domain
	 * @param domain
	 * @return removed instance of repo
	 */
	protected abstract S removeRepo(String domain);

	/**
	 * Method called to set default repo instance.
	 * @param repo instance of repo
	 */
	protected abstract void setDefault(S repo);

}
