/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2016 "Tigase, Inc." <office@tigase.org>
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
 */

/*
Set Welcome Message
AS:Description: Set Welcome Message
AS:CommandId: http://jabber.org/protocol/admin#set-welcome
AS:Component: sess-man
AS:Group: Configuration
 */
package tigase.admin

import groovy.transform.CompileStatic
import tigase.kernel.core.Kernel
import tigase.server.Command
import tigase.server.Iq
import tigase.server.Packet
import tigase.server.xmppsession.SessionManager
import tigase.xmpp.impl.JabberIqRegister

Kernel kernel = (Kernel) kernel;
SessionManager component = (SessionManager) component
packet = (Iq) packet

@CompileStatic
Packet process(Kernel kernel, SessionManager component, Iq p) {
	String MOTD = "welcome"

	if (!component.isAdmin(p.getStanzaFrom())) {
		def result = p.commandResult(Command.DataType.result)
		Command.addTextField(result, "Error", "You are not service administrator")
		return result
	}

	def motd = Command.getFieldValues(p, MOTD)
	if (!motd) {
		def result = p.commandResult(Command.DataType.form)

		Command.addTitle(result, "Setting Welcome Message")
		Command.addInstructions(result, "Fill out this form to set the welcome message for this service.")
		Command.addHiddenField(result, "FORM_TYPE", "http://jabber.org/protocol/admin")
		Command.addFieldMultiValue(result, MOTD, [], "Welcome message")

		return result
	} else {
		def result = p.commandResult(Command.DataType.result)
		JabberIqRegister registerProcessor = kernel.getInstance(JabberIqRegister.class)
		registerProcessor.setWelcomeMessage(motd.join("\n"))
		return result
	}
}

return process(kernel, component, packet)