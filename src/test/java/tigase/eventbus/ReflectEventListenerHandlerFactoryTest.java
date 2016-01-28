package tigase.eventbus;

import java.util.Collection;

import org.junit.Assert;
import org.junit.Test;

/**
 * Created by bmalkow on 26.01.2016.
 */
public class ReflectEventListenerHandlerFactoryTest {

	@Test
	public void testCreate() throws Exception {
		final ReflectEventListenerHandlerFactory f = new ReflectEventListenerHandlerFactory();
		EventBusImplementationTest.Consumer c = new EventBusImplementationTest.Consumer();
		Collection<AbstractHandler> handlers1 = f.create(c);
		Collection<AbstractHandler> handlers2 = f.create(c);

		Assert.assertEquals(handlers1.size(), handlers2.size());
		Assert.assertEquals(handlers1, handlers2);
	}
}