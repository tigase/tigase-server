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

import tigase.db.DataSourceAware;
import tigase.kernel.beans.config.ConfigField;
import tigase.stats.StatisticsInvocationHandler;
import tigase.stats.StatisticsList;
import tigase.stats.ComponentStatisticsProvider;

import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Extended version of MDRepositoryBean class with support for statistics gathering.
 *
 * Created by andrzej on 15.12.2016.
 */
public abstract class MDRepositoryBeanWithStatistics<T extends DataSourceAware>
		extends MDRepositoryBean<T>
		implements ComponentStatisticsProvider {

	private ConcurrentHashMap<String, StatisticsInvocationHandler<T>> handlers = new ConcurrentHashMap<>();
	private ConcurrentHashMap<String, T> reposProxy = new ConcurrentHashMap<>();

	@ConfigField(desc = "Enable statistics", alias = "statistics")
	private boolean statisticsEnabled = false;

	private final Class<?>[] repoInterfaces;

	public MDRepositoryBeanWithStatistics(Class<?>... repoClazz) {
		this.repoInterfaces = repoClazz;
	}

	@Override
	public void everyHour() {
		if (statisticsEnabled) {
			handlers.values().forEach(StatisticsInvocationHandler::everyHour);
		}
	}

	@Override
	public void everyMinute() {
		if (statisticsEnabled) {
			handlers.values().forEach(StatisticsInvocationHandler::everyMinute);
		}
	}

	@Override
	public void everySecond() {
		if (statisticsEnabled) {
			handlers.values().forEach(StatisticsInvocationHandler::everySecond);
		}
	}

	@Override
	protected T getRepository(String domain) {
		if (statisticsEnabled) {
			T repo = reposProxy.get(aliases.getOrDefault(domain, domain));
			if (repo == null) {
				repo = reposProxy.get("default");
			}
			return repo;
		}
		return super.getRepository(domain);
	}

	@Override
	protected Map<String, T> getRepositories() {
		if (statisticsEnabled) {
			return Collections.unmodifiableMap(reposProxy);
		}
		return super.getRepositories();
	}

	@Override
	protected void updateDataSourceAware(String domain, T newRepo, T oldRepo) {
		if (statisticsEnabled && newRepo != null) {
			wrapInProxy(domain, newRepo);
		} else {
			reposProxy.remove(domain);
		}
		super.updateDataSourceAware(domain, newRepo, oldRepo);
	}

	@Override
	public void getStatistics(String compName, StatisticsList list) {
		handlers.values().forEach(handler -> {
			handler.getStatistics(compName, getName(), list);
		});
	}

	public void setStatisticsEnabled(boolean value) {
		if (this.statisticsEnabled != value) {
			synchronized (this) {
				if (value) {
					super.getRepositories().forEach(this::wrapInProxy);
				}
				this.statisticsEnabled = value;
				if (!value) {
					reposProxy.clear();
				}
			}
		}
	}

	public void wrapInProxy(String name, T repo) {
		StatisticsInvocationHandler handler = new StatisticsInvocationHandler(name, repo, repoInterfaces);
		T proxy = (T) Proxy.newProxyInstance(repo.getClass().getClassLoader(), repoInterfaces, handler);
		handlers.put(name, handler);
		reposProxy.put(name, proxy);
	}
}
