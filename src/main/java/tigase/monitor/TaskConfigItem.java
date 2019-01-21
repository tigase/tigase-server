/**
 * Tigase XMPP Server - The instant messaging server
 * Copyright (C) 2004 Tigase, Inc. (office@tigase.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3 of the License.
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
package tigase.monitor;

import tigase.db.comp.RepositoryItemAbstract;
import tigase.form.Form;
import tigase.util.Base64;
import tigase.xml.Element;

import java.util.logging.Level;
import java.util.logging.Logger;

public class TaskConfigItem
		extends RepositoryItemAbstract
		implements Comparable<TaskConfigItem> {

	public static final String CLASS_ELEM = "class";
	public static final String ELEM_NAME = "monitor-task";
	public static final String SCRIPT_ELEM = "script";
	public static final String SCRIPT_EXT_ATT = "scriptExtension";
	public static final String TASK_CLASS_ATT = "taskClass";
	public static final String TASK_NAME_ATT = "taskName";
	public static final String TASK_TYPE_ATT = "type";
	protected static final String[] TASK_CLASS_PATH = {ELEM_NAME, CLASS_ELEM};
	protected static final String[] TASK_SCRIPT_PATH = {ELEM_NAME, SCRIPT_ELEM};

	private static final Logger log = Logger.getLogger(TaskConfigItem.class.getName());

	public static enum Type {
		scriptTask,
		scriptTimerTask,
		/**
		 * Default task, built with standard java class.
		 */
		task
	}
	private Form configuration;

	private String scriptExtension;

	private Class<? extends MonitorTask> taskClass;

	private String taskName;

	private String taskScript;

	private Type type;

	public TaskConfigItem() {
	}

	public TaskConfigItem(String taskName, Class<? extends MonitorTask> taskClass) {
		this.taskName = taskName;
		this.taskClass = taskClass;
		this.type = Type.task;
	}

	@Override
	public int compareTo(TaskConfigItem o) {
		return taskName.compareTo(o.taskName);
	}

	public Form getConfiguration() {
		return configuration;
	}

	public void setConfiguration(Form configuration) {
		this.configuration = configuration;
	}

	@Override
	public String getElemName() {
		return ELEM_NAME;
	}

	@Override
	public String getKey() {
		return taskName;
	}

	@Override
	protected void setKey(String key) {
		setTaskName(key);
	}

	public String getScriptExtension() {
		return scriptExtension;
	}

	public void setScriptExtension(String scriptExtension) {
		this.scriptExtension = scriptExtension;
	}

	public Class<? extends MonitorTask> getTaskClass() {
		return taskClass;
	}

	@SuppressWarnings("unchecked")
	private void setTaskClass(String tmp) {
		if (tmp == null) {
			return;
		}
		try {
			this.taskClass = (Class<? extends MonitorTask>) Class.forName(tmp);
		} catch (ClassNotFoundException e) {
			log.log(Level.WARNING, "Error creating instance", e);
		}
	}

	public String getTaskName() {
		return taskName;
	}

	public void setTaskName(String taskName) {
		this.taskName = taskName;
	}

	public String getTaskScript() {
		return taskScript;
	}

	public void setTaskScript(String taskScript) {
		this.taskScript = taskScript;
	}

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}

	@Override
	public void initFromElement(Element elem) {
		if (elem.getName() != ELEM_NAME) {
			throw new IllegalArgumentException("Incorrect element name, expected: " + ELEM_NAME);
		}
		super.initFromElement(elem);

		this.taskName = elem.getAttributeStaticStr(TASK_NAME_ATT);
		try {
			this.type = Type.valueOf(elem.getAttributeStaticStr(TASK_TYPE_ATT));
		} catch (Exception e) {
			e.printStackTrace();
		}
		this.scriptExtension = elem.getAttributeStaticStr(SCRIPT_EXT_ATT);

		setTaskClass(elem.getAttributeStaticStr(TASK_CLASS_ATT));
		setTaskScriptEncoded(elem.getCDataStaticStr(TASK_SCRIPT_PATH));

		Element form = elem.getChild("x", "jabber:x:data");
		if (form != null) {
			this.configuration = new Form(form);
		}
	}

	@Override
	public void initFromPropertyString(String propString) {
		// TODO Auto-generated method stub

	}

	public void setTaskClass(Class<? extends MonitorTask> taskClass) {
		this.taskClass = taskClass;
	}

	@Override
	public Element toElement() {
		final Element elem = super.toElement();

		elem.addAttribute(TASK_NAME_ATT, this.taskName);
		elem.addAttribute(TASK_TYPE_ATT, this.type.name());

		if (this.scriptExtension != null) {
			elem.addAttribute(SCRIPT_EXT_ATT, this.scriptExtension);
		}

		if (this.taskScript != null) {
			elem.addChild(new Element(SCRIPT_ELEM, Base64.encode(this.taskScript.getBytes())));
		}

		if (this.taskClass != null) {
			elem.addChild(new Element(CLASS_ELEM, this.taskClass.getName()));
		}

		if (this.configuration != null) {
			elem.addChild(this.configuration.getElement());
		}

		return elem;
	}

	@Override
	public String toPropertyString() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String toString() {
		return "taskName=" + taskName + ", taskClass=" + taskClass + ", type=" + type + ", configuration=" +
				configuration;
	}

	private void setTaskScriptEncoded(String tmp) {
		if (tmp == null) {
			return;
		}
		this.taskScript = new String(Base64.decode(tmp));
	}
}