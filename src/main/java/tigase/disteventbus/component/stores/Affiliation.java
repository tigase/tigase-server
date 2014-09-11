package tigase.disteventbus.component.stores;

public enum Affiliation {
	/** */
	member(2, true, true, false, false, false, false, false),
	/** */
	none(1, true, false, false, false, false, false, false),
	/** An entity that is disallowed from subscribing or publishing to a node. */
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

	private Affiliation(int weight, boolean subscribe, boolean retrieveItem, boolean publishItem, boolean deleteItem,
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
