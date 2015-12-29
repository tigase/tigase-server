package tigase.disteventbus.local;

import java.util.Collection;

import org.junit.Assert;
import org.junit.Test;

/**
 * Created by bmalkow on 30.12.2015.
 */
public class ReflectEventHandlerFactoryTest {

	@Test
	public void testCreate() throws Exception {
		final ReflectEventListenerFactory factory = new ReflectEventListenerFactory();
		Collection<ReflectEventHandler> newNonHandlers = factory.create(new NewConsumer());
		Assert.assertEquals(3, newNonHandlers.size());

		Collection<ReflectEventHandler> conHandlers = factory.create(new Consumer());
		Assert.assertEquals(2, conHandlers.size());
		System.out.println();
	}

}