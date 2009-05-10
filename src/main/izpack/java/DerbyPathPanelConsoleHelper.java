package com.izforge.izpack.panels;

import java.io.PrintWriter;
import java.util.Properties;

import com.izforge.izpack.installer.AutomatedInstallData;
import com.izforge.izpack.installer.PanelConsole;
import com.izforge.izpack.installer.PanelConsoleHelper;

public class DerbyPathPanelConsoleHelper  extends PanelConsoleHelper implements PanelConsole {

	public boolean runConsole(AutomatedInstallData installData) {
		new DerbyPathPanelHelper().setDefaultPath(installData);
		return true;
	}

	public boolean runConsoleFromPropertiesFile(
			AutomatedInstallData installData, Properties p) {
		new DerbyPathPanelHelper().setDefaultPath(installData);
		return true;
	}

	public boolean runGeneratePropertiesFile(AutomatedInstallData installData,
			PrintWriter printWriter) {
		new DerbyPathPanelHelper().setDefaultPath(installData);
		return true;
	}

}
