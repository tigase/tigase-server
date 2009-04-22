package com.izforge.izpack.panels;

import com.izforge.izpack.installer.AutomatedInstallData;

public class TigaseJDKPathPanelConsoleHelper 
extends JDKPathPanelConsoleHelper {

        public boolean runConsole(AutomatedInstallData installData) {
                TigaseInstallerCommon.init(installData);
                return super.runConsole(installData);
        }
        
}