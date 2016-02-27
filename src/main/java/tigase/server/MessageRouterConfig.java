/*
 * MessageRouterConfig.java
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2013 "Tigase, Inc." <office@tigase.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 *
 */



package tigase.server;

//~--- non-JDK imports --------------------------------------------------------

import tigase.annotations.TigaseDeprecatedComponent;
import tigase.osgi.ModulesManagerImpl;
import tigase.util.DNSResolverFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

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
	/** Field description */
	public static final String DISCO_NAME_PROP_KEY = "disco-name";

	/** Field description */
	public static final String DISCO_SHOW_VERSION_PROP_KEY = "disco-show-version";

	/** Field description */
	public static final String LOCAL_ADDRESSES_PROP_KEY = "hostnames";

	/** Field description */
	public static final String MSG_RECEIVERS_PROP_KEY = "components/msg-receivers/";

	/** Field description */
	public static final String REGISTRATOR_PROP_KEY = "components/registrators/";

	/** Field description */
	public static final String UPDATES_CHECKING_INTERVAL_PROP_KEY =
			"updates-checking-interval";

	/** Field description */
	public static final long UPDATES_CHECKING_INTERVAL_PROP_VAL = 7;

	/** Field description */
	public static final String    UPDATES_CHECKING_PROP_KEY        = "updates-checking";
	private static String[]       LOCAL_ADDRESSES_PROP_VALUE = { "localhost", "hostname" };
	private static final Logger   log = Logger.getLogger(
			"tigase.server.MessageRouterConfig");
	private static final String[] DEF_MSG_RECEIVERS_NAMES_PROP_VAL = {
		DEF_C2S_NAME, DEF_S2S_NAME, DEF_SM_NAME, DEF_BOSH_NAME, DEF_MONITOR_NAME,
		DEF_AMP_NAME, DEF_WS2S_NAME, DEF_EVENTBUS_NAME
	};
	private static final String[] ALL_MSG_RECEIVERS_NAMES_PROP_VAL = {
		DEF_C2S_NAME, DEF_S2S_NAME, DEF_SM_NAME, DEF_SSEND_NAME, DEF_SRECV_NAME,
		DEF_BOSH_NAME, DEF_MONITOR_NAME, DEF_WS2S_NAME, DEF_EVENTBUS_NAME
	};

	/** Field description */
	public static final String MSG_RECEIVERS_NAMES_PROP_KEY = MSG_RECEIVERS_PROP_KEY +
			"id-names";
	private static final String[] SM_MSG_RECEIVERS_NAMES_PROP_VAL = { DEF_EXT_COMP_NAME,
			DEF_SM_NAME, DEF_MONITOR_NAME, DEF_AMP_NAME, DEF_EVENTBUS_NAME };
	private static final String[] DEF_REGISTRATOR_NAMES_PROP_VAL = { DEF_VHOST_MAN_NAME,
			DEF_STATS_NAME };
	private static final String[] CS_MSG_RECEIVERS_NAMES_PROP_VAL = {
		DEF_C2S_NAME, DEF_S2S_NAME, DEF_EXT_COMP_NAME, DEF_BOSH_NAME, DEF_MONITOR_NAME,
		DEF_AMP_NAME, DEF_WS2S_NAME, DEF_EVENTBUS_NAME
	};
	private static final Map<String, String> COMPONENT_CLASSES = new LinkedHashMap<String,
			String>();
	private static final String[] COMP_MSG_RECEIVERS_NAMES_PROP_VAL = { DEF_COMP_PROT_NAME,
			DEF_MONITOR_NAME, DEF_AMP_NAME };
	private static final Map<String, String> COMP_CLUS_MAP = new LinkedHashMap<String,
			String>();
	private static final String[] CLUSTER_REGISTRATOR_NAMES_PROP_VAL = { DEF_VHOST_MAN_NAME,
			DEF_STATS_NAME, DEF_CLUST_CONTR_NAME };

	/** Field description */
	public static final Boolean UPDATES_CHECKING_PROP_VAL = true;

	/** Field description */
	public static final String REGISTRATOR_NAMES_PROP_KEY = REGISTRATOR_PROP_KEY +
			"id-names";

	/** Field description */
	public static final boolean DISCO_SHOW_VERSION_PROP_VAL = true;

	/** Field description */
	public static final String DISCO_NAME_PROP_VAL = tigase.server.XMPPServer.NAME;

	//~--- static initializers --------------------------------------------------

	static {
		COMPONENT_CLASSES.put(DEF_C2S_NAME, C2S_COMP_CLASS_NAME);
		COMPONENT_CLASSES.put(DEF_S2S_NAME, S2S_COMP_CLASS_NAME);
		COMPONENT_CLASSES.put(DEF_EXT_COMP_NAME, EXT_COMP_CLASS_NAME);
		COMPONENT_CLASSES.put(DEF_COMP_PROT_NAME, COMP_PROT_CLASS_NAME);
		COMPONENT_CLASSES.put(DEF_CL_COMP_NAME, CL_COMP_CLASS_NAME);
		COMPONENT_CLASSES.put(DEF_SM_NAME, SM_COMP_CLASS_NAME);
		COMPONENT_CLASSES.put(DEF_SSEND_NAME, SSEND_COMP_CLASS_NAME);
		COMPONENT_CLASSES.put(DEF_SRECV_NAME, SRECV_COMP_CLASS_NAME);
		COMPONENT_CLASSES.put(DEF_BOSH_NAME, BOSH_COMP_CLASS_NAME);
		COMPONENT_CLASSES.put(DEF_STATS_NAME, STATS_CLASS_NAME);
		COMPONENT_CLASSES.put(DEF_CLUST_CONTR_NAME, CLUSTER_CONTR_CLASS_NAME);
		COMPONENT_CLASSES.put(DEF_VHOST_MAN_NAME, VHOST_MAN_CLASS_NAME);
		COMPONENT_CLASSES.put(DEF_MONITOR_NAME, MONITOR_CLASS_NAME);
		COMPONENT_CLASSES.put(DEF_AMP_NAME, AMP_CLASS_NAME);
		COMPONENT_CLASSES.put(DEF_WS2S_NAME, WS2S_CLASS_NAME);
		COMPONENT_CLASSES.put(DEF_EVENTBUS_NAME, EVENTBUS_CLASS_NAME);
		COMP_CLUS_MAP.put(SM_COMP_CLASS_NAME, SM_CLUST_COMP_CLASS_NAME);
		COMP_CLUS_MAP.put(C2S_COMP_CLASS_NAME, C2S_CLUST_COMP_CLASS_NAME);
		COMP_CLUS_MAP.put(BOSH_COMP_CLASS_NAME, BOSH_CLUST_COMP_CLASS_NAME);
		COMP_CLUS_MAP.put(WS2S_CLASS_NAME, WS2S_CLUST_CLASS_NAME);
		COMP_CLUS_MAP.put(AMP_CLASS_NAME, AMP_CLUST_CLASS_NAME);
	}

	//~--- fields ---------------------------------------------------------------

	private Map<String, Object> props = null;

	//~--- constructors ---------------------------------------------------------

	/**
	 * Constructs ...
	 *
	 *
	 * @param props
	 */
	public MessageRouterConfig(Map<String, Object> props) {
		this.props = props;

		// System.out.println("MessageRouterConfig() properties: " + props.toString());
	}

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param cls_name
	 * @param currCls
	 *
	 *
	 *
	 * @return a value of <code>boolean</code>
	 */
	@SuppressWarnings("unchecked")
	public boolean componentClassEquals(String cls_name,
			Class<? extends ServerComponent> currCls) {
		Class<? extends ServerComponent> cls = null;

		try {
			cls = ModulesManagerImpl.getInstance().getServerComponentClass(cls_name);
			if (((cls == null) && (!XMPPServer.isOSGi() || COMPONENT_CLASSES.containsValue(
					cls_name) || COMP_CLUS_MAP.containsValue(cls_name))) || EXT_COMP_CLASS_NAME
					.equals(cls_name) || "tigase.cluster.VirtualComponent".equals(cls_name)) {
				cls = (Class<? extends ServerComponent>) this.getClass().getClassLoader()
						.loadClass(cls_name);
			}
		} catch (Exception ex) {}

		return (cls != null) && currCls.equals(cls);
	}

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param defs
	 * @param params
	 * @param comp_name
	 */
	public static void getDefaults(Map<String, Object> defs, Map<String, Object> params,
			String comp_name) {
		boolean cluster_mode = isTrue((String) params.get(CLUSTER_MODE));

		log.log(Level.CONFIG, "Cluster mode: {0}", params.get(CLUSTER_MODE));
		if (cluster_mode) {
			log.config("Cluster mode is on, replacing known components with cluster" +
					" versions:");
			for (Map.Entry<String, String> entry : COMPONENT_CLASSES.entrySet()) {
				String cls = COMP_CLUS_MAP.get(entry.getValue());

				if (cls != null) {
					log.log(Level.CONFIG, "Replacing {0} with {1}", new Object[] { entry.getValue(),
							cls });
					entry.setValue(cls);
				}
			}
		} else {
			log.config("Cluster mode is off.");
		}

		String   config_type = (String) params.get("config-type");
		String[] rcv_names   = DEF_MSG_RECEIVERS_NAMES_PROP_VAL;
		Object   par_names   = params.get(comp_name + "/" + MSG_RECEIVERS_NAMES_PROP_KEY);

		if (par_names != null) {
			rcv_names = (String[]) par_names;
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
			if (config_type.equals(GEN_CONFIG_COMP)) {
				rcv_names = COMP_MSG_RECEIVERS_NAMES_PROP_VAL;
			}
		}
		Arrays.sort(rcv_names);
		log.log(Level.CONFIG, "Configured type: {0}, loading components: {1}",
													new Object[] {config_type, Arrays.asList( rcv_names )});

		// Now init defaults for all extra components:
		for (String key : params.keySet()) {

			// XEP-0114 components
			if (key.startsWith(GEN_EXT_COMP)) {
				String new_comp_name = DEF_EXT_COMP_NAME + key.substring(GEN_EXT_COMP.length());

				if (Arrays.binarySearch(rcv_names, new_comp_name) < 0) {
					rcv_names                       = Arrays.copyOf(rcv_names, rcv_names.length +
							1);
					rcv_names[rcv_names.length - 1] = new_comp_name;
					Arrays.sort(rcv_names);
				}
			}    // end of if (key.startsWith(GEN_EXT_COMP))

			// All other extra components, assuming class has been given
			if (key.startsWith(GEN_COMP_NAME)) {
				String comp_name_suffix = key.substring(GEN_COMP_NAME.length());
				String c_name           = (String) params.get(GEN_COMP_NAME + comp_name_suffix);

				// Make sure the component name is converted to lowercase
				c_name = c_name.toLowerCase();

				String c_class = (String) params.get(GEN_COMP_CLASS + comp_name_suffix);

				if (Arrays.binarySearch(rcv_names, c_name) < 0) {
					defs.put(MSG_RECEIVERS_PROP_KEY + c_name + ".class", c_class);
					defs.put(MSG_RECEIVERS_PROP_KEY + c_name + ".active", true);
					rcv_names                       = Arrays.copyOf(rcv_names, rcv_names.length +
							1);
					rcv_names[rcv_names.length - 1] = c_name;
					Arrays.sort(rcv_names);

					// System.out.println(Arrays.toString(rcv_names));
				}
			}
		}    // end of for ()

		// Add XEP-0114 for cluster communication
		if (cluster_mode) {
			log.config("In cluster mode I am setting up 1 listening xep-0114 component:");
			if (Arrays.binarySearch(rcv_names, DEF_CL_COMP_NAME) < 0) {
				defs.put(MSG_RECEIVERS_PROP_KEY + DEF_CL_COMP_NAME + ".class",
						CL_COMP_CLASS_NAME);
				defs.put(MSG_RECEIVERS_PROP_KEY + DEF_CL_COMP_NAME + ".active", true);
				rcv_names                       = Arrays.copyOf(rcv_names, rcv_names.length + 1);
				rcv_names[rcv_names.length - 1] = DEF_CL_COMP_NAME;
				Arrays.sort(rcv_names);
			}
		}
		defs.put(MSG_RECEIVERS_NAMES_PROP_KEY, rcv_names);
		for (String name : rcv_names) {
			if (defs.get(MSG_RECEIVERS_PROP_KEY + name + ".class") == null) {
				String def_class = COMPONENT_CLASSES.get(name);

				if (def_class == null) {
					def_class = EXT_COMP_CLASS_NAME;
				}
				defs.put(MSG_RECEIVERS_PROP_KEY + name + ".class", def_class);
				defs.put(MSG_RECEIVERS_PROP_KEY + name + ".active", true);
			}
		}

		String[] registr = DEF_REGISTRATOR_NAMES_PROP_VAL;

		if (cluster_mode) {
			registr = CLUSTER_REGISTRATOR_NAMES_PROP_VAL;
		}
		defs.put(REGISTRATOR_NAMES_PROP_KEY, registr);
		for (String reg : registr) {
			defs.put(REGISTRATOR_PROP_KEY + reg + ".class", COMPONENT_CLASSES.get(reg));
			defs.put(REGISTRATOR_PROP_KEY + reg + ".active", true);
		}
		if (params.get(GEN_VIRT_HOSTS) != null) {
			LOCAL_ADDRESSES_PROP_VALUE = ((String) params.get(GEN_VIRT_HOSTS)).split(",");
		} else {
			LOCAL_ADDRESSES_PROP_VALUE = DNSResolverFactory.getInstance().getDefaultHosts();
		}
		defs.put(LOCAL_ADDRESSES_PROP_KEY, LOCAL_ADDRESSES_PROP_VALUE);
		defs.put(DISCO_NAME_PROP_KEY, DISCO_NAME_PROP_VAL);
		defs.put(DISCO_SHOW_VERSION_PROP_KEY, DISCO_SHOW_VERSION_PROP_VAL);
		defs.put(UPDATES_CHECKING_PROP_KEY, UPDATES_CHECKING_PROP_VAL);
		defs.put(UPDATES_CHECKING_INTERVAL_PROP_KEY, UPDATES_CHECKING_INTERVAL_PROP_VAL);
	}

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of <code>String[]</code>
	 */
	public String[] getMsgRcvActiveNames() {
		String[]     names          = (String[]) props.get(MSG_RECEIVERS_NAMES_PROP_KEY);
		List<String> availableNames = new ArrayList<String>();

		for (String name : names) {
			if (hasClassForServerComponent(name) && ((props.get(MSG_RECEIVERS_PROP_KEY + name +
					".active") != null) && (Boolean) props.get(MSG_RECEIVERS_PROP_KEY + name +
					".active"))) {
				availableNames.add(name);
			}
		}
		names = availableNames.toArray(new String[availableNames.size()]);
		if (log.isLoggable(Level.FINE)) {
			log.log(Level.FINE, "active message receivers = {0}", Arrays.toString(names));
		}

		return names;
	}

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of <code>String[]</code>
	 */
	public String[] getMsgRcvInactiveNames() {
		String[]          names = (String[]) props.get(MSG_RECEIVERS_NAMES_PROP_KEY);
		ArrayList<String> al    = new ArrayList<String>();

		for (String name : names) {
			if ((props.get(MSG_RECEIVERS_PROP_KEY + name + ".active") == null) ||
					!(Boolean) props.get(MSG_RECEIVERS_PROP_KEY + name + ".active")) {
				al.add(name);
			}
			if (hasClassForServerComponent(name)) {
				al.add(name);
			}
		}    // end of for (String name: names)
		names = al.toArray(new String[al.size()]);
		if (log.isLoggable(Level.FINE)) {
			log.log(Level.FINE, "inactive message receivers = {0}", Arrays.toString(names));
		}

		return names;
	}

	/**
	 * Method description
	 *
	 *
	 * @param name
	 *
	 *
	 *
	 *
	 * @return a value of <code>ServerComponent</code>
	 * @throws ClassNotFoundException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 */
	public ServerComponent getMsgRcvInstance(String name)
					throws ClassNotFoundException, InstantiationException, IllegalAccessException {
		String          cls_name = (String) props.get(MSG_RECEIVERS_PROP_KEY + name +
				".class");
		ServerComponent cls = null;

		cls = ModulesManagerImpl.getInstance().getServerComponent(cls_name);
		if (((cls == null) && (!XMPPServer.isOSGi() || COMPONENT_CLASSES.containsValue(
				cls_name) || COMP_CLUS_MAP.containsValue(cls_name))) || EXT_COMP_CLASS_NAME
				.equals(cls_name)  || "tigase.cluster.VirtualComponent".equals(cls_name)) {
			cls = (ServerComponent) this.getClass().getClassLoader().loadClass(cls_name)
					.newInstance();
		}

		if ( cls != null && cls.getClass().isAnnotationPresent( TigaseDeprecatedComponent.class ) ){
			TigaseDeprecatedComponent annotation = cls.getClass().getAnnotation( TigaseDeprecatedComponent.class );
			log.log( Level.WARNING, "Deprecated Component: " + cls.getClass().getCanonicalName()
															+ ", INFO: " + annotation.note() + "\n" );
		}


		return cls;
	}

	/**
	 * Method description
	 *
	 *
	 * @param name
	 *
	 *
	 *
	 *
	 * @return a value of <code>ComponentRegistrator</code>
	 * @throws ClassNotFoundException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 */
	public ComponentRegistrator getRegistrInstance(String name)
					throws ClassNotFoundException, InstantiationException, IllegalAccessException {
		String cls_name = (String) props.get(REGISTRATOR_PROP_KEY + name + ".class");

		// I changed location for the XMPPServiceCollector class
		// to avoid problems with old configuration files let's detect it here
		// and silently convert it to new package name:
		if (cls_name.equals("tigase.server.XMPPServiceCollector") || cls_name.equals(
				"tigase.disco.XMPPServiceCollector")) {
			log.warning(
					"This class is not used anymore. Correct your configuration please. Remove " +
					"all references to class: XMPPServiceCollector.");

			return null;
		}

		return (ComponentRegistrator) this.getClass().getClassLoader().loadClass(cls_name)
				.newInstance();
	}

	/**
	 * Method description
	 *
	 *
	 *
	 *
	 * @return a value of <code>String[]</code>
	 */
	public String[] getRegistrNames() {
		String[] names = (String[]) props.get(REGISTRATOR_NAMES_PROP_KEY);

		log.config(Arrays.toString(names));

		ArrayList<String> al = new ArrayList<String>();

		for (String name : names) {

			// System.out.println("Checking: '" + REGISTRATOR_PROP_KEY + name + ".active'");
			if ((Boolean) props.get(REGISTRATOR_PROP_KEY + name + ".active")) {
				al.add(name);
			}    // end of if ((Boolean)props.get())
		}      // end of for (String name: names)

		return al.toArray(new String[al.size()]);
	}

	/**
	 * Check if class exists for server component
	 *
	 * @param name
	 *
	 */
	private boolean hasClassForServerComponent(String name) {
		try {
			String cls_name = (String) props.get(MSG_RECEIVERS_PROP_KEY + name + ".class");

			if (cls_name == null) {
				if (log.isLoggable(Level.WARNING))
					log.log(Level.WARNING, "Can't load component " + name + ": Class is not defined in config.");
				return false;
			}

			// first check if there is registered class in ModuleManagerImpl as
			// it is easy
			if (ModulesManagerImpl.getInstance().hasClassForServerComponent(cls_name)) {
				return true;
			}
			if (XMPPServer.isOSGi()
					&& !(COMPONENT_CLASSES.containsValue(cls_name) || COMP_CLUS_MAP.containsValue(cls_name) 
					|| EXT_COMP_CLASS_NAME.equals(cls_name) || "tigase.cluster.VirtualComponent".equals(cls_name))) {
				if (log.isLoggable(Level.WARNING))
					log.log(Level.WARNING, "Can't load component " + name + ": " + cls_name + " is external class.");
				return false;
			}

			// it is dirty but should work
			this.getClass().getClassLoader().loadClass(cls_name);

			return true;
		} catch (Exception ex) {
			if (log.isLoggable(Level.WARNING))
				log.log(Level.WARNING, "Can't load component " + name + ": " + ex.getMessage());
			return false;
		}
	}

	private static boolean isTrue(String val) {
		if (val == null) {
			return false;
		}

		String value = val.toLowerCase();

		return (value.equals("true") || value.equals("yes") || value.equals("on") || value
				.equals("1"));
	}
}    // MessageRouterConfig


//~ Formatted in Tigase Code Convention on 13/10/15
