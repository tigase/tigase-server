package tigase.disteventbus.local;

import java.lang.reflect.Method;

import org.junit.Assert;
import org.junit.Test;

/**
 * Created by bmalkow on 30.12.2015.
 */
public class ReflectEventHandlerTest {

	@Test
	public void testEquals() throws Exception {
		Consumer c1 = new NewConsumer();
		Method m11 = c1.getClass().getMethod("onEvent01", Event01.class);
		Method m12 = c1.getClass().getMethod("onCatchSomeNiceEvent", Event02.class);
		Method m13 = c1.getClass().getMethod("onEvent03", Event01.class);

		Consumer c2 = new NewConsumer();
		Method m21 = c2.getClass().getMethod("onEvent01", Event01.class);

		Assert.assertEquals(new ReflectEventHandler(c1, m11), new ReflectEventHandler(c1, m11));
		Assert.assertEquals(new ReflectEventHandler(c1, m12), new ReflectEventHandler(c1, m12));
		Assert.assertEquals(new ReflectEventHandler(c1, m13), new ReflectEventHandler(c1, m13));

		Assert.assertNotEquals(new ReflectEventHandler(c1, m11), new ReflectEventHandler(c1, m12));
		Assert.assertNotEquals(new ReflectEventHandler(c1, m11), new ReflectEventHandler(c1, m13));
		Assert.assertNotEquals(new ReflectEventHandler(c1, m12), new ReflectEventHandler(c1, m13));

		Assert.assertNotEquals(new ReflectEventHandler(c1, m11), new ReflectEventHandler(c2, m21));
		Assert.assertNotEquals(new ReflectEventHandler(c1, m12), new ReflectEventHandler(c2, m21));
		Assert.assertNotEquals(new ReflectEventHandler(c1, m13), new ReflectEventHandler(c2, m21));
	}

	@Test
	public void testHashCode() throws Exception {
		Consumer c1 = new NewConsumer();
		Method m11 = c1.getClass().getMethod("onEvent01", Event01.class);
		Method m12 = c1.getClass().getMethod("onCatchSomeNiceEvent", Event02.class);
		Method m13 = c1.getClass().getMethod("onEvent03", Event01.class);

		Consumer c2 = new NewConsumer();
		Method m21 = c2.getClass().getMethod("onEvent01", Event01.class);

		Assert.assertEquals(new ReflectEventHandler(c1, m11).hashCode(), new ReflectEventHandler(c1, m11).hashCode());
		Assert.assertEquals(new ReflectEventHandler(c1, m12).hashCode(), new ReflectEventHandler(c1, m12).hashCode());
		Assert.assertEquals(new ReflectEventHandler(c1, m13).hashCode(), new ReflectEventHandler(c1, m13).hashCode());

		Assert.assertNotEquals(new ReflectEventHandler(c1, m11).hashCode(), new ReflectEventHandler(c1, m12).hashCode());
		Assert.assertNotEquals(new ReflectEventHandler(c1, m11).hashCode(), new ReflectEventHandler(c1, m13).hashCode());
		Assert.assertNotEquals(new ReflectEventHandler(c1, m12).hashCode(), new ReflectEventHandler(c1, m13).hashCode());

		Assert.assertNotEquals(new ReflectEventHandler(c1, m11).hashCode(), new ReflectEventHandler(c2, m21).hashCode());
		Assert.assertNotEquals(new ReflectEventHandler(c1, m12).hashCode(), new ReflectEventHandler(c2, m21).hashCode());
		Assert.assertNotEquals(new ReflectEventHandler(c1, m13).hashCode(), new ReflectEventHandler(c2, m21).hashCode());
	}
}