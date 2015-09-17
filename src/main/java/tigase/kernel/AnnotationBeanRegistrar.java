package tigase.kernel;

import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;
import tigase.kernel.core.Kernel;
import tigase.util.ClassUtil;

import java.io.IOException;
import java.util.Set;

public class AnnotationBeanRegistrar {

	@Inject
	private Kernel kernel;

	public void registerBeans() {
		try {
			Set<Class<?>> cls = ClassUtil.getClassesFromClassPath();
			for (Class<?> class1 : cls) {
				Bean bean = class1.getAnnotation(Bean.class);
				if (bean != null) {
					kernel.registerBean(class1);
				}
			}
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
