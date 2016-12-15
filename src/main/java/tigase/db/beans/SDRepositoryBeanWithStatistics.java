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

/**
 * Extended version of SDRepositoryBean class with support for statistics gathering.
 *
 * Created by andrzej on 15.12.2016.
 */
public abstract class SDRepositoryBeanWithStatistics<T extends DataSourceAware> extends SDRepositoryBean<T> implements
																											ComponentStatisticsProvider {

	private StatisticsInvocationHandler<T> handler;
	private T repoProxy;

	private final Class<T> repoClazz;

	@ConfigField(desc = "Enable statistics", alias = "statistics")
	private boolean statisticsEnabled = false;

	public SDRepositoryBeanWithStatistics(Class<T> repoClazz) {
		this.repoClazz = repoClazz;
	}

	@Override
	public void everyHour() {
		if (statisticsEnabled) {
			handler.everyHour();
		}
	}

	@Override
	public void everyMinute() {
		if (statisticsEnabled) {
			handler.everyMinute();
		}
	}

	@Override
	public void everySecond() {
		if (statisticsEnabled) {
			handler.everySecond();
		}
	}

	@Override
	protected T getRepository() {
		if (statisticsEnabled) {
			return repoProxy;
		}
		return super.getRepository();
	}

	@Override
	public void setRepository(T repository) {
		if (statisticsEnabled) {
			wrapInProxy(repository);
		}
		super.setRepository(repository);
	}

	@Override
	public void getStatistics(String compName, StatisticsList list) {
		if (handler != null) {
			handler.getStatistics(compName, getName(), list);
		}
	}

	public void setStatisticsEnabled(boolean value) {
		if (this.statisticsEnabled != value) {
			synchronized (this) {
				if (value) {
					wrapInProxy(super.getRepository());
				}
				this.statisticsEnabled = value;
				if (!value) {
					repoProxy = null;
				}
			}
		}
	}

	public void wrapInProxy(T repo) {
		handler = new StatisticsInvocationHandler(getDataSourceName(), repo, repoClazz);
		repoProxy = (T) Proxy.newProxyInstance(repo.getClass().getClassLoader(), new Class[]{repoClazz}, handler);
	}

}