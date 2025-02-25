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

package tigase.server.xmppsession.adhoc;

import tigase.component.adhoc.AdHocCommand;
import tigase.component.adhoc.AdHocCommandException;
import tigase.component.adhoc.AdHocResponse;
import tigase.component.adhoc.AdhHocRequest;
import tigase.form.Form;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;
import tigase.server.xmppserver.KnownDomainsListProvider;
import tigase.server.xmppsession.SessionManager;
import tigase.xmpp.Authorization;
import tigase.xmpp.jid.JID;

import java.util.logging.Level;
import java.util.logging.Logger;

import static tigase.form.Field.fieldTextMulti;
import static tigase.server.xmppsession.adhoc.SuggestedDomainsListAdhoc.XMLNS;

/**
 * AdHoc command to retrieve list of all know domains (local and remote)
 *
 * <pre>
 *  {@code
 *  <iq type='set'>
 * 	  <command xmlns='http://jabber.org/protocol/commands' node='tigase:instance:details#domains' action='execute'/>
 * 	</iq>
 *  }
 * 	</pre>
 */
@Bean(name = XMLNS, parent = SessionManager.class, active = true, exportable = true)
public class SuggestedDomainsListAdhoc
	implements AdHocCommand {

	protected static final String XMLNS = "tigase:instance:details#domains";

	private static final Logger log = Logger.getLogger(SuggestedDomainsListAdhoc.class.getCanonicalName());

	@Inject
	private KnownDomainsListProvider knownDomainsListProvider;

	public void execute(AdhHocRequest request, AdHocResponse response) throws AdHocCommandException {
		try {
			response.getElements().add(this.prepareForm().getElement());
			response.completeSession();
		} catch (Exception e) {
			log.log(Level.FINE, "Exception during execution of adhoc command " + this.getNode(), e);
			throw new AdHocCommandException(Authorization.INTERNAL_SERVER_ERROR, e.getMessage());
		}
	}

	@Override
	public String getName() {
		return "Information about domains on this instance";
	}

	@Override
	public String getNode() {
		return XMLNS;
	}

	@Override
	public boolean isAllowedFor(JID jid) {
		return false;
	}

	@Override
	public boolean isAllowedFor(JID from, JID to) {
		return to == null || from.getBareJID().equals(to.getBareJID());
	}

	@Override
	public boolean isForSelf() {
		return true;
	}

	protected Form prepareForm() {
		var form = new Form("form", "Know local and remote domains",
		                    "Form used to obtain list of all know local and remote domains");
		var localDomains = knownDomainsListProvider.getAllLocalDomains().toArray(String[]::new);
		var knownRemoteDomains = knownDomainsListProvider.getAuthenticatedRemoteDomains().toArray(String[]::new);

		form.addField(fieldTextMulti("knownRemoteDomains", knownRemoteDomains, "List of known remote domains"));

		form.addField(fieldTextMulti("localDomains", localDomains, "List of local instance domains"));
		return form;
	}
}
