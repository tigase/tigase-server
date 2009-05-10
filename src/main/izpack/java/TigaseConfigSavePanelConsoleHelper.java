package com.izforge.izpack.panels;

import java.io.PrintWriter;
import java.util.Properties;

import com.izforge.izpack.installer.AutomatedInstallData;
import com.izforge.izpack.installer.PanelConsole;
import com.izforge.izpack.installer.PanelConsoleHelper;

public class TigaseConfigSavePanelConsoleHelper  extends PanelConsoleHelper implements PanelConsole {

	public boolean runConsole(AutomatedInstallData installData) {
		// called for side effect of creating configuration
		new TigaseConfigSaveHelper().showConfig(installData);
		return true;
	}

	public boolean runConsoleFromPropertiesFile(
			AutomatedInstallData installData, Properties p) {
		// called for side effect of creating configuration
		new TigaseConfigSaveHelper().showConfig(installData);
		return true;
	}

	public boolean runGeneratePropertiesFile(AutomatedInstallData installData,
			PrintWriter printWriter) {
		// called for side effect of creating configuration
		new TigaseConfigSaveHelper().showConfig(installData);
		return true;
	}

}
