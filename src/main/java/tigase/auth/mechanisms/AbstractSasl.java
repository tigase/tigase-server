package tigase.auth.mechanisms;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.sasl.SaslException;
import javax.security.sasl.SaslServer;

public abstract class AbstractSasl implements SaslServer {

	protected static final boolean isEmpty(Object x) {
		return x == null || x.toString().length() == 0;
	}

	protected String authorizedId = null;

	protected final CallbackHandler callbackHandler;

	protected boolean complete = false;

	protected final Map<String, Object> negotiatedProperty = new HashMap<String, Object>();

	protected final Map<? super String, ?> props;

	protected AbstractSasl(Map<? super String, ?> props, CallbackHandler callbackHandler) {
		this.props = props;
		this.callbackHandler = callbackHandler;
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
			return new String[] {};
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

		return result.toArray(new String[] {});
	}
}
