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
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.RegistrarBeanWithDefaultBeanClass;
import tigase.kernel.beans.config.ConfigField;
import tigase.kernel.core.Kernel;

/**
 * Created by andrzej on 08.03.2016.
 */
public abstract class MDPoolBean<S extends Repository,T extends MDPoolConfigBean<S,T>> implements RegistrarBeanWithDefaultBeanClass {

	public static final String REPO_URI = "repo-uri";
	public static final String REPO_CLASS = "repo-class";
	public static final String POOL_CLASS = "pool-class";
	public static final String POOL_SIZE = "pool-size";

	@ConfigField(desc = "Domains")
	private String[] domains = {};

	private Kernel kernel;

	@Inject(nullAllowed = true)
	private MDPoolConfigBean[] configBeans;

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

	protected void registerConfigBean(String domain) {
		kernel.registerBean(domain).asClass(getConfigClass()).exec();
	}

	public String getDefaultAlias() {
		return "default";
	}

	protected abstract Class<? extends T> getConfigClass();

	protected abstract void addRepo(String domain, S repo);
	protected abstract S removeRepo(String domain);
	protected abstract void setDefault(S repo);

}
