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

import tigase.db.DBInitException;
import tigase.db.Repository;
import tigase.db.RepositoryFactory;
import tigase.db.RepositoryPool;
import tigase.kernel.beans.Initializable;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.RegistrarBean;
import tigase.kernel.beans.config.ConfigField;
import tigase.kernel.beans.config.ConfigurationChangedAware;
import tigase.kernel.core.Kernel;
import tigase.osgi.ModulesManagerImpl;

import java.util.*;

import static tigase.db.beans.MDPoolBean.*;

/**
 * Created by andrzej on 08.03.2016.
 */
public abstract class MDPoolConfigBean<A extends Repository,B extends MDPoolConfigBean<A,B>> implements Initializable, ConfigurationChangedAware, RegistrarBean {

	@Inject
	protected MDPoolBean<A,B> mdPool;

	@Inject(bean = "instance", nullAllowed = true)
	private A repository;

	@Inject(nullAllowed = true)
	private Set<A> instances;

	@Inject
	private Kernel kernel;

	@ConfigField(desc = "Name (ie. domain)")
	protected String name;

	@ConfigField(alias = REPO_URI, desc = "URI for repository", allowAliasFromParent = false)
	protected String uri;

	@ConfigField(alias = REPO_CLASS, desc = "Class implementing repository", allowAliasFromParent = false)
	protected String cls;

	@ConfigField(alias = POOL_SIZE, desc = "Pool size", allowAliasFromParent = false)
	protected int poolSize = RepositoryFactory.USER_REPO_POOL_SIZE_PROP_VAL;

	@ConfigField(alias = POOL_CLASS, desc = "Class implementing repository pool", allowAliasFromParent = false)
	protected String poolCls;

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
		// name not set yet - skip initialization
		if (name == null || getUri() == null || kernel == null)
			return;

		try {
			String cls = getRepositoryClassName();
			if (cls == null)
				return;

			Class<?> repoClass = ModulesManagerImpl.getInstance().forName(cls);
			String poolCls = getRepositoryPoolClassName();

			if (poolCls == null) {
				kernel.registerBean("instance").asClass(repoClass).exec();
			} else {
				Class<?> poolClass = ModulesManagerImpl.getInstance().forName(poolCls);

				kernel.registerBean("instance").asClass(poolClass).exec();

				for (int i = 0; i < poolSize; i++) {
					kernel.registerBean("repo-" + i).asClass(repoClass).exec();
				}
			}
		} catch (DBInitException|ClassNotFoundException ex) {
			throw new RuntimeException("Could not initialize " + getRepositoryIfc().getCanonicalName() + " for name '" + name + "'", ex);
		}
	}

	@Override
	public void initialize() {
		beanConfigurationChanged(Collections.singletonList("uri"));
	}

	@Override
	public void register(Kernel kernel) {
		this.kernel = kernel;
	}

	@Override
	public void unregister(Kernel kernel) {

	}

	public void setInstances(Set<A> instances) {
		if (instances != null) {
			for (Iterator<A> iter = instances.iterator(); iter.hasNext(); ) {
				A it = iter.next();
				if (it instanceof MDPoolBean)
					iter.remove();
			}
		}
		HashSet<A> toInitialize = new HashSet<A>(instances);
		if (this.instances != null) {
			toInitialize.removeAll(this.instances);
		}
		for (A repo :  toInitialize) {
			try {
				initRepository(repo);
			} catch (DBInitException ex) {
				throw new RuntimeException("Could not initialize " + repo.getClass().getCanonicalName() + " for name '" + name + "'", ex);
			}

			if (repository instanceof RepositoryPool && !(repo instanceof RepositoryPool)) {
				((RepositoryPool<A>) repository).addRepo(repo);
			}
		}

		this.instances = instances;
	}

	protected void initRepository(A repo) throws DBInitException {
		repo.initRepository(getUri(), Collections.EMPTY_MAP);
	}

	public void setRepository(A repo) {
		this.repository = repo;
		if (instances != null) {
			for (A instance : instances) {
				if (repository instanceof RepositoryPool && !(instance instanceof RepositoryPool)) {
					((RepositoryPool<A>) repository).addRepo(instance);
				}
			}
		}

		if (mdPool != null) {
			mdPool.addRepo(name, repo);
			if ("default".equals(name)) {
				mdPool.setDefault(repo);
			}
		}
	}

	public void setMdPool(MDPoolBean<A,B> mdPool) {
		if (mdPool != null && this.repository != null) {
			mdPool.addRepo(name, this.repository);
			if ("default".equals(name)) {
				mdPool.setDefault(this.repository);
			}
		}
		this.mdPool = mdPool;
	}
}