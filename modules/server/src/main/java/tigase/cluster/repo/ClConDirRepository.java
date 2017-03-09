/*
 * ClConDirRepository.java
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2013 "Tigase, Inc." <office@tigase.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
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
 */



/*
* To change this template, choose Tools | Templates
* and open the template in the editor.
 */
package tigase.cluster.repo;

//~--- JDK imports ------------------------------------------------------------

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import tigase.db.DBInitException;
import tigase.db.Repository;

/**
 *
 * @author kobit
 */
@Repository.Meta( supportedUris = { "file://.*" } )
public class ClConDirRepository
				extends ClConConfigRepository
				implements ClusterRepoConstants {
	/** Field description */
	public static final String REPO_FILE_EXTENSION = ".rep";

	/** Field description */
	public static final String REPO_URI_DB_DEF_VAL = "etc/";

	/**
	 * Private logger for class instances.
	 */
	private static final Logger log = Logger.getLogger(ClConDirRepository.class.getName());

	//~--- fields ---------------------------------------------------------------

	private File      repo_dir  = new File(REPO_URI_DB_DEF_VAL);
	private DirFilter dirFilter = new DirFilter();

	@Override
	public void destroy() {
		// Nothing to do here
		super.destroy();
	}
	
	//~--- get methods ----------------------------------------------------------

	@Override
	public void getDefaults(Map<String, Object> defs, Map<String, Object> params) {
		super.getDefaults(defs, params);
		defs.put(REPO_URI_PROP_KEY, REPO_URI_DB_DEF_VAL);
	}

	@Override
	public void initRepository(String conn_str, Map<String, String> params) throws DBInitException {
		// Nothing to do here
		super.initRepository(conn_str, params);
	}
	
	//~--- set methods ----------------------------------------------------------

	@Override
	public void setProperties(Map<String, Object> properties) {
		super.setProperties(properties);
		repo_dir = new File((String) properties.get(REPO_URI_PROP_KEY));
	}

	//~--- methods --------------------------------------------------------------

	@Override
	public void storeItem(ClusterRepoItem item) {
		try {
			File           file = new File(repo_dir, item.getHostname() + REPO_FILE_EXTENSION);
			BufferedWriter bw   = new BufferedWriter(new FileWriter(file, false));

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
				BufferedReader br   = new BufferedReader(new FileReader(file));
				String         data = br.readLine();

				br.close();

				ClusterRepoItem item = getItemInstance();

				item.initFromPropertyString(data);
				itemLoaded(item);
			}
		} catch (IOException e) {
			log.log(Level.WARNING, "Problem getting elements from DB: ", e);
		}
	}

	//~--- inner classes --------------------------------------------------------

	private class DirFilter
					implements FileFilter {
		/**
		 * Method description
		 *
		 *
		 * @param file
		 *
		 * 
		 */
		@Override
		public boolean accept(File file) {
			return file.isFile() && file.getName().endsWith(REPO_FILE_EXTENSION);
		}
	}
}


//~ Formatted in Tigase Code Convention on 13/03/11
