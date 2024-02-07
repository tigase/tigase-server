/*
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
package tigase.db.util.importexport;

import tigase.component.exceptions.RepositoryException;
import tigase.db.DataSource;
import tigase.db.DataSourceAware;
import tigase.db.jdbc.DataRepositoryImpl;
import tigase.db.util.SchemaManager;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DataSourceHelper {

	private final Map<String, DataSource> dataSourceMap = new ConcurrentHashMap<>();
	private final Map<String, SchemaManager.DataSourceInfo> dataSourceInfos;

	public DataSourceHelper(List<SchemaManager.DataSourceInfo> dataSourceInfos) {
		this.dataSourceInfos = dataSourceInfos.stream()
				.collect(Collectors.toMap(SchemaManager.DataSourceInfo::getName, Function.identity()));
	}

	public DataSource getDefault() throws RepositoryException {
		return get("default");
	}

	public DataSource get(String name) throws RepositoryException {
		SchemaManager.DataSourceInfo info = this.dataSourceInfos.get(name);
		if (info == null) {
			int idx = name.indexOf('.');
			if (idx > 0) {
				return get(name.substring(idx + 1));
			}
			return getDefault();
		} else {
			return get(info);
		}
	}

	public DataSource get(SchemaManager.DataSourceInfo dataSourceInfo) throws RepositoryException {
		DataSource dataSource = dataSourceMap.get(dataSourceInfo.getName());
		if (dataSource == null) {
			dataSource = new DataRepositoryImpl();
			dataSource.initialize(dataSourceInfo.getResourceUri());
			dataSourceMap.put(dataSourceInfo.getName(), dataSource);
		}
		return dataSource;
	}

	public <R> R createRepository(SchemaManager.RepoInfo repoInfo)
			throws RepositoryException, InstantiationException, IllegalAccessException {
		DataSource dataSource = get(repoInfo.getDataSource().getName());
		R repo = ((Class<? extends R>) repoInfo.getImplementation()).newInstance();
		((DataSourceAware) repo).setDataSource(dataSource);
		return repo;
	}
}
