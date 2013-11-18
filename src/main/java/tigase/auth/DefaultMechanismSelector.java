package tigase.auth;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.security.sasl.SaslServerFactory;

import tigase.auth.mechanisms.SaslEXTERNAL;
import tigase.auth.mechanisms.TigaseSaslServerFactory;
import tigase.server.XMPPServer;
import tigase.xmpp.XMPPResourceConnection;

public class DefaultMechanismSelector implements MechanismSelector {

	protected Map<String, Object> settings;

	@Override
	public Collection<String> filterMechanisms(Enumeration<SaslServerFactory> serverFactories, XMPPResourceConnection session) {
		final Map<String, ?> props = new HashMap<String, Object>();
		final ArrayList<String> result = new ArrayList<String>();
		while (serverFactories.hasMoreElements()) {
			SaslServerFactory ss = serverFactories.nextElement();
			String[] x = ss.getMechanismNames(props);
			for (String name : x) {
				if (match(ss, name, session))
					result.add(name);
			}
		}
		return result;
	}

	@Override
	public void init(Map<String, Object> settings) {
		this.settings = settings;
	}

	protected boolean match(SaslServerFactory factory, String mechanismName, XMPPResourceConnection session) {
		if (session.getDomain().isTlsRequired() && !session.isEncrypted())
			return false;
		if (factory instanceof TigaseSaslServerFactory) {
			if (!session.getDomain().isAnonymousEnabled() && "ANONYMOUS".equals(mechanismName))
				return false;
			if ("EXTERNAL".equals(mechanismName) && session.getSessionData(SaslEXTERNAL.SASL_EXTERNAL_ALLOWED) != Boolean.TRUE)
				return false;
			return true;
		}
		return false;
	}
}
