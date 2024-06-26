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
package tigase.component.adhoc;

import tigase.annotations.TigaseDeprecated;
import tigase.xmpp.jid.JID;

import java.util.Optional;
import java.util.function.Consumer;

public interface AdHocCommand {
	
	void execute(final AdhHocRequest request, AdHocResponse response) throws AdHocCommandException;

	default void execute(final AdhHocRequest request, AdHocResponse response, Runnable completionHandler, Consumer<AdHocCommandException> exceptionHandler) throws AdHocCommandException {
		execute(request, response);
		completionHandler.run();
	}

	String getName();

	String getNode();

	default Optional<String> getGroup() {
		return Optional.empty();
	}

	@TigaseDeprecated(since = "8.5.0", removeIn = "9.0.0")
	@Deprecated
	boolean isAllowedFor(JID jid);

	default boolean isAllowedFor(JID from, JID to) {
		return isAllowedFor(from);
	}

	default boolean isForSelf() {
		return false;
	}
}
