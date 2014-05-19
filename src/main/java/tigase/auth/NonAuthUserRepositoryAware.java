package tigase.auth;

import tigase.db.NonAuthUserRepository;

public interface NonAuthUserRepositoryAware extends Aware {

	void setNonAuthUserRepository(NonAuthUserRepository repo);

}
