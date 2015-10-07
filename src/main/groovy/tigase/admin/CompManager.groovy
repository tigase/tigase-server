/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2013 "Tigase, Inc." <office@tigase.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3 of the License.
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
 * $Rev: $
 * Last modified by $Author: $
 * $Date: $
 */

/*

Manage active server components

AS:Description: Manage active server components
AS:CommandId: comp-manager
AS:Component: basic-conf
AS:Group: Configuration
 */

package tigase.admin

import tigase.conf.ConfigRepositoryIfc
import tigase.conf.Configurable;
import tigase.conf.Configurator;
import tigase.osgi.ModulesManagerImpl;
import tigase.server.*

class DelayedReloadTask extends Thread {
	void run() {
		Thread.sleep(5000);
	    ((Configurator) XMPPServer.getConfigurator()).updateMessageRouter();
	}
}

try {

	def ACTION = "action";
	def ACTION_ADD = "Add";
	def ACTION_EDIT = "Edit";
	def ACTION_REMOVE = "Remove";
	def ACTION_LIST = "List";

	def COMP_NAME = "comp-name";
	def COMP_CLASS = "comp-class";

	def SUBMIT = "confirm";

	def getComponentProperties = { comp_name, comp_class ->
		def conf = XMPPServer.getConfigurator();
		def prop = conf.getProperties(comp_name);
		try {
			def comp = ModulesManagerImpl.getInstance().getServerComponent(comp_class);
			if (!comp) {
				def INTERNAL_COMPONENTS = [];
				INTERNAL_COMPONENTS.addAll(MessageRouterConfig.COMPONENT_CLASSES.values());
				INTERNAL_COMPONENTS.addAll(MessageRouterConfig.COMP_CLUS_MAP.values());
				
				if (!XMPPServer.isOSGi() || INTERNAL_COMPONENTS.contains(comp_class)) {
					comp = XMPPServer.class.getClassLoader().loadClass(comp_class).newInstance()
				}
			}
			if (comp) {
				comp.setName(comp_name)
				def defProp = comp.getDefaults(conf.getDefConfigParams() ?: [:])  ?: [:];
				defProp.putAll(prop);
				return defProp;
			}
			else {
				return [:];
			}
		}
		catch(Exception ex) {
			ex.printStackTrace();
			return [:];
		}
	};

	def prepareComponentConfigForAdHoc = { res, comp_name, comp_class ->
		def prop = getComponentProperties(comp_name, comp_class);
		def keys = [];
		keys.addAll(prop.keySet());
		keys.sort { it }.each { key ->
			def val = prop[key];
			if (val.getClass().isArray()) {
				def tmp = [];
				for (def x : val) {
					tmp.add(String.valueOf(x));
				}
				val = tmp.join(',');
			}
			if (!(val instanceof String)) {
				val = val.toString();
			}
			Command.addFieldValue(res, key, val);
		}
	};

	def getConfigFromAdHoc = { p, comp_name, comp_class ->
		def defProp = getComponentProperties(comp_name, comp_class);
        
		def props = [:];
		defProp.each { key, defVal ->
			def val = Command.getFieldValue(p, key);
                
			def tmpVal = defVal;
			if (tmpVal.getClass().isArray()) {
				def tmp = [];
				for (def x : tmpVal) {
					tmp.add(String.valueOf(x));
				}
				tmpVal = tmp.join(',');
			}

			if (val && !val.equals(tmpVal)) {
				def cls = defVal.getClass();
				if (val == "null") val = null;
				if (cls == Long[].class) {
					def out = [];
					val.split(',').each { out.add(Long.parseLong(it))};
					val = (out as Long[]);
				}
				else if (cls == Integer[].class) {
					def out = [];
					val.split(',').each { out.add(Integer.parseInt(it))};
					val = (out as Integer[]);
				}
				else if (cls == String[].class) {
					def out = [];
					val.split(',').each { out.add(it)};
					val = (out as String[]);
				}
				else if (cls == Long.class) {
					val = Long.parseLong(val);
				}
				else if (cls == Integer.class) {
					val = Integer.parseInt(val);
				}
				else if (cls == Boolean.class) {
					val = Boolean.parseBoolean(val);
				}
				else {
					println "unknown convertion for key = " + key + " , " + cls.getName()
				}
                                                
				props.put(key, val);
			}
		}
        
		return props;
	};


	def conf_repo = (ConfigRepositoryIfc)comp_repo
	def p = (Iq)packet

	// check permission
	def admins = (Set)adminsSet
	def stanzaFromBare = p.getStanzaFrom().getBareJID()
	def isServiceAdmin = admins.contains(stanzaFromBare)

	if (!isServiceAdmin) {
		def result = p.commandResult(Command.DataType.result)
		Command.addTextField(result, "Error", "You do not have enough permissions to access this data.");
		return result
	}


	def comp_name = Command.getFieldValue(p, COMP_NAME)
	def action = Command.getFieldValue(p, ACTION);
	def submit = Command.getFieldValue(p, SUBMIT);

	if (action == null) {
		def res = (Iq)p.commandResult(Command.DataType.form)
		def actions = [ACTION_LIST, ACTION_ADD, ACTION_EDIT, ACTION_REMOVE];
		Command.addFieldValue(res, ACTION, actions[0], "Action",
			(String[]) actions, (String[]) actions)
		return res;
	}

	if (action == ACTION_ADD && comp_name == null) {
		def res = (Iq)p.commandResult(Command.DataType.form)
		Command.addFieldValue(res, COMP_NAME, "", "text-single", "Component name");
		Command.addFieldValue(res, COMP_CLASS, "", "text-single", "Component class");
		Command.addHiddenField(res, ACTION, ACTION_ADD);
		return res;
	}
	else if ( action == ACTION_LIST && comp_name == null) {
		def res = (Iq)p.commandResult(Command.DataType.form)
		def compNames = []
		def conf = XMPPServer.getConfigurator();

		def mrProps = conf.getProperties("message-router");
		compNames = mrProps.get(MessageRouterConfig.MSG_RECEIVERS_NAMES_PROP_KEY).toList();
		compNames += mrProps.get(MessageRouterConfig.REGISTRATOR_NAMES_PROP_KEY).toList();

		compNames.each {
			def mr = conf.getComponent( it );
			if (mr) {
				Command.addFieldMultiValue(res, it, Arrays.asList( mr.getComponentInfo().toString() ))
			}
		}
		Command.addHiddenField(res, ACTION, action);
		return res
	}
	else if (comp_name == null) {
		def res = (Iq)p.commandResult(Command.DataType.form)
		def compNames = []
		def conf = XMPPServer.getConfigurator();
		conf_repo.getCompNames().findAll{ conf.getComponent(it) != null }.each { compNames += it }
		compNames.sort();
		Command.addFieldValue(res, COMP_NAME, comp_name ?: compNames[0], "Components",
			(String[])compNames, (String[])compNames)
		Command.addHiddenField(res, ACTION, action ?: "");
		return res
	}
	else {
		if (action == ACTION_REMOVE) {
			def res = (Iq)p.commandResult(Command.DataType.result)
			def conf = XMPPServer.getConfigurator();
			def initProps = conf.getDefConfigParams().findAll { return it.key.startsWith(Configurable.GEN_COMP_NAME) && it.value.equals(comp_name); };
			def suffix = null;
			initProps.each {
				suffix = it.key.substring(Configurable.GEN_COMP_NAME.length());
			}
			conf.getDefConfigParams().remove(Configurable.GEN_COMP_NAME + suffix);
			conf.getDefConfigParams().remove(Configurable.GEN_COMP_CLASS + suffix);

			def mrProps = conf.getProperties("message-router");
			def compNames = mrProps.get(MessageRouterConfig.MSG_RECEIVERS_NAMES_PROP_KEY).toList();
                        
			if (compNames.contains(comp_name)) {
				compNames.remove(comp_name);
			}
                        
			mrProps = [:];
			mrProps.put(MessageRouterConfig.MSG_RECEIVERS_NAMES_PROP_KEY, (compNames as String[]));
			conf.putProperties("message-router", mrProps);
                        
			new DelayedReloadTask().start();
			Command.addTextField(res, "Note", "Operation successful.")

			return res;
		}
		else if (!submit) {
			def comp_class = Command.getFieldValue(p, COMP_CLASS);
                
			if (comp_class == null) {
				def conf = XMPPServer.getConfigurator();
				def initProps = conf.getDefConfigParams().findAll { return it.key.startsWith(Configurable.GEN_COMP_NAME) && it.value.equals(comp_name); };
				def suffix = null;
				initProps.each {
					suffix = it.key.substring(Configurable.GEN_COMP_NAME.length());
				}
				comp_class = conf.getDefConfigParams().get(Configurable.GEN_COMP_CLASS + suffix);

				if (!comp_class) comp_class = MessageRouterConfig.COMPONENT_CLASSES.get(comp_name);
				if (!comp_class) comp_class = MessageRouterConfig.COMP_CLUS_MAP.get(comp_name);
				if (!comp_class && comp_name == "basic-conf") comp_class = XMPPServer.getConfigurator().getClass().getCanonicalName();
				if (!comp_class) throw new Exception("Could not find component class for component: " + comp_name);
			}
                        
			def res = (Iq)p.commandResult(Command.DataType.form)
			Command.addFieldValue(res, COMP_NAME, comp_name, "fixed", "Component name");
			Command.addHiddenField(res, COMP_CLASS, comp_class);
			Command.addHiddenField(res, ACTION, action);
			Command.addHiddenField(res, SUBMIT, SUBMIT);
        
			prepareComponentConfigForAdHoc(res, comp_name, comp_class);
         
			return res;
		}
		else if (submit) {
			def comp_class = Command.getFieldValue(p, COMP_CLASS);

			// here we should apply results of new or edit
			def res = (Iq)p.commandResult(Command.DataType.result)
			def props = getConfigFromAdHoc(p, comp_name, comp_class);
                
			def conf = XMPPServer.getConfigurator();
			conf.putProperties(comp_name, props);
                
			def initProps = conf.getDefConfigParams().findAll { return it.key.startsWith(Configurable.GEN_COMP_NAME) && it.value.equals(comp_name); };
			if (initProps.isEmpty()) {
				def suffix = 1;
				while (!(conf.getDefConfigParams().findAll { return it.key.startsWith(Configurable.GEN_COMP_NAME) && it.key.endsWith("-"+suffix); }.isEmpty())) {
					suffix += 1;
				}
				conf.getDefConfigParams().put(Configurable.GEN_COMP_NAME+"-"+suffix, comp_name);
				conf.getDefConfigParams().put(Configurable.GEN_COMP_CLASS+"-"+suffix, comp_class);
			}

			def mrProps = conf.getProperties("message-router");
			def compNames = mrProps.get(MessageRouterConfig.MSG_RECEIVERS_NAMES_PROP_KEY).toList();
                        
			if (!compNames.contains(comp_name)) {
				compNames.add(comp_name);
			}
                        
			mrProps = [:];
			mrProps.put(MessageRouterConfig.MSG_RECEIVERS_NAMES_PROP_KEY, (compNames as String[]));
			mrProps.put(MessageRouterConfig.MSG_RECEIVERS_PROP_KEY + comp_name + ".class", comp_class)
			conf.putProperties("message-router", mrProps);
                        
			new DelayedReloadTask().start();
			Command.addTextField(res, "Note", "Operation successful.");
			return res;
		}
	}


                
                
}
catch(Exception ex) {
	ex.printStackTrace();
	throw ex;
}