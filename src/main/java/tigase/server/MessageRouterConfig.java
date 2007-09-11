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
package tigase.server;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;
import tigase.util.DNSResolver;

import static tigase.conf.Configurable.*;

/**
 * Describe class MessageRouterConfig here.
 *
 *
 * Created: Fri Jan  6 14:54:21 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class MessageRouterConfig {

  private static final Logger log =
    Logger.getLogger("tigase.server.MessageRouterConfig");

  public static final String LOCAL_ADDRESSES_PROP_KEY = "hostnames";
  private static String[] LOCAL_ADDRESSES_PROP_VALUE =	{"localhost", "hostname"};

	public static final String MSG_RECEIVERS_PROP_KEY =
		"components/msg-receivers/";
	public static final String MSG_RECEIVERS_NAMES_PROP_KEY =
		MSG_RECEIVERS_PROP_KEY + "id-names";
	public static final String DEF_SM_NAME = "sess-man";
	public static final String DEF_C2S_NAME = "c2s";
	public static final String DEF_S2S_NAME = "s2s";
	public static final String DEF_EXT_COMP_NAME = "ext-comp";
	public static final String DEF_SSEND_NAME = "ssend";
	public static final String DEF_SRECV_NAME = "srecv";
	public static final String DEF_BOSH_NAME = "bosh";

	private static final String[] ALL_MSG_RECEIVERS_NAMES_PROP_VAL =
	{	DEF_C2S_NAME, DEF_S2S_NAME, DEF_SM_NAME,
		DEF_SSEND_NAME, DEF_SRECV_NAME, DEF_BOSH_NAME};
	private static final String[] DEF_MSG_RECEIVERS_NAMES_PROP_VAL =
	{	DEF_C2S_NAME, DEF_S2S_NAME, DEF_SM_NAME, DEF_BOSH_NAME };
	private static final String[] SM_MSG_RECEIVERS_NAMES_PROP_VAL =
	{	DEF_EXT_COMP_NAME, DEF_SM_NAME };
	private static final String[] CS_MSG_RECEIVERS_NAMES_PROP_VAL =
	{	DEF_C2S_NAME, DEF_S2S_NAME, DEF_EXT_COMP_NAME, DEF_BOSH_NAME };

	private static final Map<String, String> MSG_RCV_CLASSES =
		new LinkedHashMap<String, String>();

	static {
		MSG_RCV_CLASSES.put(DEF_C2S_NAME, C2S_COMP_CLASS_NAME);
		MSG_RCV_CLASSES.put(DEF_S2S_NAME, S2S_COMP_CLASS_NAME);
		MSG_RCV_CLASSES.put(DEF_EXT_COMP_NAME, EXT_COMP_CLASS_NAME);
		MSG_RCV_CLASSES.put(DEF_SM_NAME, SM_COMP_CLASS_NAME);
		MSG_RCV_CLASSES.put(DEF_SSEND_NAME, SSEND_COMP_CLASS_NAME);
		MSG_RCV_CLASSES.put(DEF_SRECV_NAME, SRECV_COMP_CLASS_NAME);
		MSG_RCV_CLASSES.put(DEF_BOSH_NAME, BOSH_COMP_CLASS_NAME);
	}

	public static final String REGISTRATOR_PROP_KEY = "components/registrators/";
	public static final String REGISTRATOR_NAMES_PROP_KEY =
		REGISTRATOR_PROP_KEY + "id-names";
	private static final String[] REGISTRATOR_NAMES_PROP_VAL =	{	"stat-1" };

	public static final String STAT_1_CLASS_PROP_KEY =
		REGISTRATOR_PROP_KEY + "stat-1.class";
	public static final String STAT_1_CLASS_PROP_VAL =
		"tigase.stats.StatisticsCollector";
	public static final String STAT_1_ACTIVE_PROP_KEY =
		REGISTRATOR_PROP_KEY + "stat-1.active";
	public static final boolean STAT_1_ACTIVE_PROP_VAL = true;

	public static void getDefaults(Map<String, Object> defs,
		Map<String, Object> params, String comp_name) {

		String config_type = (String)params.get("config-type");
		String[] rcv_names = DEF_MSG_RECEIVERS_NAMES_PROP_VAL;
		Object par_names =
			params.get(comp_name + "/" + MSG_RECEIVERS_NAMES_PROP_KEY);
		if (par_names != null) {
			rcv_names = (String[])par_names;
		} else {
			if (config_type.equals(GEN_CONFIG_ALL)) {
				rcv_names = ALL_MSG_RECEIVERS_NAMES_PROP_VAL;
			}
			if (config_type.equals(GEN_CONFIG_SM)) {
				rcv_names = SM_MSG_RECEIVERS_NAMES_PROP_VAL;
			}
			if (config_type.equals(GEN_CONFIG_CS)) {
				rcv_names = CS_MSG_RECEIVERS_NAMES_PROP_VAL;
			}
			// You can now add a component for any server instance type
			// and you can also add many different components this way...
// 			if (config_type.equals(GEN_CONFIG_COMP)) {
// 				String c_name = (String)params.get(GEN_COMP_NAME);
// 				String c_class = (String)params.get(GEN_COMP_CLASS);
// 				rcv_names = new String[] {c_name};
// 				defs.put(MSG_RECEIVERS_PROP_KEY + c_name + ".class", c_class);
// 				defs.put(MSG_RECEIVERS_PROP_KEY + c_name + ".active", true);
// 			}
		}

		Arrays.sort(rcv_names);
		// Now init defaults for all external components:
		for (String key: params.keySet()) {
			if (key.startsWith(GEN_EXT_COMP)) {
				String new_comp_name =
					DEF_EXT_COMP_NAME + key.substring(GEN_EXT_COMP.length());
				if (Arrays.binarySearch(rcv_names, new_comp_name) < 0) {
					rcv_names = Arrays.copyOf(rcv_names, rcv_names.length+1);
					rcv_names[rcv_names.length-1] = new_comp_name;
					Arrays.sort(rcv_names);
				}
			} // end of if (key.startsWith(GEN_EXT_COMP))
			if (key.startsWith(GEN_COMP_NAME)) {
				String comp_name_suffix = key.substring(GEN_COMP_NAME.length());
				String c_name = (String)params.get(GEN_COMP_NAME + comp_name_suffix);
				String c_class = (String)params.get(GEN_COMP_CLASS + comp_name_suffix);
				if (Arrays.binarySearch(rcv_names, c_name) < 0) {
					defs.put(MSG_RECEIVERS_PROP_KEY + c_name + ".class", c_class);
					defs.put(MSG_RECEIVERS_PROP_KEY + c_name + ".active", true);
					rcv_names = Arrays.copyOf(rcv_names, rcv_names.length+1);
					rcv_names[rcv_names.length-1] = c_name;
					Arrays.sort(rcv_names);
					//System.out.println(Arrays.toString(rcv_names));
				}
			}
		} // end of for ()

		defs.put(MSG_RECEIVERS_NAMES_PROP_KEY, rcv_names);
		for (String name: rcv_names) {
			if (defs.get(MSG_RECEIVERS_PROP_KEY + name + ".class") == null) {
				String def_class = MSG_RCV_CLASSES.get(name);
				if (def_class == null) {
					def_class = EXT_COMP_CLASS_NAME;
				}
				defs.put(MSG_RECEIVERS_PROP_KEY + name + ".class", def_class);
				defs.put(MSG_RECEIVERS_PROP_KEY + name + ".active", true);
			}
		}
		defs.put(REGISTRATOR_NAMES_PROP_KEY, REGISTRATOR_NAMES_PROP_VAL);
		defs.put(STAT_1_CLASS_PROP_KEY, STAT_1_CLASS_PROP_VAL);
		defs.put(STAT_1_ACTIVE_PROP_KEY, STAT_1_ACTIVE_PROP_VAL);
		if (params.get(GEN_VIRT_HOSTS) != null) {
			LOCAL_ADDRESSES_PROP_VALUE =
				 ((String)params.get(GEN_VIRT_HOSTS)).split(",");
		} else {
			LOCAL_ADDRESSES_PROP_VALUE = DNSResolver.getDefHostNames();
		}
    defs.put(LOCAL_ADDRESSES_PROP_KEY, LOCAL_ADDRESSES_PROP_VALUE);
	}

	private Map<String, Object> props = null;

	public MessageRouterConfig(Map<String, Object> props) {
		this.props = props;
	}

	public String[] getRegistrNames() {
		String[] names = (String[])props.get(REGISTRATOR_NAMES_PROP_KEY);
		log.config(Arrays.toString(names));
		ArrayList<String> al = new ArrayList<String>();
		for (String name: names) {
			if ((Boolean)props.get(REGISTRATOR_PROP_KEY + name + ".active")) {
				al.add(name);
			} // end of if ((Boolean)props.get())
		} // end of for (String name: names)
		return al.toArray(new String[al.size()]);
	}

	public String[] getMsgRcvNames() {
		String[] names = (String[])props.get(MSG_RECEIVERS_NAMES_PROP_KEY);
		log.config(Arrays.toString(names));
		ArrayList<String> al = new ArrayList<String>();
		for (String name: names) {
			if (props.get(MSG_RECEIVERS_PROP_KEY + name + ".active") != null
				&& (Boolean)props.get(MSG_RECEIVERS_PROP_KEY + name + ".active")) {
				al.add(name);
			}
		} // end of for (String name: names)
		return al.toArray(new String[al.size()]);
	}

	public ComponentRegistrator getRegistrInstance(String name) throws
		ClassNotFoundException, InstantiationException, IllegalAccessException {

		String cls_name = (String)props.get(REGISTRATOR_PROP_KEY + name + ".class");
		// I changed location for the XMPPServiceCollector class
		// to avoid problems with old configuration files let's detect it here
		// and silently convert it to new package name:
		if (cls_name.equals("tigase.server.XMPPServiceCollector")
			|| cls_name.equals("tigase.disco.XMPPServiceCollector")) {
			log.warning("This class is not used anymore. Correct your configuration please. Remove all references to class: XMPPServiceCollector.");
			return null;
		}
		return (ComponentRegistrator)Class.forName(cls_name).newInstance();
	}

	public MessageReceiver getMsgRcvInstance(String name) throws
		ClassNotFoundException, InstantiationException, IllegalAccessException {

		String cls_name = (String)props.get(MSG_RECEIVERS_PROP_KEY + name + ".class");
		return (MessageReceiver)Class.forName(cls_name).newInstance();
	}

} // MessageRouterConfig
