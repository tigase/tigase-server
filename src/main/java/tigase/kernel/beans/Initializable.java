package tigase.kernel.beans;

/**
 * If bean implements this interface, then if bean will be created and
 * configured, Kernel calls method {@link Initializable#initialize()}.
 */
public interface Initializable {

	/**
	 * Method will be called, when bean will be created, configured and ready to
	 * use.
	 */
	void initialize();

	default void completed() {
	}

}
