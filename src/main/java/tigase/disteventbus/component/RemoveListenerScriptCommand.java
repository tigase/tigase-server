package tigase.disteventbus.component;

import java.util.Collection;
import java.util.Map;

import tigase.component.adhoc.AdHocCommand;
import tigase.component.adhoc.AdHocCommandException;
import tigase.component.adhoc.AdHocResponse;
import tigase.component.adhoc.AdhHocRequest;
import tigase.form.Field;
import tigase.form.Form;
import tigase.xml.Element;
import tigase.xmpp.Authorization;
import tigase.xmpp.JID;

public class RemoveListenerScriptCommand implements AdHocCommand {

	private ListenerScriptRegistrar listenerScriptRegistrar;
	private Map<String, ListenerScript> listenersScripts;

	public RemoveListenerScriptCommand(Map<String, ListenerScript> listenersScripts,
			ListenerScriptRegistrar listenerScriptRegistrar) {
		this.listenersScripts = listenersScripts;
		this.listenerScriptRegistrar = listenerScriptRegistrar;
	}

	@Override
	public void execute(AdhHocRequest request, AdHocResponse response) throws AdHocCommandException {
		try {
			final Element data = request.getCommand().getChild("x", "jabber:x:data");

			if (request.getAction() != null && "cancel".equals(request.getAction())) {
				response.cancelSession();
			} else if (data == null) {
				Form form = new Form("form", "Delete listener script", null);

				Collection<String> scriptNames = listenersScripts.keySet();

				form.addField(Field.fieldListSingle("delete_script", "", "Script to delete",
						scriptNames.toArray(new String[] {}), scriptNames.toArray(new String[] {})));

				response.getElements().add(form.getElement());
				response.startSession();
			} else {
				Form form = new Form(data);

				if ("submit".equals(form.getType())) {
					String scriptName = form.getAsString("delete_script");

					listenerScriptRegistrar.delete(scriptName);
					ListenerScript i = listenersScripts.remove(scriptName);
					if (i != null) {
						i.unregister();
					} else
						throw new RuntimeException("Are you kidding me?");
				}

				response.completeSession();
			}

		} catch (Exception e) {
			e.printStackTrace();
			throw new AdHocCommandException(Authorization.INTERNAL_SERVER_ERROR, e.getMessage());
		}
	}

	@Override
	public String getName() {
		return "Remove listener script";
	}

	@Override
	public String getNode() {
		return "remove-listener-script";
	}

	@Override
	public boolean isAllowedFor(JID jid) {
		return true;
	}

}
