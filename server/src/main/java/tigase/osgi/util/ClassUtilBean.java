/*
 * ClassUtilBean.java
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
package tigase.osgi.util;

import tigase.osgi.Activator;
import tigase.osgi.ModulesManagerImpl;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by andrzej on 08.09.2016.
 */
public class ClassUtilBean extends tigase.util.ClassUtilBean {

	public ClassUtilBean() {
		classes.addAll(ClassUtil.getClassesFromBundle(Activator.getBundle()));
	}

	@Override
	public Set<Class<?>> getAllClasses() {
		ModulesManagerImpl modulesManager = ModulesManagerImpl.getInstance();
		Set<Class<?>> classes = new HashSet<>(super.getAllClasses());
		classes.addAll(modulesManager.getClasses());
		return classes;
	}
}
