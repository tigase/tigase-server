package tigase.auth;

public class SaslInvalidLoginExcepion
		extends XmppSaslException {

	private final String jid;

	public SaslInvalidLoginExcepion(SaslError saslError, String jid) {
		super(saslError);
		this.jid = jid;
	}

	public SaslInvalidLoginExcepion(SaslError saslError, String jid, String detail) {
		super(saslError, detail);
		this.jid = jid;
	}

	public String getJid() {
		return jid;
	}
}
