package tigase.kernel;

public class KernelException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public KernelException() {
		super();
	}

	public KernelException(String message) {
		super(message);
	}

	public KernelException(String message, Throwable cause) {
		super(message, cause);
	}

	public KernelException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public KernelException(Throwable cause) {
		super(cause);
	}

}
