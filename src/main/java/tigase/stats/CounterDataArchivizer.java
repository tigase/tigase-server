
/*
* Tigase Jabber/XMPP Server
* Copyright (C) 2004-2010 "Artur Hefczyc" <artur.hefczyc@tigase.org>
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
package tigase.stats;

//~--- non-JDK imports --------------------------------------------------------

import tigase.db.DBInitException;
import tigase.db.DataRepository;
import tigase.db.RepositoryFactory;

import tigase.server.XMPPServer;

import tigase.sys.TigaseRuntime;

//~--- JDK imports ------------------------------------------------------------

import java.sql.PreparedStatement;
import java.sql.SQLException;

import java.text.NumberFormat;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

//~--- classes ----------------------------------------------------------------

/**
 * Created: Mar 25, 2010 8:55:11 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class CounterDataArchivizer implements StatisticsArchivizerIfc {
	private static final Logger log = Logger.getLogger(CounterDataArchivizer.class.getName());

	/** Field description */
	public static final String DB_URL_PROP_KEY = "db-url";

	/** Field description */
	public static final String TABLE_NAME_PROP_KEY = "table-name";

	/** Field description */
	public static final String KEY_FIELD_PROP_KEY = "key-field";

	/** Field description */
	public static final String VAL_FIELD_PROP_KEY = "val-field";
	private static final String DEF_TABLE_NAME = "counter_data";
	private static final String DEF_KEY_FIELD_NAME = "counter_name";
	private static final String DEF_VALUE_FIELD_NAME = "counter_value";
	private static final String USER_CONNECTIONS_TEXT = "Connections c2s: ";
	private static final String SERVER_CONNECTIONS_TEXT = "Connections s2s: ";
	private static final String CPU_USAGE_TEXT = "Usage CPU [%]: ";
	private static final String MEM_USAGE_TEXT = "Usage RAM [%]: ";
	private static final String UPTIME_TEXT = "Uptime: ";
	private static final String VERSION_TEXT = "Version: ";

	//~--- fields ---------------------------------------------------------------

	private DataRepository data_repo = null;
	private String init_entry_query = null;

	// private PreparedStatement initEntry = null;
	private String tableName = DEF_TABLE_NAME;
	private String keyField = DEF_KEY_FIELD_NAME;
	private String update_entry_query = null;

	// private PreparedStatement updateEntry = null;
	private String valueField = DEF_VALUE_FIELD_NAME;

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param sp
	 */
	@Override
	public void execute(StatisticsProvider sp) {
		NumberFormat format = NumberFormat.getNumberInstance();

		format.setMaximumFractionDigits(2);
		initData(CPU_USAGE_TEXT, format.format(sp.getCPUUsage()));
		initData(MEM_USAGE_TEXT, format.format(sp.getHeapMemUsage()));
		format = NumberFormat.getIntegerInstance();
		initData(USER_CONNECTIONS_TEXT, format.format(sp.getConnectionsNumber()));
		initData(SERVER_CONNECTIONS_TEXT, format.format(sp.getServerConnections()));
		initData(UPTIME_TEXT, TigaseRuntime.getTigaseRuntime().getUptimeString());
	}

	/**
	 * Method description
	 *
	 *
	 * @param conf
	 */
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

		init_entry_query = "insert into " + tableName + " (" + keyField + ", " + valueField + ") "
				+ " values (?, ?) on duplicate key update " + valueField + " = ?";
		update_entry_query = "update " + tableName + " set " + valueField + " = ? where " + keyField
				+ " = ?";

		try {
			initRepository((String) conf.get(DB_URL_PROP_KEY), null);
			initData(VERSION_TEXT, XMPPServer.getImplementationVersion());
			initData(CPU_USAGE_TEXT, "0");
			initData(MEM_USAGE_TEXT, "0");
			initData(USER_CONNECTIONS_TEXT, "0");
			initData(SERVER_CONNECTIONS_TEXT, "0");
			initData(UPTIME_TEXT, TigaseRuntime.getTigaseRuntime().getUptimeString());
		} catch (Exception ex) {
			log.log(Level.SEVERE, "Cannot initialize connection to database: ", ex);
		}
	}

	/**
	 * Method description
	 *
	 *
	 * @param key
	 * @param value
	 */
	public void initData(String key, String value) {
		try {
			PreparedStatement initEntry = data_repo.getPreparedStatement(init_entry_query);

			synchronized (initEntry) {
				initEntry.setString(1, key);
				initEntry.setString(2, value);
				initEntry.setString(3, value);
				initEntry.executeUpdate();
			}
		} catch (SQLException e) {
			log.log(Level.WARNING, "Problem adding new entry to DB: ", e);
		}
	}

	/**
	 * Method description
	 *
	 *
	 * @param conn_str
	 * @param params
	 *
	 * @throws SQLException
	 * @throws ClassNotFoundException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws DBInitException
	 */
	public void initRepository(String conn_str, Map<String, String> params)
			throws SQLException, ClassNotFoundException, InstantiationException, IllegalAccessException,
			DBInitException {
		data_repo = RepositoryFactory.getDataRepository(null, conn_str, params);
		data_repo.initPreparedStatement(init_entry_query, init_entry_query);
		data_repo.initPreparedStatement(update_entry_query, update_entry_query);
	}

	/**
	 * Method description
	 *
	 */
	@Override
	public void release() {

		// Do nothing for now....
	}

	/**
	 * Method description
	 *
	 *
	 * @param key
	 * @param value
	 */
	public void updateData(String key, String value) {
		try {
			PreparedStatement updateEntry = data_repo.getPreparedStatement(update_entry_query);

			synchronized (updateEntry) {
				updateEntry.setString(1, value);
				updateEntry.setString(2, key);
				updateEntry.executeUpdate();
			}
		} catch (SQLException e) {
			log.log(Level.WARNING, "Problem adding new entry to DB: ", e);
		}
	}
}


//~ Formatted in Sun Code Convention


//~ Formatted by Jindent --- http://www.jindent.com
