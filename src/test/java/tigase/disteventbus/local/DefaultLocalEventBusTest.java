package tigase.disteventbus.local;

import java.util.concurrent.Executor;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Created by bmalkow on 17.12.2015.
 */
public class DefaultLocalEventBusTest {

	private DefaultLocalEventsBus eventBus;

	@Before
	public void setUp() throws Exception {
		eventBus = new DefaultLocalEventsBus();
		eventBus.setExecutor(new Executor() {
			@Override
			public void execute(Runnable command) {
				command.run();
			}
		});
	}

	@Test
	public void test1() {
		final String[] value = new String[5];

		eventBus.addHandler(new EventHandler() {

			@Override
			public void onEvent(Event event) {
				if (event instanceof Test01Event) {
					value[1] = "l" + ((Test01Event) event).getData();
				} else if (event instanceof Test02Event) {
					value[4] = "l" + ((Test02Event) event).getData();
				}
			}
		});

		eventBus.addHandler(Test02Event.class, new EventHandler() {

			@Override
			public void onEvent(Event event) {
				value[3] = "fail";
			}
		});

		Test01Event event = new Test01Event("test01");
		eventBus.fire(event);

		Assert.assertEquals("ltest01", value[1]);
		Assert.assertNull(value[2]);
		Assert.assertNull(value[3]);
		Assert.assertNull(value[4]);
	}

	@Test
	public void test2() {
		Consumer c = new Consumer();
		eventBus.registerAll(c);

		eventBus.fire(new Event01());
		eventBus.fire(new Event02());
		eventBus.fire(new Event02());

		Assert.assertEquals(1, c.getCounter01());
		Assert.assertEquals(2, c.getCounter02());

		NewConsumer nc = new NewConsumer();
		eventBus.registerAll(nc);

		eventBus.fire(new Event01());
		eventBus.fire(new Event02());
		eventBus.fire(new Event02());

		Assert.assertEquals(2, c.getCounter01());
		Assert.assertEquals(4, c.getCounter02());

		Assert.assertEquals(0, nc.getCounter01());
		Assert.assertEquals(2, nc.getCounter02());
		Assert.assertEquals(1, nc.getCounter03());
		Assert.assertEquals(1, nc.getCounter01_1());

		eventBus.unregisterAll(nc);

		eventBus.fire(new Event01());
		eventBus.fire(new Event02());
		eventBus.fire(new Event02());

		Assert.assertEquals(3, c.getCounter01());
		Assert.assertEquals(6, c.getCounter02());

		Assert.assertEquals(0, nc.getCounter01());
		Assert.assertEquals(2, nc.getCounter02());
		Assert.assertEquals(1, nc.getCounter03());
		Assert.assertEquals(1, nc.getCounter01_1());

		eventBus.unregisterAll(new Consumer());

		eventBus.fire(new Event01());
		eventBus.fire(new Event02());
		eventBus.fire(new Event02());

		Assert.assertEquals(4, c.getCounter01());
		Assert.assertEquals(8, c.getCounter02());

		Assert.assertEquals(0, nc.getCounter01());
		Assert.assertEquals(2, nc.getCounter02());
		Assert.assertEquals(1, nc.getCounter03());
		Assert.assertEquals(1, nc.getCounter01_1());

		eventBus.unregisterAll(c);

		Assert.assertEquals(4, c.getCounter01());
		Assert.assertEquals(8, c.getCounter02());

		Assert.assertEquals(0, nc.getCounter01());
		Assert.assertEquals(2, nc.getCounter02());
		Assert.assertEquals(1, nc.getCounter03());
		Assert.assertEquals(1, nc.getCounter01_1());

	}

	@Test
	public void testRemove() {
		final String[] value = new String[2];
		EventHandler l1 = new EventHandler() {

			@Override
			public void onEvent(Event event) {
				value[1] = ((Test01Event) event).getData();
			}
		};
		EventHandler l0 = new EventHandler<Test01Event>() {

			@Override
			public void onEvent(Test01Event event) {
				value[0] = event.getData();
			}
		};
		eventBus.addHandler(Test01Event.class, l1);
		eventBus.addHandler(Test01Event.class, l0);

		Test01Event event = new Test01Event("t1");
		eventBus.fire(event);

		Assert.assertEquals("t1", value[0]);
		Assert.assertEquals("t1", value[1]);

		event = new Test01Event("t2");
		eventBus.fire(event);

		Assert.assertEquals("t2", value[0]);
		Assert.assertEquals("t2", value[1]);

		eventBus.remove(Test01Event.class, l1);

		event = new Test01Event("t3");
		eventBus.fire(event);

		Assert.assertEquals("t3", value[0]);
		Assert.assertEquals("t2", value[1]);

		eventBus.remove(l0);

		event = new Test01Event("t4");
		eventBus.fire(event);

		Assert.assertEquals("t3", value[0]);
		Assert.assertEquals("t2", value[1]);
	}

	public static class Test01Event implements Event {
		private final String data;

		public Test01Event(String data) {
			this.data = data;
		}

		public String getData() {
			return data;
		}
	}

	public static class Test02Event implements Event {
		private final String data;

		public Test02Event(String data) {
			this.data = data;
		}

		public String getData() {
			return data;
		}
	}

}