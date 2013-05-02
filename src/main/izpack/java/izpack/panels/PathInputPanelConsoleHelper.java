package com.izforge.izpack.panels;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Properties;

import com.izforge.izpack.installer.AutomatedInstallData;
import com.izforge.izpack.installer.ConsoleInstallHelper;
import com.izforge.izpack.installer.PanelConsole;
import com.izforge.izpack.installer.PanelConsoleHelper;
import com.izforge.izpack.installer.ValidatingConverter;

public abstract class PathInputPanelConsoleHelper extends PanelConsoleHelper implements PanelConsole {

    protected ConsoleInstallHelper helper = ConsoleInstallHelper.getInstance();
    
    private String getIntroText(AutomatedInstallData installData) {
        String introText = getI18nStringForClass("extendedIntro", "PathInputPanel", installData);
        if (introText == null || introText.endsWith("extendedIntro")
                || introText.indexOf('$') > -1)
        {
            introText = getI18nStringForClass("intro", "PathInputPanel", installData);
            if (introText == null || introText.endsWith("intro"))
            {
                introText = "";
            }
        }
        return introText;
    }
    
	public boolean runConsole(AutomatedInstallData installData) 
	{
		try {
			final String introText = getIntroText(installData);
	        helper.console.displayLabel(introText);
	        
	        // get valid path
	        ValidatingConverter<String, File> pathValidator = getPathValidator();
			File inputPath = helper.console.readUntilValid(
					"Enter path",
					pathValidator);	
			
			onResult(inputPath, installData);
		}
		catch (IOException ioe) {
			ioe.printStackTrace();
			return false;
		}
		
		return true;
	}

	public abstract ValidatingConverter<String, File> getPathValidator();
	
	abstract public void onResult(File inputPath, AutomatedInstallData iData);

	public boolean runConsoleFromPropertiesFile(
			AutomatedInstallData installData, Properties p) {
		return false;
	}

	public boolean runGeneratePropertiesFile(AutomatedInstallData installData,
			PrintWriter printWriter) {
		return false;
	}

}
