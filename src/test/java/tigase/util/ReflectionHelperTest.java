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
package tigase.util;

import org.junit.Test;
import tigase.cluster.repo.ClConConfigRepository;
import tigase.cluster.repo.ClConDirRepository;
import tigase.cluster.repo.ClConSQLRepository;
import tigase.cluster.repo.ClusterRepoItem;
import tigase.db.comp.ComponentRepository;
import tigase.db.comp.RepositoryItem;
import tigase.util.reflection.ReflectionHelper;
import tigase.vhosts.VHostItem;
import tigase.vhosts.VHostJDBCRepository;

import java.lang.reflect.Field;
import java.lang.reflect.Type;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by andrzej on 19.03.2016.
 */
public class ReflectionHelperTest {

	@Test
	public void testClassMatchesType() throws NoSuchFieldException {
		Field f = null;
		Type type = null;

		f = Test1.class.getDeclaredField("repo1");
		type = f.getGenericType();
		assertTrue(ReflectionHelper.classMatchesType(ClConSQLRepository.class, type));
		assertTrue(ReflectionHelper.classMatchesType(ClConConfigRepository.class, type));
		assertTrue(ReflectionHelper.classMatchesType(ClConDirRepository.class, type));
		assertFalse(ReflectionHelper.classMatchesType(VHostJDBCRepository.class, type));

		f = Test1.class.getDeclaredField("repo2");
		type = f.getGenericType();
		assertFalse(ReflectionHelper.classMatchesType(ClConSQLRepository.class, type));
		assertFalse(ReflectionHelper.classMatchesType(ClConConfigRepository.class, type));
		assertFalse(ReflectionHelper.classMatchesType(ClConDirRepository.class, type));
		assertTrue(ReflectionHelper.classMatchesType(VHostJDBCRepository.class, type));
	}

	@Test
	public void testClassMatchesClassWithParameters() {
		assertTrue(ReflectionHelper.classMatchesClassWithParameters(ClConSQLRepository.class, ComponentRepository.class,
																	new Type[]{ClusterRepoItem.class}));
		assertFalse(
				ReflectionHelper.classMatchesClassWithParameters(ClConSQLRepository.class, ComponentRepository.class,
																 new Type[]{VHostItem.class}));
		assertTrue(ReflectionHelper.classMatchesClassWithParameters(ClConSQLRepository.class, ComponentRepository.class,
																	new Type[]{RepositoryItem.class}));
	}

	private class Test1 {

		private ComponentRepository<ClusterRepoItem> repo1;
		private ComponentRepository<VHostItem> repo2;
	}
}
