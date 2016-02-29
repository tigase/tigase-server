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

package com.izforge.izpack.panels;

import java.io.File;
import java.io.FileWriter;
import java.util.Map;
import java.util.Properties;
import java.net.URLEncoder;

import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import com.izforge.izpack.gui.IzPanelLayout;
import com.izforge.izpack.installer.AutomatedInstallData;
import com.izforge.izpack.installer.InstallData;
import com.izforge.izpack.installer.InstallerFrame;
import com.izforge.izpack.installer.IzPanel;
import com.izforge.izpack.util.Debug;
import com.izforge.izpack.util.OsVersion;
import java.io.UnsupportedEncodingException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The Hello panel class.
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class TigaseConfigSavePanel extends IzPanel {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	private JTextArea textArea = null;

	private final TigaseConfigSaveHelper helper = new TigaseConfigSaveHelper();


	/**
	 * The constructor.
	 *
	 * @param parent The parent.
	 * @param idata  The installation data.
	 */
	public TigaseConfigSavePanel(InstallerFrame parent, InstallData idata) {
		super(parent, TigaseInstallerCommon.init(idata), new IzPanelLayout());

		// The config label.
		String msg = parent.langpack.getString("TigaseConfigSavePanel.info");
		add(createMultiLineLabel(msg));
		add(IzPanelLayout.createParagraphGap());
		// The text area which shows the info.
		textArea = new JTextArea("");
		textArea.setCaretPosition(0);
		textArea.setEditable(true);
		JScrollPane scroller = new JScrollPane(textArea);
		add(scroller, NEXT_LINE);
		// At end of layouting we should call the completeLayout method also they do nothing.
		getLayoutHelper().completeLayout();
	}

	public void panelActivate() {
		super.panelActivate();
		String config = helper.showConfig(
				new IzPackInstallDataVariablesSource(idata));
		textArea.setText(config);
	}



	/**
	 * Indicates wether the panel has been validated or not.
	 *
	 * @return Always true.
	 */
	public boolean isValidated() {
		String errorStr =  helper.saveConfig(idata, textArea.getText());
		if (errorStr != null) {
			emitError("Can not write to config file", errorStr);
		}
		return true;
	}

}

abstract class VariablesSource {
	abstract String getVariable(String key);
	abstract String getEncodedVariable(String key);
}

class IzPackInstallDataVariablesSource extends VariablesSource {
	private final AutomatedInstallData idata;

	public IzPackInstallDataVariablesSource(AutomatedInstallData idata) {
		this.idata = idata;

	}

	@Override
	String getVariable(String key) {
		return idata.getVariable(key);
	}

	@Override
	String getEncodedVariable(String key) {

		String variable = idata.getVariable(key);
		String value = null;
		try {
			value = URLEncoder.encode(variable, "UTF-8");
		} catch ( Exception ex ) {
			Logger.getLogger( TigaseConfigSaveHelper.class.getName() ).log( Level.SEVERE, null, ex );
		}
		return value;
	}
}

class TigaseConfigSaveHelper {

	String showConfig(VariablesSource variablesSource) {
		TigaseConfigConst.props.clear();
		StringBuilder config = new StringBuilder();
		int comp_idx = 0;
		for (Map.Entry<String, String> entry:
		TigaseConfigConst.tigaseIzPackMap.entrySet()) {
			String varName = entry.getValue();
			String varValue = variablesSource.getVariable(varName);

			Debug.trace("=== Processing varName: varValue :: " + varName + " : " + varValue);
			
			if (varName.equals(TigaseConfigConst.DEBUG)) {
				String debugVar = getDebugs(variablesSource);
				if (!debugVar.isEmpty()) {
					TigaseConfigConst.props.setProperty(entry.getKey(), debugVar);
				}
				Debug.trace("Set: " + entry.getKey() + " = " + debugVar);
				continue;
			}
			if (varName.equals(TigaseConfigConst.PLUGINS)) {
				String pluginsVar = getPlugins(variablesSource);
				if (!pluginsVar.isEmpty()) {
					TigaseConfigConst.props.setProperty(entry.getKey(), pluginsVar);
				}
				Debug.trace("Set: " + entry.getKey() + " = " + pluginsVar);
				continue;
			}
			if (varName.equals(TigaseConfigConst.USER_DB_URI)) {
				TigaseConfigConst.props.setProperty(entry.getKey(), getDBUri(variablesSource));
				TigaseConfigConst.props.setProperty("root-tigase-db-uri",getRootTigaseDBUri(variablesSource));
				TigaseConfigConst.props.setProperty("root-db-uri", getRootDBUri(variablesSource));
				Debug.trace("Set: " + entry.getKey() + " = " + getDBUri(variablesSource));
				Debug.trace("Set: " + "root-tigase-db-uri" + " = " + getRootTigaseDBUri(variablesSource));
				Debug.trace("Set: " + "root-db-uri" + " = " + getRootDBUri(variablesSource));
				continue;
			}

			if (varValue == null) continue;

			if (varName.equals(TigaseConfigConst.DB_TYPE)) {
				TigaseConfigConst.props.setProperty(entry.getKey(), getUserDB(variablesSource));
				Debug.trace("Set: " + entry.getKey() + " = " + getUserDB(variablesSource));
				continue;
			}
			if (varName.equals(TigaseConfigConst.AUTH_HANDLE)) {
				TigaseConfigConst.props.setProperty(entry.getKey(),
					getAuthHandler(varValue, variablesSource));
				Debug.trace("Set: " + entry.getKey() + " = " + getAuthHandler(varValue, variablesSource));
				continue;
			}
			if (varName.equals(TigaseConfigConst.MUC_COMP))  {
				if ( varValue.equals( "on" ) ){
					++comp_idx;
					TigaseConfigConst.props.setProperty( "--comp-name-" + comp_idx, "muc" );

					String acsMUC = TigaseConfigConst.tigaseIzPackMap.get( "--muc_acs" );
					String acsMUCValue = variablesSource.getVariable( acsMUC );
					Debug.trace( " @@ Set: acsMUC " + acsMUC + " / acsMUCValue: " + acsMUCValue );

					if ( acsMUC != null && acsMUCValue != null && acsMUCValue.equals( "acs" ) ){
						TigaseConfigConst.props.setProperty( "--comp-class-" + comp_idx,
																								 TigaseConfigConst.ACS_MUC_COMP_CLASS );
						Debug.trace( "Set: " + "--comp-name-" + comp_idx + " = " + "muc" + " % to:" + "--comp-class-" + comp_idx + "=" + TigaseConfigConst.ACS_MUC_COMP_CLASS );
					} else {
						TigaseConfigConst.props.setProperty( "--comp-class-" + comp_idx,
																								 TigaseConfigConst.MUC_COMP_CLASS );
						Debug.trace( "Set: " + "--comp-name-" + comp_idx + " = " + "muc" + " % to:" + "--comp-class-" + comp_idx + "=" + TigaseConfigConst.MUC_COMP_CLASS );
					}
				}
				continue;
			}
			
			if ( varName.equals( TigaseConfigConst.PUBSUB_COMP ) ){
				if ( varValue.equals( "on" ) ){
					++comp_idx;
					TigaseConfigConst.props.setProperty( "--comp-name-" + comp_idx, "pubsub" );
					String acsPubSub = TigaseConfigConst.tigaseIzPackMap.get( "--pubsub_acs" );
					String acsPubSubValue = variablesSource.getVariable( acsPubSub );
					Debug.trace( " @@ Set: acsMUC " + acsPubSub + " / acsMUCValue: " + acsPubSubValue );

					if ( acsPubSub != null && acsPubSubValue != null && acsPubSubValue.equals( "acs" ) ){
						TigaseConfigConst.props.setProperty( "--comp-class-" + comp_idx,
																								 TigaseConfigConst.ACS_PUBSUB_COMP_CLASS );
						Debug.trace( "Set: " + "--comp-name-" + comp_idx + " = " + "pubsub" + " % to:" + "--comp-class-" + comp_idx + "=" + TigaseConfigConst.ACS_PUBSUB_COMP_CLASS );
					} else {
						TigaseConfigConst.props.setProperty( "--comp-class-" + comp_idx,
																								 TigaseConfigConst.PUBSUB_COMP_CLASS );
						Debug.trace( "Set: " + "--comp-name-" + comp_idx + " = " + "pubsub" + " % to:" + "--comp-class-" + comp_idx + "=" + TigaseConfigConst.PUBSUB_COMP_CLASS );
					}
				}
				continue;
			}

			if (varName.equals(TigaseConfigConst.ACS_PUBSUB_COMP) || varName.equals(TigaseConfigConst.ACS_MUC_COMP)) {
				continue;
			}

			if (varName.equals(TigaseConfigConst.SOCKS5_COMP)) {
				if (varValue.equals("on")) {
					++comp_idx;
					TigaseConfigConst.props.setProperty("--comp-name-"+comp_idx, "proxy");
					TigaseConfigConst.props.setProperty("--comp-class-"+comp_idx,
						TigaseConfigConst.SOCKS5_COMP_CLASS);
				}
				Debug.trace("Set: " + "--comp-name-"+comp_idx + " = " + "proxy" + " % to:" + "--comp-class-"+comp_idx + "=" + TigaseConfigConst.SOCKS5_COMP_CLASS);
				continue;
			}
			if (varName.equals(TigaseConfigConst.STUN_COMP)) {
				if (varValue.equals("on")) {
					++comp_idx;
					TigaseConfigConst.props.setProperty("--comp-name-"+comp_idx, "stun");
					TigaseConfigConst.props.setProperty("--comp-class-"+comp_idx,
						TigaseConfigConst.STUN_COMP_CLASS);
				}
				Debug.trace("Set: " + "--comp-name-"+comp_idx + " = " + "stun" + " % to:" + "--comp-class-"+comp_idx + "=" + TigaseConfigConst.STUN_COMP_CLASS);
				continue;
			}
			if (varName.equals(TigaseConfigConst.HTTP_COMP)) {
				if (varValue.equals("on")) {
					++comp_idx;
					TigaseConfigConst.props.setProperty("--comp-name-"+comp_idx, "http");
					TigaseConfigConst.props.setProperty("--comp-class-"+comp_idx,
						TigaseConfigConst.HTTP_COMP_CLASS);
				}
				Debug.trace("Set: " + "--comp-name-"+comp_idx + " = " + "http" + " % to:" + "--comp-class-"+comp_idx + "=" + TigaseConfigConst.HTTP_COMP_CLASS);
				continue;
			}
			if (varName.equals(TigaseConfigConst.ARCHIVE_COMP)) {
				if (varValue.equals("on")) {
					++comp_idx;
					TigaseConfigConst.props.setProperty("--comp-name-"+comp_idx, "message-archive");
					TigaseConfigConst.props.setProperty("--comp-class-"+comp_idx,
						TigaseConfigConst.ARCHIVE_COMP_CLASS);
				}
				Debug.trace("Set: " + "--comp-name-"+comp_idx + " = " + "message-archive" + " % to:" + "--comp-class-"+comp_idx + "=" + TigaseConfigConst.HTTP_COMP_CLASS);
				continue;
			}

			if (varName.equals(TigaseConfigConst.ACS_COMP)) {
				if (varValue.equals("on")) {
					TigaseConfigConst.props.setProperty("--sm-cluster-strategy-class", TigaseConfigConst.ACS_COMP_CLASS);
				}
				Debug.trace("Set: " + "--sm-cluster-strategy-class = " + TigaseConfigConst.ACS_COMP_CLASS);
				continue;
			}

			if (varName.equals(TigaseConfigConst.AUTH_DB_URI)) {
				String auth_db_uri = getAuthUri(variablesSource);
				if (auth_db_uri != null) {
					TigaseConfigConst.props.setProperty(entry.getKey(), auth_db_uri);
					Debug.trace("Set: " + entry.getKey() + " = " + auth_db_uri);
				} else {
					Debug.trace("Not set: " + entry.getKey());
				}
				continue;
			}
			if (!varValue.trim().isEmpty()) {
				TigaseConfigConst.props.setProperty(entry.getKey(), varValue);
			}
			Debug.trace("Set: " + entry.getKey() + " = " + varValue);
		}
		for (String name: TigaseConfigConst.props.stringPropertyNames()) {
			if (!name.startsWith("root")) {
				config.append(name + " = " + TigaseConfigConst.props.getProperty(name) + "\n");
			}
		}
		return config.toString();
	}

	private String getDBUri(VariablesSource variablesSource) {
		String db_uri = "jdbc:";
		String database = getUserDB(variablesSource);
		Debug.trace("getDBUri | database: "  +database);
		if (database.equals("pgsql")) {
			db_uri += "postgresql:";
		} else if (database.equals("sqlserver")) {
			db_uri += "jtds:sqlserver:";
		} else {
			db_uri += database + ":";
		}
		if (database.equals("derby")) {
			String derby_path = variablesSource.getVariable("DerbyDBPath");
			if (OsVersion.IS_WINDOWS) {
				derby_path = derby_path.replace("\\", "\\\\");
			}
			db_uri += derby_path;
		} else if ( database.equals( "sqlserver" ) ){
			db_uri += "//" + variablesSource.getVariable("dbHost");
			db_uri += ";databaseName=" + variablesSource.getVariable("dbName");
			db_uri += ";user=" + variablesSource.getEncodedVariable("dbUser");
			if ( variablesSource.getEncodedVariable( "dbPass" ) != null
					 && !variablesSource.getEncodedVariable( "dbPass" ).isEmpty() ){
				db_uri += ";password=" + variablesSource.getEncodedVariable( "dbPass" );
			}
			db_uri += ";schema=dbo";
			db_uri += ";lastUpdateCount=false";
			db_uri += ";cacheMetaData=false";
		} else {
			db_uri += "//" + variablesSource.getVariable("dbHost");
			db_uri += "/" + variablesSource.getVariable("dbName");
			db_uri += "?user=" + variablesSource.getEncodedVariable("dbUser");
			if (variablesSource.getEncodedVariable("dbPass") != null
				&& !variablesSource.getEncodedVariable("dbPass").isEmpty()) {
				db_uri += "&password=" + variablesSource.getEncodedVariable("dbPass");
			}
		}
		return db_uri;
	}

	private String getRootTigaseDBUri(VariablesSource variablesSource) {
		String db_uri = "jdbc:";
		String database = getUserDB(variablesSource);
		Debug.trace("getDBUri | database: " + database);
		if (database.equals("pgsql")) {
			db_uri += "postgresql:";
		} else if (database.equals("sqlserver")) {
			db_uri += "jtds:sqlserver:";
		} else {
			db_uri += database + ":";
		}
		if (database.equals("derby")) {
			db_uri += variablesSource.getVariable("DerbyDBPath") + ";create=true";
		} else if ( database.equals( "sqlserver" ) ){
			db_uri += "//" + variablesSource.getVariable("dbHost");
			db_uri += ";databaseName=" + variablesSource.getVariable("dbName");
			db_uri += ";user=" + variablesSource.getEncodedVariable("dbSuperuser");
			if ( variablesSource.getEncodedVariable( "dbSuperpass" ) != null
					 && !variablesSource.getEncodedVariable( "dbSuperpass" ).isEmpty() ){
				db_uri += ";password=" + variablesSource.getEncodedVariable( "dbSuperpass" );
			}
			db_uri += ";schema=dbo";
			db_uri += ";lastUpdateCount=false";
			db_uri += ";cacheMetaData=false";
		} else {
			db_uri += "//" + variablesSource.getVariable("dbHost");
			db_uri += "/" + variablesSource.getVariable("dbName");
			db_uri += "?user=" + variablesSource.getEncodedVariable("dbSuperuser");
			if (variablesSource.getEncodedVariable("dbSuperpass") != null
				&& !variablesSource.getEncodedVariable("dbSuperpass").isEmpty()) {
				db_uri += "&password=" + variablesSource.getEncodedVariable("dbSuperpass");
			}
		}
		return db_uri;
	}

	private String getRootDBUri(VariablesSource variablesSource) {
		String db_uri = "jdbc:";
		String db = "";
		String database = getUserDB(variablesSource);
		Debug.trace("getDBUri | database: " + database);
		if (database.equals("pgsql")) {
			db_uri += "postgresql:";
			db = "/postgres";
		} else if (database.equals("sqlserver")) {
			db_uri += "jtds:sqlserver:";
			db = ";databaseName=master";
		} else {
			db_uri += database + ":";
			if (database.equals("mysql")) {
				db = "/mysql";
			}
		}
		if (database.equals("derby")) {
			db_uri += variablesSource.getVariable("DerbyDBPath") + ";create=true";
		} else if ( database.equals( "sqlserver" ) ){
			db_uri += "//" + variablesSource.getVariable("dbHost");
			db_uri += ";databaseName=master";
			db_uri += ";user=" + variablesSource.getEncodedVariable("dbSuperuser");
			if ( variablesSource.getEncodedVariable( "dbSuperpass" ) != null
					 && !variablesSource.getEncodedVariable( "dbSuperpass" ).isEmpty() ){
				db_uri += ";password=" + variablesSource.getEncodedVariable( "dbSuperpass" );
			}
			db_uri += ";schema=dbo";
			db_uri += ";lastUpdateCount=false";
			db_uri += ";cacheMetaData=false";
		} else {
			db_uri += "//" + variablesSource.getVariable("dbHost");
			db_uri += db;
			db_uri += "?user=" + variablesSource.getEncodedVariable("dbSuperuser");
			if (variablesSource.getEncodedVariable("dbSuperpass") != null
				&& !variablesSource.getEncodedVariable("dbSuperpass").isEmpty()) {
				db_uri += "&password=" + variablesSource.getEncodedVariable("dbSuperpass");
			}
		}
		return db_uri;
	}

	private String getAuthUri(VariablesSource variablesSource) {
		String db_uri = "jdbc:";
		String database = variablesSource.getVariable(TigaseConfigConst.AUTH_DB_URI);
		db_uri += database + ":";
		if (database.equals("derby")) {
			String derby_path = variablesSource.getVariable("DerbyDBPath");
			if (derby_path != null) {
				db_uri += derby_path;
			} else {
				return null;
			}
		} else if ( database.equals( "sqlserver" ) ){
			db_uri += "//" + variablesSource.getVariable("dbAuthHost");
			db_uri += ";databaseName=" + variablesSource.getVariable("dbAuthName");
			db_uri += ";user=" + variablesSource.getEncodedVariable("dbAuthUser");
			if ( variablesSource.getEncodedVariable( "dbAuthPass" ) != null
					 && !variablesSource.getEncodedVariable( "dbAuthPass" ).isEmpty() ){
				db_uri += ";password=" + variablesSource.getEncodedVariable( "dbAuthPass" );
			}
			db_uri += ";schema=dbo";
			db_uri += ";lastUpdateCount=false";
			db_uri += ";cacheMetaData=false";
		} else {
			db_uri += "//" + variablesSource.getVariable("dbAuthHost");
			db_uri += "/" + variablesSource.getVariable("dbAuthName");
			db_uri += "?user=" + variablesSource.getEncodedVariable("dbAuthUser");
			if (variablesSource.getEncodedVariable("dbAuthPass") != null
				&& !variablesSource.getEncodedVariable("dbAuthPass").isEmpty()) {
				db_uri += "&password=" + variablesSource.getEncodedVariable("dbAuthPass");
			}
		}
		return db_uri;
	}

	private String getPlugins(VariablesSource variablesSource) {
		String plugins = "";
		if (variablesSource.getVariable(TigaseConfigConst.ALL_PLUGINS[0]) == null) {
			// The Panel with debuging settings was not shown so all
			// settins are null, then we set a default: 'server'
			return "";
		}
		for (String plugin: TigaseConfigConst.ALL_PLUGINS) {
			if (variablesSource.getVariable(plugin) == null) {
				Debug.trace("Missing variables for: " + plugin);
				continue;
			}

			final String value = variablesSource.getVariable(plugin);
			final String prefix;
			final String pluginId = TigaseConfigConst.getPluginId(plugin);
			if (value.equals("off")) {
				prefix = "-";
			} else {
				prefix = "+";
			}

			if (!plugins.isEmpty()) {
				plugins += ",";
			}
			plugins += prefix + pluginId;
		}

		return plugins;
	}

	private String getDebugs(VariablesSource variablesSource) {

		String debugs = "";
		if (variablesSource.getVariable(TigaseConfigConst.ALL_DEBUGS[0]) == null) {
			// The Panel with debuging settings was not shown so all
			// settins are null, then we set a default: 'server'
			return "server";
		}
		for (String deb: TigaseConfigConst.ALL_DEBUGS) {
			if (variablesSource.getVariable(deb) == null
					|| variablesSource.getVariable(deb).equals("off")) {
				continue;
			}
			if (!debugs.isEmpty()) {
				debugs += ",";
			}
			debugs += variablesSource.getVariable(deb);
		}
		return debugs;
	}

	private String getAuthHandler(String var, VariablesSource variablesSource) {
		if (var.equals("Standard")) {
			return getUserDB(variablesSource);
		}
		return var;
	}

	private String getUserDB( VariablesSource variablesSource ) {
		String dbVar = variablesSource.getVariable( TigaseConfigConst.DB_TYPE );
		Debug.trace( "@@ getUserDB | dbVar: " + dbVar );
		String result = TigaseConfigConst.userDBMap.get( dbVar );
		Debug.trace( "@@ getUserDB | result: " + result );
		return result != null ? result : "derby";
	}

	// returns null if ok, error string on error
	String saveConfig(AutomatedInstallData variablesSource, String config) {
		// Try to read the config file.
		File configPath = null;
		File xmlConfigPath = null;
		try {
			if (variablesSource.getVariable("searchTigaseHome") == null
				|| variablesSource.getVariable("searchTigaseHome").isEmpty()) {
				configPath = new File(variablesSource.getVariable("INSTALL_PATH"),
					"etc/init.properties");
				xmlConfigPath = new File(variablesSource.getVariable("INSTALL_PATH"),
					"etc/tigase.xml");
			} else {
				configPath = new File(variablesSource.getVariable("searchTigaseHome"),
					"etc/init.properties");
				xmlConfigPath = new File(variablesSource.getVariable("searchTigaseHome"),
					"etc/tigase.xml");
			}
			FileWriter fw = new FileWriter(configPath, false);
			fw.write(config);
			fw.close();
			if (xmlConfigPath.exists()) {
				xmlConfigPath.delete();
			}
		} catch (Exception err) {
			String error = "Error : could not write to the config file: " + configPath + "\n";
			error += err.toString() + "\n";
			for (StackTraceElement ste: err.getStackTrace()) {
				error += ste.toString() + "\n";
			}
			return error;
		}
		return null;
	}

}