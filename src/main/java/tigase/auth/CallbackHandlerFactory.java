/**
 * Tigase XMPP Server - The instant messaging server
 * Copyright (C) 2004 Tigase, Inc. (office@tigase.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 */
package tigase.auth;

import tigase.auth.callbacks.CallbackHandlerFactoryIfc;
import tigase.auth.impl.PlainCallbackHandler;
import tigase.auth.impl.ScramCallbackHandler;
import tigase.auth.mechanisms.SaslSCRAM;
import tigase.auth.mechanisms.SaslSCRAMPlus;
import tigase.auth.mechanisms.SaslSCRAMSha256;
import tigase.auth.mechanisms.SaslSCRAMSha256Plus;
import tigase.db.NonAuthUserRepository;
import tigase.kernel.beans.Bean;
import tigase.xmpp.XMPPResourceConnection;

import javax.security.auth.callback.CallbackHandler;
import java.util.Map;

/**
 * Factory of {@linkplain CallbackHandler CallbackHandlers}.
 */
@Bean(name = "callback-handler-factory", parent = TigaseSaslProvider.class, active = true)
public class CallbackHandlerFactory
		implements CallbackHandlerFactoryIfc {

	public static final String AUTH_JID = "authentication-jid";

	private static final String CALLBACK_HANDLER_KEY = "callbackhandler";

	@Override
	public CallbackHandler create(String mechanismName, XMPPResourceConnection session, NonAuthUserRepository repo,
								  Map<String, Object> settings)
			throws ClassNotFoundException, InstantiationException, IllegalAccessException {
		String handlerClassName = getHandlerClassname(mechanismName, session, repo, settings);
		if (handlerClassName == null) {
			handlerClassName = PlainCallbackHandler.class.getName();
		}
		@SuppressWarnings("unchecked") Class<CallbackHandler> handlerClass = (Class<CallbackHandler>) Class.forName(
				handlerClassName);

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

		if (handler instanceof MechanismNameAware) {
			((MechanismNameAware) handler).setMechanismName(mechanismName);
		}

		return handler;
	}

	private String getHandlerClassname(String mechanismName, XMPPResourceConnection session, NonAuthUserRepository repo,
									   Map<String, Object> settings) {
		if (settings != null && settings.containsKey(CALLBACK_HANDLER_KEY + "-" + mechanismName)) {
			return (String) settings.get(CALLBACK_HANDLER_KEY + "-" + mechanismName);
		} else if (settings != null && settings.containsKey(CALLBACK_HANDLER_KEY)) {
			return (String) settings.get(CALLBACK_HANDLER_KEY);
		} else {
			switch (mechanismName) {
				case SaslSCRAM.NAME:
				case SaslSCRAMPlus.NAME:
				case SaslSCRAMSha256.NAME:
				case SaslSCRAMSha256Plus.NAME:
					return ScramCallbackHandler.class.getName();
				default:
					return null;
			}
		}
	}

}
