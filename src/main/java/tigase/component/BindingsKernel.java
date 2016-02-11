package tigase.component;

import javax.script.SimpleBindings;

import tigase.kernel.core.Kernel;

public class BindingsKernel extends SimpleBindings {

	private Kernel kernel;

	public BindingsKernel(Kernel kernel) {
		this.kernel = kernel;
	}

	public BindingsKernel() {
	}

	@Override
	public boolean containsKey(Object key) {
		boolean v = super.containsKey(key);
		if (!v) {
			v = kernel.isBeanClassRegistered(key.toString());
		}
		return v;
	}

	@Override
	public Object get(Object key) {
		Object v = super.get(key);
		if (v == null && kernel.isBeanClassRegistered(key.toString())) {
			v = kernel.getInstance(key.toString());
		}
		return v;
	}

	public Kernel getKernel() {
		return kernel;
	}

	public void setKernel(Kernel kernel) {
		this.kernel = kernel;
	}
}
