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

List server/component configuration

AS:Description: List server/component configuration
AS:CommandId: config-list
AS:Component: basic-conf
AS:Group: Configuration
*/

package tigase.admin

import tigase.conf.*
import tigase.server.*

def COMP_NAME = "comp-name"
def PARAMS_SET = "params-set"

def conf_repo = (ConfigRepositoryIfc)comp_repo
def p = (Iq)packet
def comp_name = Command.getFieldValue(p, COMP_NAME)

def admins = (Set)adminsSet
def stanzaFromBare = p.getStanzaFrom().getBareJID()
def isServiceAdmin = admins.contains(stanzaFromBare)

try {

	if (!isServiceAdmin) {
			def result = p.commandResult(Command.DataType.result)
			Command.addTextField(result, "Error", "You do not have enough permissions to access this data.");
			return result
	}

	if (comp_name == null) {
		def res = (Iq)p.commandResult(Command.DataType.form)
		def compNames = []
		conf_repo.getCompNames().each { compNames += it }
		Command.addFieldValue(res, COMP_NAME, comp_name ?: compNames[0], "Components",
			(String[])compNames, (String[])compNames)
		return res
	}

	def params_set = Command.getFieldValue(p, PARAMS_SET)

	if (params_set == null) {
		def res = (Iq)p.commandResult(Command.DataType.result)
		def compNames = []
		conf_repo.getCompNames().each { compNames += it }
		Command.addTextField(res, COMP_NAME, comp_name ?: compNames[0])
		Command.addHiddenField(res, PARAMS_SET, PARAMS_SET)
		conf_repo.getItemsForComponent(comp_name).each {
			Command.addFieldValue(res, it.getKey(), it.getConfigValToString())
		}
		return res
	}

} catch (Exception e) {
	e.printStackTrace();
}