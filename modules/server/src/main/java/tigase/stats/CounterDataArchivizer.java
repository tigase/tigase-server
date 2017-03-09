/*
 * CounterDataArchivizer.java
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



package tigase.stats;

//~--- non-JDK imports --------------------------------------------------------

import tigase.db.DataRepository;
import tigase.db.DBInitException;
import tigase.db.RepositoryFactory;

import tigase.server.XMPPServer;

import tigase.sys.TigaseRuntime;

//~--- JDK imports ------------------------------------------------------------

import java.sql.PreparedStatement;
import java.sql.SQLException;

import java.text.NumberFormat;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Map;

/**
 * Created: Mar 25, 2010 8:55:11 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class CounterDataArchivizer
				implements StatisticsArchivizerIfc {
	public static final String DB_URL_PROP_KEY = "db-url";
	public static final String KEY_FIELD_PROP_KEY = "key-field";
	public static final String TABLE_NAME_PROP_KEY = "table-name";
	public static final String  VAL_FIELD_PROP_KEY   = "val-field";
	private static final String CPU_USAGE_TEXT       = "Usage CPU [%]: ";
	private static final String DEF_KEY_FIELD_NAME   = "counter_name";
	private static final String DEF_TABLE_NAME       = "counter_data";
	private static final String DEF_VALUE_FIELD_NAME = "counter_value";
	private static final String MEM_USAGE_TEXT       = "Usage RAM [%]: ";
	private static final Logger log = Logger.getLogger(CounterDataArchivizer.class
			.getName());
	private static final String SERVER_CONNECTIONS_TEXT = "Connections s2s: ";
	private static final String UPTIME_TEXT             = "Uptime: ";
	private static final String USER_CONNECTIONS_TEXT   = "Connections c2s: ";
	private static final String USER_REGISTERED_TEXT    = "Registered user: ";
	private static final String VERSION_TEXT            = "Version: ";
	private static final String VHOSTS_TEXT             = "VHosts: ";

	//~--- fields ---------------------------------------------------------------

	private String         create_table_query = null;
	private DataRepository data_repo          = null;
	private String         init_entry_query   = null;

	// private PreparedStatement initEntry = null;
	private String tableName          = DEF_TABLE_NAME;
	private String keyField           = DEF_KEY_FIELD_NAME;
	private String update_entry_query = null;

	// private PreparedStatement updateEntry = null;
	private String valueField = DEF_VALUE_FIELD_NAME;

	//~--- methods --------------------------------------------------------------

	@Override
	public void execute(StatisticsProvider sp) {
		NumberFormat format = NumberFormat.getNumberInstance();

		format.setMaximumFractionDigits(2);
		initData(CPU_USAGE_TEXT, format.format(sp.getCPUUsage()));
		initData(MEM_USAGE_TEXT, format.format(sp.getHeapMemUsage()));
		format = NumberFormat.getIntegerInstance();
		initData(USER_REGISTERED_TEXT, format.format(sp.getRegistered()));
		initData(USER_CONNECTIONS_TEXT, format.format(sp.getConnectionsNumber()));
		initData(SERVER_CONNECTIONS_TEXT, format.format(sp.getServerConnections()));
		initData(UPTIME_TEXT, TigaseRuntime.getTigaseRuntime().getUptimeString());
		initData(VHOSTS_TEXT, format.format(sp.getStats("vhost-man", "Number of VHosts", 0)));
	}

	@Override
	public void init(Map<String, Object> conf) {
		String prop = (String) conf.get(TABLE_NAME_PROP_KEY);

		if (prop != null) {
			tableName = prop;
		}
		prop = (String) conf.get(KEY_FIELD_PROP_KEY);
		if (prop != null) {
			keyField = prop;
		}
		prop = (String) conf.get(VAL_FIELD_PROP_KEY);
		if (prop != null) {
			valueField = prop;
		}
		log.log(Level.SEVERE, "Initialize stats archive, table: {0} ", tableName);
		init_entry_query = "insert into " + tableName + " (" + keyField + ", " + valueField +
				") " + " (select ?, ? from " + tableName + " where " + keyField +
				" = ? HAVING count(*)=0)";
		update_entry_query = "update " + tableName + " set " + valueField + " = ? where " +
				keyField + " = ?";
		create_table_query = "CREATE TABLE " + tableName + " ( " + keyField +
				" varchar(255) NOT NULL DEFAULT '0', " + valueField +
				" varchar(255) NOT NULL DEFAULT '0'," + "  PRIMARY KEY ( " + keyField + " ));";
		try {
			initRepository((String) conf.get(DB_URL_PROP_KEY), null);
			initData(VERSION_TEXT, XMPPServer.getImplementationVersion());
			initData(CPU_USAGE_TEXT, "0");
			initData(MEM_USAGE_TEXT, "0");
			initData(USER_CONNECTIONS_TEXT, "0");
			initData(SERVER_CONNECTIONS_TEXT, "0");
			initData(VHOSTS_TEXT, "0");
			initData(UPTIME_TEXT, TigaseRuntime.getTigaseRuntime().getUptimeString());
		} catch (Exception ex) {
			log.log(Level.SEVERE, "Cannot initialize connection to database: ", ex);
		}
	}

	public void initData(String key, String value) {
		try {
			PreparedStatement updateEntry = data_repo.getPreparedStatement(null,
					update_entry_query);
			PreparedStatement initEntry = data_repo.getPreparedStatement(null,
					init_entry_query);

			synchronized (updateEntry) {
				updateEntry.setString(1, value);
				updateEntry.setString(2, key);
				updateEntry.executeUpdate();
			}
			synchronized (initEntry) {
				initEntry.setString(1, key);
				initEntry.setString(2, value);
				initEntry.setString(3, key);
				initEntry.executeUpdate();
			}
		} catch (SQLException e) {
			log.log(Level.WARNING, "Problem adding new entry to DB: ", e);
		}
	}

	public void initRepository(String conn_str, Map<String, String> params)
					throws SQLException, ClassNotFoundException, InstantiationException,
							IllegalAccessException, DBInitException {
		data_repo = RepositoryFactory.getDataRepository(null, conn_str, params);
		checkDB();
		data_repo.initPreparedStatement(init_entry_query, init_entry_query);
		data_repo.initPreparedStatement(update_entry_query, update_entry_query);
	}

	@Override
	public void release() {

		// Do nothing for now....
	}

	public void updateData(String key, String value) {
		try {
			PreparedStatement updateEntry = data_repo.getPreparedStatement(null,
					update_entry_query);

			synchronized (updateEntry) {
				updateEntry.setString(1, value);
				updateEntry.setString(2, key);
				updateEntry.executeUpdate();
			}
		} catch (SQLException e) {
			log.log(Level.WARNING, "Problem adding new entry to DB: ", e);
		}
	}

	private void checkDB() throws SQLException {
		data_repo.checkTable(tableName, create_table_query);
	}
}


//~ Formatted in Tigase Code Convention on 13/11/02
