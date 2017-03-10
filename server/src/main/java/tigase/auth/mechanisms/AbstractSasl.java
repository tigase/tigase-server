package tigase.auth.mechanisms;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.sasl.SaslException;
import javax.security.sasl.SaslServer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public abstract class AbstractSasl implements SaslServer {

	public static final String SASL_STRICT_MODE_KEY = "sasl-strict";
	protected final Logger log = Logger.getLogger(this.getClass().getName());
	protected final CallbackHandler callbackHandler;
	protected final Map<String, Object> negotiatedProperty = new HashMap<String, Object>();
	protected final Map<? super String, ?> props;
	protected String authorizedId = null;
	protected boolean complete = false;

	protected AbstractSasl(Map<? super String, ?> props, CallbackHandler callbackHandler) {
		this.props = props;
		this.callbackHandler = callbackHandler;
	}

	public static boolean isAuthzIDIgnored() {
		String x = System.getProperty(SASL_STRICT_MODE_KEY, "false");
		return !Boolean.parseBoolean(x);
	}

	protected static final boolean isEmpty(Object x) {
		return x == null || x.toString().length() == 0;
	}

	@Override
	public void dispose() throws SaslException {
		this.authorizedId = null;
	}

	@Override
	public Object getNegotiatedProperty(String propName) {
		if (!isComplete())
			throw new IllegalStateException("Server negotiation not complete");
		return negotiatedProperty.get(propName);
	}

	protected void handleCallbacks(Callback... callbacks) throws SaslException {
		try {
			callbackHandler.handle(callbacks);
		} catch (IOException e) {
			throw new SaslException(e.getMessage(), e);
		} catch (UnsupportedCallbackException e) {
			throw new SaslException("Callback not supported by handler", e);
		}
	}

	@Override
	public boolean isComplete() {
		return complete;
	}

	protected String[] split(final byte[] byteArray, final String defaultValue) {
		if (byteArray == null)
			return new String[]{};
		ArrayList<String> result = new ArrayList<String>();

		int pi = 0;
		for (int i = 0; i < byteArray.length; i++) {
			if (byteArray[i] == 0) {
				String item;
				if (pi == i) {
					item = defaultValue;
					pi = i + 1;
				} else {
					item = new String(byteArray, pi, i - pi);
					pi = i + 1;
				}
				result.add(item);
			}

		}
		if (pi < byteArray.length) {
			String item = new String(byteArray, pi, byteArray.length - pi);
			result.add(item);
		} else
			result.add(defaultValue);

		return result.toArray(new String[]{});
	}
}
