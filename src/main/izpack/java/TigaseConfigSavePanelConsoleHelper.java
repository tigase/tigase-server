package com.izforge.izpack.panels;

import java.io.PrintWriter;
import java.util.Properties;

import com.izforge.izpack.installer.AutomatedInstallData;
import com.izforge.izpack.installer.PanelConsole;
import com.izforge.izpack.installer.PanelConsoleHelper;

public class TigaseConfigSavePanelConsoleHelper  
extends PanelConsoleHelper implements PanelConsole {

	public boolean runConsole(AutomatedInstallData installData) {
		TigaseConfigSaveHelper helper = new TigaseConfigSaveHelper();
		
		String config =  helper.showConfig(
				new IzPackInstallDataVariablesSource(installData));
		helper.saveConfig(installData, config);
		
		return true;
	}

	
	
	public boolean runConsoleFromPropertiesFile(
			AutomatedInstallData installData, Properties p) {
		return false;
	}

	public boolean runGeneratePropertiesFile(AutomatedInstallData installData,
			PrintWriter printWriter) {
		return false;
	}

}
