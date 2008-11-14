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
import com.izforge.izpack.util.Debug;
import com.izforge.izpack.util.OsVersion;
import com.izforge.izpack.installer.IzPanel;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Map;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.File;

/**
 * The Hello panel class.
 *
 * @author Julien Ponge
 */
public class TigaseConfigSavePanel extends IzPanel {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	private JTextArea textArea = null;

	private String config = "";

	/**
	 * The constructor.
	 *
	 * @param parent The parent.
	 * @param idata  The installation data.
	 */
	public TigaseConfigSavePanel(InstallerFrame parent, InstallData idata) {
		super(parent, idata, new IzPanelLayout());

		// The config label.
		String msg = parent.langpack.getString("TigaseConfigSavePanel.info");
		add(createMultiLineLabel(msg));
		add(IzPanelLayout.createParagraphGap());
		// The text area which shows the info.
		textArea = new JTextArea(config);
		textArea.setCaretPosition(0);
		textArea.setEditable(true);
		JScrollPane scroller = new JScrollPane(textArea);
		add(scroller, NEXT_LINE);
		// At end of layouting we should call the completeLayout method also they do nothing.
		getLayoutHelper().completeLayout();
	}

	public void panelActivate() {
		super.panelActivate();
		// Existing configuration loading
		showConfig();
	}

	private void showConfig() {
		TigaseConfigConst.props = new Properties();
		config = "";
		int comp_idx = 0;
		for (Map.Entry<String, String> entry:
        TigaseConfigConst.tigaseIzPackMap.entrySet()) {
			String varName = entry.getValue();
			String varValue = idata.getVariable(varName);

			if (varName.equals(TigaseConfigConst.DEBUG)) {
				String debugVar = getDebugs();
				if (!debugVar.isEmpty()) {
					TigaseConfigConst.props.setProperty(entry.getKey(), debugVar);
				}
				Debug.trace("Set: " + entry.getKey() + " = " + debugVar);
				continue;
			}
			if (varName.equals(TigaseConfigConst.PLUGINS)) {
				String pluginsVar = getPlugins();
				if (!pluginsVar.isEmpty()) {
					TigaseConfigConst.props.setProperty(entry.getKey(), pluginsVar);
				}
				Debug.trace("Set: " + entry.getKey() + " = " + pluginsVar);
				continue;
			}
			if (varName.equals(TigaseConfigConst.USER_DB_URI)) {
				TigaseConfigConst.props.setProperty(entry.getKey(), getDBUri());
				TigaseConfigConst.props.setProperty("root-tigase-db-uri",
					getRootTigaseDBUri());
				TigaseConfigConst.props.setProperty("root-db-uri", getRootDBUri());
				Debug.trace("Set: " + entry.getKey() + " = " + getDBUri());
				continue;
			}

			if (varValue == null) continue;

			if (varName.equals(TigaseConfigConst.DB_TYPE)) {
				TigaseConfigConst.props.setProperty(entry.getKey(), getUserDB());
				Debug.trace("Set: " + entry.getKey() + " = " + getUserDB());
				continue;
			}
			if (varName.equals(TigaseConfigConst.AUTH_HANDLE)) {
				TigaseConfigConst.props.setProperty(entry.getKey(),
					getAuthHandler(varValue));
				Debug.trace("Set: " + entry.getKey() + " = " + getAuthHandler(varValue));
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
				String auth_db_uri = getAuthUri();
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
				config += name + " = " + TigaseConfigConst.props.getProperty(name) + "\n";
			}
		}
		textArea.setText(config);
	}

	private String getDBUri() {
		String db_uri = "jdbc:";
		String database = getUserDB();
		if (database.equals("pgsql")) {
			db_uri += "postgresql:";
		} else {
			db_uri += database + ":";
		}
		if (database.equals("derby")) {
			String derby_path = idata.getVariable("DerbyDBPath");
			if (OsVersion.IS_WINDOWS) {
				derby_path = derby_path.replace("\\", "\\\\");
			}
			db_uri += derby_path;
		} else {
			db_uri += "//" + idata.getVariable("dbHost");
			db_uri += "/" + idata.getVariable("dbName");
			db_uri += "?user=" + idata.getVariable("dbUser");
			if (idata.getVariable("dbPass") != null
				&& !idata.getVariable("dbPass").isEmpty()) {
				db_uri += "&password=" + idata.getVariable("dbPass");
			}
		}
		return db_uri;
	}

	private String getRootTigaseDBUri() {
		String db_uri = "jdbc:";
		String database = getUserDB();
		if (database.equals("pgsql")) {
			db_uri += "postgresql:";
		} else {
			db_uri += database + ":";
		}
		if (database.equals("derby")) {
			db_uri += idata.getVariable("DerbyDBPath") + ";create=true";
		} else {
			db_uri += "//" + idata.getVariable("dbHost");
			db_uri += "/" + idata.getVariable("dbName");
			db_uri += "?user=" + idata.getVariable("dbSuperuser");
			if (idata.getVariable("dbSuperpass") != null
				&& !idata.getVariable("dbSuperpass").isEmpty()) {
				db_uri += "&password=" + idata.getVariable("dbSuperpass");
			}
		}
		return db_uri;
	}

	private String getRootDBUri() {
		String db_uri = "jdbc:";
		String db = "";
		String database = getUserDB();
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
			db_uri += idata.getVariable("DerbyDBPath") + ";create=true";
		} else {
			db_uri += "//" + idata.getVariable("dbHost");
			db_uri += db;
			db_uri += "?user=" + idata.getVariable("dbSuperuser");
			if (idata.getVariable("dbSuperpass") != null
				&& !idata.getVariable("dbSuperpass").isEmpty()) {
				db_uri += "&password=" + idata.getVariable("dbSuperpass");
			}
		}
		return db_uri;
	}

	private String getAuthUri() {
		String db_uri = "jdbc:";
		String database = idata.getVariable(TigaseConfigConst.AUTH_DB_URI);
		db_uri += database + ":";
		if (database.equals("derby")) {
			String derby_path = idata.getVariable("DerbyDBPath");
			if (derby_path != null) {
				db_uri += derby_path;
			} else {
				return null;
			}
		} else {
			db_uri += "//" + idata.getVariable("dbAuthHost");
			db_uri += "/" + idata.getVariable("dbAuthName");
			db_uri += "?user=" + idata.getVariable("dbAuthUser");
			if (idata.getVariable("dbAuthPass") != null
				&& !idata.getVariable("dbAuthPass").isEmpty()) {
				db_uri += "&password=" + idata.getVariable("dbAuthPass");
			}
		}
		return db_uri;
	}

	private String getPlugins() {
		String plugins = "";
		if (idata.getVariable(TigaseConfigConst.ALL_PLUGINS[0]) == null) {
			// The Panel with debuging settings was not shown so all
			// settins are null, then we set a default: 'server'
			return "";
		}
		for (String plugin: TigaseConfigConst.ALL_PLUGINS) {
			if (!idata.getVariable(plugin).equals("off")) {
				if (!plugins.isEmpty()) {
					plugins += ",";
				}
				plugins += idata.getVariable(plugin);
			}
		}
		return plugins;
	}

	private String getDebugs() {

		String debugs = "";
		if (idata.getVariable(TigaseConfigConst.ALL_DEBUGS[0]) == null) {
			// The Panel with debuging settings was not shown so all
			// settins are null, then we set a default: 'server'
			return "server";
		}
		for (String deb: TigaseConfigConst.ALL_DEBUGS) {
			if (idata.getVariable(deb) == null || idata.getVariable(deb).equals("off")) {
				continue;
			}
			if (!debugs.isEmpty()) {
				debugs += ",";
			}
			debugs += idata.getVariable(deb);
		}
		return debugs;
	}

	private String getAuthHandler(String var) {
		if (var.equals("Standard")) {
			return getUserDB();
		}
		return var;
	}

	private String getUserDB() {
		String dbVar = idata.getVariable(TigaseConfigConst.DB_TYPE);
		String result = TigaseConfigConst.userDBMap.get(dbVar);
		return result != null ? result : "derby";
	}

	private void saveConfig() {
		// Try to read the config file.
		File configPath = null;
		File xmlConfigPath = null;
		try {
			if (idata.getVariable("searchTigaseHome") == null
				|| idata.getVariable("searchTigaseHome").isEmpty()) {
				configPath = new File(idata.getVariable("INSTALL_PATH"),
					"etc/init.properties");
				xmlConfigPath = new File(idata.getVariable("INSTALL_PATH"),
					"etc/tigase.xml");
			} else {
				configPath = new File(idata.getVariable("searchTigaseHome"),
					"etc/init.properties");
				xmlConfigPath = new File(idata.getVariable("searchTigaseHome"),
					"etc/tigase.xml");
			}
			FileWriter fw = new FileWriter(configPath, false);
			fw.write(textArea.getText());
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
			emitError("Can not write to config file", error);
		}
	}

	/**
	 * Indicates wether the panel has been validated or not.
	 *
	 * @return Always true.
	 */
	public boolean isValidated() {
		saveConfig();
		return true;
	}

}
