package tigase.kernel;

import org.junit.Test;
import tigase.kernel.beans.Inject;
import tigase.kernel.core.Kernel;

import java.util.List;

/**
 * Created by andrzej on 31.10.2016.
 */
public class GenericBeanKernelTest {

	@Test
	public void testGenerics() {

		Kernel kernel = new Kernel();
		kernel.registerBean("converter1").asClass(Converter1.class).exec();
		kernel.registerBean("converter2").asClass(Converter2.class).exec();

		kernel.registerBean("consumer").asClass(ConverterConsumer.class).exec();

		kernel.getInstance(ConverterConsumer.class);

	}


	public interface Converter<T> {
		String toString(T object);
	}

	public static class Converter1 implements Converter<String> {

		@Override
		public String toString(String object) {
			return object;
		}
	}

	public static class Converter2 implements Converter<Long> {

		@Override
		public String toString(Long object) {
			return String.valueOf(object);
		}
	}

	public static class ConverterConsumer {

		@Inject
		private List<Converter> converters;

	}

}
