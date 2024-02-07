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
import tigase.kernel.core.BeanConfig;
import tigase.kernel.core.Kernel;

import java.io.Writer;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Logger;

public abstract class RepositoryManagerExtensionBase
		implements RepositoryManagerExtension {

	private static final Logger log = Logger.getLogger(RepositoryManagerExtensionBase.class.getSimpleName());

	private Kernel kernel;
	private DataSourceHelper dataSourceHelper;
	private RepositoryHolder repositoryHolder;
	private Path rootPath;

	@Override
	public void initialize(Kernel kernel, DataSourceHelper dataSourceHelper,
						   RepositoryHolder repositoryHolder, Path rootPath) {
		this.kernel = kernel;
		this.dataSourceHelper = dataSourceHelper;
		this.repositoryHolder = repositoryHolder;
		this.rootPath = rootPath;
	}

	public <X> X getRepository(Class<X> ifc, String name)
			throws RepositoryException, InstantiationException, IllegalAccessException {
		return repositoryHolder.getRepository(ifc, name);
	}

	public List<String> getNamesOfComponent(Class<?> clazz) {
		return kernel.getDependencyManager()
				.getBeanConfigs()
				.stream()
				.filter(beanConfig -> beanConfig.getState() != BeanConfig.State.inactive)
				.filter(beanConfig -> clazz.isAssignableFrom(beanConfig.getClazz()))
				.map(BeanConfig::getBeanName)
				.toList();
	}

	public Path getRootPath() {
		return rootPath;
	}

	protected void exportInclude(Writer parentWriter, Path filePath,
								 RepositoryManager.ThrowingConsumer<Writer> consumer) throws Exception {
		Exporter.exportInclude(parentWriter, rootPath, filePath, consumer);
	}
}
