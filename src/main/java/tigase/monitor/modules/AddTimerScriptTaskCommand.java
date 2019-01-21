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
import tigase.monitor.TasksScriptRegistrar;
import tigase.xml.Element;
import tigase.xmpp.Authorization;
import tigase.xmpp.jid.JID;

import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Bean(name = "x-add-timer-task", parent = MonitorComponent.class, active = true)
public class AddTimerScriptTaskCommand
		implements AdHocCommand {

	private final static Logger log = Logger.getLogger(AddTimerScriptTaskCommand.class.getName());
	@Inject
	private MonitorComponent component;
	@Inject
	private Kernel kernel;

	public AddTimerScriptTaskCommand() {
	}

	@Override
	public void execute(AdhHocRequest request, AdHocResponse response) throws AdHocCommandException {
		try {
			final Element data = request.getCommand().getChild("x", "jabber:x:data");

			if (request.getAction() != null && "cancel".equals(request.getAction())) {
				response.cancelSession();
			} else if (data == null) {
				Form form = new Form("form", "Add monitor script", null);

				List<ScriptEngineFactory> sef = kernel.getInstance(ScriptEngineManager.class).getEngineFactories();
				ArrayList<String> labels = new ArrayList<String>();
				ArrayList<String> values = new ArrayList<String>();
				for (ScriptEngineFactory scriptEngineFactory : sef) {
					labels.add(scriptEngineFactory.getLanguageName());
					values.add(scriptEngineFactory.getExtensions().get(0));
				}

				form.addField(Field.fieldTextSingle("scriptName", "", "Script name"));
				form.addField(Field.fieldTextSingle("delay", "1000", "Delay"));
				form.addField(
						Field.fieldListSingle("scriptExtension", "", "Script engine", labels.toArray(new String[]{}),
											  values.toArray(new String[]{})));
				form.addField(Field.fieldTextMulti("scriptContent", "", "Script"));

				response.getElements().add(form.getElement());
				response.startSession();
			} else {
				Form form = new Form(data);

				if ("submit".equals(form.getType())) {
					String scriptName = form.getAsString("scriptName");
					String scriptExtension = form.getAsString("scriptExtension");
					String scriptContent = form.getAsString("scriptContent");
					Long delay = form.getAsLong("delay");

					((TasksScriptRegistrar) kernel.getInstance(TasksScriptRegistrar.ID)).registerTimerScript(scriptName,
																											 scriptExtension,
																											 scriptContent,
																											 delay);
				}

				form = new Form("form", "Completed", null);
				form.addField(Field.fieldFixed("Script added."));
				response.getElements().add(form.getElement());

				response.completeSession();
			}

		} catch (Exception e) {
			log.log(Level.FINEST, "Error adding script", e);
			throw new AdHocCommandException(Authorization.INTERNAL_SERVER_ERROR, e.getMessage());
		}
	}

	@Override
	public String getName() {
		return "Add monitor timer task";
	}

	@Override
	public String getNode() {
		return "x-add-timer-task";
	}

	@Override
	public boolean isAllowedFor(JID jid) {
		return component.isAdmin(jid);
	}

}
