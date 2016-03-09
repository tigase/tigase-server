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
import tigase.kernel.beans.config.ConfigField;
import tigase.kernel.beans.config.ConfigurationChangedAware;
import tigase.osgi.ModulesManagerImpl;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static tigase.db.factories.MDPoolBean.*;

/**
 * Created by andrzej on 08.03.2016.
 */
public abstract class MDPoolConfigBean<A extends Repository,B extends MDPoolConfigBean<A,B>> implements ConfigurationChangedAware {

	protected MDPoolBean<A,B> mdPool;
	protected String domain;

	@ConfigField(alias = REPO_URI, desc = "URI for repository")
	protected String uri;

	@ConfigField(alias = REPO_CLASS, desc = "Class implementing repository")
	protected String cls;

	@ConfigField(alias = POOL_SIZE, desc = "Pool size")
	protected int poolSize = RepositoryFactory.USER_REPO_POOL_SIZE_PROP_VAL;

	protected void setDomain(String domain) {
		this.domain = domain;
	}

	protected void setMDPool(MDPoolBean<A, B> mdPool) {
		this.mdPool = mdPool;
	}

	protected abstract Class<A> getRepositoryIfc();
	protected abstract String getRepositoryPoolClassName();

	@Override
	public void beanConfigurationChanged(Collection<String> changedFields) {
		// domain not set yet - skip initialization
		if (domain == null || uri == null)
			return;

		RepositoryPool<A> pool;
		try {
			if (cls == null) {
				cls = RepositoryFactory.getRepoClassName(getRepositoryIfc(), uri);
			}

			Map<String, String> params = new HashMap<>();
			pool = (RepositoryPool<A>) ModulesManagerImpl.getInstance().forName(getRepositoryPoolClassName()).newInstance();
			pool.initRepository(uri, params);
			for (int i = 0; i < poolSize; i++) {
				A repo = (A) ModulesManagerImpl.getInstance().forName(cls).newInstance();
				repo.initRepository(uri, params);
				pool.addRepo(repo);
			}
		} catch (DBInitException |ClassNotFoundException|IllegalAccessException|InstantiationException ex) {
			throw new RuntimeException("Could not initialize " + getRepositoryIfc().getCanonicalName() + " for domain '" + domain + "'");
		}

		if ("default".equals(domain)) {
			mdPool.addRepo("default".equals(domain) ? "" : domain, (A) pool);
			mdPool.setDefault((A) pool);
		}
	}


}