/*
 * PlantUMLGrapher.java
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2016 "Tigase, Inc." <office@tigase.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
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

package tigase.kernel.core;

public class PlantUMLGrapher {

	private Kernel kernel;

	private static String n(BeanConfig beanConfig) {
		Kernel bk = beanConfig.getKernel();
		String kernelName = bk.getName();
		String beanName = beanConfig.getBeanName();
		return (kernelName + "." + beanName).replace("#", "_").replace("<","_").replace(">","_");
	}

	public PlantUMLGrapher() {
	}

	public PlantUMLGrapher(Kernel krnl) {
		setKernel(krnl);
	}

	public String getDependencyGraph() {
		StringBuilder sb = new StringBuilder();
		sb.append("@startuml").append('\n');

		sb.append(makePackage(kernel));

		sb.append("@enduml").append('\n');
		return sb.toString();
	}

	public Kernel getKernel() {
		return kernel;
	}

	public void setKernel(Kernel kernel) {
		this.kernel = kernel;
	}

	private StringBuilder makeObject(BeanConfig bc) {
		StringBuilder sb = new StringBuilder();
		sb.append("object ")
				.append("\"")
				.append(bc.getBeanName())
				.append("\" as ")
				.append(n(bc));
		if (true) {
			sb.append("{\n");
			for (Dependency d : bc.getFieldDependencies().values()) {
				sb.append(d.getField().getName());
				sb.append('\n');
			}

			sb.append("}");
		}
		sb.append('\n');

//		sb.append("note bottom of [")
//				.append(n(bc.getKernel().getName(), bc.getBeanName()))
//				.append("]\n")
//				.append("State: ")
//				.append(bc.getState())
//				.append('\n')
//				.append("end note\n");

		return sb;
	}

	private StringBuilder makePackage(Kernel k) {
		StringBuilder sb = new StringBuilder();
		sb.append("package ").append(k.getName()).append(" {\n");

		for (BeanConfig bc : k.getDependencyManager().getBeanConfigs()) {

			if (Kernel.class.isAssignableFrom(bc.getClazz())) {
				Kernel sk = k.getInstance(bc);
				if (sk != k) {
					sb.append(makePackage(sk));
				}
			}

			sb.append(makeObject(bc));

			for (Dependency dp : bc.getFieldDependencies().values()) {
				BeanConfig[] dBeans = k.getDependencyManager().getBeanConfig(dp);

				for (BeanConfig dBean : dBeans) {
					if(dBean!=null)
					sb.append(n(dBean))
							.append(" *- ")
							.append(n(bc))
							.append('\n');
				}

			}

			if (bc instanceof Kernel.DelegatedBeanConfig) {
				final BeanConfig orginal = ((Kernel.DelegatedBeanConfig) bc).getOriginal();
				sb.append(n(orginal)).
				append(" .. ")
				.append(n(bc)).append('\n');

			}

			}

		sb.append("}\n");
		return sb;
	}
}
