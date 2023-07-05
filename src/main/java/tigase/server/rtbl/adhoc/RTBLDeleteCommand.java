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
import tigase.server.rtbl.RTBLSubscribeModule;
import tigase.xmpp.Authorization;
import tigase.xmpp.jid.JID;

import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Bean(name = "rtbl-command-delete", parent = RTBLComponent.class, active = true)
public class RTBLDeleteCommand extends AbstractAdHocCommand {

	private static final Logger log = Logger.getLogger(RTBLDeleteCommand.class.getCanonicalName());

	@Inject
	private RTBLSubscribeModule subscribeModule;
	
	@Override
	public String getName() {
		return "Delete real-time blocklist";
	}

	@Override
	public String getNode() {
		return "rtbl-delete";
	}

	@Override
	protected Form prepareForm(AdhHocRequest request, AdHocResponse response) throws AdHocCommandException {
		Form form = new Form("form", "Delete real-time blocklist", "Select blocklist to delete");
		List<RTBL> blocklists = getRepository().getBlockLists();
		blocklists.sort(Comparator.comparing(RTBL::getJID).thenComparing(RTBL::getNode));
		String[] ids = blocklists.stream()
				.map(rtbl -> rtbl.getJID().toString() + "/" + rtbl.getNode())
				.toArray(String[]::new);
		form.addField(Field.fieldListSingle("blocklist", "", "Blocklist", ids, ids));
		return form;
	}

	@Override
	protected Form submitForm(AdhHocRequest request, AdHocResponse response, Form form) throws AdHocCommandException {
		String blocklistId = assertNotEmpty(form.getAsString("blocklist"), "You need to select blocklist!");
		JID tmp = JID.jidInstanceNS(blocklistId);
		try {
			getRepository().remove(tmp.getBareJID(), tmp.getResource());
			subscribeModule.unsubscribe(tmp.getBareJID(), tmp.getResource());
		} catch (TigaseDBException e) {
			if (log.isLoggable(Level.WARNING)) {
				log.log(Level.WARNING, "failed to update database", e);
			}
			throw new AdHocCommandException(Authorization.INTERNAL_SERVER_ERROR);
		}
		return null;
	}
}
