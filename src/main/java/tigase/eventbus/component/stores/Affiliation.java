/*
 * Affiliation.java
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
package tigase.eventbus.component.stores;

public enum Affiliation {
	/** */
	member(2, true, true, false, false, false, false, false),
	/** */
	none(1, false, false, false, false, false, false, false),
	/**
	 * An entity that is disallowed from subscribing or publishing to a node.
	 */
	outcast(0, false, false, false, false, false, false, false),
	/**
	 * The manager of a node, of which there may be more than one; often but not
	 * necessarily the node creator.
	 */
	owner(4, true, true, true, true, true, true, true),
	/** An entity that is allowed to publish items to a node. */
	publisher(3, true, true, true, true, false, false, false);

	private final boolean configureNode;

	private final boolean deleteItem;

	private final boolean deleteNode;

	private final boolean publishItem;

	private final boolean purgeNode;

	private final boolean retrieveItem;

	private final boolean subscribe;

	private final int weight;

	Affiliation(int weight, boolean subscribe, boolean retrieveItem, boolean publishItem, boolean deleteItem,
			boolean configureNode, boolean deleteNode, boolean purgeNode) {
		this.subscribe = subscribe;
		this.weight = weight;
		this.retrieveItem = retrieveItem;
		this.publishItem = publishItem;
		this.deleteItem = deleteItem;
		this.configureNode = configureNode;
		this.deleteNode = deleteNode;
		this.purgeNode = purgeNode;
	}

	public int getWeight() {
		return weight;
	}

	public boolean isConfigureNode() {
		return configureNode;
	}

	public boolean isDeleteItem() {
		return deleteItem;
	}

	public boolean isDeleteNode() {
		return deleteNode;
	}

	public boolean isPublishItem() {
		return publishItem;
	}

	public boolean isPurgeNode() {
		return purgeNode;
	}

	public boolean isRetrieveItem() {
		return retrieveItem;
	}

	public boolean isSubscribe() {
		return subscribe;
	}
}
