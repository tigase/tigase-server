package com.izforge.izpack.panels;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Properties;

import com.izforge.izpack.installer.AutomatedInstallData;
import com.izforge.izpack.installer.ConsoleInstallHelper;
import com.izforge.izpack.installer.PanelConsole;
import com.izforge.izpack.installer.PanelConsoleHelper;
import com.izforge.izpack.installer.ResourceNotFoundException;


// hardcoded resource name, should work only for standard one html panel
// uses simple html format, and extracts text from it using regex
// only interactive mode implemented
public class HTMLInfoPanelConsoleHelper extends PanelConsoleHelper implements PanelConsole 
{
	private ConsoleInstallHelper helper = ConsoleInstallHelper.getInstance();
		
	public boolean runConsole(AutomatedInstallData installData) 
	{
		try {
			// load resource
			//TODO how to get resource id from installData?
			String resourceId = "HTMLInfoPanel.info"; 
			String htmlInfo = helper.resource.getResourceAsString(resourceId);
			htmlInfo = helper.variables.expand(htmlInfo, installData);
			
			// convert from html
			String info = helper.html.extractTextFromSimplifiedHTML(htmlInfo);

			// display using pager
			helper.pager.displayLongText(info);
		}
		catch (ResourceNotFoundException re) {
			re.printStackTrace();
			return false;
		}
		catch (IOException ioe) {
			ioe.printStackTrace();
			return false;
		}
				
		return continueQuitOrReboot(installData, this);
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
