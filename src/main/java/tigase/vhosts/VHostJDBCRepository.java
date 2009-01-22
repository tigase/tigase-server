/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2008 "Artur Hefczyc" <artur.hefczyc@tigase.org>
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 * 
 * $Rev$
 * Last modified by $Author$
 * $Date$
 */

package tigase.vhosts;

import java.util.Map;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;
import tigase.db.RepositoryFactory;
import tigase.db.UserExistsException;
import tigase.db.UserRepository;
import tigase.xml.DomBuilderHandler;
import tigase.xml.Element;
import tigase.xml.SimpleParser;
import tigase.xml.SingletonFactory;
import static tigase.conf.Configurable.*;

/**
 * This implementation stores virtual domains in the UserRepository database.
 * It loads initial settings and virtual hosts from the configuration file
 * and then loads more vhosts from the database. Virtual domains from the
 * database can overwrite (disable) vhosts loaded from the configuration file.
 *
 * This implementation keeps all virtual hosts and their parameters in a single
 * database field. This might not be very efficient if you want to manager big
 * number of virtual domains. It is sufficient for hundreds of vhosts. If you
 * need thousands of VHosts support I advice to implement this storage in
 * more efficient way using separate database tables instead of UserRepository.
 * Please note there is a limit of about 300 vhosts if you use Derby database.
 *
 *
 * Created: Nov 29, 2008 2:32:48 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class VHostJDBCRepository extends VhostConfigRepository {

	private static final Logger log =
					Logger.getLogger(VHostJDBCRepository.class.getName());

	public static final String VHOST_REPO_CLASS_PROP_KEY = "vhost-repo-class";
	public static final String VHOST_REPO_URI_PROP_KEY = "vhost-repo-uri";

	private String vhost_user = "vhost-manager";
	private String vhost_list_pkey = "vhosts-lists";

	private UserRepository repo = null;

	@Override
	public void getDefaults(Map<String, Object> defs,
					Map<String, Object> params) {
		// Something to initialize database with, in case it is empty
		// Otherwise the server would not work at all with empty VHost database
		super.getDefaults(defs, params);

		// Now the real VHost data storage:
		String repo_class = DERBY_REPO_CLASS_PROP_VAL;
		String repo_uri = DERBY_REPO_URL_PROP_VAL;
		if (params.get(GEN_USER_DB) != null) {
			repo_class = (String)params.get(GEN_USER_DB);
		}
		if (params.get(GEN_USER_DB_URI) != null) {
			repo_uri = (String)params.get(GEN_USER_DB_URI);
		}
		defs.put(VHOST_REPO_CLASS_PROP_KEY, repo_class);
		defs.put(VHOST_REPO_URI_PROP_KEY, repo_uri);
	}

	@Override
	public void setProperties(Map<String, Object> properties) {
		// Let's load VHosts from configuration first. Later we can overwrite
		// them with vhosts settings in the database.
		super.setProperties(properties);

		repo = (UserRepository) properties.get(SHARED_USER_REPO_PROP_KEY);
		if (repo != null) {
			log.config("Using shared repository instance.");
		} else {
			String repo_class = (String) properties.get(VHOST_REPO_CLASS_PROP_KEY);
			String repo_uri = (String) properties.get(VHOST_REPO_URI_PROP_KEY);
			try {
				repo = RepositoryFactory.getUserRepository(vhost_user, repo_class,
								repo_uri, null);
			} catch (Exception e) {
				log.log(Level.SEVERE, "Can't initialize VHost repository", e);
				repo = null;
			}
		}
		if (repo != null) {
			// If this is the first run of the VHost manager the database might not
			// be properly initialized yet....
			try {
				repo.addUser(vhost_user);
			} catch (UserExistsException e) {
				// This is expected when the VHost manager was already running on
				// this databaseso and can be ignored.
			} catch (Exception e) {
				// This is not expected so let's signal an error:
				log.log(Level.SEVERE,
								"Problem with adding 'vhost-manager' user to the database", e);
			}
			reload();
		}
	}

	@Override
	public void reload() {
		super.reload();
		try {
			// It is now time to load all VHost settings from the database:
			String vhosts_list = repo.getData(vhost_user, vhost_list_pkey);
			if (vhosts_list != null && !vhosts_list.isEmpty()) {
				DomBuilderHandler domHandler = new DomBuilderHandler();
				SimpleParser parser = SingletonFactory.getParserInstance();
				parser.parse(domHandler, vhosts_list.toCharArray(), 0,
								vhosts_list.length());
				Queue<Element> elems = domHandler.getParsedElements();
				if (elems != null && elems.size() > 0) {
					for (Element elem : elems) {
						VHostItem item = new VHostItem(elem);
						vhosts.put(item.getVhost(), item);
					}
				}
			}
		} catch (Exception ex) {
			log.log(Level.SEVERE,
							"Problem with loading VHosts list from the database.", ex);
		}
	}

	@Override
	public void store() {
		super.store();
		StringBuilder sb = new StringBuilder();
		for (VHostItem vhost : vhosts.values()) {
			sb.append(vhost.toXML().toString());
		}
		try {
			repo.setData(vhost_user, vhost_list_pkey, sb.toString());
		} catch (Exception e) {
			log.log(Level.SEVERE, "Error storing VHosts list in the repository", e);
		}
	}

}
