/*
 * BeanSelector.java
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
package tigase.kernel.beans;

import tigase.kernel.core.Kernel;

/**
 * Interface used by bean configurators to detect is additional beans should be registered
 *
 * Created by andrzej on 10.03.2016.
 */
public interface BeanSelector {

	/**
	 * Method needs to return true if bean in which annotation class implementing this interface is specified
	 * and this bean should be registered
	 *
	 * @param kernel
	 * @return
	 */
	boolean shouldRegister(Kernel kernel);

	class Always implements BeanSelector {

		@Override
		public boolean shouldRegister(Kernel kernel) {
			return true;
		}
	}

	class NonClusterMode implements BeanSelector {

		@Override
		public boolean shouldRegister(Kernel kernel) {
			return !Boolean.valueOf(System.getProperty("cluster-mode", "false"));
		}
	}

	class ClusterMode implements BeanSelector {

		@Override
		public boolean shouldRegister(Kernel kernel) {
			return Boolean.valueOf(System.getProperty("cluster-mode", "false"));
		}
	}
}
