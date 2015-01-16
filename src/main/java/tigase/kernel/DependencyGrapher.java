package tigase.kernel;

public class DependencyGrapher {

	private Kernel kernel;

	public DependencyGrapher() {
	}

	public DependencyGrapher(Kernel krnl) {
		setKernel(krnl);
	}

	public String getDependencyGraph() {
		StringBuilder sb = new StringBuilder();
		sb.append("digraph g{\n");

		for (BeanConfig bc : kernel.getDependencyManager().getBeanConfigs()) {
			sb.append(bc.getBeanName()).append("[");

			sb.append("label=<");
			sb.append(bc.getBeanName()).append("<br/>").append("(").append(bc.getClazz().getName()).append(")");

			sb.append(">");

			sb.append("];\n");
		}

		int c = 0;
		for (BeanConfig bc : kernel.getDependencyManager().getBeanConfigs()) {
			++c;
			for (Dependency dp : bc.getFieldDependencies().values()) {
				BeanConfig[] dBeans = kernel.getDependencyManager().getBeanConfig(dp);
				for (BeanConfig dBean : dBeans) {

					sb.append(bc.getBeanName()).append(':').append(dp.getField().getName());
					sb.append("->");
					if (dBean == null)
						sb.append("{UNKNOWN_").append(c).append("[label=\"").append(dp).append(
								"\", fillcolor=red, style=filled, shape=box]}");
					else
						sb.append(dBean.getBeanName());
					sb.append('\n');
				}
			}
		}

		sb.append("}\n");
		return sb.toString();
	}

	public Kernel getKernel() {
		return kernel;
	}

	public void setKernel(Kernel kernel) {
		this.kernel = kernel;
	}

}
