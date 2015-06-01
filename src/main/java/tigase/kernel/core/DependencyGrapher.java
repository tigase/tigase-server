package tigase.kernel.core;

import tigase.kernel.core.Kernel.DelegatedBeanConfig;

import java.util.HashSet;

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
        for (final BeanConfig bc : dependencyManager.getBeanConfigs()) {
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

            if (bc instanceof DelegatedBeanConfig) {
                final BeanConfig oryginal = ((DelegatedBeanConfig) bc).getOriginal();
                StringBuilder sbi = new StringBuilder();
                sbi.append('"').append(oryginal.getKernel().getName() + "." + oryginal.getBeanName()).append('"');
                sbi.append("->");
                sbi.append('"').append(bc.getKernel().getName() + "." + bc.getBeanName()).append('"').append(
                        "[style=dotted,arrowtail=none,arrowhead=none]");
                connections.add(sbi.toString());
                continue;
            }

            for (Dependency dp : bc.getFieldDependencies().values()) {

                BeanConfig[] dBeans = dependencyManager.getBeanConfig(dp);
                for (BeanConfig dBean : dBeans) {
                    BeanConfig fromBC = bc;
                    StringBuilder sbi = new StringBuilder();
                    if (dBean != null && dBean.getKernel() != bc.getKernel()) {
                        sbi.append("/* inne kernele */ ");
                        fromBC = findDelegateIn(bc, dBean.getKernel().getDependencyManager());
                    }


                    if (dBean == null) {
                        sbi.append('"').append(fromBC.getKernel().getName() + "." + fromBC.getBeanName()).append('"');
                        sbi.append("->");
                        sbi.append("{UNKNOWN_").append(c).append("[label=\"").append(dp).append(
                                "\", fillcolor=red, style=filled, shape=box]}");
                    } else {
                        sbi.append('"').append(fromBC.getKernel().getName() + "." + fromBC.getBeanName()).append('"');
                        sbi.append("->");
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

    private BeanConfig findDelegateIn(BeanConfig dBean, DependencyManager dependencyManager) {
        for (BeanConfig bc : dependencyManager.getBeanConfigs()) {
            if (bc instanceof DelegatedBeanConfig) {
                BeanConfig orig = ((DelegatedBeanConfig) bc).getOriginal();
                if (orig == dBean)
                    return bc;
            }
        }
        return dBean;
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
