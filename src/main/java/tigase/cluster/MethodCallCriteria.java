/*  Tigase Jabber/XMPP Server
 *  Copyright (C) 2004-2008 "Artur Hefczyc" <artur.hefczyc@tigase.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 *
 * $Rev$
 * Last modified by $Author$
 * $Date$
 */
package tigase.cluster;

import java.util.ArrayList;
import java.util.List;

import tigase.criteria.Criteria;
import tigase.criteria.ElementCriteria;
import tigase.xml.Element;

/**
 * Describe class MethodCallCriteria here.
 *
 *
 * Created: Sat Aug 16 17:25:43 2008
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class MethodCallCriteria extends ElementCriteria {

	public MethodCallCriteria(String methodName) {
		super(ClusterElement.CLUSTER_EL_NAME, (String)null,
			new String[] {"xmlns"}, new String[] {ClusterElement.XMLNS});
		add(ElementCriteria.name(ClusterElement.CLUSTER_CONTROL_EL_NAME)).add(
			ElementCriteria.name(ClusterElement.CLUSTER_METHOD_EL_NAME,
				new String[] {ClusterElement.CLUSTER_NAME_ATTR},
				new String[] {methodName}));
	}

}
