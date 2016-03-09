package tigase.server;

import org.junit.Ignore;
import org.junit.Test;
import tigase.db.AuthRepositoryMDImpl;
import tigase.db.UserRepositoryMDImpl;
import tigase.kernel.core.Kernel;
import tigase.server.xmppclient.ClientConnectionManager;
import tigase.server.xmppserver.S2SConnectionManager;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertNotNull;

/**
 * Created by andrzej on 07.03.2016.
 */
@Ignore
public class BootstrapTest {

	@Test
	public void test1() {
		Bootstrap bootstrap = new Bootstrap();

		bootstrap.setProperties(getProps());

		bootstrap.start();

		Kernel kernel = bootstrap.getKernel();
		assertNotNull(kernel);

		MessageRouter mr = kernel.getInstance("message-router");
		ClientConnectionManager c2s = kernel.getInstance("c2s");
		S2SConnectionManager s2s = kernel.getInstance("s2s");
		UserRepositoryMDImpl userRepository = kernel.getInstance("userRepository");
		AuthRepositoryMDImpl authRepository = kernel.getInstance("authRepository");
		assertNotNull(mr);
		assertNotNull(c2s);
		assertNotNull(s2s);
		assertNotNull(userRepository);
		assertNotNull(userRepository.getRepo(null));
		assertNotNull(authRepository);
		assertNotNull(authRepository.getRepo("default"));

	}

	public Map<String, Object> getProps() {
		Map<String, Object> props = new HashMap<>();

		props.put("userRepository/repo-uri", "jdbc:");
		props.put("repo-uri", "jdbc:");

		return props;
	}
}
