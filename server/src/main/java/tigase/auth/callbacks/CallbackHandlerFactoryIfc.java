/*
 * CallbackHandlerFactoryIfc.java
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2017 "Tigase, Inc." <office@tigase.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 *
 */
package tigase.auth.callbacks;

import tigase.db.NonAuthUserRepository;
import tigase.xmpp.XMPPResourceConnection;

import javax.security.auth.callback.CallbackHandler;
import java.util.Map;

/**
 * Created by andrzej on 20.03.2017.
 */
public interface CallbackHandlerFactoryIfc {

	CallbackHandler create(String mechanismName, XMPPResourceConnection session, NonAuthUserRepository repo,
						   Map<String, Object> settings) throws ClassNotFoundException, InstantiationException, IllegalAccessException;

}
