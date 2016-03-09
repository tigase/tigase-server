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

import tigase.db.Repository;
import tigase.kernel.beans.RegistrarBean;
import tigase.kernel.core.Kernel;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by andrzej on 08.03.2016.
 */
public interface MDPoolBean<S extends Repository,T extends MDPoolConfigBean<S,T>> extends tigase.kernel.beans.config.ConfigurationChangedAware, RegistrarBean {

	String REPO_URI = "repo-uri";
	String REPO_CLASS = "repo-class";
	String POOL_SIZE = "pool-size";

	default void register(Kernel kernel) {
		registerConfigBean("default");

		for (String domain : getDomains()) {
			registerConfigBean(domain);
		}
	}

	default void registerConfigBean(String domain) {
		Kernel kernel = getKernel();

		kernel.registerBean(domain).asClass(getConfigClass()).exec();
		T configBean = kernel.getInstance(domain);
		configBean.setDomain(domain);
		configBean.setMDPool(this);
		if ("default".equals(domain)) {
			updateConfigForDefault(configBean);
		}
		configBean.beanConfigurationChanged(Collections.singleton("domain"));
	}

	default void updateConfigForDefault(T defaultBean) {
		defaultBean.uri = getDefUri();
		defaultBean.cls = getDefClass();
		defaultBean.poolSize = getDefPoolSize();
	}

	default void updateDomains(String[] oldDomains, String[] newDomains) {
		Set<String> removed = new HashSet<>(Arrays.asList(oldDomains));
		removed.remove(Arrays.asList(newDomains));
		Set<String> added = new HashSet<>(Arrays.asList(newDomains));
		added.remove(Arrays.asList(oldDomains));

		Kernel kernel = getKernel();

		for (String domain : removed) {
			kernel.unregister(domain);
			this.removeRepo(domain);
		}
		for (String domain : added) {
			registerConfigBean(domain);
		}

	}

	String getDefUri();
	String getDefClass();
	int getDefPoolSize();
	String[] getDomains();
	Kernel getKernel();
	Class<? extends T> getConfigClass();

	void addRepo(String domain, S repo);
	S removeRepo(String domain);
	void setDefault(S repo);

}
