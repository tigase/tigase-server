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
import java.io.FileReader;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import com.izforge.izpack.gui.IzPanelLayout;
import com.izforge.izpack.gui.LabelFactory;
import com.izforge.izpack.installer.AutomatedInstallData;
import com.izforge.izpack.installer.InstallData;
import com.izforge.izpack.installer.InstallerFrame;
import com.izforge.izpack.installer.IzPanel;
import com.izforge.izpack.util.Debug;

/**
 * The Hello panel class.
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class TigaseConfigLoadPanel extends IzPanel {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	private JTextArea textArea = null;

	/**
	 * The constructor.
	 *
	 * @param parent The parent.
	 * @param idata  The installation data.
	 */
	public TigaseConfigLoadPanel(InstallerFrame parent, InstallData idata) {
		super(parent, TigaseInstallerCommon.init(idata), new IzPanelLayout());

		// The config label.
		add(LabelFactory.create(parent.langpack.getString("TigaseConfigLoadPanel.info"),
				parent.icons.getImageIcon("edit"), LEADING), NEXT_LINE);
		// The text area which shows the info.
		textArea = new JTextArea("");
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
		String config = new TigaseConfigLoadHelper().loadConfig(idata);
		textArea.setText(config);
	}


	/**
	 * Indicates whether the panel has been validated or not.
	 *
	 * @return Always true.
	 */
	public boolean isValidated() {
		return true;
	}

}

class TigaseConfigLoadHelper {

	String loadConfig(AutomatedInstallData idata) {
		// Try to read the config file.
		File configPath = null;
		StringBuilder config = new StringBuilder();
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
					config.append(name + " = " + props.getProperty(name) + "\n");
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
								parseDebugs(props.getProperty(name), idata);
								Debug.trace("Loaded: " + varName + " = " + props.getProperty(name));
							} else {
								Debug.trace("Missing configuration for " + varName);
							}
							continue;
						}

						if (varName.equals(TigaseConfigConst.PLUGINS)) {
							if (props.getProperty(name) != null) {
								parsePlugins(props.getProperty(name), idata);
								Debug.trace("Loaded: " + varName + " = " + props.getProperty(name));
							} else {
								Debug.trace("Missing configuration for " + varName);
							}
							continue;
						}

						if (varName.equals(TigaseConfigConst.USER_DB_URI)) {
							if (props.getProperty(name) != null) {
								parseUserDbUri(props.getProperty(name), idata);
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
							for (int i = 1 ; i <= 10 ; i++ ) {
							if ((props.getProperty("--comp-name-" + i) != null
									&& props.getProperty("--comp-name-" + i).equals("muc"))) {
									idata.setVariable(TigaseConfigConst.MUC_COMP, "on");
							}}
								Debug.trace("Loaded: " + varName + " = " +
									idata.getVariable(TigaseConfigConst.MUC_COMP));
							continue;
						}

						if (varName.equals(TigaseConfigConst.PUBSUB_COMP)) {
							for (int i = 1 ; i <= 10 ; i++ ) {
							if ((props.getProperty("--comp-name-" + i) != null
									&& props.getProperty("--comp-name-" + i).equals("pubsub"))) {
									idata.setVariable(TigaseConfigConst.PUBSUB_COMP, "on");
							}}
								Debug.trace("Loaded: " + varName + " = " +
									idata.getVariable(TigaseConfigConst.PUBSUB_COMP));
							continue;
						}

						if (varName.equals(TigaseConfigConst.SOCKS5_COMP)) {
							for (int i = 1 ; i <= 10 ; i++ ) {
							if ((props.getProperty("--comp-name-" + i) != null
									&& props.getProperty("--comp-name-" + i).equals("proxy"))) {
									idata.setVariable(TigaseConfigConst.SOCKS5_COMP, "on");
							}}
								Debug.trace("Loaded: " + varName + " = " +
									idata.getVariable(TigaseConfigConst.SOCKS5_COMP));
							continue;
						}

						if (varName.equals(TigaseConfigConst.STUN_COMP)) {
							for (int i = 1 ; i <= 10 ; i++ ) {
							if ((props.getProperty("--comp-name-" + i) != null
									&& props.getProperty("--comp-name-" + i).equals("stun"))) {
									idata.setVariable(TigaseConfigConst.STUN_COMP, "on");
							}}
								Debug.trace("Loaded: " + varName + " = " +
									idata.getVariable(TigaseConfigConst.STUN_COMP));
							continue;
						}

						if (varName.equals(TigaseConfigConst.HTTP_COMP)) {
							for (int i = 1 ; i <= 10 ; i++ ) {
							if ((props.getProperty("--comp-name-" + i) != null
									&& props.getProperty("--comp-name-" + i).equals("rest"))) {
									idata.setVariable(TigaseConfigConst.HTTP_COMP, "on");
							}}
								Debug.trace("Loaded: " + varName + " = " +
									idata.getVariable(TigaseConfigConst.HTTP_COMP));
							continue;
						}

						if (varName.equals(TigaseConfigConst.ARCHIVE_COMP)) {
							for (int i = 1 ; i <= 10 ; i++ ) {
							if ((props.getProperty("--comp-name-" + i) != null
									&& props.getProperty("--comp-name-" + i).equals("message-archive"))) {
									idata.setVariable(TigaseConfigConst.ARCHIVE_COMP, "on");
							}}
								Debug.trace("Loaded: " + varName + " = " +
									idata.getVariable(TigaseConfigConst.ARCHIVE_COMP));
							continue;
						}


						if (varName.equals(TigaseConfigConst.AUTH_DB_URI)) {
							if (props.getProperty(name) != null) {
								parseAuthDbUri(props.getProperty(name), idata);
								Debug.trace("Loaded: " + varName + " = " + props.getProperty(name));
							} else {
								Debug.trace("Missing configuration for " + varName);
							}
							continue;
						}


						if (varName.equals(TigaseConfigConst.ACS_MUC_COMP)) {
							for (int i = 1 ; i <= 10 ; i++ ) {
								if ((props.getProperty("--comp-name-" + i) != null
										&& props.getProperty("--comp-class-" + i).equals(TigaseConfigConst.ACS_MUC_COMP_CLASS))) {
										idata.setVariable(TigaseConfigConst.ACS_MUC_COMP, "acs");
								}
							}
								Debug.trace("Loaded: " + varName + " = " +
									idata.getVariable(TigaseConfigConst.MUC_COMP));
							continue;
						}

						if (varName.equals(TigaseConfigConst.ACS_PUBSUB_COMP)) {
							for (int i = 1 ; i <= 10 ; i++ ) {
								if ((props.getProperty("--comp-name-" + i) != null
										&& props.getProperty("--comp-class-" + i).equals(TigaseConfigConst.ACS_PUBSUB_COMP_CLASS))) {
										idata.setVariable(TigaseConfigConst.ACS_PUBSUB_COMP, "acs");
								}
							}
								Debug.trace("Loaded: " + varName + " = " +
									idata.getVariable(TigaseConfigConst.PUBSUB_COMP));
							continue;
						}

						if (varName.equals(TigaseConfigConst.ACS_COMP)) {
							if ((props.getProperty("--sm-cluster-strategy-class") != null
									&& props.getProperty("--sm-cluster-strategy-class").equals(TigaseConfigConst.ACS_COMP_CLASS))) {
									idata.setVariable(TigaseConfigConst.ACS_COMP, "on");
							} else {
								idata.setVariable(TigaseConfigConst.ACS_COMP, "off");
							}
								Debug.trace("Loaded: " + varName + " = " +
									idata.getVariable(TigaseConfigConst.ACS_COMP));
							continue;
						}


						if (props.getProperty(name) != null) {
							idata.setVariable(varName, props.getProperty(name));
						}
					}
				}
				Debug.trace("Done.");
			} else {
				config.append("The config file: " + configPath + " seems to not exist...");
			}
		} catch (Exception err) {
			StringBuilder errorConfig = new StringBuilder();
			errorConfig.append("Error : could not load the config file: " + configPath + "\n");
			errorConfig.append(err.toString() + "\n");
			for (StackTraceElement ste: err.getStackTrace()) {
				errorConfig.append(ste.toString() + "\n");
			}
			return errorConfig.toString();
		}
		return config.toString();
	}

	private void parseDebugs(String debugs, AutomatedInstallData idata) {
		String[] ardebugs = debugs.split(",");
		Set<String> knownDebugs = TigaseConfigConst.debugMap.keySet();
		for (String debug: ardebugs) {
			if (knownDebugs.contains(debug)) {
				idata.setVariable(TigaseConfigConst.debugMap.get(debug), debug);
			}
		}
	}

	private void parsePlugins(String plugins, AutomatedInstallData idata) {
		String[] arplugins = plugins.split(",");
		Set<String> knownPlugins = TigaseConfigConst.pluginsMap.keySet();
		for (String plugin: arplugins) {
			if (knownPlugins.contains(plugin)) {
				idata.setVariable(TigaseConfigConst.pluginsMap.get(plugin), plugin);
			}
		}
	}

	private static Pattern dbUriPattern =
    Pattern.compile(
			"jdbc:([^:]+(:[^:]+)?):(//([^/]+))?/?([0-9.a-zA-Z_/-]+)[;\\?]?(user=([^;&]+))?[;&]?(password=([^;&]+))?[;&]?(.*)");

	private void parseUserDbUri(String dbUri, AutomatedInstallData idata) {
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
			if (jdbcDriver.equals("sqlserver")) {
				idata.setVariable("dbSuperuser", "root");
			}
			if (host != null) {
				idata.setVariable("dbHost", host);
			}
			if (dbName != null) {
				if (jdbcDriver.equals("derby")) {
					idata.setVariable("DerbyDBPath", "/" + dbName);
					Debug.trace("DerbyDBPath set to /" + dbName);
				} else {
					idata.setVariable("dbName", dbName);
					Debug.trace("dbName read: " + dbName);
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

	private void parseAuthDbUri(String dbUri, AutomatedInstallData idata) {
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

}
