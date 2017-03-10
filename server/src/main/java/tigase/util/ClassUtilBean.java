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
package tigase.util;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by andrzej on 08.09.2016.
 */
public class ClassUtilBean {

	private static Logger log = Logger.getLogger(ClassUtilBean.class.getCanonicalName());

	private static ClassUtilBean instance;
	protected HashSet<Class<?>> classes = new HashSet<>();

	public ClassUtilBean() {
		try {
			classes.addAll(ClassUtil.getClassesFromClassPath());
		} catch (IOException|ClassNotFoundException e) {
			log.log(Level.SEVERE, "Could not initialize list of classes", e);
		}
		instance = this;
	}

	public Set<Class<?>> getAllClasses() {
		return Collections.unmodifiableSet(classes);
	}

	public static ClassUtilBean getInstance() {
		if (instance == null) {
			instance = new ClassUtilBean();
		}
		return instance;
	}
}
