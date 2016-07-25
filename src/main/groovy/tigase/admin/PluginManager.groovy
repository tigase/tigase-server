/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2012 "Artur Hefczyc" <artur.hefczyc@tigase.org>
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

Manage active server plugins

AS:Description: Manage active server plugins
AS:CommandId: plugin-manager
AS:Component: basic-conf
AS:Group: Configuration
 */

package tigase.admin

import tigase.conf.ConfigRepositoryIfc
import tigase.conf.Configurable;
import tigase.conf.Configurator;
import tigase.osgi.ModulesManagerImpl;
import tigase.server.*

class DelayedReloadTaskPlugMan extends Thread {
	void run() {
		Thread.sleep(5000);
	    ((Configurator) XMPPServer.getConfigurator()).updateMessageRouter();
	}
}

try {

def SUBMIT = "exec";
        
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

        
//def submit = Command.getFieldValue(p, SUBMIT);
def submit = p.getElement().findChild(["iq","command","x"] as String[])?.getAttribute("type");

if (!submit) {
        def res = (Iq)p.commandResult(Command.DataType.form)
                
        def pluginsAll = [];
		if (XMPPServer.isOSGi()) {
			pluginsAll.addAll(ModulesManagerImpl.getInstance().plugins.keySet());
		}
		else {
			pluginsAll.addAll(tigase.xmpp.ProcessorFactory.processors.keySet());
		}
        def conf = XMPPServer.getConfigurator();
        def pluginsEnabled = [];
        pluginsEnabled.addAll(tigase.server.xmppsession.SessionManagerConfig.PLUGINS_FULL_PROP_VAL);
        
        def pluginsStr = conf.getDefConfigParams().get(Configurable.GEN_SM_PLUGINS);
        if (pluginsStr) {
                pluginsStr.split(",").each { tmp ->
                        def id = tmp;
                        switch (tmp.charAt(0)) {
                                case '+':
                                        id = tmp.substring(1);
                                        if (!pluginsAll.contains(id)) {
                                                pluginsAll.add(id);
                                        }
                                        if (!pluginsEnabled.contains(id)) {
                                                pluginsEnabled.add(id);
                                        }
                                        break;
                                case '-':
                                        id = tmp.substring(1);
                                        if (!pluginsAll.contains(id)) {
                                                pluginsAll.add(id);
                                        }
                                        pluginsEnabled.remove(id);
                                        break;
                                default:
                                        pluginsEnabled.add(id);
                                        break;
                        }
                }
        }
        
        Command.addHiddenField(res, SUBMIT, SUBMIT);
                
        pluginsAll.sort();
        pluginsAll.each { id ->
                Command.addCheckBoxField(res, id, pluginsEnabled.contains(id));
        }
        
        return res;
}
else {
        def pluginsEnabled = [];
        pluginsEnabled.addAll(tigase.server.xmppsession.SessionManagerConfig.PLUGINS_FULL_PROP_VAL);
        
        def str = "";
                
        def data = Command.getData(p, "x", "jabber:x:data");
        data.getChildren().each { child ->
                if (child.getName() != 'field') return;
                if (child.getAttribute("value") == SUBMIT) return;
                def id = tigase.xml.XMLUtils.escape(child.getAttribute("var"));
                def enable = Command.getCheckBoxFieldValue(p, id);
                if (enable && pluginsEnabled.contains(id)) return;
                if (!enable && !pluginsEnabled.contains(id)) return;
                if (enable && !pluginsEnabled.contains(id)) {
                        if (!str.isEmpty()) str += ",";
                        str += "+" + id;
                        pluginsEnabled.add(id);
                }
                else if (!enable && pluginsEnabled.contains(id)) {
                        if (!str.isEmpty()) str += ",";
                        str += "-" + id;
						pluginsEnabled.remove(id);
                }
        }

        def conf = XMPPServer.getConfigurator();
        conf.getDefConfigParams().put(Configurable.GEN_SM_PLUGINS, str.isEmpty() ? null : str);
	
        def props = [:];
        props[tigase.server.xmppsession.SessionManagerConfig.PLUGINS_PROP_KEY] = (pluginsEnabled as String[]);
        conf.putProperties("sess-man", props);
                
		new DelayedReloadTaskPlugMan().start();
		
        def res = (Iq)p.commandResult(Command.DataType.result)

        Command.addTextField(res, "Note", "Operation successful.");
                
        return res;
}        

}
catch (Exception ex) {
        ex.printStackTrace();
        throw ex;
}