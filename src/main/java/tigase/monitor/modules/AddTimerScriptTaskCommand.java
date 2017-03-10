package tigase.monitor.modules;

import tigase.component.adhoc.AdHocCommand;
import tigase.component.adhoc.AdHocCommandException;
import tigase.component.adhoc.AdHocResponse;
import tigase.component.adhoc.AdhHocRequest;
import tigase.form.Field;
import tigase.form.Form;
import tigase.monitor.MonitorComponent;
import tigase.monitor.MonitorContext;
import tigase.monitor.TasksScriptRegistrar;
import tigase.xml.Element;
import tigase.xmpp.Authorization;
import tigase.xmpp.JID;

import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import java.util.ArrayList;
import java.util.List;

public class AddTimerScriptTaskCommand
		implements AdHocCommand {

	private MonitorContext monitorContext;

	public AddTimerScriptTaskCommand(MonitorContext monitorContext) {
		this.monitorContext = monitorContext;
	}

	@Override
	public void execute(AdhHocRequest request, AdHocResponse response) throws AdHocCommandException {
		try {
			final Element data = request.getCommand().getChild("x", "jabber:x:data");

			if (request.getAction() != null && "cancel".equals(request.getAction())) {
				response.cancelSession();
			} else if (data == null) {
				Form form = new Form("form", "Add monitor script", null);

				List<ScriptEngineFactory> sef = monitorContext.getKernel()
						.getInstance(ScriptEngineManager.class)
						.getEngineFactories();
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

					((TasksScriptRegistrar) monitorContext.getKernel()
							.getInstance(TasksScriptRegistrar.ID)).registerTimerScript(scriptName, scriptExtension,
																					   scriptContent, delay);
				}

				form = new Form("form", "Completed", null);
				form.addField(Field.fieldFixed("Script added."));
				response.getElements().add(form.getElement());

				response.completeSession();
			}

		} catch (Exception e) {
			e.printStackTrace();
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
		return monitorContext.getKernel().getInstance(MonitorComponent.class).isAdmin(jid);
	}

}
