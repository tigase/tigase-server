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
import tigase.form.Field;
import tigase.form.Form;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;
import tigase.kernel.core.Kernel;
import tigase.monitor.MonitorComponent;
import tigase.monitor.MonitorTask;
import tigase.monitor.TasksScriptRegistrar;
import tigase.xml.Element;
import tigase.xmpp.Authorization;
import tigase.xmpp.jid.JID;

import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

@Bean(name = "x-delete-task", parent = MonitorComponent.class, active = true)
public class DeleteScriptTaskCommand
		implements AdHocCommand {

	private final static Logger log = Logger.getLogger(DeleteScriptTaskCommand.class.getName());
	@Inject
	private MonitorComponent component;
	@Inject
	private Kernel kernel;

	public DeleteScriptTaskCommand() {
	}

	@Override
	public void execute(AdhHocRequest request, AdHocResponse response) throws AdHocCommandException {
		try {
			final Element data = request.getCommand().getChild("x", "jabber:x:data");

			if (request.getAction() != null && "cancel".equals(request.getAction())) {
				response.cancelSession();
			} else if (data == null) {
				Form form = new Form("form", "Delete monitor task", null);

				Collection<String> taskNames = kernel.getNamesOf(MonitorTask.class);

				form.addField(
						Field.fieldListSingle("delete_task", "", "Task to delete", taskNames.toArray(new String[]{}),
											  taskNames.toArray(new String[]{})));

				response.getElements().add(form.getElement());
				response.startSession();
			} else {
				Form form = new Form(data);

				if ("submit".equals(form.getType())) {
					String taskName = form.getAsString("delete_task");

					Object i = kernel.getInstance(taskName);
					if (i instanceof MonitorTask) {
						((TasksScriptRegistrar) kernel.getInstance(TasksScriptRegistrar.ID)).delete(taskName);
					} else {
						throw new RuntimeException("Are you kidding me?");
					}
				}

				form = new Form("form", "Completed", null);
				form.addField(Field.fieldFixed("Script removed"));
				response.getElements().add(form.getElement());

				response.completeSession();
			}

		} catch (Exception e) {
			log.log(Level.FINEST, "Error executing script", e);
			throw new AdHocCommandException(Authorization.INTERNAL_SERVER_ERROR, e.getMessage());
		}
	}

	@Override
	public String getName() {
		return "Delete monitor task";
	}

	@Override
	public String getNode() {
		return "x-delete-task";
	}

	@Override
	public boolean isAllowedFor(JID jid) {
		return component.isAdmin(jid);
	}

}
