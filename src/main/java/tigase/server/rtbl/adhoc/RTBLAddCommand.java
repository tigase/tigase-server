/*
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
package tigase.server.rtbl.adhoc;

import tigase.component.adhoc.AdHocCommandException;
import tigase.component.adhoc.AdHocResponse;
import tigase.component.adhoc.AdhHocRequest;
import tigase.db.TigaseDBException;
import tigase.form.Field;
import tigase.form.Form;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;
import tigase.server.rtbl.RTBL;
import tigase.server.rtbl.RTBLComponent;
import tigase.server.rtbl.RTBLFetchModule;
import tigase.server.rtbl.RTBLSubscribeModule;
import tigase.util.stringprep.TigaseStringprepException;
import tigase.xmpp.Authorization;
import tigase.xmpp.jid.BareJID;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;

@Bean(name = "rtbl-command-add", parent = RTBLComponent.class, active = true)
public class RTBLAddCommand
		extends AbstractAdHocCommand {

	private static final Logger log = Logger.getLogger(RTBLAddCommand.class.getCanonicalName());

	@Inject
	private RTBLSubscribeModule subscribeModule;
	@Inject
	private RTBLFetchModule fetchModule;

	@Override
	public String getName() {
		return "Add real-time blocklist";
	}

	@Override
	public String getNode() {
		return "rtbl-add";
	}
	
	@Override
	protected Form prepareForm(AdhHocRequest request, AdHocResponse response) throws AdHocCommandException {
		Form form = new Form("form", "Add real-time blocklist", "Fill out and submit this form to add a new real-time blocklist");
		form.addField(Field.fieldJidSingle("pubsubJid", "", "Service address (JID)"));
		form.addField(Field.fieldTextSingle("node", "", "Node"));
		form.addField(Field.fieldTextSingle("hash", "SHA-256", "Hashing algorithm"));
		return form;
	}

	@Override
	protected Form submitForm(AdhHocRequest request, AdHocResponse response, Form form) throws AdHocCommandException {
		try {
			BareJID jid = BareJID.bareJIDInstance(assertNotEmpty(form.getAsString("pubsubJid"), "Service address is required!"));
			String node = assertNotEmpty(form.getAsString("node"), "Node is required!");
			String hash = assertNotEmpty(form.getAsString("hash"), "Hash algorithm is required!");
			MessageDigest.getInstance(hash);
			RTBL rtbl = getRepository().getBlockList(jid, node);
			if (rtbl != null) {
				throw new AdHocCommandException(Authorization.CONFLICT, "This blocklist was already added.");
			}
			getRepository().add(jid, node, hash);
			subscribeModule.subscribe(jid, node);
			fetchModule.fetch(jid, node);
			return null;
		} catch (TigaseStringprepException e) {
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "could not parse JID", e);
			}
			throw new AdHocCommandException(Authorization.BAD_REQUEST, "Invalid service address");
		} catch (NoSuchAlgorithmException e) {
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "could not find hash algorithm", e);
			}
			throw new AdHocCommandException(Authorization.BAD_REQUEST, "Unsupported hashing algorithm");
		} catch (TigaseDBException e) {
			if (log.isLoggable(Level.WARNING)) {
				log.log(Level.WARNING, "failed to update database", e);
			}
			throw new AdHocCommandException(Authorization.INTERNAL_SERVER_ERROR);
		}
	}
	
}
