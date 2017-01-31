/*
 * StreamError.java
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2016 "Tigase, Inc." <office@tigase.com>
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
package tigase.xmpp;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author andrzej
 */
public enum StreamError {
	
	BadFormat("bad-format"),
	BadNamespacePrefix("bad-namespace-prefix"),
	Conflict("conflict"),
	ConnectionTimeout("connection-timeout"),
	HostGone("host-gone"),
	HostUnknown("host-unknown"),
	ImproperAddressing("improper-addressing"),
	InternalServerError("internal-server-error"),
	InvalidFrom("invalid-from"),
	InvalidNamespace("invalid-namespace"),
	InvalidXml("invalid-xml"),
	NotAuthorized("not-authorized"),
	NotWellFormed("not-well-formed"),
	PolicyViolation("policy-violation"),
	RemoteConnectionFailed("remote-connection-failed"),
	Reset("reset"),
	ResourceConstraint("resource-constraint"),
	RestrictedXml("restricted-xml"),
	SeeOtherHost("see-other-host"),
	SystemShutdown("system-shutdown"),
	UndefinedCondition("undefined-condition"),
	UnsupportedEncoding("unsupported-encoding"),
	UnsupportedFeature("unsupported-feature"),
	UnsupportedStanzaType("unsupported-stanza-type"),
	UnsupportedVersion("unsupported-version");
	
	private static final Map<String,StreamError> BY_CONDITION = new HashMap<>();
	
	static {
		for (StreamError err : StreamError.values()) {
			BY_CONDITION.put(err.getCondition(), err);
		}
	}
	
	public static StreamError getByCondition(String condition) {
		StreamError err = BY_CONDITION.get(condition);
		if (err == null)
			return UndefinedCondition;
		return err;
	}

	private final String condition;
	
	private StreamError(String condition) {
		this.condition = condition;
	}

	public String getCondition() {
		return condition;
	}
}
