package com.izforge.izpack.panels;

import java.io.InputStream;

import com.izforge.izpack.LocaleDatabase;
import com.izforge.izpack.installer.ResourceManager;
import com.izforge.izpack.installer.InstallData;
import com.izforge.izpack.util.Debug;

class TigaseInstallerCommon {

	private final static String LANG_FILE_NAME = "panelsLangPack.xml";

	public static InstallData init(InstallData iData) {
		try {
			InputStream inputStream = 
				ResourceManager.getInstance().getInputStream(LANG_FILE_NAME);
			iData.langpack.add(inputStream);
		}
		catch (Exception exc) {
			Debug.trace(exc);
		}

		return iData;
	}

}
