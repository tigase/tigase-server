package tigase.util;

import org.junit.Test;
import tigase.cluster.repo.ClConConfigRepository;
import tigase.cluster.repo.ClConDirRepository;
import tigase.cluster.repo.ClConSQLRepository;
import tigase.cluster.repo.ClusterRepoItem;
import tigase.db.comp.ComponentRepository;
import tigase.db.comp.RepositoryItem;
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
		assertTrue(ReflectionHelper.classMatchesClassWithParameters(ClConSQLRepository.class, ComponentRepository.class, new Type[] { ClusterRepoItem.class }));
		assertFalse(ReflectionHelper.classMatchesClassWithParameters(ClConSQLRepository.class, ComponentRepository.class, new Type[] { VHostItem.class }));
		assertTrue(ReflectionHelper.classMatchesClassWithParameters(ClConSQLRepository.class, ComponentRepository.class, new Type[] { RepositoryItem.class }));
	}

	private class Test1 {
		private ComponentRepository<ClusterRepoItem> repo1;
		private ComponentRepository<VHostItem> repo2;
	}
}
