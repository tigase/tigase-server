/*
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
import tigase.auth.impl.XTokenCallbackHandler;
import tigase.auth.mechanisms.*;
import tigase.db.NonAuthUserRepository;
import tigase.kernel.beans.Bean;
import tigase.xmpp.XMPPResourceConnection;

import javax.security.auth.callback.CallbackHandler;

/**
 * Factory of {@linkplain CallbackHandler CallbackHandlers}.
 */
@Bean(name = "callback-handler-factory", parent = TigaseSaslProvider.class, active = true)
public class CallbackHandlerFactory
		implements CallbackHandlerFactoryIfc {

	public static final String AUTH_JID = "authentication-jid";

	private static final String CALLBACK_HANDLER_KEY = "callbackhandler";

	@Override
	public CallbackHandler create(String mechanismName, XMPPResourceConnection session, NonAuthUserRepository repo)
			throws ClassNotFoundException, InstantiationException, IllegalAccessException {
		String handlerClassName = getHandlerClassname(mechanismName, session, repo);
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

		if (handler instanceof MechanismNameAware) {
			((MechanismNameAware) handler).setMechanismName(mechanismName);
		}

		return handler;
	}

	protected String getHandlerClassname(String mechanismName, XMPPResourceConnection session,
										 NonAuthUserRepository repo) {
		switch (mechanismName) {
			case SaslSCRAM.NAME:
			case SaslSCRAMPlus.NAME:
			case SaslSCRAMSha256.NAME:
			case SaslSCRAMSha256Plus.NAME:
			case SaslSCRAMSha512.NAME:
			case SaslSCRAMSha512Plus.NAME:
				return ScramCallbackHandler.class.getName();
			case SaslXTOKEN.NAME:
				return XTokenCallbackHandler.class.getName();
			default:
				return null;
		}
	}
}
