/**
 * Tigase XMPP Server - The instant messaging server
 * Copyright (C) 2004 Tigase, Inc. (office@tigase.com)
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
 */
package tigase.db.beans;

import tigase.component.exceptions.RepositoryException;
import tigase.db.DBInitException;
import tigase.db.RepositoryFactory;
import tigase.db.RepositoryPool;
import tigase.kernel.beans.Initializable;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.RegistrarBean;
import tigase.kernel.beans.config.ConfigField;
import tigase.kernel.beans.config.ConfigurationChangedAware;
import tigase.kernel.core.BeanConfig;
import tigase.kernel.core.Kernel;
import tigase.osgi.ModulesManagerImpl;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import static tigase.db.beans.MDPoolBean.*;

/**
 * Base class for configuration beans of {@link tigase.db.beans.DataSourceBean}, {@link
 * tigase.db.beans.AuthRepositoryMDPoolBean} and {@link tigase.db.beans.UserRepositoryMDPoolBean}
 * <br>
 * Created by andrzej on 08.03.2016.
 */
public abstract class MDPoolConfigBean<A, B extends MDPoolConfigBean<A, B>>
		implements Initializable, ConfigurationChangedAware, RegistrarBean {

	private static final Logger log = Logger.getLogger(MDPoolConfigBean.class.getCanonicalName());
	@ConfigField(alias = REPO_CLASS, desc = "Class implementing repository", allowAliasFromParent = false)
	protected String cls;
	@Inject
	protected MDPoolBean<A, B> mdPool;
	@ConfigField(desc = "Name (ie. domain)")
	protected String name;
	@ConfigField(alias = POOL_CLASS, desc = "Class implementing repository pool", allowAliasFromParent = false)
	protected String poolCls;
	@ConfigField(alias = POOL_SIZE, desc = "Pool size", allowAliasFromParent = false)
	protected int poolSize = RepositoryFactory.USER_REPO_POOL_SIZE_PROP_VAL;
	@ConfigField(alias = REPO_URI, desc = "URI for repository", allowAliasFromParent = false)
	protected String uri;
	@Inject(nullAllowed = true)
	private Set<A> instances;
	private Kernel kernel;
	@Inject(bean = "instance", nullAllowed = true)
	private A repository;
	private boolean skipInitializationErrors = false;

	@Override
	public void beanConfigurationChanged(Collection<String> changedFields) {
		// name not set yet - skip initialization
		if (name == null || getUri() == null || kernel == null) {
			return;
		}

		try {
			String cls = getRepositoryClassName();
			if (cls == null) {
				return;
			}

			Class<?> repoClass = ModulesManagerImpl.getInstance().forName(cls);
			String poolCls = getRepositoryPoolClassName();

			Kernel.DelayedDependencyInjectionQueue queue = kernel.beginDependencyDelayedInjection();

			if (poolCls == null) {
				if (repository == null || changedFields.contains("poolCls")) {
					kernel.registerBean("instance").asClass(repoClass).exec();
				}
			} else {
				if (repository == null || changedFields.contains("poolCls") || changedFields.contains("poolSize")) {
					Class<?> poolClass = ModulesManagerImpl.getInstance().forName(poolCls);

					for (int i = 0; i < poolSize; i++) {
						kernel.registerBean("repo-" + i).asClass(repoClass).exec();
					}

					kernel.registerBean("instance").asClass(poolClass).exec();
				}
			}
			kernel.finishDependecyDelayedInjection(queue);
			unloadOldBeans();
		} catch (DBInitException | ClassNotFoundException | InstantiationException | InvocationTargetException | IllegalAccessException ex) {
			throw new RuntimeException(
					"Could not initialize " + getRepositoryIfc().getCanonicalName() + " for name '" + name + "'", ex);
		}
	}

	public void unloadOldBeans() {
		List<BeanConfig> beanConfigs = new ArrayList<>(kernel.getDependencyManager().getBeanConfigs());
		for (BeanConfig bc : beanConfigs) {
			if (bc.getBeanName().startsWith("repo-")) {
				try {
					Integer pos = Integer.parseInt(bc.getBeanName().replace("repo-", ""));
					if (getRepositoryPoolClassName() == null || pos >= poolSize) {
						kernel.unregister(bc.getBeanName());
					}
				} catch (NumberFormatException | DBInitException ex) {
					// this is not instance create by us, ignoring
				}
			}
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
				if (it instanceof MDPoolBean) {
					iter.remove();
				}
			}
		}
		HashSet<A> toInitialize = new HashSet<A>(instances);
		if (this.instances != null) {
			toInitialize.removeAll(this.instances);
		}

		if (!toInitialize.isEmpty()) {
			Queue<ForkJoinTask<A>> tasks = new ArrayDeque<>();
			final ForkJoinPool pool = new ForkJoinPool(Math.min(toInitialize.size(), 128));
			for (A repo : toInitialize) {
				tasks.offer(pool.submit(() -> {
					try {
						initRepository(repo);
					} catch (RepositoryException ex) {
						if (skipInitializationErrors) {
							// maybe we should not ignore this error but delay initialization and await successful database initialization?
							// or maybe we should unload this bean totally as it is not working as it should?
							// Two things to consider:
							// * initialization during bootstrap
							// * initialization during reconfiguration of server
							Logger.getLogger(this.getClass().getCanonicalName())
									.log(Level.WARNING,
										 "Could not initialize " + repo.getClass().getCanonicalName() + " for name '" +
												 name + "'", ex);
						} else {
							throw new RuntimeException(
									"Could not initialize " + repo.getClass().getCanonicalName() + " for name '" +
											name + "'", ex);
						}
					}
					return repo;
				}));
			}
			ForkJoinTask<A> task;
			while ((task = tasks.poll()) != null) {
				A repo = task.join();
				if (repository instanceof RepositoryPool && !(repo instanceof RepositoryPool)) {
					((RepositoryPool<A>) repository).addRepo(repo);
				}
			}
			pool.shutdown();
		}

		this.instances = instances;
	}

	public void setMdPool(MDPoolBean<A, B> mdPool) {
		if (mdPool != null && this.repository != null) {
			mdPool.addRepo(name, this.repository);
			if ("default".equals(name)) {
				mdPool.setDefault(this.repository);
			}
		}
		this.mdPool = mdPool;
	}

	/**
	 * Get interface to which instances initialized by this config bean must conform to.
	 *
	 * @return interface
	 */
	protected abstract Class<? extends A> getRepositoryIfc();

	/**
	 * Get name of a pool which should be used if any.
	 *
	 * @return class name
	 *
	 * @throws DBInitException
	 */
	protected abstract String getRepositoryPoolClassName() throws DBInitException;

	/**
	 * Method used to initialize provided instance
	 *
	 * @param repo
	 *
	 * @throws RepositoryException
	 */
	protected abstract void initRepository(A repo) throws RepositoryException;

	/**
	 * Get class name to initialize as repository
	 *
	 * @return
	 *
	 * @throws DBInitException
	 */
	protected String getRepositoryClassName() throws DBInitException {
		if (cls != null) {
			return cls;
		}
		return RepositoryFactory.getRepoClassName(getRepositoryIfc(), uri);
	}

	protected String getUri() {
		return uri;
	}

	protected A getRepository() {
		return repository;
	}

	public void setRepository(A repo) {
		this.repository = repo;
		if (repo != null) {
			if (instances != null) {
				for (A instance : instances) {
					if (repository instanceof RepositoryPool && !(instance instanceof RepositoryPool)) {
						((RepositoryPool) repository).addRepo(instance);
					}
				}
			}
		}

		if (mdPool != null) {
			if (repo != null) {
				mdPool.addRepo(name, repo);
			} else {
				mdPool.removeRepo(name);
			}
			if ("default".equals(name)) {
				mdPool.setDefault(repo);
			}
		}
	}
}