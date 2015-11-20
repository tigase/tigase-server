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

Add an SSL certificate for a given domain.

AS:Description: Add SSL Certificate
AS:CommandId: ssl-certificate-add
AS:Component: vhost-man
 */

package tigase.admin

import tigase.db.*
import tigase.db.comp.*
import tigase.server.*
import tigase.cert.*
import tigase.io.*
import tigase.xmpp.JID
import tigase.cluster.*
import tigase.conf.ConfiguratorAbstract;

def MARKER = "command-marker"
//def ITEMS = "item-list"

try {

def VHOST = "VHost"
def CERTIFICATE = "Certificate in PEM format"
def SAVE_TO_DISK = "Save to disk"

def repo = (ComponentRepository)comp_repo
def p = (Packet)packet
def admins = (Set)adminsSet
def stanzaFromBare = p.getStanzaFrom().getBareJID()
def isServiceAdmin = admins.contains(stanzaFromBare)

def itemKey = Command.getFieldValue(packet, VHOST)
def marker = Command.getFieldValue(packet, MARKER)
def pemCertVals = Command.getFieldValues(packet, CERTIFICATE)
def saveToDisk = Command.getCheckBoxFieldValue(packet, SAVE_TO_DISK)

Queue results = new LinkedList()
def supportedComponents = ["vhost-man"]
def NOTIFY_CLUSTER = "notify-cluster"
boolean clusterMode =  Boolean.valueOf( System.getProperty("cluster-mode", false.toString()) );
boolean notifyCluster = Boolean.valueOf( Command.getFieldValue(packet, NOTIFY_CLUSTER) )

// The first step - provide a list of all vhosts for the user
if (itemKey == null) {
	def items = repo.allItems()
	def itemsStr = []
	if (items.size() > 0) {
		items.each {
			if (isServiceAdmin || it.isOwner(stanzaFromBare.toString())) {
				itemsStr += it.getKey()
			}
		}
	}
	if(itemsStr.size() > 0) {
		def result = p.commandResult(Command.DataType.form)
		Command.addFieldValue(result, VHOST, itemsStr[0], "List of VHosts",
			(String[])itemsStr, (String[])itemsStr)
		return result
	} else {
		def result = p.commandResult(Command.DataType.result)
		Command.addTextField(result, "Note", "You have no VHosts to manage");
		return result
	}
}

// The second step - provide a form to fill be by the user for selected vhost
if (marker == null) {
	def item = repo.getItem(itemKey)
	if (item == null) {
		Command.addTextField(result, "Error", "No such VHost, adding SSL Certificate impossible.");
	} else {
		if (isServiceAdmin || item.isOwner(stanzaFromBare.toString()) || item.isAdmin(stanzaFromBare.toString())) {
			def result = p.commandResult(Command.DataType.form)
      Command.addFieldValue(result, VHOST, itemKey, "text-single")
			Command.addFieldMultiValue(result, CERTIFICATE, [""])
			Command.addCheckBoxField(result, SAVE_TO_DISK, true)
			Command.addHiddenField(result, MARKER, MARKER)
			if 	( clusterMode  ) {
				Command.addHiddenField(result, NOTIFY_CLUSTER, true.toString())
			}
			return result
		} else {
			def result = p.commandResult(Command.DataType.result)
			Command.addTextField(result, "Error", "You do not have enough permissions to manage this VHost.")
			return result
		}
	}
}

if 	( clusterMode && notifyCluster && supportedComponents.contains(componentName) ) {
	def nodes = (List)connectedNodes
	if (nodes && nodes.size() > 0 ) {
		nodes.each { node ->
			def forward = p.copyElementOnly();
			Command.removeFieldValue(forward, NOTIFY_CLUSTER)
			Command.addHiddenField(forward, NOTIFY_CLUSTER, false.toString())
			forward.setPacketTo( node );
			forward.setPermissions( Permissions.ADMIN );

			results.offer(forward)
		}
	}
}

// The last step - process the form submitted by the user
def result = p.commandResult(Command.DataType.result)
def item = repo.getItem(itemKey)

if (item == null) {
	Command.addTextField(result, "Error", "No such VHost, loading SSL certificate impossible.")
} else {
  if (isServiceAdmin || item.isOwner(stanzaFromBare.toString())) {
		def pemCert = pemCertVals.join('\n')
    // Basic certificate checks
		// For XMPP service nonAdmins (domain owners) the alias must match CN name in the certificate
		CertificateEntry certEntry = CertificateUtil.parseCertificate(new CharArrayReader(pemCert.toCharArray()))
		if (certEntry.getPrivateKey() == null) {
			Command.addTextField(result, "Error", "Missing private key or private key encoded in uknown format.")
			Command.addTextField(result, "Note", "Private key cannot be encoded with a password.")
			//println(pemCert);
		} else {
			def certCName = CertificateUtil.getCertCName(certEntry.getCertChain()[0])
			def subjectAltName = CertificateUtil.getCertAltCName(certEntry.getCertChain()[0])
			if (certCName != itemKey && !subjectAltName.contains(itemKey) && !isServiceAdmin) {
				Command.addTextField(result, "Error", "Neither certificate CName nor any of SubjectAlternativeNames match the domain name!")
			} else {
				def params = new HashMap()
				params.put(SSLContextContainerIfc.PEM_CERTIFICATE_KEY, pemCert)
				params.put(SSLContextContainerIfc.CERT_ALIAS_KEY, itemKey)
				params.put(SSLContextContainerIfc.CERT_SAVE_TO_DISK_KEY, saveToDisk.toString())
				TLSUtil.addCertificate(params)
				Command.addTextField(result, "Note", "SSL Certificate for domain: " + itemKey + " loaded successfully")
			}
		}
	} else {
		Command.addTextField(result, "Error", "You are not the VHost owner or you have no "
			+ "enough permission to change the VHost.")
	}
}

results.add(result);
return results;

} catch (Exception e) {
			e.printStackTrace();
			return e.getLocalizedMessage();
}