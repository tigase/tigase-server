/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2017 "Tigase, Inc." <office@tigase.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3 of the License,
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
 */

package tigase.db.xml;

import tigase.component.exceptions.RepositoryException;
import tigase.db.DBInitException;
import tigase.db.DataSource;
import tigase.db.Repository;
import tigase.xml.db.XMLDB;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by andrzej on 04.04.2017.
 */
@Repository.Meta( supportedUris = {"memory://.*" } )
public class XMLDataSource implements DataSource {

	private static final Logger log = Logger.getLogger(XMLDataSource.class.getCanonicalName());

	private String resource_uri;
	private XMLDB xmldb;

	@Override
	public String getResourceUri() {
		return resource_uri;
	}

	@Override
	public void initialize(String file) throws RepositoryException {
		initRepository(file, new HashMap<>());
	}

	@Override
	@Deprecated
	public void initRepository(String file, Map<String, String> params) throws DBInitException {
		this.resource_uri = file;
		String file_name = file;

		log.log(Level.FINEST, "Initializing repository, file: {0}, params: {1}",
				new Object[] { file, params } );

		try {
			int idx = file.indexOf("?");

			if (idx > 0) {
				file_name = file.substring(0, idx);
			}

			xmldb = new XMLDB(file_name);
		} catch (Exception e) {
			log.warning("Can not open existing user repository file, creating new one, " + e);
			xmldb = XMLDB.createDB(file_name, "users", "user");
		}      // end of try-catch
	}

	public XMLDB getXMLDB() {
		return xmldb;
	}
}
