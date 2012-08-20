/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2012 "Artur Hefczyc" <artur.hefczyc@tigase.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License.
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

package tigase.db;

//~--- non-JDK imports --------------------------------------------------------

import static tigase.conf.Configurable.*;

//~--- JDK imports ------------------------------------------------------------

import java.sql.SQLException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

//~--- classes ----------------------------------------------------------------

/**
 * Describe class RepositoryFactory here.
 *
 *
 * Created: Tue Oct 24 22:13:52 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public abstract class RepositoryFactory {

	/** Field description */
	public static final String DATA_REPO_CLASS_PROP_KEY = "data-repo";

	/** Field description */
	public static final String DATA_REPO_CLASS_PROP_VAL = "tigase.db.jdbc.DataRepositoryImpl";

	/** Field description */
	public static final String DATA_REPO_POOL_CLASS_PROP_KEY = "data-repo-pool";

	/** Field description */
	public static final String DATA_REPO_POOL_CLASS_PROP_VAL = "tigase.db.DataRepositoryPool";

	/** Field description */
	public static final String DATA_REPO_POOL_SIZE_PROP_KEY = "data-repo-pool-size";

	/** Field description */
	public static final int DATA_REPO_POOL_SIZE_PROP_VAL = 10;

	/** Field description */
	public static final String AUTH_REPO_CLASS_PROP_KEY = "auth-repo-class";

	/** Field description */
	public static final String AUTH_REPO_CLASS_PROP_VAL = "tigase.db.jdbc.TigaseCustomAuth";

	/** Field description */
	public static final String AUTH_REPO_POOL_CLASS_PROP_KEY = "auth-repo-pool";

	/** Field description */
	public static final String AUTH_REPO_POOL_CLASS_PROP_DEF = "tigase.db.AuthRepositoryPool";

	/** Field description */
	public static final String AUTH_REPO_POOL_CLASS_PROP_VAL = null;

	/** Field description */
	public static final String AUTH_REPO_POOL_SIZE_PROP_KEY = "auth-repo-pool-size";

	/** Field description */
	public static final int AUTH_REPO_POOL_SIZE_PROP_VAL = 10;

	/** Field description */
	public static final String USER_REPO_CLASS_PROP_KEY = "user-repo-class";

	/** Field description */
	public static final String USER_REPO_CLASS_PROP_VAL = "tigase.db.jdbc.JDBCRepository";

	/** Field description */
	public static final String USER_REPO_POOL_CLASS_PROP_KEY = "user-repo-pool";

	/** Field description */
	public static final String USER_REPO_POOL_CLASS_PROP_DEF = "tigase.db.UserRepositoryPool";

	/** Field description */
	public static final String USER_REPO_POOL_CLASS_PROP_VAL = null;

	/** Field description */
	public static final String USER_REPO_POOL_SIZE_PROP_KEY = "user-repo-pool-size";

	/** Field description */
	public static final int USER_REPO_POOL_SIZE_PROP_VAL = 10;
	private static ConcurrentMap<String, UserRepository> user_repos = new ConcurrentHashMap<String,
		UserRepository>(5);
	private static ConcurrentMap<String, AuthRepository> auth_repos = new ConcurrentHashMap<String,
		AuthRepository>(5);
	private static ConcurrentMap<String, DataRepository> data_repos = new ConcurrentHashMap<String,
		DataRepository>(10);

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param class_name
	 * @param resource
	 * @param params
	 *
	 * @return
	 *
	 * @throws ClassNotFoundException
	 * @throws DBInitException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 */
	public static AuthRepository getAuthRepository(String class_name, String resource,
			Map<String, String> params)
			throws ClassNotFoundException, InstantiationException, IllegalAccessException,
			DBInitException {
		String cls = class_name;

		if (cls == null) {
			cls = System.getProperty(AUTH_REPO_CLASS_PROP_KEY, AUTH_REPO_CLASS_PROP_VAL);
		}

		cls = getRepoClass(cls);

		AuthRepository repo = auth_repos.get(cls + resource);

		if (repo == null) {
			if (System.getProperty(AUTH_REPO_POOL_CLASS_PROP_KEY, AUTH_REPO_POOL_CLASS_PROP_VAL)
					!= null) {
				int repo_pool_size = Integer.getInteger(AUTH_REPO_POOL_SIZE_PROP_KEY,
					AUTH_REPO_POOL_SIZE_PROP_VAL);
				AuthRepositoryPool repo_pool =
					(AuthRepositoryPool) Class.forName(System.getProperty(AUTH_REPO_POOL_CLASS_PROP_KEY,
						AUTH_REPO_POOL_CLASS_PROP_VAL)).newInstance();

				repo_pool.initRepository(resource, params);

				for (int i = 0; i < repo_pool_size; i++) {
					repo = (AuthRepository) Class.forName(cls).newInstance();
					repo.initRepository(resource, params);
					repo_pool.addRepo(repo);
				}

				repo = repo_pool;
			} else {
				repo = (AuthRepository) Class.forName(cls).newInstance();
				repo.initRepository(resource, params);
			}

			auth_repos.put(cls + resource, repo);
		}

		return repo;
	}

	/**
	 * Method description
	 *
	 *
	 * @param class_name
	 * @param resource
	 * @param params
	 *
	 * @return
	 *
	 * @throws ClassNotFoundException
	 * @throws DBInitException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 * @throws SQLException
	 */
	public static DataRepository getDataRepository(String class_name, String resource,
			Map<String, String> params)
			throws ClassNotFoundException, InstantiationException, IllegalAccessException,
			DBInitException, SQLException {
		String cls = class_name;

		if (cls == null) {
			cls = System.getProperty(DATA_REPO_CLASS_PROP_KEY, DATA_REPO_CLASS_PROP_VAL);
		}

		DataRepository repo = data_repos.get(cls + resource);

		if (repo == null) {
			int repo_pool_size = Integer.getInteger(DATA_REPO_POOL_SIZE_PROP_KEY,
				DATA_REPO_POOL_SIZE_PROP_VAL);
			DataRepositoryPool repo_pool =
				(DataRepositoryPool) Class.forName(System.getProperty(DATA_REPO_POOL_CLASS_PROP_KEY,
					DATA_REPO_POOL_CLASS_PROP_VAL)).newInstance();

			repo_pool.initRepository(resource, params);

			for (int i = 0; i < repo_pool_size; i++) {
				repo = (DataRepository) Class.forName(cls).newInstance();
				repo.initRepository(resource, params);
				repo_pool.addRepo(repo);
			}

			repo = repo_pool;
			data_repos.put(cls + resource, repo);
		}

		return repo;
	}

	/**
	 * Method description
	 *
	 *
	 * @param repo_name
	 *
	 * @return
	 */
	public static String getRepoClass(String repo_name) {
		String result = repo_name;

		if (repo_name.equals("mysql")) {
			result = MYSQL_REPO_CLASS_PROP_VAL;
		}

		if (repo_name.equals("pgsql")) {
			result = PGSQL_REPO_CLASS_PROP_VAL;
		}

		if (repo_name.equals("derby")) {
			result = DERBY_REPO_CLASS_PROP_VAL;
		}

		if (repo_name.equals("tigase-custom-auth") || repo_name.equals("tigase-custom")
				|| repo_name.equals("custom-auth")) {
			result = TIGASE_CUSTOM_AUTH_REPO_CLASS_PROP_VAL;
		}

		if (repo_name.equals("tigase-auth")) {
			result = TIGASE_AUTH_REPO_CLASS_PROP_VAL;
		}

		if (repo_name.equals("drupal") || repo_name.equals("wp")) {
			result = DRUPALWP_REPO_CLASS_PROP_VAL;
		}

		if (repo_name.equals("libresource")) {
			result = LIBRESOURCE_REPO_CLASS_PROP_VAL;
		}

		return result;
	}

	/**
	 * Method description
	 *
	 *
	 * @param class_name
	 * @param resource
	 * @param params
	 *
	 * @return
	 *
	 * @throws ClassNotFoundException
	 * @throws DBInitException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 */
	public static UserRepository getUserRepository(String class_name, String resource,
			Map<String, String> params)
			throws ClassNotFoundException, InstantiationException, IllegalAccessException,
			DBInitException {
		String cls = class_name;

		if (cls == null) {
			cls = System.getProperty(USER_REPO_CLASS_PROP_KEY, USER_REPO_CLASS_PROP_VAL);
		}

		cls = getRepoClass(cls);

		UserRepository repo = user_repos.get(cls + resource);

		if (repo == null) {
			if (System.getProperty(USER_REPO_POOL_CLASS_PROP_KEY, USER_REPO_POOL_CLASS_PROP_VAL)
					!= null) {
				int repo_pool_size = Integer.getInteger(USER_REPO_POOL_SIZE_PROP_KEY,
					USER_REPO_POOL_SIZE_PROP_VAL);
				UserRepositoryPool repo_pool =
					(UserRepositoryPool) Class.forName(System.getProperty(USER_REPO_POOL_CLASS_PROP_KEY,
						USER_REPO_POOL_CLASS_PROP_VAL)).newInstance();

				repo_pool.initRepository(resource, params);

				for (int i = 0; i < repo_pool_size; i++) {
					repo = (UserRepository) Class.forName(cls).newInstance();
					repo.initRepository(resource, params);
					repo_pool.addRepo(repo);
				}

				repo = repo_pool;
			} else {
				repo = (UserRepository) Class.forName(cls).newInstance();
				repo.initRepository(resource, params);
			}

			user_repos.put(cls + resource, repo);
		}

		return repo;
	}
}    // RepositoryFactory


//~ Formatted in Sun Code Convention


//~ Formatted by Jindent --- http://www.jindent.com
