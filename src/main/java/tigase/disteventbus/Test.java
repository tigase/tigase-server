package tigase.disteventbus;

import java.lang.reflect.Method;

import tigase.disteventbus.local.*;
import tigase.disteventbus.local.EventHandler;

/**
 * Created by bmalkow on 30.12.2015.
 */
public class Test {

	public static void main(String[] args) throws Exception {
		final DefaultLocalEventsBus eventsBus = new DefaultLocalEventsBus();

		Consumer c1 = new Consumer();
		Method m1 = c1.getClass().getMethod("onSomething", SampleEvent.class);
		ReflHandler l1 = new ReflHandler(c1, m1);

		eventsBus.addHandler(SampleEvent.class, l1);

		eventsBus.addHandler(SampleEvent.class, new tigase.disteventbus.local.EventHandler() {
			private int counter = 0;

			@Override
			public void onEvent(Event event) {
				++counter;
			}

		});

		for (int j = 0; j < 10; j++) {

			final long t1 = System.nanoTime();
			final int n = 1000000;
			for (int i = 0; i < n; i++) {
				SampleEvent x = new SampleEvent(String.valueOf(i));
				eventsBus.fire(x);
			}
			final long t2 = System.nanoTime();

			System.out.println(j + ". t=" + (t2 - t1) + "  (" + ((t2 - t1) / n) + " per call)");
		}
	}

	public static class SampleEvent implements Event {

		private final String data;

		public SampleEvent(String data) {
			this.data = data;
		}

		public String getData() {
			return data;
		}
	}

	public static class Consumer {

		private int counter = 0;

		public void onSomething(SampleEvent event) {
			++counter;
		}

		@Override
		public String toString() {
			return "counter:" + counter;
		}
	}

	public static class ReflHandler implements EventHandler {

		private final Object obj;

		private final Method method;

		public ReflHandler(Object c1, Method m1) {
			this.obj = c1;
			this.method = m1;
		}

		@Override
		public void onEvent(final Event event) {
			try {
				method.invoke(obj, event);
			} catch (Exception e) {
				e.printStackTrace();
			}

		}
	}

}
