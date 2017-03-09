package tigase.auth;

import javax.security.auth.callback.CallbackHandler;

import tigase.db.AuthRepository;

/**
 * Interface should be implemented by {@linkplain CallbackHandler} instance if
 * {@linkplain AuthRepository} from session should be injected.
 */
public interface AuthRepositoryAware extends Aware {

	/**
	 * Sets {@linkplain AuthRepository}.
	 * 
	 * @param repo
	 *            {@linkplain AuthRepository}.
	 */
	void setAuthRepository(AuthRepository repo);

}
