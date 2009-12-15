/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2008 "Artur Hefczyc" <artur.hefczyc@tigase.org>
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

package tigase.conf;

import tigase.db.RepositoryItem;
import tigase.server.Packet;
import tigase.xml.Element;

/**
 * Created: Dec 10, 2009 2:40:26 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class ConfigItem implements RepositoryItem {

	public enum FLAGS {
		INITIAL,
		DEFAULT,
		UPDATED;
	}

	enum TYPE {

	}

	private String clusterNode = null;
	private String compName = null;
	private String nodeName = null;
	private String keyName = null;
	private Object value = null;
  private FLAGS flag = FLAGS.DEFAULT;
	private long lastModificationTime = -1;

	/**
	 * Returns a configuration property key which is constructed in a following way:
	 * <code>
	 * nodeName + "/" + keyName
	 * </code>
	 * @return
	 */
	public String getConfigKey() {
		return (nodeName != null ? nodeName + "/" : "") + keyName;
	}

	/**
	 * Returns a property key which is constructed in a following way:
	 * <code>
	 * keyName
	 * </code>
	 * @return
	 */
	public String getKeyName() {
		return keyName;
	}

	/**
	 * Returns a configuration property value.
	 * @return
	 */
	public Object getConfigVal() {
		return value;
	}

	public String getCompName() {
		return compName;
	}

	public void set(String clusterNode, String compName, String nodeName, String key,
			Object value, FLAGS flag) {
		if (clusterNode != null) {
			this.clusterNode = clusterNode;
		}
		if (compName != null) {
			this.compName = compName;
		}
		if (nodeName != null) {
			this.nodeName = nodeName;
		}
		if (key != null) {
			this.keyName = key;
		}
		if (value != null) {
			this.value = value;
		}
		if (flag != null) {
			this.flag = flag;
		}
	}

	public void set(String compName, String nodeName, String key, Object value,
			FLAGS flag) {
		set(null, compName, nodeName, key, value, flag);
	}

	public void set(String compName, String nodeName, String key, Object value) {
		set(null, compName, nodeName, key, value, null);
	}

	public void setNodeKey(String compName, String nodeKey, Object value) {
		int key_idx = nodeKey.lastIndexOf("/");
		String method_key = nodeKey;
		String method_node = null;
		if (key_idx >= 0) {
			method_key = nodeKey.substring(key_idx + 1);
			method_node = nodeKey.substring(0, key_idx);
		}
		set(compName, method_node, method_key, value);
	}

	public boolean isCompNodeKey(String comp, String node, String key) {
		return isComponent(comp) && isNode(node) && isKey(key);
	}

	/**
	 * Checks if the given node and key are equal to this item nodeName and keyName.
	 * This method call works the same way as following statement:
	 * <code>
	 * isNode(node) && isKey(key)
	 * </code>
	 * @param node
	 * @param key
	 * @return
	 */
	public boolean isNodeKey(String node, String key) {
		return isNode(node) && isKey(key);
	}

	/**
	 * Checks if the given component name is equal to this item compName.
	 * @param comp
	 * @return
	 */
	public boolean isComponent(String comp) {
		if (compName != comp) {
			return compName != null ? compName.equals(comp) : false;
		}
		return true;
	}

	/**
	 * Checks if the given node is equal to this item nodeName
	 * @param node
	 * @return
	 */
	public boolean isNode(String node) {
		if (nodeName != node) {
			// At least one is not null
			return nodeName != null ? nodeName.equals(node) : false;
		}
		return true;
	}

	/**
	 * Checks if the given key is equal to this item keyName.
	 * @param key
	 * @return
	 */
	public boolean isKey(String key) {
		if (keyName != key) {
			return keyName != null ? keyName.equals(key) : false;
		}
		return true;
	}

	@Override
	public void initFromPropertyString(String propString) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public String toPropertyString() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void initFromElement(Element elem) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public Element toElement() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	/**
	 * Returns ConfigItem key which is constructed in a following way:
	 * <code>
	 * compName + "/" + nodeName + "/" + keyName
	 * </code>
	 * @return
	 */
	@Override
	public String getKey() {
		return (compName != null ? compName + "/" : "") +
				(nodeName != null ? nodeName + "/" : "") + keyName;
	}

	@Override
	public void addCommandFields(Packet packet) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void initFromCommand(Packet packet) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

}
