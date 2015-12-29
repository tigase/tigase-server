package tigase.disteventbus.local;

/**
 * Created by bmalkow on 31.12.2015.
 */
public class RegistrationException extends RuntimeException {
	public RegistrationException() {
	}

	public RegistrationException(String message) {
		super(message);
	}

	public RegistrationException(String message, Throwable cause) {
		super(message, cause);
	}

	public RegistrationException(Throwable cause) {
		super(cause);
	}

	public RegistrationException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
