package tigase.eventbus;

import tigase.xml.Element;
import tigase.xmpp.JID;

public class Event1 {

	private String v1;

	private int v2;

	private transient String transientField;

	private JID jid;

	private String emptyField;

	private Element elementField;
	
	private String[] strArrField;

	public Element getElementField() {
		return elementField;
	}

	public void setElementField(Element elementField) {
		this.elementField = elementField;
	}

	public String getEmptyField() {
		return emptyField;
	}

	public void setEmptyField(String emptyField) {
		this.emptyField = emptyField;
	}

	public JID getJid() {
		return jid;
	}

	public void setJid(JID jid) {
		this.jid = jid;
	}

	public String getTransientField() {
		return transientField;
	}

	public void setTransientField(String transientField) {
		this.transientField = transientField;
	}

	public String getV1() {
		return v1;
	}

	public void setV1(String v1) {
		this.v1 = v1;
	}

	public int getV2() {
		return v2;
	}

	public void setV2(int v2) {
		this.v2 = v2;
	}

	public String[] getStrArrField() {
		return this.strArrField;
	}
	
	public void setStrArrField(String[] v) {
		this.strArrField = v;
	}
	
}
