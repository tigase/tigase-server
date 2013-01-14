package tigase.auth;

import tigase.db.NonAuthUserRepository;

public interface NonAuthUserRepositoryAware {

	void setNonAuthUserRepository(NonAuthUserRepository repo);

}
