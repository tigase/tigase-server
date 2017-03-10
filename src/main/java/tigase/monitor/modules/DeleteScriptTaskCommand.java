package tigase.monitor.modules;

import tigase.component.adhoc.AdHocCommand;
import tigase.component.adhoc.AdHocCommandException;
import tigase.component.adhoc.AdHocResponse;
import tigase.component.adhoc.AdhHocRequest;
import tigase.form.Field;
import tigase.form.Form;
import tigase.monitor.MonitorComponent;
import tigase.monitor.MonitorContext;
import tigase.monitor.MonitorTask;
import tigase.monitor.TasksScriptRegistrar;
import tigase.xml.Element;
import tigase.xmpp.Authorization;
import tigase.xmpp.JID;

import java.util.Collection;

public class DeleteScriptTaskCommand
		implements AdHocCommand {

	private MonitorContext monitorContext;

	public DeleteScriptTaskCommand(MonitorContext monitorContext) {
		this.monitorContext = monitorContext;
	}

	@Override
	public void execute(AdhHocRequest request, AdHocResponse response) throws AdHocCommandException {
		try {
			final Element data = request.getCommand().getChild("x", "jabber:x:data");

			if (request.getAction() != null && "cancel".equals(request.getAction())) {
				response.cancelSession();
			} else if (data == null) {
				Form form = new Form("form", "Delete monitor task", null);

				Collection<String> taskNames = monitorContext.getKernel().getNamesOf(MonitorTask.class);

				form.addField(
						Field.fieldListSingle("delete_task", "", "Task to delete", taskNames.toArray(new String[]{}),
											  taskNames.toArray(new String[]{})));

				response.getElements().add(form.getElement());
				response.startSession();
			} else {
				Form form = new Form(data);

				if ("submit".equals(form.getType())) {
					String taskName = form.getAsString("delete_task");

					Object i = monitorContext.getKernel().getInstance(taskName);
					if (i instanceof MonitorTask) {
						((TasksScriptRegistrar) monitorContext.getKernel().getInstance(TasksScriptRegistrar.ID)).delete(
								taskName);
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
			e.printStackTrace();
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
		return monitorContext.getKernel().getInstance(MonitorComponent.class).isAdmin(jid);
	}

}
