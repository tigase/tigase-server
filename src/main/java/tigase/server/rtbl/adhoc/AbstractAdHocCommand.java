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

import tigase.component.adhoc.AdHocCommand;
import tigase.component.adhoc.AdHocCommandException;
import tigase.component.adhoc.AdHocResponse;
import tigase.component.adhoc.AdhHocRequest;
import tigase.form.Form;
import tigase.kernel.beans.Inject;
import tigase.server.rtbl.RTBLComponent;
import tigase.server.rtbl.RTBLRepository;
import tigase.xml.Element;
import tigase.xmpp.Authorization;
import tigase.xmpp.jid.JID;

import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class AbstractAdHocCommand implements AdHocCommand {

	private static final Logger log = Logger.getLogger(AbstractAdHocCommand.class.getCanonicalName());

	@Inject
	private RTBLComponent component;
	@Inject
	private RTBLRepository repository;
	@Override
	public void execute(AdhHocRequest request, AdHocResponse response) throws AdHocCommandException {
		try {
			final Element data = request.getCommand().getChild("x", "jabber:x:data");

			if (request.isAction("cancel")) {
				response.cancelSession();
			} else {
				if (data == null) {
					response.getElements().add(prepareForm(request, response).getElement());
					response.startSession();
				} else {
					Form form = new Form(data);
					if (form.isType("submit")) {
						Form responseForm = submitForm(request, response, form);
						if (responseForm != null) {
							response.getElements().add(responseForm.getElement());
						}
					}
				}
			}
		} catch (AdHocCommandException ex) {
			throw ex;
		} catch (Exception e) {
			log.log(Level.FINE, "Exception during execution of adhoc command " + getNode(), e);
			throw new AdHocCommandException(Authorization.INTERNAL_SERVER_ERROR, e.getMessage());
		}
	}

	@Override
	public boolean isAllowedFor(JID jid) {
		return component.isAdmin(jid);
	}

	protected RTBLRepository getRepository() {
		return repository;
	}

	protected abstract Form prepareForm(AdhHocRequest request, AdHocResponse response) throws AdHocCommandException;

	protected abstract Form submitForm(AdhHocRequest request, AdHocResponse response, Form form)
			throws AdHocCommandException;

	protected String assertNotEmpty(String input, String message) throws AdHocCommandException {
		if (input == null || input.isBlank()) {
			throw new AdHocCommandException(Authorization.BAD_REQUEST, message);
		}
		return input.trim();
	}
}
