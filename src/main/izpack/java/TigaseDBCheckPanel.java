/*
 * IzPack - Copyright 2001-2008 Julien Ponge, All Rights Reserved.
 * 
 * http://izpack.org/
 * http://izpack.codehaus.org/
 * 
 * Copyright 2002 Jan Blok
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.izforge.izpack.panels;

import com.izforge.izpack.Info;
import com.izforge.izpack.gui.IzPanelLayout;
import com.izforge.izpack.gui.LabelFactory;
import com.izforge.izpack.gui.LayoutConstants;
import com.izforge.izpack.installer.InstallData;
import com.izforge.izpack.installer.InstallerFrame;
import com.izforge.izpack.installer.ResourceManager;
import com.izforge.izpack.util.Debug;
import com.izforge.izpack.installer.IzPanel;
import com.izforge.izpack.util.*;

import javax.swing.*;
import javax.swing.table.TableModel;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.File;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;

/**
 * The Hello panel class.
 *
 * @author Julien Ponge
 */
public class TigaseDBCheckPanel extends IzPanel {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	private static final int DB_CONNEC_POS = 0;
	private static final int DB_EXISTS_POS = 1;
	private static final int DB_SCHEMA_POS = 2;
	private static final int DB_CONVER_POS = 3;

	private JTable table = null;
	private boolean connection_ok = false;
	private boolean db_ok = false;
	private boolean schema_ok = false;
	private boolean schema_exists = false;
	private boolean conv_ok = false;

	private String res_prefix = "";
	private String schema_ver_query = TigaseConfigConst.DERBY_GETSCHEMAVER_QUERY;
	private Timer delayedTasks = new Timer("DelayedTasks", true);

	/**
	 * The constructor.
	 *
	 * @param parent The parent.
	 * @param idata  The installation data.
	 */
	public TigaseDBCheckPanel(InstallerFrame parent, InstallData idata) {
		super(parent, idata, new IzPanelLayout());

		// The config label.
		String msg = parent.langpack.getString("TigaseDBCheckPanel.info");
		add(createMultiLineLabel(msg));
		add(IzPanelLayout.createParagraphGap());

		final String[] names = new String[] {"Action", "Result"};
		final String[][] datas = new String[][] {
				{"Checking connection to the database", ""},
				{"Checking if the database exists", ""},
				{"Checking the database schema", ""},
 				{"Checking whether the database needs conversion", ""}
		};

		TableModel dataModel = new AbstractTableModel() {
				private String[] columnNames = names;
				private Object[][] data = datas;
				public int getColumnCount() { return columnNames.length; }
				public int getRowCount() { return data.length; }
				public String getColumnName(int col) { return columnNames[col]; }
				public Object getValueAt(int row, int col) { return data[row][col]; }
				public Class getColumnClass(int c) { return getValueAt(0, c).getClass(); }
				public boolean isCellEditable(int row, int col) { return false; }
				public void setValueAt(Object value, int row, int col) {
					data[row][col] = value;
					fireTableCellUpdated(row, col);
				}
      };
		// The table area which shows the info.
		table = new JTable(dataModel);
		//		table.setEditable(false);
		//add(table, NEXT_LINE);
		JScrollPane scroller = new JScrollPane(table);
		table.setFillsViewportHeight(true);
		add(scroller, NEXT_LINE);
		// At end of layouting we should call the completeLayout
		// method also they do nothing.
		getLayoutHelper().completeLayout();
	}

	public void panelActivate() {
		super.panelActivate();
		delayedTasks.schedule(new TimerTask() {
				public void run() {
					table.setValueAt(validateDBConnection(), DB_CONNEC_POS, 1);
					table.setValueAt(validateDBExists(), DB_EXISTS_POS, 1);
					table.setValueAt(validateDBSchema(), DB_SCHEMA_POS, 1);
					table.setValueAt(validateDBConversion(), DB_CONVER_POS, 1);
				}
			}, 1000);

	}

	private void selectDatabase(String db_uri) {
		schema_ver_query = TigaseConfigConst.JDBC_GETSCHEMAVER_QUERY;
		if (db_uri.startsWith("jdbc:postgresql")) {
			System.setProperty("jdbc.drivers", TigaseConfigConst.PGSQL_DRIVER);
			res_prefix = "pgsql";
		}
		if (db_uri.startsWith("jdbc:mysql")) {
			System.setProperty("jdbc.drivers", TigaseConfigConst.MYSQL_DRIVER);
			res_prefix = "mysql";
		}
		if (db_uri.startsWith("jdbc:derby")) {
			System.setProperty("jdbc.drivers", TigaseConfigConst.DERBY_DRIVER);
			res_prefix = "derby";
			schema_ver_query = TigaseConfigConst.DERBY_GETSCHEMAVER_QUERY;
		}
	}

	private String validateDBConnection() {
		connection_ok = false;
		String db_conn = TigaseConfigConst.props.getProperty("root-db-uri");
		if (db_conn == null) {
			return "Missing DB connection URL";
		} else {
			selectDatabase(db_conn);
			try {
				Connection conn = DriverManager.getConnection(db_conn);
				conn.close();
				connection_ok = true;
				return "Connection OK";
			} catch (Exception e) {
				//e.printStackTrace();
				return e.getMessage();
			}
		}
	}

	private String validateDBExists() {
		if (!connection_ok) {
			return "Connection not validated";
		}
		String result = "Exists OK";
		db_ok = false;
		String db_conn = TigaseConfigConst.props.getProperty("--user-db-uri");
		if (db_conn == null) {
			return "Missing DB connection URL";
		} else {
			Connection conn = null;
			try {
				conn = DriverManager.getConnection(db_conn);
				conn.close();
				db_ok = true;
				return result;
			} catch (Exception e) {
				result = "Doesn't exist";
				table.setValueAt(result, DB_EXISTS_POS, 1);
				result += ", creating...";
				table.setValueAt(result, DB_EXISTS_POS, 1);
				db_conn = TigaseConfigConst.props.getProperty("root-db-uri");
				try {
					conn = DriverManager.getConnection(db_conn);
					ArrayList<String> queries = loadSQLQueries(res_prefix + ".create");
					for (String query: queries) {
						Debug.trace("Executing query: " + query);
						Statement stmt = conn.createStatement();
						// Some queries may fail and this is still fine
						// the user or the database may already exist
						try {
							stmt.execute(query);
							stmt.close();
						} catch (Exception ex) {
							Debug.trace("Query failed: " + ex.getMessage());
						}
					}
					conn.close();
					result += " OK";
					db_ok = true;
				} catch (Exception ex) {
					result += ex.getMessage();
				}
			}
		}
		return result;
	}

	private String validateDBSchema() {
		if (!connection_ok) {
			return "Connection not validated";
		}
		if (!db_ok) {
			return "Database not validated";
		}
		schema_exists = false;
		schema_ok = false;
		Connection conn = null;
		String db_conn = TigaseConfigConst.props.getProperty("--user-db-uri");
		long users = 0;
		try {
			conn = DriverManager.getConnection(db_conn);
			Statement stmt = conn.createStatement();

			String query = TigaseConfigConst.JDBC_CHECKUSERTABLE_QUERY;
			ResultSet rs = stmt.executeQuery(query);
			if (rs.next()) {
				users = rs.getLong(1);
				schema_exists = true;
				Debug.trace("Schema exists, users: " + users);
			}

			query = schema_ver_query;
			rs = stmt.executeQuery(query);
			if (rs.next()) {
				String schema_version = rs.getString(1);
				if ("4.0".equals(schema_version)) {
					schema_ok = true;
				}
			}
		} catch (Exception e) {
			Debug.trace("Exception, posibly schema hasn't been loaded yet: " + e);
		}
		if (schema_ok) {
			return "Schema OK, accounts number: " + users;
		}
		if (!schema_exists) {
			Debug.trace("DB schema doesn't exists, creating one...");
			db_conn = TigaseConfigConst.props.getProperty("root-tigase-db-uri");
			try {
				//conn.close();
				conn = DriverManager.getConnection(db_conn);
				Statement stmt = conn.createStatement();
				ArrayList<String> queries = loadSchemaQueries();
				for (String query: queries) {
					Debug.trace("Executing query: " + query);
					stmt.execute(query);
				}
				stmt.close();
				conn.close();
				schema_ok = true;
				return "New schema loaded OK";
			} catch (Exception ex) {
				return "Can't load schema: " + ex.getMessage();
			}
		} else {
			return "Old schema, accounts number: " + users;
		}
	}

	private ArrayList<String> loadSchemaQueries() throws Exception {
		ArrayList<String> queries = loadSQLQueries(res_prefix + ".schema");
		queries.addAll(loadSQLQueries(res_prefix + ".sp"));
		queries.addAll(loadSQLQueries(res_prefix + ".props"));
		return queries;
	}

	private String validateDBConversion() {
		if (!connection_ok) {
			return "Connection not validated";
		}
		if (!db_ok) {
			return "Database not validated";
		}
		if (schema_ok) {
			return "Conversion not needed";
		}
		if (!schema_exists) {
			return "Something wrong, the schema still is not loaded....";
		}
		String result = "Converting...";
		table.setValueAt(result, DB_CONVER_POS, 1);
		String db_conn = TigaseConfigConst.props.getProperty("root-tigase-db-uri");
		try {
			//conn.close();
			Connection conn = DriverManager.getConnection(db_conn);
			Statement stmt = conn.createStatement();
			ArrayList<String> queries = loadSQLQueries(res_prefix + ".upgrade");
			for (String query: queries) {
				Debug.trace("Executing query: " + query);
				stmt.execute(query);
			}
			stmt.close();
			conn.close();
			schema_ok = true;
			result += " completed OK";
		} catch (Exception ex) {
			return "Can't upgrade schema: " + ex.getMessage();
		}
		return result;
	}

	private enum SQL_LOAD_STATE {
		INIT, IN_SQL;
	}

	private ArrayList<String> loadSQLQueries(String resource) throws Exception {
		ArrayList<String> results = new ArrayList<String>();
		VariableSubstitutor vs = new VariableSubstitutor(idata.getVariables());
		BufferedReader br =
      new BufferedReader(new
				InputStreamReader(ResourceManager.getInstance().getInputStream(resource)));
		String line = null;
		String sql_query = "";
		SQL_LOAD_STATE state = SQL_LOAD_STATE.INIT;
		while ((line = br.readLine()) != null) {
			switch (state) {
			case INIT:
				if (line.startsWith("-- QUERY START:")) {
					sql_query = "";
					state = SQL_LOAD_STATE.IN_SQL;
				}
				if (line.startsWith("-- LOAD SCHEMA:")) {
					results.addAll(loadSchemaQueries());
				}
				break;
			case IN_SQL:
				if (line.startsWith("-- QUERY END:")) {
					state = SQL_LOAD_STATE.INIT;
					sql_query = sql_query.trim();
					if (sql_query.endsWith(";")) {
						sql_query = sql_query.substring(0, sql_query.length()-1);
					}
					if (sql_query.endsWith("//")) {
						sql_query = sql_query.substring(0, sql_query.length()-2);
					}
					results.add(vs.substitute(sql_query, null));
				}
				if (line.isEmpty() || line.trim().startsWith("--")) {
					continue;
				} else {
					sql_query += " " + line.trim();
				}
				break;
			default:
				break;
			}
		}
		br.close();
		return results;
	}

	/**
	 * Indicates wether the panel has been validated or not.
	 *
	 * @return Always true.
	 */
	public boolean isValidated() {
		return true;
	}

}
