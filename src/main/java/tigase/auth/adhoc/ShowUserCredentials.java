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
package tigase.auth.adhoc;

import tigase.component.adhoc.AdHocCommandException;
import tigase.component.adhoc.AdHocResponse;
import tigase.component.adhoc.AdhHocRequest;
import tigase.db.TigaseDBException;
import tigase.form.Field;
import tigase.form.Fields;
import tigase.form.Form;
import tigase.form.MultiItemForm;
import tigase.kernel.beans.Bean;
import tigase.server.xmppsession.SessionManager;
import tigase.xmpp.jid.BareJID;

import java.util.Collection;

@Bean(name = ShowUserCredentials.NODE, parent = SessionManager.class, active = true)
public class ShowUserCredentials
		extends AbstractCredentialsCommand {

	public static final String NODE = "auth-credentials-list";

	public ShowUserCredentials() {
	}

	@Override
	public String getName() {
		return "List user credentials";
	}

	@Override
	public String getNode() {
		return NODE;
	}

	@Override
	protected void processForm(Form form, AdhHocRequest request, AdHocResponse response)
			throws TigaseDBException, AdHocCommandException {
		BareJID jid = BareJID.bareJIDInstanceNS(form.getAsString(FIELD_JID));

		checkIfCanModifyJID(request, jid);

		MultiItemForm resp = new MultiItemForm();
		resp.setType("form");

		Collection<String> usernames = authRepository.getUsernames(jid);
		for (String username : usernames) {
			Fields ff = new Fields();
			ff.addField(Field.fieldTextSingle("username", username, "Username"));
			resp.addItem(ff);
		}

		response.getElements().add(resp.getElement());
		response.completeSession();
	}

	@Override
	protected void processNoForm(AdhHocRequest request, AdHocResponse response) {
		Form form = new Form("form", getName(), null);

		form.addField(Field.fieldJidSingle(FIELD_JID, null, "The Jabber ID for the account"));

		response.startSession();
		response.getElements().add(form.getElement());
	}
}
