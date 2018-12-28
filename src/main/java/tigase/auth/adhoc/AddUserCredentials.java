package tigase.auth.adhoc;

import tigase.auth.credentials.Credentials;
import tigase.component.adhoc.AdHocCommandException;
import tigase.component.adhoc.AdHocResponse;
import tigase.component.adhoc.AdhHocRequest;
import tigase.db.TigaseDBException;
import tigase.form.Field;
import tigase.form.Form;
import tigase.kernel.beans.Bean;
import tigase.server.xmppsession.SessionManager;
import tigase.xmpp.Authorization;
import tigase.xmpp.jid.BareJID;

@Bean(name = AddUserCredentials.NODE, parent = SessionManager.class, active = true)
public class AddUserCredentials
		extends AbstractCredentialsCommand {

	public static final String NODE = "auth-credentials-add";

	public AddUserCredentials() {
	}

	@Override
	public String getName() {
		return "Add user credentials";
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

		String username = form.getAsString(FIELD_USERNAME);
		String password = form.getAsString(FIELD_PASSWORD);

		if (username == null || username.trim().isEmpty()) {
			username = Credentials.DEFAULT_USERNAME;
		}

		if (password == null || username.isEmpty()) {
			throw new AdHocCommandException(Authorization.BAD_REQUEST, "Passwor cannot be null");
		}

		Form resp = new Form("result", null, null);

		authRepository.updateCredential(jid, username.trim(), password);

		resp.addField(Field.fieldFixed("OK"));

		response.getElements().add(resp.getElement());
		response.completeSession();
	}

	@Override
	protected void processNoForm(AdhHocRequest request, AdHocResponse response) {
		Form form = new Form("form", getName(), null);

		form.addField(Field.fieldJidSingle(FIELD_JID, null, "The Jabber ID for the account"));
		form.addField(Field.fieldJidSingle(FIELD_USERNAME, null, "Username"));
		form.addField(Field.fieldJidSingle(FIELD_PASSWORD, null, "Password"));

		response.startSession();
		response.getElements().add(form.getElement());
	}
}
