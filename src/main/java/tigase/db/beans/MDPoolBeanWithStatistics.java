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
import tigase.kernel.beans.config.ConfigField;
import tigase.stats.ComponentStatisticsProvider;
import tigase.stats.StatisticsInvocationHandler;
import tigase.stats.StatisticsList;

import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * Class extends MDPoolBean class by adding support for statistics gathering for every managed repository.
 *
 * Created by andrzej on 14.12.2016.
 */
public abstract class MDPoolBeanWithStatistics<S extends Repository, T extends MDPoolConfigBean<S, T>>
		extends MDPoolBean<S, T>
		implements ComponentStatisticsProvider {

	private ConcurrentHashMap<String, StatisticsInvocationHandler<S>> handlers = new ConcurrentHashMap<>();
	private ConcurrentHashMap<String, S> repos = new ConcurrentHashMap<>();
	private ConcurrentHashMap<String, S> reposProxy = new ConcurrentHashMap<>();

	private S def;
	private S defProxy;

	@ConfigField(desc = "Enable statistics", alias = "statistics")
	private boolean statisticsEnabled = false;

	private final Class<S> repoClazz;

	public MDPoolBeanWithStatistics(Class<S> repoClazz) {
		this.repoClazz = repoClazz;
	}

	public void addRepo(String name, S repo) {
		synchronized (this) {
			if (statisticsEnabled) {
				wrapInProxy(name, repo);
			}
			repos.put(name, repo);
		}
	}

	public S removeRepo(String domain) {
		synchronized (this) {
			S repo = repos.remove(domain);
			if (statisticsEnabled) {
				reposProxy.remove(domain);
			}
			return repo;
		}
	}

	public Collection<String> getDomainsList() {
		return Collections.unmodifiableCollection(repos.keySet());
	}

	public S getDefaultRepository() {
		return statisticsEnabled ? defProxy : def;
	}

	public Stream<S> repositoriesStream() {
		if (statisticsEnabled) {
			return reposProxy.values().stream();
		} else {
			return repos.values().stream();
		}
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

	public void setDefault(S repo) {
		def = repo;
		defProxy = reposProxy.get("default");
	}

	@Override
	public void getStatistics(String compName, StatisticsList list) {
		handlers.values().forEach(handler -> {
			handler.getStatistics(compName, null, list);
		});
	}

	public void setStatisticsEnabled(boolean value) {
		if (this.statisticsEnabled != value) {
			synchronized (this) {
				if (value) {
					repos.forEach(this::wrapInProxy);
				}
				this.statisticsEnabled = value;
				if (!value) {
					reposProxy.clear();
					defProxy = null;
				}
			}
		}
	}

	public S getRepo(String domain) {
		if (statisticsEnabled) {
			if (domain == null) {
				return defProxy;
			}
			S result = reposProxy.get(domain);

			if (result == null) {
				result = defProxy;
			}

			return result;

		} else {
			if (domain == null) {
				return def;
			}
			S result = repos.get(domain);

			if (result == null) {
				result = def;
			}

			return result;
		}
	}

	public void wrapInProxy(String name, S repo) {
		StatisticsInvocationHandler handler = new StatisticsInvocationHandler(name, repo, repoClazz);
		S proxy = (S) Proxy.newProxyInstance(repo.getClass().getClassLoader(), new Class[]{repoClazz}, handler);
		handlers.put(name, handler);
		reposProxy.put(name, proxy);
	}

}
