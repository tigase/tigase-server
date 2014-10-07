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
AdHoc Command for setting OAuth credentials for JabberIQRegister

AS:Description: OAuth credentials
AS:CommandId: oauth-credentials
AS:Component: sess-man
*/

package tigase.admin

import tigase.server.*

def p = (Packet)packet

def oauthTokenKey = Command.getFieldValue(packet, "oauthTokenKey")
def oauthTokenSecret = Command.getFieldValue(packet, "oauthTokenSecret")
def signedFormRequired = Command.getFieldValue(packet, "signedFormRequired")

if (signedFormRequired == null) {
	def res = p.commandResult(Command.DataType.form)
	Command.addTitle(res, "OAuth Credentials")
	Command.addInstructions(res, "It allows to set new OAuth credentials and enable or disable requirement of registration with Signed Form.")
	Command.addFieldValue(res, "oauthTokenKey", "", "text-single", "OAuth Token Key")
	Command.addFieldValue(res, "oauthTokenSecret", "", "text-single", "OAuth Token Secret")
	Command.addFieldValue(res, "signedFormRequired", Boolean.toString(JabberIqRegister.isSignedFormRequired()), "boolean", "Signed Form required to registration") 
	return res
} else {
	JabberIqRegister.setOAuthCredentials(oauthTokenKey, oauthTokenSecret)
	JabberIqRegister.setSignedFormRequired(signedFormRequired.equals("1") || signedFormRequired.equals("true"))
	
	def res = p.commandResult(Command.DataType.result)
	Command.addTitle(res, "OAuth Credentials")
	Command.addInstructions(res, "Credentials set.")
	return res
}

