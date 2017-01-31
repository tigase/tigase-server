package tigase.monitor;

import tigase.db.comp.RepositoryItemAbstract;
import tigase.form.Form;
import tigase.util.Base64;
import tigase.xml.Element;

public class TaskConfigItem extends RepositoryItemAbstract implements Comparable<TaskConfigItem> {

	@Override
	public String toString() {
		return "taskName=" + taskName
					 + ", taskClass=" + taskClass
					 + ", type=" + type
					 + ", configuration=" + configuration;
	}

	public static enum Type {
		scriptTask,
		scriptTimerTask,
		/**
		 * Default task, built with standard java class.
		 */
		task
	}

	public static final String CLASS_ELEM = "class";

	public static final String ELEM_NAME = "monitor-task";

	public static final String SCRIPT_ELEM = "script";

	public static final String SCRIPT_EXT_ATT = "scriptExtension";

	public static final String TASK_CLASS_ATT = "taskClass";

	protected static final String[] TASK_CLASS_PATH = { ELEM_NAME, CLASS_ELEM };

	public static final String TASK_NAME_ATT = "taskName";

	protected static final String[] TASK_SCRIPT_PATH = { ELEM_NAME, SCRIPT_ELEM };

	public static final String TASK_TYPE_ATT = "type";

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

	@Override
	public String getElemName() {
		return ELEM_NAME;
	}

	@Override
	public String getKey() {
		return taskName;
	}

	public String getScriptExtension() {
		return scriptExtension;
	}

	public Class<? extends MonitorTask> getTaskClass() {
		return taskClass;
	}

	public String getTaskName() {
		return taskName;
	}

	public String getTaskScript() {
		return taskScript;
	}

	public Type getType() {
		return type;
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

	public void setConfiguration(Form configuration) {
		this.configuration = configuration;
	}

	public void setScriptExtension(String scriptExtension) {
		this.scriptExtension = scriptExtension;
	}

	public void setTaskClass(Class<? extends MonitorTask> taskClass) {
		this.taskClass = taskClass;
	}

	@SuppressWarnings("unchecked")
	private void setTaskClass(String tmp) {
		if (tmp == null)
			return;
		try {
			this.taskClass = (Class<? extends MonitorTask>) Class.forName(tmp);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	public void setTaskName(String taskName) {
		this.taskName = taskName;
	}

	public void setTaskScript(String taskScript) {
		this.taskScript = taskScript;
	}

	private void setTaskScriptEncoded(String tmp) {
		if (tmp == null)
			return;
		this.taskScript = new String(Base64.decode(tmp));
	}

	public void setType(Type type) {
		this.type = type;
	}

	@Override
	public Element toElement() {
		final Element elem = super.toElement();

		elem.addAttribute(TASK_NAME_ATT, this.taskName);
		elem.addAttribute(TASK_TYPE_ATT, this.type.name());

		if (this.scriptExtension != null)
			elem.addAttribute(SCRIPT_EXT_ATT, this.scriptExtension);

		if (this.taskScript != null)
			elem.addChild(new Element(SCRIPT_ELEM, Base64.encode(this.taskScript.getBytes())));

		if (this.taskClass != null)
			elem.addChild(new Element(CLASS_ELEM, this.taskClass.getName()));

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
}