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
import tigase.db.util.SchemaManager;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class RepositoryHolder {

	private record RepoCacheKey(Class<?> ifc, String name) {

	}

	private final DataSourceHelper dataSourceHelper;
	private final List<SchemaManager.RepoInfo> allRepoInfos;
	private final Map<RepoCacheKey, Object> repoCache = new ConcurrentHashMap<>();
	private final Map<Class, Function> prepFuncs = new ConcurrentHashMap<>();

	public RepositoryHolder(DataSourceHelper dataSourceHelper,
							List<SchemaManager.RepoInfo> allRepoInfos) {
		this.dataSourceHelper = dataSourceHelper;
		this.allRepoInfos = allRepoInfos;
	}

	public <X> X getDefaultRepository(Class<X> ifc)
			throws RepositoryException, InstantiationException, IllegalAccessException {
		return getRepository(ifc, "default");
	}

	public <X> X getRepository(Class<X> ifc, String name)
			throws RepositoryException, InstantiationException, IllegalAccessException {
		RepoCacheKey key = new RepoCacheKey(ifc, name);
		X repo = (X) repoCache.get(key);
		if (repo == null) {
			List<SchemaManager.RepoInfo> matchingClasses = allRepoInfos.stream()
					.filter(repoInfo -> ifc.isAssignableFrom(repoInfo.getImplementation()))
					.toList();
			SchemaManager.RepoInfo repoInfo = findRepoInfo(matchingClasses, name).or(
					() -> findRepoInfo(matchingClasses, "default")).orElseThrow();
			repo = prepareRepository((X) dataSourceHelper.createRepository(repoInfo));
			repoCache.put(key, repo);
		}
		return repo;
	}

	public <X> void registerPrepFn(Class<X> ifc, Function<X, X> func) {
		prepFuncs.put(ifc, func);
	}

	protected <X> X prepareRepository(X repo)
			throws RepositoryException, InstantiationException, IllegalAccessException {
		for (Map.Entry<Class, Function> e : prepFuncs.entrySet()) {
			if (e.getKey().isAssignableFrom(repo.getClass())) {
				repo = (X) e.getValue().apply(repo);
			}
		}
		return repo;
	}

	private Optional<SchemaManager.RepoInfo> findRepoInfo(List<SchemaManager.RepoInfo> repoInfos, String name) {
		return repoInfos.stream().filter(repoInfo -> name.equals(repoInfo.getDataSource().getName())).findFirst();
	}
}
