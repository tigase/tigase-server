/*
 * Tigase MIX - MIX component for Tigase
 * Copyright (C) 2020 Tigase, Inc. (office@tigase.com)
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
package tigase.mix;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Mix {

	public static final String CORE1_XMLNS = "urn:xmpp:mix:core:1";
	public static final String ADMIN0_XMLNS = "urn:xmpp:mix:admin:0";

	public static class Nodes {
		public static final String ALLOWED = "urn:xmpp:mix:nodes:allowed";
		public static final String BANNED = "urn:xmpp:mix:nodes:banned";
		public static final String CONFIG = "urn:xmpp:mix:nodes:config";
		public static final String PARTICIPANTS = "urn:xmpp:mix:nodes:participants";
		public static final String INFO = "urn:xmpp:mix:nodes:info";
		public static final String MESSAGES = "urn:xmpp:mix:nodes:messages";

		public static final Set<String> ALL_NODES = Collections.unmodifiableSet(
				Stream.of(Mix.Nodes.CONFIG, Mix.Nodes.INFO, Mix.Nodes.MESSAGES, Mix.Nodes.PARTICIPANTS).collect(
						Collectors.toSet()));
	}

}
