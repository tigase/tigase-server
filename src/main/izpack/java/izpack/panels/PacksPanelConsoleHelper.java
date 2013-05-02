package com.izforge.izpack.panels;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.izforge.izpack.Pack;
import com.izforge.izpack.installer.AutomatedInstallData;
import com.izforge.izpack.installer.ConsoleInstallHelper;
import com.izforge.izpack.installer.ConsoleMenu;
import com.izforge.izpack.installer.IConsoleMenuItem;
import com.izforge.izpack.installer.PanelConsole;
import com.izforge.izpack.installer.PanelConsoleHelper;
import com.izforge.izpack.panels.PacksHelper.PackSelection;
public class PacksPanelConsoleHelper 
extends PanelConsoleHelper 
implements PanelConsole 
{

	private PacksHelper packsHelper = new PacksHelper();
	private ConsoleInstallHelper helper = ConsoleInstallHelper.getInstance();
	
	public boolean runConsole(final AutomatedInstallData installData) {

		final List<IConsoleMenuItem> items 
			= packsHelper.getPacksSelections(installData);
		try {
			final String info = installData.langpack.getString("PacksPanel.info");
			
			ConsoleMenu consoleMenu = new ConsoleMenu(helper.console) {
				protected List<IConsoleMenuItem> getMenuItems() {
					return items;
				}

				protected String getHeader() {
					return info;
				}
			};
			consoleMenu.run();
			
			List<Pack> selectedPacks = new ArrayList<Pack>();
			for (IConsoleMenuItem pack : items) {
				PackSelection selection = (PackSelection) pack;
				if (selection.isSelected) 
				{
					selectedPacks.add(selection.getPack());
				}
			}
			
			installData.selectedPacks = selectedPacks;
			
		} catch (IOException e) {
			e.printStackTrace();
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

class PacksHelper {
	
	public List<IConsoleMenuItem> getPacksSelections(AutomatedInstallData installData) {
		List<IConsoleMenuItem> result = new ArrayList<IConsoleMenuItem>();
		for (Pack pack : installData.availablePacks) {
			result.add(new PackSelection(pack)); 
		}
		return result;
	}
	
	class PackSelection implements IConsoleMenuItem {
		final Pack pack;
		boolean isSelected;
		
		public PackSelection(Pack pack) {
			this.pack = pack;
			this.isSelected = pack.preselected;
		}

		public Pack getPack() {
			return pack;
		}

		public String renderToString() {
			String option = "";
			if (pack.required == false) {
				String selected = isSelected ? "x" : " ";
				option = "[" + selected + "] ";
			}
			return option + pack.name + ", " + pack.description;
		}

		public void runAction() {
			if (pack.required == false) {
				isSelected = !isSelected;
			} 
		}
	}
	
}

