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

import tigase.component.adhoc.AdHocCommand;
import tigase.component.adhoc.AdHocCommandException;
import tigase.component.adhoc.AdHocResponse;
import tigase.component.adhoc.AdhHocRequest;
import tigase.component.modules.impl.AdHocCommandModule;
import tigase.db.AuthRepository;
import tigase.db.TigaseDBException;
import tigase.form.Form;
import tigase.kernel.beans.Inject;
import tigase.server.xmppsession.SessionManager;
import tigase.xml.Element;
import tigase.xmpp.Authorization;
import tigase.xmpp.jid.BareJID;
import tigase.xmpp.jid.JID;

import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class AbstractCredentialsCommand
		implements AdHocCommand {

	protected static final String FIELD_JID = "jid";
	protected static final String FIELD_USERNAME = "username";
	protected static final String FIELD_PASSWORD = "password";
	protected final Logger log = Logger.getLogger(this.getClass().getName());
	@Inject
	protected AuthRepository authRepository;

	@Inject
	private AdHocCommandModule.ScriptCommandProcessor scriptCommandProcessor;

	protected void checkIfCanModifyJID(final AdhHocRequest request, final BareJID jidToModify)
			throws AdHocCommandException {
		if (!jidToModify.equals(request.getSender().getBareJID())) {
			log.log(Level.FINEST, "Cannot modify credentials of different user");
			throw new AdHocCommandException(Authorization.NOT_AUTHORIZED,
											"You are not allowed to modify credentials " + "of user " + jidToModify);
		}
	}

	@Override
	public void execute(AdhHocRequest request, AdHocResponse response) throws AdHocCommandException {
		try {
			final Element data = request.getCommand().getChild("x", "jabber:x:data");
			if (request.getAction() != null && "cancel".equals(request.getAction())) {
				response.cancelSession();
			} else if (data == null) {
				processNoForm(request, response);
			} else {
				Form form = new Form(data);
				processForm(form, request, response);
			}
		} catch (Exception e) {
			log.log(Level.FINEST, "Error during processing command", e);
			throw new AdHocCommandException(Authorization.INTERNAL_SERVER_ERROR, e.getMessage());
		}
	}

	@Override
	public boolean isAllowedFor(JID jid) {
		return scriptCommandProcessor.isAllowed(getNode(), jid);
	}

	protected abstract void processForm(Form form, AdhHocRequest request, AdHocResponse response)
			throws TigaseDBException, AdHocCommandException;

	protected abstract void processNoForm(AdhHocRequest request, AdHocResponse response) throws TigaseDBException;
}
