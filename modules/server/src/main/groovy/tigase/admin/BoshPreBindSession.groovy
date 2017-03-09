/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2013 "Tigase, Inc." <office@tigase.com>
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3 of the License,
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
 */

/*
AS:Description: Pre-Bind BOSH user session
AS:CommandId: pre-bind-bosh-session
AS:Component: bosh
 */


package tigase.admin

import tigase.server.*
import tigase.server.bosh.*
import tigase.util.*
import tigase.xmpp.*
import tigase.db.*
import tigase.xml.*
import tigase.vhosts.*

try {

	def USER_JID = "from";
	def RID = "rid"
	def HOLD = "hold"
	def WAIT = "wait"
	def SID = "sid"
	def HOSTNAME = "hostname"

	def p = (Iq)packet

	def vhost_man = (VHostManagerIfc)vhostMan
	def admins = (Set)adminsSet
	def stanzaFromBare = p.getStanzaFrom().getBareJID()
	def isServiceAdmin = admins.contains(stanzaFromBare)

	def userJid = Command.getFieldValue(p, USER_JID)
	def rid = 0
	def hold = Command.getFieldValue(p,HOLD)
	def wait = Command.getFieldValue(p,WAIT)

	if (userJid == null || userJid.isEmpty() ) {
		def res = (Iq)p.commandResult(Command.DataType.form);
		Command.addTitle(res, "Pre-bind BOSH user session")
		Command.addInstructions(res, "Fill out this form to create and pre-bind BOSH user session.")

		Command.addFieldValue(res, "FORM_TYPE", "http://jabber.org/protocol/admin", "hidden")

		Command.addFieldValue(res, USER_JID, "", "jid-single", "JID of the user for which session should be created - either BareJID or FullJID, the former will result in randomly generated resource")
		Command.addFieldValue(res, HOLD, hold ?: "1","text-single", "HOLD value (optional)")
		Command.addFieldValue(res, WAIT, wait ?: "60","text-single", "WAIT value (optional)")

		return res
	}

	bareJID = BareJID.bareJIDInstance(userJid)
	VHostItem vhost = vhost_man.getVHostItem(bareJID.getDomain())

	def result = (Iq)p.commandResult(Command.DataType.result)

	if (vhost == null ) {
		Command.addTextField(result, "Error", "Domain of the JID doesn't exists");
	} else if (isServiceAdmin ||
		(vhost != null && (vhost.isOwner(stanzaFromBare.toString()) || vhost.isAdmin(stanzaFromBare.toString())))) {

		Map args = new HashMap();
		if (userJid != null && !userJid.isEmpty()) {
			args.put(USER_JID,userJid)
		}
		if (hold != null && !hold.isEmpty()) args.put(HOLD,hold)
		if (wait != null && !wait.isEmpty()) args.put(WAIT,wait)

		args = boshCM.preBindSession(args)

		rid = args.get(RID);
		def sid = args.get(SID);
		def hostname = args.get(HOSTNAME)
		userJid = args.get(USER_JID)

		if (hostname != null ) {
		Command.addFieldValue(result, USER_JID, userJid,"jid-single",  "JID")
		Command.addFieldValue(result, HOSTNAME, hostname,"jid-single",  "hostname")
		Command.addFieldValue(result, RID, rid, "text-single", "RID")
		Command.addFieldValue(result, SID, sid, "text-single", "SID")
		Command.addFieldValue(result, HOLD, hold, "text-single", "HOLD")
		Command.addFieldValue(result, WAIT, wait, "text-single", "WAIT")
		} else {
			Command.addTextField(result, "Error", "Error processing request, provided data is invalid");
		}
	} else {
		Command.addTextField(result, "Error", "You do not have enough permissions");
	}
	return result

}  catch (Exception ex) { ex.printStackTrace(); }