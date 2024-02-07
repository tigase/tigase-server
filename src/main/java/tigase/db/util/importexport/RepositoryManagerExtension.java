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

import tigase.kernel.core.Kernel;
import tigase.util.ui.console.CommandlineParameter;
import tigase.xmpp.jid.BareJID;

import java.io.Writer;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Stream;

public interface RepositoryManagerExtension {

	default Stream<CommandlineParameter> getExportParameters() {
		return Stream.empty();
	}

	default Stream<CommandlineParameter> getImportParameters() {
		return Stream.empty();
	}

	default void initialize(Kernel kernel, DataSourceHelper dataSourceHelper,
							RepositoryHolder repositoryHolder, Path rootPath) {
	}

	void exportDomainData(String domain, Writer writer) throws Exception;

	void exportUserData(Path userDirPath, BareJID user, Writer writer) throws Exception;

	default ImporterExtension startImportDomainData(String domain, String name,
													Map<String, String> attrs) throws Exception {
		return null;
	}

	default ImporterExtension startImportUserData(BareJID userJid, String name,
												  Map<String, String> attrs) throws Exception {
		return null;
	}

}
