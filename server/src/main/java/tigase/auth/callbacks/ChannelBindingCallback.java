package tigase.auth.callbacks;

import tigase.auth.mechanisms.AbstractSaslSCRAM;

import javax.security.auth.callback.Callback;

public class ChannelBindingCallback implements Callback, java.io.Serializable {

	private final String prompt;
	private final AbstractSaslSCRAM.BindType requestedBindType;
	private byte[] bindingData;

	public ChannelBindingCallback(String prompt, AbstractSaslSCRAM.BindType requestedBindType) {
		this.prompt = prompt;
		this.requestedBindType = requestedBindType;
	}

	public byte[] getBindingData() {
		return bindingData;
	}

	public void setBindingData(byte[] bindingData) {
		this.bindingData = bindingData;
	}

	public String getPrompt() {
		return prompt;
	}

	public AbstractSaslSCRAM.BindType getRequestedBindType() {
		return requestedBindType;
	}
}
