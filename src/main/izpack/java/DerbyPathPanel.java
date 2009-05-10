package com.izforge.izpack.panels;

import com.izforge.izpack.installer.AutomatedInstallData;
import com.izforge.izpack.installer.InstallData;
import com.izforge.izpack.installer.InstallerFrame;
import com.izforge.izpack.util.AbstractUIHandler;
import com.izforge.izpack.util.FileExecutor;
import com.izforge.izpack.util.OsVersion;
import com.izforge.izpack.util.os.RegistryDefaultHandler;
import com.izforge.izpack.util.os.RegistryHandler;
import com.izforge.izpack.util.VariableSubstitutor;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * Panel which asks for the Derby database path.
 *
 * @author Klaus Bartz
 */
public class DerbyPathPanel extends PathInputPanel {

	private static final long serialVersionUID = 1L;

	private String variableName = null;

	private DerbyPathPanelHelper helper = new DerbyPathPanelHelper();
	
	/**
	 * The constructor.
	 *
	 * @param parent The parent window.
	 * @param idata  The installation data.
	 */
	public DerbyPathPanel(InstallerFrame parent, InstallData idata) {
		super(parent, TigaseInstallerCommon.init(idata));
		setMustExist(false);
		setVariableName(helper.getVariableName());
	}

	/**
	 * Indicates wether the panel has been validated or not.
	 *
	 * @return Wether the panel has been validated or not.
	 */
	public boolean isValidated()
	{
		boolean retval = false;
		if (super.isValidated()) {
			idata.setVariable(getVariableName(), pathSelectionPanel.getPath());
			return true;
		}
		return false;
	}

	/**
	 * Called when the panel becomes active.
	 */
	public void panelActivate()
	{
		// Resolve the default for chosenPath
		super.panelActivate();
		// The variable will be exist if we enter this panel
		// second time. We would maintain the previos
		// selected path.
//		if (idata.getVariable(getVariableName()) != null) {
//			chosenPath = idata.getVariable(getVariableName());
//		} else {
//			if (OsVersion.IS_WINDOWS)	{
//				chosenPath = idata.getVariable(getVariableName()+".windows");
//			}
//			if (OsVersion.IS_OSX)	{
//				chosenPath = idata.getVariable(getVariableName()+".mac");
//			} else {
//				if (OsVersion.IS_UNIX)	{
//					chosenPath = idata.getVariable(getVariableName()+".unix");
//				}
//			}
//		}
//		VariableSubstitutor vs = new VariableSubstitutor(idata.getVariables());
//		chosenPath = vs.substitute(chosenPath, null);
//		// Set the path for method pathIsValid ...
		
		String chosenPath = helper.getDefaultPath(idata);
		pathSelectionPanel.setPath(chosenPath);

		if (!pathIsValid())
		{
			chosenPath = "";
		}
		// Set the default to the path selection panel.
		pathSelectionPanel.setPath(chosenPath);
// 		String var = idata.getVariable("DerbyPathPanel.skipIfValid");
// 		// Should we skip this panel?
// 		if (chosenPath.length() > 0 && var != null && "yes".equalsIgnoreCase(var))
// 		{
// 			idata.setVariable(getVariableName(), chosenPath);
// 			parent.skipPanel();
// 		}

	}

	/**
	 * Returns the name of the variable which should be used for the path.
	 *
	 * @return the name of the variable which should be used for the path
	 */
	public String getVariableName()
	{
		return variableName;
	}

	/**
	 * Sets the name for the variable which should be set with the path.
	 *
	 * @param string variable name to be used
	 */
	public void setVariableName(String string)
	{
		variableName = string;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.izforge.izpack.installer.IzPanel#getSummaryBody()
	 */
	public String getSummaryBody()
	{
		return (idata.getVariable(getVariableName()));
	}
}

class DerbyPathPanelHelper {
	
	String getVariableName() {
		return "DerbyDBPath";
	}
	
	String getDefaultPath(AutomatedInstallData idata) {
		String chosenPath = "";
		if (idata.getVariable(getVariableName()) != null) {
			chosenPath = idata.getVariable(getVariableName());
		} else {
			if (OsVersion.IS_WINDOWS)	{
				chosenPath = idata.getVariable(getVariableName()+".windows");
			}
			if (OsVersion.IS_OSX)	{
				chosenPath = idata.getVariable(getVariableName()+".mac");
			} else {
				if (OsVersion.IS_UNIX)	{
					chosenPath = idata.getVariable(getVariableName()+".unix");
				}
			}
		}
		VariableSubstitutor vs = new VariableSubstitutor(idata.getVariables());
		chosenPath = vs.substitute(chosenPath, null);
		return chosenPath;
	}

	public void setDefaultPath(AutomatedInstallData installData) {
		installData.setVariable(getVariableName(), getDefaultPath(installData));
	}
}
