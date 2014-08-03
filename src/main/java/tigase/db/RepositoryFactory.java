/*
 * RepositoryFactory.java
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



package tigase.db;

//~--- JDK imports ------------------------------------------------------------

import java.sql.SQLException;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.LinkedHashMap;
import java.util.Map;
import tigase.osgi.ModulesManagerImpl;

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
	public static final String AUTH_DOMAIN_POOL_CLASS = "--auth-domain-repo-pool";

	/** Field description */
	public static final String AUTH_DOMAIN_POOL_CLASS_PROP_KEY = "auth-domain-repo-pool";

	/** Field description */
	public static final String AUTH_DOMAIN_POOL_CLASS_PROP_VAL =
			"tigase.db.AuthRepositoryMDImpl";

	/** Field description */
	public static final String AUTH_REPO_CLASS_PROP_KEY = "auth-repo-class";

	/** Field description */
	public static final String AUTH_REPO_CLASS_PROP_VAL = "tigase.db.jdbc.TigaseCustomAuth";

	/** Field description */
	public static final String AUTH_REPO_DOMAINS_PROP_KEY = "auth-repo-domains";

	/** Field description */
	public static final String AUTH_REPO_PARAMS_NODE = "auth-repo-params";

	/** Field description */
	public static final String AUTH_REPO_POOL_CLASS = "--auth-repo-pool";

	/** Field description */
	public static final String AUTH_REPO_POOL_CLASS_PROP_DEF =
			"tigase.db.AuthRepositoryPool";

	/** Field description */
	public static final String AUTH_REPO_POOL_CLASS_PROP_KEY = "auth-repo-pool";

	/** Field description */
	public static final String AUTH_REPO_POOL_CLASS_PROP_VAL = null;

	/** Field description */
	public static final String AUTH_REPO_POOL_SIZE = "--auth-repo-pool-size";

	/** Field description */
	public static final String AUTH_REPO_POOL_SIZE_PROP_KEY = "auth-repo-pool-size";

	// AuthRepository properties

	/** Field description */
	public static final int AUTH_REPO_POOL_SIZE_PROP_VAL = 10;

	/** Field description */
	public static final String AUTH_REPO_URL_PROP_KEY = "auth-repo-url";

	/** Field description */
	public static final String DATA_REPO_CLASS_PROP_KEY = "data-repo";

	/** Field description */
	public static final String DATA_REPO_CLASS_PROP_VAL =
			"tigase.db.jdbc.DataRepositoryImpl";

	/** Field description */
	public static final String DATA_REPO_POOL_CLASS_PROP_KEY = "data-repo-pool";

	/** Field description */
	public static final String DATA_REPO_POOL_CLASS_PROP_VAL =
			"tigase.db.DataRepositoryPool";

	/** Field description */
	public static final String DATA_REPO_POOL_SIZE = "--data-repo-pool-size";

	/** Field description */
	public static final String DATA_REPO_POOL_SIZE_PROP_KEY = "data-repo-pool-size";

	// DataRepository properties

	/** Field description */
	public static final int DATA_REPO_POOL_SIZE_PROP_VAL = 10;

	// repositories classes and URLs

	/** Field description */
	public static final String DERBY_REPO_CLASS_PROP_VAL = "tigase.db.jdbc.JDBCRepository";

	/** Field description */
	public static final String DERBY_REPO_URL_PROP_VAL =
			"jdbc:derby:tigase-derbydb;create=true";

	/** Field description */
	public static final String DRUPAL_REPO_URL_PROP_VAL =
			"jdbc:mysql://localhost/drupal?user=root&password=mypass";

	/** Field description */
	public static final String DRUPALWP_REPO_CLASS_PROP_VAL = "tigase.db.jdbc.DrupalWPAuth";

	/** Field description */
	public static final String DUMMY_REPO_CLASS_PROP_VAL = "tigase.db.DummyRepository";

	/** Field description */
	public static final String GEN_AUTH_DB = "--auth-db";

	/** Field description */
	public static final String GEN_AUTH_DB_URI = "--auth-db-uri";

	/** Field description */
	public static final String GEN_USER_DB = "--user-db";

	/** Field description */
	public static final String GEN_USER_DB_URI = "--user-db-uri";

	/** Field description */
	public static final String GEN_USER_DB_URI_PROP_KEY = "user-db-uri";

	/** Field description */
	public static final String LIBRESOURCE_REPO_CLASS_PROP_VAL =
			"tigase.db.jdbc.LibreSourceAuth";

	/** Field description */
	public static final String LIBRESOURCE_REPO_URL_PROP_VAL =
			"jdbc:postgresql://localhost/libresource?user=demo";

	/** Default MS SQL Server JDBC class */
	public static final String SQLSERVER_REPO_CLASS_PROP_VAL =
			"tigase.db.jdbc.JDBCRepository";

	/** Default MS SQL Server JDBC connection string */
	public static final String SQLSERVER_REPO_URL_PROP_VAL =
			"jdbc:sqlserver://localhost:1433;databaseName=tigasedb;user=tigase;password=tigase;schema=dbo";

	/** Field description */
	public static final String MYSQL_REPO_CLASS_PROP_VAL = "tigase.db.jdbc.JDBCRepository";

	/** Field description */
	public static final String MYSQL_REPO_URL_PROP_VAL =
			"jdbc:mysql://localhost/tigase?user=root&password=mypass";

	/** Field description */
	public static final String PGSQL_REPO_CLASS_PROP_VAL = "tigase.db.jdbc.JDBCRepository";

	/** Field description */
	public static final String PGSQL_REPO_URL_PROP_VAL =
			"jdbc:postgresql://localhost/tigase?user=tigase";

	/** Field description */
	public static final String SHARED_AUTH_REPO_PARAMS_PROP_KEY = "shared-auth-repo-params";

	/** Field description */
	public static final String SHARED_AUTH_REPO_PROP_KEY = "shared-auth-repo";

	/** Field description */
	public static final String SHARED_USER_REPO_PARAMS_PROP_KEY = "shared-user-repo-params";

	/** Field description */
	public static final String SHARED_USER_REPO_PROP_KEY = "shared-user-repo";

	/** Field description */
	public static final String TIGASE_AUTH_REPO_CLASS_PROP_VAL =
			"tigase.db.jdbc.TigaseAuth";

	/** Field description */
	public static final String TIGASE_AUTH_REPO_URL_PROP_VAL =
			"jdbc:mysql://localhost/tigasedb?user=tigase_user&password=mypass";

	/** Field description */
	public static final String TIGASE_CUSTOM_AUTH_REPO_CLASS_PROP_VAL =
			"tigase.db.jdbc.TigaseCustomAuth";

	/** Field description */
	public static final String USER_DOMAIN_POOL_CLASS = "--user-domain-repo-pool";

	/** Field description */
	public static final String USER_DOMAIN_POOL_CLASS_PROP_KEY = "user-domain-repo-pool";

	/** Field description */
	public static final String USER_DOMAIN_POOL_CLASS_PROP_VAL =
			"tigase.db.UserRepositoryMDImpl";

	/** Field description */
	public static final String USER_REPO_CLASS_PROP_KEY = "user-repo-class";

	/** Field description */
	public static final String USER_REPO_CLASS_PROP_VAL = "tigase.db.jdbc.JDBCRepository";

	/** Field description */
	public static final String USER_REPO_DOMAINS_PROP_KEY = "user-repo-domains";

	/** Field description */
	public static final String USER_REPO_PARAMS_NODE = "user-repo-params";

	/** Field description */
	public static final String USER_REPO_POOL_CLASS = "--user-repo-pool";

	/** Field description */
	public static final String USER_REPO_POOL_CLASS_PROP_DEF =
			"tigase.db.UserRepositoryPool";

	/** Field description */
	public static final String USER_REPO_POOL_CLASS_PROP_KEY = "user-repo-pool";

	/** Field description */
	public static final String USER_REPO_POOL_CLASS_PROP_VAL = null;

	/** Field description */
	public static final String USER_REPO_POOL_SIZE = "--user-repo-pool-size";

	/** Field description */
	public static final String USER_REPO_POOL_SIZE_PROP_KEY = "user-repo-pool-size";

	// UserRepository properties

	/** Field description */
	public static final int USER_REPO_POOL_SIZE_PROP_VAL = 10;

	/** Field description */
	public static final String USER_REPO_URL_PROP_KEY = "user-repo-url";

	/** Field description */
	public static final String XML_REPO_CLASS_PROP_VAL = "tigase.db.xml.XMLRepository";

	/** Field description */
	public static final String XML_REPO_URL_PROP_VAL = "user-repository.xml";

	/** Field description */
	public static final String DATABASE_TYPE_PROP_KEY = "database-type";

	/** Field description */
	private static ConcurrentMap<String, UserRepository> user_repos =
			new ConcurrentHashMap<String, UserRepository>(5);
	private static ConcurrentMap<String, DataRepository> data_repos =
			new ConcurrentHashMap<String, DataRepository>(10);
	private static ConcurrentMap<String, AuthRepository> auth_repos =
			new ConcurrentHashMap<String, AuthRepository>(5);

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param class_name
	 * @param resource
	 * @param params
	 *
	 * 
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
		if (params == null) {
			params = new LinkedHashMap<String, String>(10);
		}
		cls = getRepoClass(cls);

		AuthRepository repo = auth_repos.get(cls + resource);

		if (repo == null) {
			String repo_pool_cls = System.getProperty(AUTH_REPO_POOL_CLASS_PROP_KEY,
					AUTH_REPO_POOL_CLASS_PROP_VAL);
			int repo_pool_size;

			if (params.get(RepositoryFactory.AUTH_REPO_POOL_SIZE_PROP_KEY) != null) {
				repo_pool_size = Integer.parseInt(params.get(RepositoryFactory
						.AUTH_REPO_POOL_SIZE_PROP_KEY));
			} else {
				repo_pool_size = Integer.getInteger(AUTH_REPO_POOL_SIZE_PROP_KEY,
						AUTH_REPO_POOL_SIZE_PROP_VAL);
			}
			params.put(RepositoryFactory.DATA_REPO_POOL_SIZE_PROP_KEY, String.valueOf(
					repo_pool_size));
			params.put(RepositoryFactory.DATABASE_TYPE_PROP_KEY, class_name);
			if (repo_pool_cls != null) {
				AuthRepositoryPool repo_pool = (AuthRepositoryPool) ModulesManagerImpl.getInstance().forName(repo_pool_cls)
						.newInstance();

				repo_pool.initRepository(resource, params);
				for (int i = 0; i < repo_pool_size; i++) {
					repo = (AuthRepository) ModulesManagerImpl.getInstance().forName(cls).newInstance();
					repo.initRepository(resource, params);
					repo_pool.addRepo(repo);
				}
				repo = repo_pool;
			} else {
				repo = (AuthRepository) ModulesManagerImpl.getInstance().forName(cls).newInstance();
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
	 * 
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
		if (params == null) {
			params = new LinkedHashMap<String, String>(10);
		}

		DataRepository repo = data_repos.get(cls + resource);

		if (repo == null) {
			int repo_pool_size;

			if (params.get(RepositoryFactory.DATA_REPO_POOL_SIZE_PROP_KEY) != null) {
				repo_pool_size = Integer.parseInt(params.get(RepositoryFactory
						.DATA_REPO_POOL_SIZE_PROP_KEY));
			} else {
				repo_pool_size = Integer.getInteger(DATA_REPO_POOL_SIZE_PROP_KEY,
						DATA_REPO_POOL_SIZE_PROP_VAL);
			}
			params.put(RepositoryFactory.DATABASE_TYPE_PROP_KEY, class_name);

			DataRepositoryPool repo_pool = (DataRepositoryPool) ModulesManagerImpl.getInstance().forName(System
					.getProperty(DATA_REPO_POOL_CLASS_PROP_KEY, DATA_REPO_POOL_CLASS_PROP_VAL))
					.newInstance();

			repo_pool.initRepository(resource, params);
			for (int i = 0; i < repo_pool_size; i++) {
				repo = (DataRepository) ModulesManagerImpl.getInstance().forName(cls).newInstance();
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
	 * 
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
		if (repo_name.equals("tigase-custom-auth") || repo_name.equals("tigase-custom") ||
				repo_name.equals("custom-auth")) {
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
		if (repo_name.equals("sqlserver")) {
			result = SQLSERVER_REPO_CLASS_PROP_VAL;
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
	 * 
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
		if (params == null) {
			params = new LinkedHashMap<String, String>(10);
		}
		cls = getRepoClass(cls);

		UserRepository repo = user_repos.get(cls + resource);

		if (repo == null) {
			String repo_pool_cls = System.getProperty(USER_REPO_POOL_CLASS_PROP_KEY,
					USER_REPO_POOL_CLASS_PROP_VAL);
			int repo_pool_size;

			if (params.get(RepositoryFactory.USER_REPO_POOL_SIZE_PROP_KEY) != null) {
				repo_pool_size = Integer.parseInt(params.get(RepositoryFactory
						.USER_REPO_POOL_SIZE_PROP_KEY));
			} else {
				repo_pool_size = Integer.getInteger(USER_REPO_POOL_SIZE_PROP_KEY,
						USER_REPO_POOL_SIZE_PROP_VAL);
			}
			params.put(RepositoryFactory.DATA_REPO_POOL_SIZE_PROP_KEY, String.valueOf(
					repo_pool_size));
			params.put(RepositoryFactory.DATABASE_TYPE_PROP_KEY, class_name);
			if (repo_pool_cls != null) {
				UserRepositoryPool repo_pool = (UserRepositoryPool) ModulesManagerImpl.getInstance().forName(repo_pool_cls)
						.newInstance();

				repo_pool.initRepository(resource, params);
				for (int i = 0; i < repo_pool_size; i++) {
					repo = (UserRepository) ModulesManagerImpl.getInstance().forName(cls).newInstance();
					repo.initRepository(resource, params);
					repo_pool.addRepo(repo);
				}
				repo = repo_pool;
			} else {
				repo = (UserRepository) ModulesManagerImpl.getInstance().forName(cls).newInstance();
				repo.initRepository(resource, params);
			}
			user_repos.put(cls + resource, repo);
		}

		return repo;
	}
}    // RepositoryFactory


//~ Formatted in Tigase Code Convention on 13/05/27
