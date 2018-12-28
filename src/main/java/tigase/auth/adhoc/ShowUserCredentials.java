package tigase.auth.adhoc;

import tigase.component.adhoc.AdHocCommandException;
import tigase.component.adhoc.AdHocResponse;
import tigase.component.adhoc.AdhHocRequest;
import tigase.db.TigaseDBException;
import tigase.form.Field;
import tigase.form.Fields;
import tigase.form.Form;
import tigase.form.MultiItemForm;
import tigase.kernel.beans.Bean;
import tigase.server.xmppsession.SessionManager;
import tigase.xmpp.jid.BareJID;

import java.util.Collection;

@Bean(name = ShowUserCredentials.NODE, parent = SessionManager.class, active = true)
public class ShowUserCredentials
		extends AbstractCredentialsCommand {

	public static final String NODE = "auth-credentials-list";

	public ShowUserCredentials() {
	}

	@Override
	public String getName() {
		return "List user credentials";
	}

	@Override
	public String getNode() {
		return NODE;
	}

	@Override
	protected void processForm(Form form, AdhHocRequest request, AdHocResponse response)
			throws TigaseDBException, AdHocCommandException {
		BareJID jid = BareJID.bareJIDInstanceNS(form.getAsString(FIELD_JID));

		checkIfCanModifyJID(request, jid);

		MultiItemForm resp = new MultiItemForm();
		resp.setType("form");

		Collection<String> usernames = authRepository.getUsernames(jid);
		for (String username : usernames) {
			Fields ff = new Fields();
			ff.addField(Field.fieldTextSingle("username", username, "Username"));
			resp.addItem(ff);
		}

		response.getElements().add(resp.getElement());
		response.completeSession();
	}

	@Override
	protected void processNoForm(AdhHocRequest request, AdHocResponse response) {
		Form form = new Form("form", getName(), null);

		form.addField(Field.fieldJidSingle(FIELD_JID, null, "The Jabber ID for the account"));

		response.startSession();
		response.getElements().add(form.getElement());
	}
}
