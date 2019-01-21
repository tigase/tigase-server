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
package tigase.monitor.modules;

import tigase.component.adhoc.AdHocCommand;
import tigase.component.adhoc.AdHocCommandException;
import tigase.component.adhoc.AdHocResponse;
import tigase.component.adhoc.AdhHocRequest;
import tigase.form.Form;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;
import tigase.kernel.core.Kernel;
import tigase.monitor.InfoTask;
import tigase.monitor.MonitorComponent;
import tigase.xmpp.Authorization;
import tigase.xmpp.jid.JID;

import java.util.logging.Level;
import java.util.logging.Logger;

import static tigase.monitor.modules.InfoTaskCommand.NODE;

@Bean(name = NODE, parent = MonitorComponent.class, active = true)
public class InfoTaskCommand
		implements AdHocCommand {

	private final static Logger log = Logger.getLogger(InfoTaskCommand.class.getName());
	public static final String NODE = "x-info";

	@Inject
	private MonitorComponent component;
	@Inject
	private Kernel kernel;

	public InfoTaskCommand() {
	}

	@Override
	public void execute(AdhHocRequest request, AdHocResponse response) throws AdHocCommandException {
		try {
			if (request.getAction() != null && "cancel".equals(request.getAction())) {
				response.cancelSession();
			} else {
				final InfoTask taskInstance = kernel.getInstance(request.getIq().getStanzaTo().getResource());

				Form form = taskInstance.getTaskInfo();

				response.getElements().add(form.getElement());
			}
		} catch (Exception e) {
			log.log(Level.FINEST, "Error executing script", e);
			throw new AdHocCommandException(Authorization.INTERNAL_SERVER_ERROR, e.getMessage());
		}
	}

	@Override
	public String getName() {
		return "Task info";
	}

	@Override
	public String getNode() {
		return NODE;
	}

	@Override
	public boolean isAllowedFor(JID jid) {
		return component.isAdmin(jid);
	}

}
