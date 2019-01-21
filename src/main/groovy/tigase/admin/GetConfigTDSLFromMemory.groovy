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

Get config.tdsl configuration from memory.

AS:Description: Get config.tdsl configuration
AS:CommandId: get-config-tdsl
AS:Component: message-router
AS:Group: Configuration
*/

package tigase.admin

import tigase.component.DSLBeanConfigurator
import tigase.kernel.core.Kernel
import tigase.server.Command
import tigase.server.Iq

Kernel kernel = (Kernel) kernel;
Iq p = (Iq) packet
Set<String> admins = (Set<String>) adminsSet
def stanzaFromBare = p.getStanzaFrom().getBareJID()
def isServiceAdmin = admins.contains(stanzaFromBare)

def result = p.commandResult(Command.DataType.result);

if (!isServiceAdmin) {
	Command.addTextField(result, "Error", "You are not service administrator");
} else {
	DSLBeanConfigurator configurator = kernel.getInstance(DSLBeanConfigurator.class);
	StringWriter writer = new StringWriter();
	configurator.dumpConfiguration(writer);
	
	Command.addFieldMultiValue(result, "config.tdsl", Arrays.asList(writer.toString().split("\n")));
}

return result;
