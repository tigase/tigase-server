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

Get configuration file

AS:Description: Get configuration file
AS:CommandId: get-config-file
AS:Component: basic-conf
*/

package tigase.admin

import java.io.File;

import tigase.db.*
import tigase.db.comp.*
import tigase.server.*
import tigase.conf.*
import tigase.io.*

def repo = (ComponentRepository)comp_repo
def p = (Packet)packet
def admins = (Set)adminsSet
def stanzaFromBare = p.getStanzaFrom().getBareJID()
def isServiceAdmin = admins.contains(stanzaFromBare)

def CFGFILE_TYPE = "config-file";
def CFGFILE_OPTIONS = ["init.properties", "tigase.conf"];

def cfgfile = Command.getFieldValue(p, CFGFILE_TYPE);

def result = p.commandResult(cfgfile ? Command.DataType.result : Command.DataType.form);

if (!isServiceAdmin) {
	Command.addTextField(result, "Error", "You are not service administrator");
}
else if (cfgfile == null) {
	def filesArray = CFGFILE_OPTIONS.toArray(new String[CFGFILE_OPTIONS.size()]);
	Command.addFieldValue(result, CFGFILE_TYPE, "init.properties", "File", filesArray, filesArray);
}
else {
	def filepath = []
	switch (cfgfile) {
		case "init.properties":
			if (initProperties.get(ConfiguratorAbstract.PROPERTY_FILENAME_PROP_KEY) != null ) {
				filepath = initProperties.get(ConfiguratorAbstract.PROPERTY_FILENAME_PROP_KEY).tokenize(',');
			}
			break;

		case "tigase.conf":
			def filenames = ["/etc/default/tigase", "/etc/tigase/tigase.conf", "etc/tigase.conf"];
			filenames.each { it ->
				def file = new File(it);
				if (filepath.size() == 0 && file.exists()) {
					filepath.add(it);
				}
			};
			break;

		default:
			break;
	}

	if (filepath == null) {
		Command.addTextField(result, "Error", "Config file not specified");
	}
	else {
		filepath.each{ it ->
			def file = new File(it);
			def lines = [];
			file.eachLine { line -> lines += line; };
			Command.addFieldMultiValue(result, "Content", lines);
		}
	}
}

return result;
