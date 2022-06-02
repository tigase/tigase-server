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

import tigase.mix.Mix;
import tigase.pubsub.exceptions.PubSubException;
import tigase.server.Command;
import tigase.server.DataForm;
import tigase.util.datetime.TimestampHelper;
import tigase.util.stringprep.TigaseStringprepException;
import tigase.xml.Element;
import tigase.xmpp.Authorization;
import tigase.xmpp.jid.BareJID;
import tigase.xmpp.jid.JID;

import java.text.ParseException;
import java.util.*;

public class ChannelConfiguration {

	public static void updateLastChangeMadeBy(Element item, JID publisherJID) {
		Element form = item.getChild("x", "jabber:x:data");
		Element lastChangeMadeByField = form.findChild(el -> el.getName() == "field" && "Last Change Made By".equals(el.getAttributeStaticStr("var")));
		if (lastChangeMadeByField != null) {
			List<Element> oldValues = lastChangeMadeByField.findChildren(el -> el.getName() == "value");
			if (oldValues != null) {
				oldValues.forEach(lastChangeMadeByField::removeChild);
			}
			lastChangeMadeByField.addChild(new Element("value", publisherJID.getBareJID().toString()));
		} else {
			DataForm.addFieldValue(form, "Last Change Made By", publisherJID.getBareJID().toString(), "jid-single");
		}
	}

	private static final TimestampHelper timestampHelper = new TimestampHelper();

	private static final String[] NODES_PRESENT_OPTIONS = new String[] {
			"participants", "presence", "information", "allowed", "banned", "jidmap-visible", "avatar"
	};

	private BareJID lastChangeMadeBy;
	private Set<BareJID> owners = new HashSet<>();
	private Set<BareJID> administrators = new HashSet<>();
	private Date endOfLife;
	private String[] nodesPresent = {};

	private ChannelNodePermission messagesNodeSubscription = ChannelNodePermission.participants;
	private ChannelNodePermission presenceNodeSubscription = ChannelNodePermission.participants;
	private ChannelNodePermission participantsNodeSubscription = ChannelNodePermission.participants;
	private ChannelNodePermission informationNodeSubscription = ChannelNodePermission.participants;
	private ChannelNodePermission allowedNodeSubscription = ChannelNodePermission.admins;
	private ChannelNodePermission bannedNodeSubscription = ChannelNodePermission.admins;
	private ChannelNodePermission configurationNodeAccess = ChannelNodePermission.owners;
	private ChannelNodePermission informationNodeUpdateRights = ChannelNodePermission.admins;
	private ChannelNodePermission avatarNodesUpdateRights = ChannelNodePermission.admins;
	private boolean openPresence = false;
	private boolean participantsMustProvidePresence = false;
	private boolean userMessageRetraction = false;
	// should be owners
	private ChannelNodePermission administratorMessageRetractionRights = ChannelNodePermission.nobody;
	private boolean participantAdditionByInvitation = false;
	// should be true
	private boolean privateMessages = true;
	private boolean mandatoryNicks = true;

	public ChannelConfiguration() {

	}

	public ChannelConfiguration(Element el) throws PubSubException {
		Element form = el.getChild("x", "jabber:x:data");
		if (form == null || !Mix.ADMIN0_XMLNS.equals(DataForm.getFieldValue(form, "FORM_TYPE"))) {
			throw new PubSubException(Authorization.NOT_ACCEPTABLE, "This is not a valid configuration form!");
		}
		applyFrom(form);
	}

	public BareJID getLastChangeMadeBy() {
		return lastChangeMadeBy;
	}

	public void setLastChangeMadeBy(BareJID jid) {
		this.lastChangeMadeBy = jid;
	}

	public Set<BareJID> getOwners() {
		return owners;
	}

	public void setOwners(Set<BareJID> owners) {
		if (owners == null || owners.isEmpty()) {
			this.owners = Collections.emptySet();
		} else {
			this.owners = owners;
		}
	}

	public boolean isOwner(BareJID jid) {
		return owners.contains(jid);
	}

	public boolean isAdministrator(BareJID jid) {
		return administrators.contains(jid);
	}

	public Set<BareJID> getAdministrators() {
		return administrators;
	}

	public void setAdministrators(Set<BareJID> administrators) {
		if (administrators == null || administrators.isEmpty()) {
			this.administrators = Collections.emptySet();
		} else {
			this.administrators = administrators;
		}
	}

	public String[] getNodesPresent() {
		return nodesPresent;
	}

	public void setNodesPresent(String[] nodesPresent) {
		this.nodesPresent = nodesPresent;
	}

	public ChannelNodePermission getMessagesNodeSubscription() {
		return messagesNodeSubscription;
	}

	public ChannelNodePermission getParticipantsNodeSubscription() {
		return participantsNodeSubscription;
	}

	public ChannelNodePermission getInformationNodeSubscription() {
		return informationNodeSubscription;
	}

	public ChannelNodePermission getConfigurationNodeAccess() {
		return configurationNodeAccess;
	}

	public ChannelNodePermission getInformationNodeUpdateRights() {
		return informationNodeUpdateRights;
	}

	public ChannelNodePermission getAvatarNodesUpdateRights() {
		return avatarNodesUpdateRights;
	}

	public Element toElement(String id) {
		Element item = new Element("item");
		item.setAttribute("id", id);

		item.addChild(toFormElement());

		return item;
	}

	public Element toFormElement() {
		return new DataForm.Builder(Command.DataType.result).withFields(builder -> {
			builder.addField(DataForm.FieldType.Hidden, "FORM_TYPE").setValue(Mix.ADMIN0_XMLNS).build();
			builder.addField(DataForm.FieldType.JidSingle, "Last Change Made By").setValue(lastChangeMadeBy.toString()).build();
			builder.addField(DataForm.FieldType.JidMulti, "Owner")
					.setRequired(true)
					.setValues(owners.stream().map(BareJID::toString).toArray(String[]::new)).build();
			builder.addField(DataForm.FieldType.JidMulti, "Administrator")
					.setValues(administrators.stream().map(BareJID::toString).toArray(String[]::new)).build();
			builder.addField(DataForm.FieldType.TextSingle, "End of Life")
					.setValue(Optional.ofNullable(endOfLife).map(timestampHelper::format).orElse("")).build();
			builder.addField(DataForm.FieldType.ListMulti, "Nodes Present")
					.setOptions(NODES_PRESENT_OPTIONS)
					.setValues(nodesPresent).build();
			builder.addField(DataForm.FieldType.ListSingle, "Messages Node Subscription")
					.setOptions(ChannelNodePermission.MESSAGE_NODE_SUBSCRIPTIONS.stream()
										.map(Enum::name)
										.toArray(String[]::new))
					.setValue(messagesNodeSubscription.name()).build();
			builder.addField(DataForm.FieldType.ListSingle, "Presence Node Subscription")
					.setOptions(ChannelNodePermission.PRESENCE_NODE_SUBSCRIPTIONS.stream()
										.map(Enum::name)
										.toArray(String[]::new))
					.setValue(presenceNodeSubscription.name()).build();
			builder.addField(DataForm.FieldType.ListSingle, "Participants Node Subscription")
					.setOptions(ChannelNodePermission.PARTICIPANTS_NODE_SUBSCRIPTIONS.stream()
										.map(Enum::name)
										.toArray(String[]::new))
					.setValue(participantsNodeSubscription.name()).build();
			builder.addField(DataForm.FieldType.ListSingle, "Information Node Subscription")
					.setOptions(ChannelNodePermission.INFORMATION_NODE_SUBSCRIPTIONS.stream()
										.map(Enum::name)
										.toArray(String[]::new))
					.setValue(informationNodeSubscription.name()).build();
			builder.addField(DataForm.FieldType.ListSingle, "Allowed Node Subscription")
					.setOptions(ChannelNodePermission.ALLOWED_NODE_SUBSCRIPTIONS.stream()
										.map(Enum::name)
										.toArray(String[]::new))
					.setValue(allowedNodeSubscription.name()).build();
			builder.addField(DataForm.FieldType.ListSingle, "Banned Node Subscription")
					.setOptions(ChannelNodePermission.BANNED_NODE_SUBSCRIPTIONS.stream()
										.map(Enum::name)
										.toArray(String[]::new))
					.setValue(bannedNodeSubscription.name()).build();
			builder.addField(DataForm.FieldType.ListSingle, "Configuration Node Access")
					.setOptions(ChannelNodePermission.CONFIGURATION_NODE_ACCESS.stream()
										.map(Enum::name)
										.toArray(String[]::new))
					.setValue(configurationNodeAccess.name()).build();
			builder.addField(DataForm.FieldType.ListSingle, "Information Node Update Rights")
					.setOptions(ChannelNodePermission.INFORMATION_NODE_UPDATE_RIGHTS.stream()
										.map(Enum::name)
										.toArray(String[]::new))
					.setValue(informationNodeUpdateRights.name()).build();
			builder.addField(DataForm.FieldType.ListSingle, "Avatar Nodes Update Rights")
					.setOptions(ChannelNodePermission.AVATAR_NODES_UPDATE_RIGHTS.stream()
										.map(Enum::name)
										.toArray(String[]::new))
					.setValue(avatarNodesUpdateRights.name()).build();

			builder.addField(DataForm.FieldType.Boolean, "Open Presence").setValue(openPresence).build();
			builder.addField(DataForm.FieldType.Boolean, "Participants Must Provide Presence").setValue(participantsMustProvidePresence).build();
			builder.addField(DataForm.FieldType.Boolean, "User Message Retraction").setValue(userMessageRetraction).build();
			builder.addField(DataForm.FieldType.ListSingle, "Administrator Message Retraction Rights")
					.setOptions(ChannelNodePermission.ADMINISTRATOR_MESSAGE_RETRACTION_RIGHTS.stream()
										.map(Enum::name)
										.toArray(String[]::new))
					.setValue(administratorMessageRetractionRights.name()).build();
			builder.addField(DataForm.FieldType.Boolean, "Participation Addition by Invitation from Participant")
					.setValue(participantAdditionByInvitation).build();
			builder.addField(DataForm.FieldType.Boolean, "Private Messages")
					.setValue(privateMessages).build();
			builder.addField(DataForm.FieldType.Boolean, "Mandatory Nicks")
					.setValue(mandatoryNicks).build();
		}).build();
	}

	public boolean isNickMandator() {
		return mandatoryNicks;
	}

	public boolean arePrivateMessagesAllowed() {
		return privateMessages;
	}

	public ChannelConfiguration apply(Element form) throws PubSubException {
		ChannelConfiguration result = new ChannelConfiguration();
		result.applyFrom(form);
		return result;
	}
	
	protected void applyFrom(Element form) throws PubSubException {
		if (form == null) {
			throw new PubSubException(Authorization.NOT_ACCEPTABLE, "This is not a valid configuration form!");
		}
		if (form.getName() != "x" && form.getXMLNS() != "jabber:x:data") {
			throw new PubSubException(Authorization.NOT_ACCEPTABLE, "This is not a valid data form form!");
		}
		
		lastChangeMadeBy = getBareJIDFromField(form, "Last Change Made By", lastChangeMadeBy);

		owners = getListOfBareJIDsFromField(form, "Owner", owners);
		administrators = getListOfBareJIDsFromField(form, "Administrator", administrators);
		endOfLife = getDateFromField(form, "End of Life", endOfLife);

		nodesPresent = getFieldValues(form, "Nodes Present", nodesPresent);
		Set<String> allowedNodes = new HashSet<>(Arrays.asList(NODES_PRESENT_OPTIONS));
		for (String node : nodesPresent) {
			if (!allowedNodes.contains(node)) {
				throw new PubSubException(Authorization.NOT_ACCEPTABLE,
										  "Unacceptable value '" + node + "' for present nodes");
			}
		}

		messagesNodeSubscription = getPermissionFromField(form, "Messages Node Subscription",
														  ChannelNodePermission.MESSAGE_NODE_SUBSCRIPTIONS,
														  messagesNodeSubscription);
		presenceNodeSubscription = getPermissionFromField(form, "Presence Node Subscription",
														  ChannelNodePermission.PRESENCE_NODE_SUBSCRIPTIONS,
														  presenceNodeSubscription);
		participantsNodeSubscription = getPermissionFromField(form, "Participants Node Subscription",
															  ChannelNodePermission.PARTICIPANTS_NODE_SUBSCRIPTIONS,
															  participantsNodeSubscription);
		informationNodeSubscription = getPermissionFromField(form, "Information Node Subscription",
															 ChannelNodePermission.INFORMATION_NODE_SUBSCRIPTIONS,
															 informationNodeSubscription);
		allowedNodeSubscription = getPermissionFromField(form, "Allowed Node Subscription",
														 ChannelNodePermission.ALLOWED_NODE_SUBSCRIPTIONS,
														 allowedNodeSubscription);
		bannedNodeSubscription = getPermissionFromField(form, "Banned Node Subscription",
														ChannelNodePermission.BANNED_NODE_SUBSCRIPTIONS,
														bannedNodeSubscription);
		configurationNodeAccess = getPermissionFromField(form, "Configuration Node Access",
														 ChannelNodePermission.CONFIGURATION_NODE_ACCESS,
														 configurationNodeAccess);
		informationNodeUpdateRights = getPermissionFromField(form, "Information Node Update Rights",
															 ChannelNodePermission.INFORMATION_NODE_UPDATE_RIGHTS,
															 informationNodeUpdateRights);
		avatarNodesUpdateRights = getPermissionFromField(form, "Avatar Nodes Update Rights",
														 ChannelNodePermission.AVATAR_NODES_UPDATE_RIGHTS,
														 avatarNodesUpdateRights);

		openPresence = getBoolFromField(form, "Open Presence", openPresence);
		participantsMustProvidePresence = getBoolFromField(form, "Participants Must Provide Presence",
														   participantsMustProvidePresence);
		userMessageRetraction = getBoolFromField(form, "User Message Retraction", userMessageRetraction);
		administratorMessageRetractionRights = getPermissionFromField(form, "Administrator Message Retraction Rights",
																	  ChannelNodePermission.ADMINISTRATOR_MESSAGE_RETRACTION_RIGHTS,
																	  administratorMessageRetractionRights);
		participantAdditionByInvitation = getBoolFromField(form,
														   "Participation Addition by Invitation from Participant",
														   participantAdditionByInvitation);
		privateMessages = getBoolFromField(form, "Private Messages", privateMessages);
		mandatoryNicks = getBoolFromField(form, "Mandatory Nicks", mandatoryNicks);

		validate();
	}

	public boolean isValid() {
		try {
			validate();
			return true;
		} catch (PubSubException ex) {
			return false;
		}
	}

	private void validate() throws PubSubException {
		if (owners.isEmpty()) {
			throw new PubSubException(Authorization.NOT_ALLOWED, "There MUST be at least one channel owner!");
		}
		if (Arrays.stream(nodesPresent).anyMatch(node -> "presence".equals(node) || "jidmap-visible".equals(node))) {
			throw new PubSubException(Authorization.NOT_ACCEPTABLE, "Only participants and information nodes are supported!");
		}
		if (openPresence) {
			throw new PubSubException(Authorization.NOT_ACCEPTABLE, "Open Presence is not supported!");
		}
		if (participantsMustProvidePresence) {
			throw new PubSubException(Authorization.NOT_ACCEPTABLE, "Support for presences is not implemented yet!");
		}
		if (userMessageRetraction || administratorMessageRetractionRights != ChannelNodePermission.nobody) {
			throw new PubSubException(Authorization.NOT_ACCEPTABLE, "Message retraction is not supported!");
		}
		if (participantAdditionByInvitation) {
			throw new PubSubException(Authorization.NOT_ACCEPTABLE,"Invitations are not supported!");
		}
	}

	private static Element getField(Element form, String fieldName) {
		return form.findChild(el -> el.getName() == "field" && fieldName.equals(el.getAttributeStaticStr("var")));
	}

	private static String getFieldValue(Element field) {
		Element value = field.findChild(el -> el.getName() == "value");
		return value == null ? null : value.getCData();
	}

	private static List<String> getFieldValues(Element field) {
		List<String> values = field.mapChildren(el -> el.getName() == "value", el -> el.getCData());
		if (values == null) {
			return Collections.emptyList();
		}
		return values;
	}

	private static List<String> getFieldValues(Element form, String fieldName, List<String> defValue) {
		Element field = getField(form, fieldName);
		if (field == null) {
			return defValue;
		}
		return getFieldValues(field);
	}

	private static String[] getFieldValues(Element form, String fieldName, String[] defValue) {
		Element field = getField(form, fieldName);
		if (field == null) {
			return defValue;
		}
		return getFieldValues(field).toArray(String[]::new);
	}

	private static boolean getBoolFromField(Element form, String fieldName, boolean defValue) {
		Element field = getField(form, fieldName);
		if (field == null) {
			return defValue;
		}
		
		String value = getFieldValue(field);
		return "true".equals(value) || "1".equals(value);
	}

	private static ChannelNodePermission getPermissionFromField(Element form, String fieldName, List<ChannelNodePermission> acceptedValues, ChannelNodePermission defValue) throws PubSubException {
		Element field = getField(form, fieldName);
		if (field == null) {
			return defValue;
		}
		
		String value = getFieldValue(field);
		if (value == null) {
			throw new PubSubException(Authorization.NOT_ACCEPTABLE, "Missing value for field " + fieldName);
		}
		value = value.trim();
		if (value.isEmpty()) {
			throw new PubSubException(Authorization.NOT_ACCEPTABLE, "Missing value for field " + fieldName);
		}

		try {
			return ChannelNodePermission.valueOf(value);
		} catch (IllegalArgumentException ex) {
			throw new PubSubException(Authorization.NOT_ACCEPTABLE, "Invalid value 'value' for field " + fieldName);
		}
	}

	private static Date getDateFromField(Element form, String fieldName, Date defValue) throws PubSubException {
		try {
			Element field = getField(form, fieldName);
			if (field == null) {
				return defValue;
			}
			
			String value = getFieldValue(field);
			if (value == null) {
				return null;
			}
			value = value.trim();
			if (value.isEmpty()) {
				return null;
			}
			return timestampHelper.parseTimestamp(value);
		} catch (ParseException ex) {
			throw new PubSubException(Authorization.NOT_ACCEPTABLE, "Incorrect date in field " + fieldName, ex);
		}
	}

	private static BareJID getBareJIDFromField(Element form, String fieldName, BareJID defValue) throws PubSubException {
		try {
			Element field = getField(form, fieldName);
			if (field == null) {
				return defValue;
			}
			return convertStringToBareJID(getFieldValue(field));
		} catch (TigaseStringprepException ex) {
			throw new PubSubException(Authorization.NOT_ACCEPTABLE, "Incorrect JID in field " + fieldName, ex);
		}
	}

	private static Set<BareJID> getListOfBareJIDsFromField(Element form, String fieldName, Set<BareJID> defValue) throws PubSubException {
		Element field = getField(form, fieldName);
		if (field == null) {
			return defValue;
		}

		List<String> values = getFieldValues(field);
		HashSet<BareJID> result = new HashSet<>();
		for (String value : values) {
			try {
				BareJID jid = convertStringToBareJID(value);
				if (jid != null) {
					result.add(jid);
				}
			} catch (TigaseStringprepException ex) {
				throw new PubSubException(Authorization.NOT_ACCEPTABLE,
										  "Incorrect JID '" + value + "' in field " + fieldName, ex);
			}
		}
		return result;
	}

	private static BareJID convertStringToBareJID(String value) throws TigaseStringprepException {
		if (value == null) {
			return null;
		}
		String tmp = value.trim();
		if (tmp.isEmpty()) {
			return null;
		}
		return BareJID.bareJIDInstance(tmp);
	}
}