package tigase.kernel.beans;

/**
 * If bean implements this interface then just before unregistering bean Kernel
 * calls method {@link UnregisterAware#beforeUnregister()}.
 */
public interface UnregisterAware {

	/**
	 * Method called before bean unregister.
	 */
	void beforeUnregister();

}
