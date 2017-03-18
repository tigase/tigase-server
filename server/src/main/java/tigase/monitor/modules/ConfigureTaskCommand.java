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
import tigase.monitor.ConfigurableTask;
import tigase.monitor.MonitorComponent;
import tigase.monitor.TasksScriptRegistrar;
import tigase.xml.Element;
import tigase.xmpp.Authorization;
import tigase.xmpp.JID;

import static tigase.monitor.modules.ConfigureTaskCommand.NODE;

@Bean(name = NODE, parent = MonitorComponent.class, active = true)
public class ConfigureTaskCommand
		implements AdHocCommand {
	
	public static final String NODE = "x-config";

	@Inject
	private MonitorComponent component;
	@Inject
	private Kernel kernel;
	@Inject
	private TasksScriptRegistrar registrar;

	public ConfigureTaskCommand() {
	}

	@Override
	public void execute(AdhHocRequest request, AdHocResponse response) throws AdHocCommandException {
		try {
			final Element data = request.getCommand().getChild("x", "jabber:x:data");

			if (request.getAction() != null && "cancel".equals(request.getAction())) {
				response.cancelSession();
			} else if (data == null) {
				final ConfigurableTask taskInstance = kernel.getInstance(request.getIq().getStanzaTo().getResource());
				Form form = taskInstance.getCurrentConfiguration();
				response.getElements().add(form.getElement());
				response.startSession();
			} else {
				Form form = new Form(data);
				if ("submit".equals(form.getType())) {
					final ConfigurableTask taskInstance = kernel.getInstance(
							request.getIq().getStanzaTo().getResource());

					registrar.updateConfig(request.getIq().getStanzaTo().getResource(), form);

					form = new Form("form", "Completed", null);
					form.addField(Field.fieldFixed("Script configured"));
					response.getElements().add(form.getElement());

					response.completeSession();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new AdHocCommandException(Authorization.INTERNAL_SERVER_ERROR, e.getMessage());
		}
	}

	@Override
	public String getName() {
		return "Task config";
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
