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
/*
* To change this template, choose Tools | Templates
* and open the template in the editor.
 */
package tigase.cluster.repo;

import tigase.annotations.TigaseDeprecated;
import tigase.db.DBInitException;
import tigase.db.Repository;

import java.io.*;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author kobit
 */
@Repository.Meta(supportedUris = {"file://.*"})
public class ClConDirRepository
		extends ClConConfigRepository
		implements ClusterRepoConstants {

	public static final String REPO_FILE_EXTENSION = ".rep";

	public static final String REPO_URI_DB_DEF_VAL = "etc/";

	private static final Logger log = Logger.getLogger(ClConDirRepository.class.getName());

	private DirFilter dirFilter = new DirFilter();
	private File repo_dir = new File(REPO_URI_DB_DEF_VAL);

	@Override
	public void destroy() {
		// Nothing to do here
		super.destroy();
	}

	@Override
	public void getDefaults(Map<String, Object> defs, Map<String, Object> params) {
		super.getDefaults(defs, params);
		defs.put(REPO_URI_PROP_KEY, REPO_URI_DB_DEF_VAL);
	}

	@Override
	@Deprecated
	@TigaseDeprecated(since = "8.0.0")
	public void initRepository(String conn_str, Map<String, String> params) throws DBInitException {
		// Nothing to do here
		super.initRepository(conn_str, params);
	}

	@Override
	public void setProperties(Map<String, Object> properties) {
		super.setProperties(properties);
		repo_dir = new File((String) properties.get(REPO_URI_PROP_KEY));
	}

	@Override
	public void storeItem(ClusterRepoItem item) {
		try {
			File file = new File(repo_dir, item.getHostname() + REPO_FILE_EXTENSION);
			BufferedWriter bw = new BufferedWriter(new FileWriter(file, false));

			bw.write(item.toPropertyString());
			bw.newLine();
			bw.close();
		} catch (IOException e) {
			log.log(Level.WARNING, "Problem getting elements from DB: ", e);
		}
	}

	@Override
	public void reload() {
		super.reload();
		try {
			File[] files = repo_dir.listFiles(dirFilter);

			for (File file : files) {
				BufferedReader br = new BufferedReader(new FileReader(file));
				String data = br.readLine();

				br.close();

				ClusterRepoItem item = getItemInstance();

				item.initFromPropertyString(data);
				itemLoaded(item);
			}
		} catch (IOException e) {
			log.log(Level.WARNING, "Problem getting elements from DB: ", e);
		}
		if (auto_remove_obsolete_items) {
			removeObsoloteItems(5000);
		}
	}

	private class DirFilter
			implements FileFilter {

		@Override
		public boolean accept(File file) {
			return file.isFile() && file.getName().endsWith(REPO_FILE_EXTENSION);
		}
	}
}

