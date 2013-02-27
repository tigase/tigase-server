package tigase.io;

import static org.junit.Assert.*;

import java.util.HashMap;

import org.junit.Test;

public class SSLContextContainerTest {

	@Test
	public void testFind() {
		final HashMap<String, String> domains = new HashMap<String, String>();
		domains.put("one.com", "one.com");
		domains.put("a.two.com", "a.two.com");
		domains.put("*.two.com", "*.two.com");

		assertEquals("one.com", SSLContextContainer.find(domains, "one.com"));
		assertNull(SSLContextContainer.find(domains, "tone.com"));
		assertNull(SSLContextContainer.find(domains, "zero.com"));
		assertEquals("a.two.com", SSLContextContainer.find(domains, "a.two.com"));
		assertEquals("*.two.com", SSLContextContainer.find(domains, "b.two.com"));
		assertEquals("*.two.com", SSLContextContainer.find(domains, "b.two.com"));
		assertNull(SSLContextContainer.find(domains, "btwo.com"));
		assertEquals("*.two.com", SSLContextContainer.find(domains, ".two.com"));
	}

}
