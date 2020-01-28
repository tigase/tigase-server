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
package tigase.mix.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public enum ChannelNodePermission {
	nobody,
	owners,
	admins,
	allowed,
	participants,
	anyone;

	public static List<ChannelNodePermission> MESSAGE_NODE_SUBSCRIPTIONS = Collections.unmodifiableList(
			Arrays.asList(ChannelNodePermission.participants, ChannelNodePermission.allowed,
						  ChannelNodePermission.anyone));
	public static List<ChannelNodePermission> PRESENCE_NODE_SUBSCRIPTIONS = Collections.unmodifiableList(
			Arrays.asList(ChannelNodePermission.participants, ChannelNodePermission.allowed,
						  ChannelNodePermission.anyone));
	public static List<ChannelNodePermission> PARTICIPANTS_NODE_SUBSCRIPTIONS = Collections.unmodifiableList(
			Arrays.asList(ChannelNodePermission.participants, ChannelNodePermission.allowed,
						  ChannelNodePermission.anyone, ChannelNodePermission.nobody, ChannelNodePermission.admins,
						  ChannelNodePermission.owners));
	public static List<ChannelNodePermission> INFORMATION_NODE_SUBSCRIPTIONS = Collections.unmodifiableList(
			Arrays.asList(ChannelNodePermission.participants, ChannelNodePermission.allowed,
						  ChannelNodePermission.anyone));
	public static List<ChannelNodePermission> ALLOWED_NODE_SUBSCRIPTIONS = Collections.unmodifiableList(
			Arrays.asList(ChannelNodePermission.participants, ChannelNodePermission.allowed,
						  ChannelNodePermission.nobody, ChannelNodePermission.admins, ChannelNodePermission.owners));
	public static List<ChannelNodePermission> BANNED_NODE_SUBSCRIPTIONS = Collections.unmodifiableList(
			Arrays.asList(ChannelNodePermission.participants, ChannelNodePermission.allowed,
						  ChannelNodePermission.nobody, ChannelNodePermission.admins, ChannelNodePermission.owners));
	public static List<ChannelNodePermission> CONFIGURATION_NODE_ACCESS = Collections.unmodifiableList(
			Arrays.asList(ChannelNodePermission.participants, ChannelNodePermission.allowed,
						  ChannelNodePermission.nobody, ChannelNodePermission.admins, ChannelNodePermission.owners));
	public static List<ChannelNodePermission> INFORMATION_NODE_UPDATE_RIGHTS = Collections.unmodifiableList(
			Arrays.asList(ChannelNodePermission.participants, ChannelNodePermission.admins, ChannelNodePermission.owners));
	public static List<ChannelNodePermission> AVATAR_NODES_UPDATE_RIGHTS = Collections.unmodifiableList(
			Arrays.asList(ChannelNodePermission.participants, ChannelNodePermission.admins, ChannelNodePermission.owners));

	public static List<ChannelNodePermission> ADMINISTRATOR_MESSAGE_RETRACTION_RIGHTS = Collections.unmodifiableList(
			Arrays.asList(ChannelNodePermission.nobody, ChannelNodePermission.admins, ChannelNodePermission.owners));


}