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
import tigase.xmpp.JID;

import static tigase.monitor.modules.InfoTaskCommand.NODE;

@Bean(name = NODE, parent = MonitorComponent.class, active = true)
public class InfoTaskCommand
		implements AdHocCommand {

	public static final String NODE = "x-info";

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
			e.printStackTrace();
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
		return kernel.getInstance(MonitorComponent.class).isAdmin(jid);
	}

}
