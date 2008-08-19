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
package tigase.cluster.methodcalls;

import java.util.List;

import tigase.cluster.MethodCall;
import tigase.criteria.Criteria;
import tigase.cluster.MethodCallCriteria;
import tigase.cluster.ClusterMethods;
import tigase.xml.Element;

/**
 * Describe class SessionTransferMC here.
 *
 *
 * Created: Tue Aug 19 12:51:27 2008
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class SessionTransferMC implements MethodCall {

	private Criteria criteria = null;

	/**
	 * Creates a new <code>SessionTransferMC</code> instance.
	 *
	 */
	public SessionTransferMC() {
		criteria = new MethodCallCriteria(ClusterMethods.SESSION_TRANSFER.toString());
	}

	public Criteria getModuleCriteria() {
		return criteria;
	}

	public List<Element> process(final Element element) {
		return null;
	}

}
