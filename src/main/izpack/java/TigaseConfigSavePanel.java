/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2007 "Artur Hefczyc" <artur.hefczyc@tigase.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License.
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

package com.izforge.izpack.panels;

import java.io.File;
import java.io.FileWriter;
import java.util.Map;
import java.util.Properties;

import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import com.izforge.izpack.gui.IzPanelLayout;
import com.izforge.izpack.installer.AutomatedInstallData;
import com.izforge.izpack.installer.InstallData;
import com.izforge.izpack.installer.InstallerFrame;
import com.izforge.izpack.installer.IzPanel;
import com.izforge.izpack.util.Debug;
import com.izforge.izpack.util.OsVersion;

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
}

class TigaseConfigSaveHelper {
	
	String showConfig(VariablesSource variablesSource) {
		StringBuilder config = new StringBuilder();
		int comp_idx = 0;
		for (Map.Entry<String, String> entry:
        TigaseConfigConst.tigaseIzPackMap.entrySet()) {
			String varName = entry.getValue();
			String varValue = variablesSource.getVariable(varName);

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
				TigaseConfigConst.props.setProperty("root-tigase-db-uri",
					getRootTigaseDBUri(variablesSource));
				TigaseConfigConst.props.setProperty("root-db-uri", getRootDBUri(variablesSource));
				Debug.trace("Set: " + entry.getKey() + " = " + getDBUri(variablesSource));
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
			if (varName.equals(TigaseConfigConst.MUC_COMP)) {
				if (varValue.equals("on")) {
					++comp_idx;
					TigaseConfigConst.props.setProperty("--comp-name-"+comp_idx, "muc");
					TigaseConfigConst.props.setProperty("--comp-class-"+comp_idx,
						"tigase.muc.MUCComponent");
				}
				Debug.trace("Set: " + "--comp-name-"+comp_idx + " = " + "muc");
				continue;
			}
			if (varName.equals(TigaseConfigConst.PUBSUB_COMP)) {
				if (varValue.equals("on")) {
					++comp_idx;
					TigaseConfigConst.props.setProperty("--comp-name-"+comp_idx, "pubsub");
					TigaseConfigConst.props.setProperty("--comp-class-"+comp_idx,
						"tigase.pubsub.PubSubClusterComponent");
				}
				Debug.trace("Set: " + "--comp-name-"+comp_idx + " = " + "pubsub");
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
		if (database.equals("pgsql")) {
			db_uri += "postgresql:";
		} else {
			db_uri += database + ":";
		}
		if (database.equals("derby")) {
			String derby_path = variablesSource.getVariable("DerbyDBPath");
			if (OsVersion.IS_WINDOWS) {
				derby_path = derby_path.replace("\\", "\\\\");
			}
			db_uri += derby_path;
		} else {
			db_uri += "//" + variablesSource.getVariable("dbHost");
			db_uri += "/" + variablesSource.getVariable("dbName");
			db_uri += "?user=" + variablesSource.getVariable("dbUser");
			if (variablesSource.getVariable("dbPass") != null
				&& !variablesSource.getVariable("dbPass").isEmpty()) {
				db_uri += "&password=" + variablesSource.getVariable("dbPass");
			}
		}
		return db_uri;
	}

	private String getRootTigaseDBUri(VariablesSource variablesSource) {
		String db_uri = "jdbc:";
		String database = getUserDB(variablesSource);
		if (database.equals("pgsql")) {
			db_uri += "postgresql:";
		} else {
			db_uri += database + ":";
		}
		if (database.equals("derby")) {
			db_uri += variablesSource.getVariable("DerbyDBPath") + ";create=true";
		} else {
			db_uri += "//" + variablesSource.getVariable("dbHost");
			db_uri += "/" + variablesSource.getVariable("dbName");
			db_uri += "?user=" + variablesSource.getVariable("dbSuperuser");
			if (variablesSource.getVariable("dbSuperpass") != null
				&& !variablesSource.getVariable("dbSuperpass").isEmpty()) {
				db_uri += "&password=" + variablesSource.getVariable("dbSuperpass");
			}
		}
		return db_uri;
	}

	private String getRootDBUri(VariablesSource variablesSource) {
		String db_uri = "jdbc:";
		String db = "";
		String database = getUserDB(variablesSource);
		if (database.equals("pgsql")) {
			db_uri += "postgresql:";
			db = "/postgres";
		} else {
			db_uri += database + ":";
			if (database.equals("mysql")) {
				db = "/mysql";
			}
		}
		if (database.equals("derby")) {
			db_uri += variablesSource.getVariable("DerbyDBPath") + ";create=true";
		} else {
			db_uri += "//" + variablesSource.getVariable("dbHost");
			db_uri += db;
			db_uri += "?user=" + variablesSource.getVariable("dbSuperuser");
			if (variablesSource.getVariable("dbSuperpass") != null
				&& !variablesSource.getVariable("dbSuperpass").isEmpty()) {
				db_uri += "&password=" + variablesSource.getVariable("dbSuperpass");
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
		} else {
			db_uri += "//" + variablesSource.getVariable("dbAuthHost");
			db_uri += "/" + variablesSource.getVariable("dbAuthName");
			db_uri += "?user=" + variablesSource.getVariable("dbAuthUser");
			if (variablesSource.getVariable("dbAuthPass") != null
				&& !variablesSource.getVariable("dbAuthPass").isEmpty()) {
				db_uri += "&password=" + variablesSource.getVariable("dbAuthPass");
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

	private String getUserDB(VariablesSource variablesSource) {
		String dbVar = variablesSource.getVariable(TigaseConfigConst.DB_TYPE);
		String result = TigaseConfigConst.userDBMap.get(dbVar);
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