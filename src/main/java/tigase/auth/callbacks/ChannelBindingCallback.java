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
package tigase.auth.callbacks;

import tigase.auth.mechanisms.AbstractSaslSCRAM;

import javax.security.auth.callback.Callback;

public class ChannelBindingCallback
		implements Callback, java.io.Serializable {

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
