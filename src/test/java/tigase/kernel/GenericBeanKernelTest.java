/**
 * Tigase XMPP Server - The instant messaging server
 * Copyright (C) 2004 Tigase, Inc. (office@tigase.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 */
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

	public static class Converter1
			implements Converter<String> {

		@Override
		public String toString(String object) {
			return object;
		}
	}

	public static class Converter2
			implements Converter<Long> {

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
