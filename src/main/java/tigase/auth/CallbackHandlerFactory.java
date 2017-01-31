package tigase.auth;

import java.util.Map;

import javax.security.auth.callback.CallbackHandler;

import tigase.auth.impl.AuthRepoPlainCallbackHandler;
import tigase.auth.impl.ScramCallbackHandler;
import tigase.db.NonAuthUserRepository;
import tigase.xmpp.XMPPResourceConnection;

/**
 * Factory of {@linkplain CallbackHandler CallbackHandlers}.
 * 
 */
public class CallbackHandlerFactory {

	private static final String CALLBACK_HANDLER_KEY = "callbackhandler";

	public CallbackHandler create(String mechanismName, XMPPResourceConnection session, NonAuthUserRepository repo,
			Map<String, Object> settings) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
		String handlerClassName = getHandlerClassname(mechanismName, session, repo, settings);
		if (handlerClassName == null)
			handlerClassName = AuthRepoPlainCallbackHandler.class.getName();
		@SuppressWarnings("unchecked")
		Class<CallbackHandler> handlerClass = (Class<CallbackHandler>) Class.forName(handlerClassName);

		CallbackHandler handler = handlerClass.newInstance();

		if (handler instanceof SessionAware) {
			((SessionAware) handler).setSession(session);
		}

		if (handler instanceof DomainAware) {
			((DomainAware) handler).setDomain(session.getDomain().getVhost().getDomain());
		}

		if (handler instanceof NonAuthUserRepositoryAware) {
			((NonAuthUserRepositoryAware) handler).setNonAuthUserRepository(repo);
		}

		if (handler instanceof AuthRepositoryAware) {
			((AuthRepositoryAware) handler).setAuthRepository(session.getAuthRepository());
		}

        if (handler instanceof PluginSettingsAware) {
            ((PluginSettingsAware) handler).setPluginSettings(settings);
        }

		return handler;
	}

	private String getHandlerClassname(String mechanismName, XMPPResourceConnection session, NonAuthUserRepository repo,
									   Map<String, Object> settings) {
		if (settings != null && settings.containsKey(CALLBACK_HANDLER_KEY + "-" + mechanismName)) {
			return (String) settings.get(CALLBACK_HANDLER_KEY + "-" + mechanismName);
		} else if (settings != null && settings.containsKey(CALLBACK_HANDLER_KEY)) {
			return (String) settings.get(CALLBACK_HANDLER_KEY);
		} else if (mechanismName.equals("SCRAM-SHA-1")) {
			return ScramCallbackHandler.class.getName();
		} else if (mechanismName.equals("SCRAM-SHA-1-PLUS")) {
			return ScramCallbackHandler.class.getName();
		} else {
			return null;
		}
	}

}
