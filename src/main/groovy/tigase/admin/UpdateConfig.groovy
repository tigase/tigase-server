/**
 * Tigase XMPP Server - The instant messaging server
 * Copyright (C) 2004 Tigase, Inc. (office@tigase.com)
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
 */
/*

Update config.tdsl configuration

AS:Description: Update config.tdsl configuration
AS:CommandId: update-config-tdsl
AS:Component: message-router
AS:Group: Configuration
*/

package tigase.admin

import groovy.transform.CompileStatic
import tigase.component.DSLBeanConfigurator
import tigase.conf.ConfigReader
import tigase.conf.ConfigWriter
import tigase.kernel.core.Kernel
import tigase.server.Command
import tigase.server.Iq
import tigase.server.Packet

Kernel kernel = (Kernel) kernel;
Iq p = (Iq) packet
Set<String> admins = (Set<String>) adminsSet
def stanzaFromBare = p.getStanzaFrom().getBareJID()
def isServiceAdmin = admins.contains(stanzaFromBare)

@CompileStatic
class ConfigReloadTask
		extends Thread {

	private final Map<String, Object> props;
	private final String configStr;
	private final DSLBeanConfigurator beanConfigurator;
	
	ConfigReloadTask(DSLBeanConfigurator beanConfigurator, Map<String, Object> props, String configStr) {
		this.props = props;
		this.configStr = configStr;
		this.beanConfigurator = beanConfigurator;
	}

	void run() {
		Thread.sleep(5000);

		System.out.println("Updating configuration at " + new Date());
		Map<String, Object> oldConfig = beanConfigurator.getConfigHolder().getProperties();
		try {
			beanConfigurator.getConfigHolder().setProperties(props);
			beanConfigurator.configurationChanged();

			String filepath = beanConfigurator.
					getConfigHolder().
					getConfigFilePath().
					toString();

			if (new File(filepath + ".backup").exists()) {
				new File(filepath + ".backup").delete();
			}
			new File(filepath).renameTo(filepath + ".backup");
			new File(filepath + ".new").write(configStr);

			// Dumping new configuration
			System.out.println("Configuration updated and saved!");
			File f = new File("etc/config-dump.properties");
			if (f.exists()) {
				f.delete();
			}
			beanConfigurator.dumpConfiguration(f);
		} catch (Throwable ex) {
			System.out.println("Could not apply configuration:");
			ex.printStackTrace();

			beanConfigurator.getConfigHolder().setProperties(oldConfig);
			beanConfigurator.configurationChanged();
		}
	}
}

@CompileStatic
Packet process(Kernel kernel, Iq p, boolean isServiceAdmin) {
	if (!isServiceAdmin) {
		Packet result = p.commandResult(Command.DataType.result);
		Command.addTextField(result, "Error", "You are not service administrator");
		return result;
	}

	String newConfig =  Arrays.asList(Command.getFieldValues(p, "Config") ?: new String[0]).join("\n");
	if (!newConfig.trim().isEmpty()) {
		try {
			Map<String, Object> newConfigMap = new ConfigReader().read(new StringReader(newConfig));

			new ConfigReloadTask(kernel.getInstance(DSLBeanConfigurator.class), newConfigMap, newConfig).start();

			Packet result = p.commandResult(Command.DataType.result);
			Command.addTextField(result, "Note", "Config initially validated and scheduled to be applied.");
			return result;
		} catch (ConfigReader.UnsupportedOperationException e) {
			Packet result = p.commandResult(Command.DataType.form);
			Command.addTextField(result, "Error", e.getMessage() + " at line " + e.getLine() + " position " +
										 e.getPosition() + "\nLine: " + e.getLineContent());
			Command.addFieldMultiValue(result, "Config", Command.getFieldValues(p, "Config") as List<String>)
			return result;
		} catch (ConfigReader.ConfigException e) {
			Packet result = p.commandResult(Command.DataType.form);
			Command.addTextField(result, "Error", "There is an error in your config. Please fix it.");
			Command.addFieldMultiValue(result, "Config", Command.getFieldValues(p, "Config") as List<String>)
			return result;
		}
	} else {
		Packet result = p.commandResult(Command.DataType.form);
		DSLBeanConfigurator configurator = kernel.getInstance(DSLBeanConfigurator.class);
		StringWriter writer = new StringWriter();
		new ConfigWriter().write(writer, configurator.getConfigHolder().getProperties());

		Command.addFieldMultiValue(result, "Config", Arrays.asList(writer.toString().split("\n")));
		return result;
	}
}

try {
	return process(kernel, p, isServiceAdmin);
} catch (Exception ex) {
	ex.printStackTrace();
	throw ex;
}
