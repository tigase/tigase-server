package com.izforge.izpack.panels;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Properties;

import com.izforge.izpack.installer.AutomatedInstallData;
import com.izforge.izpack.installer.ConsoleInstallHelper;
import com.izforge.izpack.installer.PanelConsole;
import com.izforge.izpack.installer.PanelConsoleHelper;
import com.izforge.izpack.installer.ResourceNotFoundException;

public class HTMLLicencePanelConsoleHelper 
extends PanelConsoleHelper 
implements PanelConsole
{

	private ConsoleInstallHelper helper = ConsoleInstallHelper.getInstance();
	
	public boolean runConsole(AutomatedInstallData installData) {
		String headerLabel = installData.langpack.getString("LicencePanel.info");
		helper.console.displayLabel(headerLabel);
		
		try {
			// load resource
			//TODO how to get resource id from installData?
			String resourceId = "HTMLLicencePanel.licence"; 
			String htmlInfo = helper.resource.getResourceAsString(resourceId);
			htmlInfo = helper.variables.expand(htmlInfo, installData);
			
			// convert from html
			String info = helper.html.extractTextFromSimplifiedHTML(htmlInfo);

			// display using pager
			helper.pager.displayLongText(info);
			
			String iAgree = installData.langpack.getString("LicencePanel.agree");
			String iDoNotAgree = installData.langpack.getString("LicencePanel.notagree");
			switch (helper.console.chooseAction(iAgree, iDoNotAgree)) {
			case 1: return continueQuitOrReboot(installData, this);
			case 2: return false;
			}
			
		}
		catch (ResourceNotFoundException re) {
			re.printStackTrace();
			return false;
		}
		catch (IOException ioe) {
			ioe.printStackTrace();
			return false;
		}
		
		// should be unreachable
		return false;
	}

	
	// not implemented
	public boolean runConsoleFromPropertiesFile(
			AutomatedInstallData installData, Properties p) {
		return false;
	}

	public boolean runGeneratePropertiesFile(AutomatedInstallData installData,
			PrintWriter printWriter) {
		return false;
	}
}
