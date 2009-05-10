package com.izforge.izpack.panels;

import java.io.PrintWriter;
import java.util.Properties;

import com.izforge.izpack.installer.AutomatedInstallData;
import com.izforge.izpack.installer.ConsoleInstallHelper;
import com.izforge.izpack.installer.PanelConsole;
import com.izforge.izpack.installer.PanelConsoleHelper;
import com.izforge.izpack.panels.TigaseDBHelper.ResultMessage;
import com.izforge.izpack.panels.TigaseDBHelper.TigaseDBTask;

public class TigaseDBCheckPanelConsoleHelper extends PanelConsoleHelper implements PanelConsole {

	private ConsoleInstallHelper helper = ConsoleInstallHelper.getInstance();
	
	public boolean runConsole(AutomatedInstallData installData) {
		
		TigaseDBHelper dbHelper = new TigaseDBHelper();
	
		helper.console.displayLabel("Performing DB tasks");
				
		for (TigaseDBTask task : TigaseDBHelper.Tasks.getTasksInOrder()) {
			helper.console.displayEmptyLine();
			helper.console.displayRaw(task.getDescription() + "  ");
			
			TigaseDBHelper.MsgTarget msgTarget = new TigaseDBHelper.MsgTarget() {
				public ResultMessage addResultMessage() {
					helper.console.displayEmptyLine();
					return new ResultMessage() {
						public void append(String msg) {
							helper.console.displayRaw(msg);
						}
					};
				}
			};

			task.execute(dbHelper, installData.getVariables(), msgTarget);
		}
		
		helper.console.displayEmptyLine();
		helper.console.displayEmptyLine();
		helper.console.displayEmptyLine();

		return true;
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
