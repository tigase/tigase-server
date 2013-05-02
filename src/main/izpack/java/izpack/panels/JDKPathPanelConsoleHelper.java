package com.izforge.izpack.panels;

import java.io.File;
import java.io.PrintWriter;
import java.util.Properties;

import com.izforge.izpack.installer.AutomatedInstallData;
import com.izforge.izpack.installer.Option;
import com.izforge.izpack.installer.ValidatingConverter;

public class JDKPathPanelConsoleHelper extends PathInputPanelConsoleHelper {

	private final static String JDK_PATH_VARIABLE_NAME = "JDKPath";
	
	public boolean runConsole(AutomatedInstallData installData) {
		return super.runConsole(installData);
	}

	public boolean runConsoleFromPropertiesFile(
			AutomatedInstallData installData, Properties p) {
		return super.runConsoleFromPropertiesFile(installData, p);
	}

	public boolean runGeneratePropertiesFile(AutomatedInstallData installData,
			PrintWriter printWriter) {
		return super.runGeneratePropertiesFile(installData, printWriter);
	}

	// sorry, no proper version validation, no version check
	// everything needs to be extracted
	// or (preferably) accessed from JDKPathPanel, I couldn't do it, too complicated :/
	public ValidatingConverter<String, File> getPathValidator() {
		return new ValidatingConverter<String, File>() {
			public Option<File> convert(String line) {
				File file = new File(line);
				if (file.exists() == false) {
					return Option.empty("Path doesn't exist");
				} else {
					return Option.full(file);
				}
			}        	
		};
	}	

	public void onResult(File inputPath, AutomatedInstallData iData) {
		iData.setVariable(
				JDK_PATH_VARIABLE_NAME, 
				inputPath.getAbsolutePath().toString());
	}

}