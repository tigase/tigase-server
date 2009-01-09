/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2007 "Artur Hefczyc" <artur.hefczyc@tigase.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 *
 * $Rev$
 * Last modified by $Author$
 * $Date$
 */

package tigase.stats;
import java.util.List;
import javax.management.*;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Class StatisticsProvider
 *
 * @author kobit
 */
public class StatisticsProvider extends StandardMBean
				implements StatisticsProviderMBean {

	private StatisticsCollector theRef;

	public StatisticsProvider(StatisticsCollector theRef)
					throws NotCompliantMBeanException {
		//WARNING Uncomment the following call to super() to make this class
		//compile (see BUG ID 122377)
		super(StatisticsProviderMBean.class, false);
		this.theRef = theRef;
	}
	
	@Override
	public MBeanInfo getMBeanInfo() {
		MBeanInfo mbinfo = super.getMBeanInfo();
		return new MBeanInfo(mbinfo.getClassName(),
						mbinfo.getDescription(),
						mbinfo.getAttributes(),
						mbinfo.getConstructors(),
						mbinfo.getOperations(),
						getNotificationInfo());
	}
	
	public MBeanNotificationInfo[] getNotificationInfo() {
		return new MBeanNotificationInfo[] {};
	}

	/**
	 * Override customization hook:
	 * You can supply a customized description for MBeanInfo.getDescription()
	 *
	 * @param info
	 * @return
	 */
	@Override
	protected String getDescription(MBeanInfo info) {
		return "Provides the Tigase server statistics";
	}

	/**
	 * Override customization hook:
	 * You can supply a customized description for MBeanAttributeInfo.getDescription()
	 *
	 * @param info
	 * @return
	 */
	@Override
	protected String getDescription(MBeanAttributeInfo info) {
		String description = null;
		if (info.getName().equals("AllStats")) {
			description = "Collection of statistics from all components.";
		} else if (info.getName().equals("ComponentsNames")) {
			description = "List of components names for which statistics are available";
		} else if (info.getName().equals("Name")) {
			description = "This component name - name of the statistics collector component,";
		}
		return description;
	}

	/**
	 * Override customization hook:
	 * You can supply a customized description for MBeanParameterInfo.getDescription()
	 *
	 * @param op
	 * @param param
	 * @param sequence
	 * @return
	 */
	@Override
	protected String getDescription(MBeanOperationInfo op,
					MBeanParameterInfo param, int sequence) {
		if (op.getName().equals("getAllStats")) {
			switch (sequence) {
				case 0:
					return "Statistics level, 0 - All, 500 - Medium, 800 - Minimal";
				default:
					return null;
			}
		} else if (op.getName().equals("getComponentStats")) {
			switch (sequence) {
				case 0:
					return "The component name to provide statistics for";
				case 1:
					return "Statistics level, 0 - All, 500 - Medium, 800 - Minimal";
				default:
					return null;
			}
		}
		return null;
	}

	/**
	 * Override customization hook:
	 * You can supply a customized description for MBeanParameterInfo.getName()
	 *
	 * @param op
	 * @param param
	 * @param sequence
	 * @return
	 */
	@Override
	protected String getParameterName(MBeanOperationInfo op,
					MBeanParameterInfo param, int sequence) {
		if (op.getName().equals("getAllStats")) {
			switch (sequence) {
				case 0:
					return "param0";
				default:
					return null;
			}
		} else if (op.getName().equals("getComponentStats")) {
			switch (sequence) {
				case 0:
					return "param0";
				case 1:
					return "param1";
				default:
					return null;
			}
		}
		return null;
	}

	/**
	 * Override customization hook:
	 * You can supply a customized description for MBeanOperationInfo.getDescription()
	 *
	 * @param info
	 * @return
	 */
	@Override
	protected String getDescription(MBeanOperationInfo info) {
		String description = null;
		MBeanParameterInfo[] params = info.getSignature();
		String[] signature = new String[params.length];
		for (int i = 0; i < params.length;
						i++) {
			signature[i] = params[i].getType();
		}
		String[] methodSignature;
		methodSignature = new String[]{java.lang.Integer.TYPE.getName()};
		if (info.getName().equals("getAllStats") &&
						Arrays.equals(signature, methodSignature)) {
			description = "Provides statistics for all components for a given level.";
		}
		methodSignature =
						new String[]{java.lang.String.class.getName(),
						java.lang.Integer.TYPE.getName()};
		if (info.getName().equals("getComponentStats") &&
						Arrays.equals(signature, methodSignature)) {
			description = "Provides statistics for a given component name and statistics level.";
		}
		return description;
	}

//	/**
//	 * Get Attribute exposed for management
//	 * @return java.util.Map<String, String>
//	 */
//	@Override
//	public Map getAllStats() {
//		return getAllStats(0);
//	}

	/**
	 * Get Attribute exposed for management
	 */
	@Override
	public List getComponentsNames() {
		return theRef.getComponentsNames();
	}

	/**
	 * Get Attribute exposed for management
	 */
	@Override
	public String getName() {
		return theRef.getName();
	}

	private Map getMapFromList(List<StatRecord> stats) {
		Map<String, String> result = new LinkedHashMap<String, String>();
		for (StatRecord rec : stats) {
			String key = rec.getComponent() + "/" + rec.getDescription();
			String value = rec.getValue();
			if (rec.getType() == StatisticType.LIST) {
				value = rec.getListValue().toString();
			}
			result.put(key, value);
		}
		return result;
	}

	/**
	 * Operation exposed for management
	 * @param param0 Statistics level, 0 - All, 500 - Medium, 800 - Minimal
	 * @return java.util.Map<String, String>
	 */
	@Override
	public Map getAllStats(int param0) {
		return getMapFromList(theRef.getAllStats(param0));
	}

	/**
	 * Operation exposed for management
	 * @param param0 The component name to provide statistics for
	 * @param param1 Statistics level, 0 - All, 500 - Medium, 800 - Minimal
	 * @return java.util.Map<String, String>
	 */
	@Override
	public Map getComponentStats(String param0, int param1) {
		return getMapFromList(theRef.getComponentStats(param0, param1));
	}
}


