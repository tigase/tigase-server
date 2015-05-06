package tigase.kernel.core;

import java.util.HashSet;

import tigase.kernel.core.Kernel.DelegatedBeanConfig;

public class DependencyGrapher {

	private Kernel kernel;

	public DependencyGrapher() {
	}

	public DependencyGrapher(Kernel krnl) {
		setKernel(krnl);
	}

	private void drawContext(StringBuilder structureSB, HashSet<String> connections, Kernel kernel) {

		final DependencyManager dependencyManager = kernel.getDependencyManager();
		structureSB.append("subgraph ").append(" {\n");

		for (BeanConfig bc : dependencyManager.getBeanConfigs()) {
			if (bc.getClazz().equals(Kernel.class))
				continue;
			structureSB.append('"').append(bc.getKernel().getName() + "." + bc.getBeanName()).append('"').append("[");

			if (bc instanceof DelegatedBeanConfig) {
				structureSB.append("label=\"");
				structureSB.append(bc.getBeanName());
				structureSB.append("\"");
				structureSB.append("shape=oval");
			} else {
				structureSB.append("label=\"{");
				structureSB.append(bc.getBeanName()).append("\\n").append("(").append(bc.getClazz().getName()).append(")");
				structureSB.append("}\"");
			}
			structureSB.append("];\n");
		}
		structureSB.append("}\n");

		int c = 0;
		for (BeanConfig bc : dependencyManager.getBeanConfigs()) {
			++c;
			if (bc.getFactory() != null) {
				BeanConfig dBean = bc.getFactory();
				StringBuilder sbi = new StringBuilder();
				sbi.append('"').append(bc.getKernel().getName() + "." + bc.getBeanName()).append('"');
				// sb.append(':').append(dp.getField().getName());
				sbi.append("->");

				sbi.append('"').append(dBean.getKernel().getName() + "." + dBean.getBeanName()).append('"').append(
						"[style=\"dashed\"]");

				connections.add(sbi.toString());
			}
			for (Dependency dp : bc.getFieldDependencies().values()) {
				BeanConfig[] dBeans = dependencyManager.getBeanConfig(dp);
				for (BeanConfig dBean : dBeans) {
					StringBuilder sbi = new StringBuilder();
					sbi.append('"').append(bc.getKernel().getName() + "." + bc.getBeanName()).append('"');

					if (dBean instanceof DelegatedBeanConfig) {

					} else {
						sbi.append("->");
						if (dBean == null)
							sbi.append("{UNKNOWN_").append(c).append("[label=\"").append(dp).append(
									"\", fillcolor=red, style=filled, shape=box]}");
						else
							sbi.append('"').append(dBean.getKernel().getName() + "." + dBean.getBeanName()).append('"');
					}
					connections.add(sbi.toString());

				}
			}
		}

		for (BeanConfig kc : dependencyManager.getBeanConfigs(Kernel.class)) {
			Kernel ki = kernel.getInstance(kc.getBeanName());
			structureSB.append("subgraph ").append("cluster_").append(ki.hashCode()).append(" {\n");
			structureSB.append("label=").append("\"").append(ki.getName()).append("\"\n");
			if (ki != kernel)
				drawContext(structureSB, connections, ki);
			structureSB.append("}\n");
		}

	}

	public String getDependencyGraph() {
		StringBuilder sb = new StringBuilder();
		sb.append("digraph ").append("Context").append(" {\n");
		sb.append("label=").append("\"").append(kernel.getName()).append("\"\n");
		sb.append("node[shape=record,style=filled,fillcolor=khaki1, color=brown]\n");
		sb.append("edge[color=brown]\n");

		// sb.append("rank=same\n");

		HashSet<String> connections = new HashSet<String>();
		drawContext(sb, connections, kernel);

		for (String string : connections) {
			sb.append(string).append('\n');
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
