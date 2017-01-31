package tigase.server;

import org.junit.Ignore;
import org.junit.Test;
import tigase.TestLogger;
import tigase.db.AuthRepositoryMDImpl;
import tigase.db.UserRepositoryMDImpl;
import tigase.kernel.core.Kernel;
import tigase.server.xmppclient.ClientConnectionManager;
import tigase.server.xmppserver.S2SConnectionManager;
import tigase.server.xmppsession.SessionManager;

import java.lang.reflect.Field;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.Assert.assertNotNull;

/**
 * Created by andrzej on 07.03.2016.
 */
@Ignore
public class BootstrapTest {

	private static final Logger log = TestLogger.getLogger(BootstrapTest.class);
	
	private Map<String, Object> props = new HashMap<>();

	@Test
	public void testNonCluster() throws InterruptedException {
		props.put("--cluster-mode","false");
		Bootstrap  bootstrap = executeTest();
		Thread.sleep(10 * 60 * 1000);
		bootstrap.stop();
	}

	@Test
	public void testCluster() {
		props.put("--cluster-mode","true");
		Bootstrap bootstrap = executeTest();
		bootstrap.stop();
	}

	public Bootstrap executeTest() {
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

		try {
			SessionManager sm = kernel.getInstance(SessionManager.class);
			Field commandsAcl = BasicComponent.class.getDeclaredField("commandsACL");
			commandsAcl.setAccessible(true);
			Map<String,EnumSet<CmdAcl>> val = (Map<String, EnumSet<CmdAcl>>) commandsAcl.get(sm);
			log.log(Level.FINE, "ACL = " + val);
			EnumSet<CmdAcl> acl = val.get("ala-ma-kota");
			log.log(Level.FINE, "" + acl.getClass() + ", " + acl);
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		return bootstrap;
	}

	public Map<String, Object> getProps() {
		Map<String, Object> props = new HashMap<>(this.props);

		//props.put("userRepository/repo-uri", "jdbc:postgresql://127.0.0.1/tigase?user=test&password=test&autoCreateUser=true");
		props.put("dataSource/repo-uri", "jdbc:postgresql://127.0.0.1/tigase?user=test&password=test&autoCreateUser=true");
		props.put("sess-man/commands/ala-ma-kota", "DOMAIN");
		props.put("c2s/incoming-filters", "tigase.server.filters.PacketCounter,tigase.server.filters.PacketCounter");

		return props;
	}

}
