package tigase.component;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import tigase.kernel.core.Kernel;
import tigase.xmpp.JID;

/**
 * Created by bmalkow on 19.10.2015.
 */
public class PropertiesBeanConfiguratorTest {

	@Test
	public void testConfigure() throws Exception {
		Map<String, Object> props = new HashMap<>();
		System.setProperty("bean-config-access-to-all", "true");
		props.put("bean1/field1", "abc");
		props.put("bean1/field2", "124");
		props.put("bean1/field3", "a1@b.c/d");
		props.put("alias1", "a0@b.c/d");
		props.put("alias2", "a2@b.c/d");


		Kernel k = new Kernel("test");
		k.registerBean(PropertiesBeanConfigurator.class).exec();
		k.registerBean(Bean1.class).exec();


		k.getInstance(PropertiesBeanConfigurator.class).setProperties(props);


		Assert.assertEquals("abc", k.getInstance(Bean1.class).getField1());
		Assert.assertEquals(124, k.getInstance(Bean1.class).getField2());
		Assert.assertEquals(JID.jidInstanceNS("a1@b.c/d"), k.getInstance(Bean1.class).getField3());
		Assert.assertEquals(JID.jidInstanceNS("a2@b.c/d"), k.getInstance(Bean1.class).getField4());
	}
}