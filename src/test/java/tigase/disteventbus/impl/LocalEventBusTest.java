package tigase.disteventbus.impl;

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.concurrent.Executor;

import org.hamcrest.CoreMatchers;
import org.junit.Test;

import tigase.disteventbus.EventBus;
import tigase.disteventbus.EventHandler;
import tigase.xml.Element;

/**
 * Created by bmalkow on 17.11.2015.
 */
public class LocalEventBusTest {

	@Test
	public void test01() {
		final HashSet<String> results = new HashSet<>();

		LocalEventBus eb = new LocalEventBus();
		eb.setExecutor(new Executor() {
			@Override
			public void execute(Runnable command) {
				command.run();
			}
		});
		eb.addHandler(null, null, new EventHandler() {
			@Override
			public void onEvent(String name, String xmlns, Element event) {
				results.add("*.*_" + name + "." + xmlns);
			}
		});
		eb.addHandler(null, "b", new EventHandler() {
			@Override
			public void onEvent(String name, String xmlns, Element event) {
				results.add("*.b_" + name + "." + xmlns);
			}
		});
		eb.addHandler("a", "b", new EventHandler() {
			@Override
			public void onEvent(String name, String xmlns, Element event) {
				results.add("a.b_" + name + "." + xmlns);
			}
		});
		try {
			eb.addHandler("a", null, new EventHandler() {
				@Override
				public void onEvent(String name, String xmlns, Element event) {
					System.out.println("!!! ");
					results.add("a.*_" + name + "." + xmlns);
				}
			});
			fail("This registration should not be possible!");
		} catch (RuntimeException e) {
			assertEquals("Illegal handler registration. If name is specified, then xmlns must also be specified.",
					e.getMessage());
		}
		eb.fire(new Element("a", new String[] { "xmlns" }, new String[] { "b" }));
		eb.fire(new Element("c", new String[] { "xmlns" }, new String[] { "d" }));

		assertThat(results, CoreMatchers.hasItem("*.*_c.d"));
		assertThat(results, CoreMatchers.hasItem("*.*_a.b"));
		assertThat(results, CoreMatchers.hasItem("*.b_a.b"));
		assertThat(results, CoreMatchers.hasItem("a.b_a.b"));

	}

}