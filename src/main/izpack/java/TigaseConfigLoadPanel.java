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
import com.izforge.izpack.installer.IzPanel;
import com.izforge.izpack.util.Debug;


import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.io.FileReader;
import java.io.File;

/**
 * The Hello panel class.
 *
 * @author Julien Ponge
 */
public class TigaseConfigLoadPanel extends IzPanel {

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
	public TigaseConfigLoadPanel(InstallerFrame parent, InstallData idata) {
		super(parent, idata, new IzPanelLayout());

		// The config label.
		add(LabelFactory.create(parent.langpack.getString("TigaseConfigLoadPanel.info"),
				parent.icons.getImageIcon("edit"), LEADING), NEXT_LINE);
		// The text area which shows the info.
		textArea = new JTextArea(config);
		textArea.setCaretPosition(0);
		textArea.setEditable(false);
		JScrollPane scroller = new JScrollPane(textArea);
		add(scroller, NEXT_LINE);
		// At end of layouting we should call the completeLayout method also they do nothing.
		getLayoutHelper().completeLayout();
	}

	public void panelActivate() {
		super.panelActivate();
		// Existing configuration loading
		Debug.trace("panelActivate called for load pael");
		loadConfig();
	}

	private void loadConfig() {
		// Try to read the config file.
		File configPath = null;
		config = "";
		try {
			if (idata.getVariable("searchTigaseHome") == null
				|| idata.getVariable("searchTigaseHome").isEmpty()) {
				configPath = new File(idata.getVariable("INSTALL_PATH"),
					"etc/init.properties");
			} else {
				configPath = new File(idata.getVariable("searchTigaseHome"),
					"etc/init.properties");
			}
			if (configPath.exists()) {
				Properties props = new Properties();
				props.load(new FileReader(configPath));
				Debug.trace("Loading init.properties file...");
				for (String name: props.stringPropertyNames()) {
					config += name + " = " + props.getProperty(name) + "\n";
				}
				Debug.trace(config);
				Debug.trace("Done.");
				Debug.trace("Loading variables....");
				for (String name: TigaseConfigConst.tigaseIzPackMap.keySet()) {
					String varName = TigaseConfigConst.tigaseIzPackMap.get(name);
					if (varName != null) {
						Debug.trace("Loading: " + varName + " = " + props.getProperty(name));

						if (varName.equals(TigaseConfigConst.DEBUG)) {
							if (props.getProperty(name) != null) {
								parseDebugs(props.getProperty(name));
								Debug.trace("Loaded: " + varName + " = " + props.getProperty(name));
							} else {
								Debug.trace("Missing configuration for " + varName);
							}
							continue;
						}

						if (varName.equals(TigaseConfigConst.PLUGINS)) {
							if (props.getProperty(name) != null) {
								parsePlugins(props.getProperty(name));
								Debug.trace("Loaded: " + varName + " = " + props.getProperty(name));
							} else {
								Debug.trace("Missing configuration for " + varName);
							}
							continue;
						}

						if (varName.equals(TigaseConfigConst.USER_DB_URI)) {
							if (props.getProperty(name) != null) {
								parseUserDbUri(props.getProperty(name));
								Debug.trace("Loaded: " + varName + " = " + props.getProperty(name));
							} else {
								Debug.trace("Missing configuration for " + varName);
							}
							continue;
						}

						if (varName.equals(TigaseConfigConst.DB_TYPE)) {
							if (props.getProperty(name) != null) {
								String dbType =
                  TigaseConfigConst.userDBUriMap.get(props.getProperty(name));
								if (dbType == null) {
									dbType = "Other";
								}
								idata.setVariable(TigaseConfigConst.DB_TYPE, dbType);
								Debug.trace("Loaded: " + varName + " = " + dbType);
							} else {
								Debug.trace("Missing configuration for " + varName);
							}
							continue;
						}

						if (varName.equals(TigaseConfigConst.AUTH_HANDLE)) {
							if (props.getProperty(name) != null) {
								idata.setVariable(TigaseConfigConst.AUTH_HANDLE,
									props.getProperty(name));
								Debug.trace("Loaded: " + varName + " = " + props.getProperty(name));
							} else {
								Debug.trace("Missing configuration for " + varName);
							}
							continue;
						}

						if (varName.equals(TigaseConfigConst.MUC_COMP)) {
							if ((props.getProperty("--comp-name-1") != null
									&& props.getProperty("--comp-name-1").equals("muc"))
								|| (props.getProperty("--comp-name-2") != null
									&& props.getProperty("--comp-name-2").equals("muc"))) {
									idata.setVariable(TigaseConfigConst.MUC_COMP, "on");
							}
								Debug.trace("Loaded: " + varName + " = " +
									idata.getVariable(TigaseConfigConst.MUC_COMP));
							continue;
						}

						if (varName.equals(TigaseConfigConst.PUBSUB_COMP)) {
							if ((props.getProperty("--comp-name-1") != null
									&& props.getProperty("--comp-name-1").equals("pubsub"))
								|| (props.getProperty("--comp-name-2") != null
									&& props.getProperty("--comp-name-2").equals("pubsub"))) {
									idata.setVariable(TigaseConfigConst.PUBSUB_COMP, "on");
							}
							Debug.trace("Load: " + "--comp-name-" + " = " + "pubsub");
							continue;
						}

						if (varName.equals(TigaseConfigConst.AUTH_DB_URI)) {
							parseAuthDbUri(props.getProperty(name));
							Debug.trace("Load: " + varName + " = " + props.getProperty(name));
							continue;
						}

						idata.setVariable(varName, props.getProperty(name));
					}
				}
				Debug.trace("Done.");
			} else {
				config += "The config file: " + configPath + " seems to not exist...";
			}
		} catch (Exception err) {
			config = "Error : could not load the config file: " + configPath + "\n";
			config += err.toString() + "\n";
			for (StackTraceElement ste: err.getStackTrace()) {
				config += ste.toString() + "\n";
			}
		}
		textArea.setText(config);
	}

	private void parseDebugs(String debugs) {
		String[] ardebugs = debugs.split(",");
		Set<String> knownDebugs = TigaseConfigConst.debugMap.keySet();
		for (String debug: ardebugs) {
			if (knownDebugs.contains(debug)) {
				idata.setVariable(TigaseConfigConst.debugMap.get(debug), debug);
			}
		}
	}

	private void parsePlugins(String plugins) {
		String[] arplugins = plugins.split(",");
		Set<String> knownPlugins = TigaseConfigConst.pluginsMap.keySet();
		for (String plugin: arplugins) {
			if (knownPlugins.contains(plugin)) {
				idata.setVariable(TigaseConfigConst.pluginsMap.get(plugin), plugin);
			}
		}
	}

	private Pattern dbUriPattern =
    Pattern.compile(
			"jdbc:([^:]+(:[^:]+)?):(//([^/]+))?/?([a-zA-Z_/]+)[;\\?]?(user=([^;&]+))?[;&]?(password=([^;&]+))?[;&]?(.*)");

	private void parseUserDbUri(String dbUri) {
		Matcher m = dbUriPattern.matcher(dbUri);
		if (m.matches()) {
			String jdbcDriver = m.group(1);
			String host = m.group(4);
			String dbName = m.group(5);
			String userName = m.group(7);
			String userPass = m.group(9);
			String otherPars = m.group(10);
// 		idata.setVariable(TigaseConfigConst.DB_TYPE,
// 			TigaseConfigConst.userDBUriMap.get(jdbcDriver));
			if (jdbcDriver.equals("mysql")) {
				idata.setVariable("dbSuperuser", "root");
			}
			if (jdbcDriver.equals("postgresql")) {
				idata.setVariable("dbSuperuser", "postgres");
			}
			if (host != null) {
				idata.setVariable("dbHost", host);
			}
			if (dbName != null) {
				if (jdbcDriver.equals("derby")) {
					idata.setVariable("DerbyDBPath", "/"+dbName);
				} else {
					idata.setVariable("dbName", dbName);
				}
			}
			if (userName != null) {
				idata.setVariable("dbUser", userName);
			}
			if (userPass != null) {
				idata.setVariable("dbPass", userPass);
			}
			if (otherPars != null) {
				idata.setVariable("dbParams", otherPars);
			}
		} else {
			Debug.trace("Hm, the dbUri doesn't match regex: " + dbUri);
		}
	}

	private void parseAuthDbUri(String dbUri) {
		Matcher m = dbUriPattern.matcher(dbUri);
		if (m.matches()) {
			String jdbcDriver = m.group(1);
			String host = m.group(4);
			String dbName = m.group(5);
			String userName = m.group(7);
			String userPass = m.group(9);
			String otherPars = m.group(10);
			if (jdbcDriver != null) {
				idata.setVariable("dbAuthType", jdbcDriver);
			}
			if (host != null) {
				idata.setVariable("dbAuthHost", host);
			}
			if (dbName != null) {
				idata.setVariable("dbAuthName", dbName);
			}
			if (userName != null) {
				idata.setVariable("dbAuthUser", userName);
			}
			if (userPass != null) {
				idata.setVariable("dbAuthPass", userPass);
			}
			if (otherPars != null) {
				idata.setVariable("dbAuthParams", otherPars);
			}
		} else {
			Debug.trace("Hm, the dbAuthUri doesn't match regex: " + dbUri);
		}
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
