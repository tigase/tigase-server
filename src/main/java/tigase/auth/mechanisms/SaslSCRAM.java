package tigase.auth.mechanisms;

import java.util.Map;

import javax.security.auth.callback.CallbackHandler;

public class SaslSCRAM extends AbstractSaslSCRAM {

	public SaslSCRAM(Map<? super String, ?> props, CallbackHandler callbackHandler) {
		super("SCRAM-SHA-1", "SHA1", "Client Key".getBytes(), "Server Key".getBytes(), props, callbackHandler);
	}
}
