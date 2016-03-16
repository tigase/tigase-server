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
import tigase.kernel.beans.config.ConfigField;
import tigase.kernel.beans.config.ConfigurationChangedAware;
import tigase.osgi.ModulesManagerImpl;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static tigase.db.beans.MDPoolBean.*;

/**
 * Created by andrzej on 08.03.2016.
 */
public abstract class MDPoolConfigBean<A extends Repository,B extends MDPoolConfigBean<A,B>> implements ConfigurationChangedAware {

	protected MDPoolBean<A,B> mdPool;
	protected String domain;

	@ConfigField(alias = REPO_URI, desc = "URI for repository", allowAliasFromParent = false)
	protected String uri;

	@ConfigField(alias = REPO_CLASS, desc = "Class implementing repository", allowAliasFromParent = false)
	protected String cls;

	@ConfigField(alias = POOL_SIZE, desc = "Pool size", allowAliasFromParent = false)
	protected int poolSize = RepositoryFactory.USER_REPO_POOL_SIZE_PROP_VAL;

	@ConfigField(alias = POOL_CLASS, desc = "Class implementing repository pool", allowAliasFromParent = false)
	protected String poolCls;

	protected void setDomain(String domain) {
		this.domain = domain;
	}

	protected void setMDPool(MDPoolBean<A, B> mdPool) {
		this.mdPool = mdPool;
	}

	protected abstract Class<? extends A> getRepositoryIfc();
	protected abstract String getRepositoryPoolClassName() throws DBInitException;

	protected String getRepositoryClassName() throws DBInitException {
		if (cls != null)
			return cls;
		return RepositoryFactory.getRepoClassName(getRepositoryIfc(), uri);
	}

	protected String getUri() {
		return uri;
	}

	@Override
	public void beanConfigurationChanged(Collection<String> changedFields) {
		// domain not set yet - skip initialization
		if (domain == null || getUri() == null)
			return;

		try {
			String cls = getRepositoryClassName();

			Map<String, String> params = new HashMap<>();
			String poolCls = getRepositoryPoolClassName();

			if (poolCls == null) {
				A repo = initRepository(cls, params);
				setRepository(repo);
			} else {
				RepositoryPool<A> pool;

				pool = (RepositoryPool<A>) ModulesManagerImpl.getInstance().forName(poolCls).newInstance();
				pool.initRepository(getUri(), params);
				for (int i = 0; i < poolSize; i++) {
					A repo = initRepository(cls, params);
					pool.addRepo(repo);
				}
				setRepository((A) pool);
			}
		} catch (DBInitException|ClassNotFoundException|IllegalAccessException|InstantiationException ex) {
			throw new RuntimeException("Could not initialize " + getRepositoryIfc().getCanonicalName() + " for domain '" + domain + "'", ex);
		}
	}

	protected A initRepository(String cls, Map<String, String> params) throws ClassNotFoundException, IllegalAccessException, InstantiationException, DBInitException {
		A repo = newRepositoryInstance(cls);
		repo.initRepository(getUri(), params);
		return repo;
	}

	protected A newRepositoryInstance(String cls) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
		return (A) ModulesManagerImpl.getInstance().forName(cls).newInstance();
	}

	protected void setRepository(A repo) {
		mdPool.addRepo(domain, repo);
		if ("default".equals(domain)) {
			mdPool.setDefault((A) repo);
		}
	}

}