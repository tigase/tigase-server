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
package tigase.component.adhoc;

import tigase.xml.Element;
import tigase.xmpp.Authorization;

public class AdHocCommandException
		extends Exception {

	private static final long serialVersionUID = 1L;

	private Authorization errorCondition;

	private Element item;

	private String message;

	private String xmlns = "urn:ietf:params:xml:ns:xmpp-stanzas";

	public AdHocCommandException(final Authorization errorCondition) {
		this(null, errorCondition, (String) null);
	}

	public AdHocCommandException(final Authorization errorCondition, String message) {
		this(null, errorCondition, message);
	}

	public AdHocCommandException(final Element item, final Authorization errorCondition) {
		this(item, errorCondition, (String) null);
	}

	public AdHocCommandException(final Element item, final Authorization errorCondition, final String message) {
		this.item = item;
		this.errorCondition = errorCondition;
		this.message = message;
	}

	/**
	 * @return Returns the code.
	 */
	public String getCode() {
		return String.valueOf(this.errorCondition.getErrorCode());
	}

	public Authorization getErrorCondition() {
		return errorCondition;
	}

	/**
	 * @return Returns the item.
	 */
	public Element getItem() {
		return item;
	}

	@Override
	public String getMessage() {
		return message;
	}

	/**
	 * @return Returns the name.
	 */
	public String getName() {
		return errorCondition.getCondition();
	}

	/**
	 * @return Returns the type.
	 */
	public String getType() {
		return errorCondition.getErrorType();
	}

	public Element makeElement() {
		return makeElement(true);
	}

	public Element makeElement(boolean insertOriginal) {
		Element answer = insertOriginal ? item.clone() : new Element(item.getName());

		answer.addAttribute("id", item.getAttributeStaticStr("id"));
		answer.addAttribute("type", "error");
		answer.addAttribute("to", item.getAttributeStaticStr("from"));
		answer.addAttribute("from", item.getAttributeStaticStr("to"));
		if (this.message != null) {
			Element text = new Element("text", this.message, new String[]{"xmlns"},
									   new String[]{"urn:ietf:params:xml:ns:xmpp-stanzas"});

			answer.addChild(text);
		}
		answer.addChild(makeErrorElement());

		return answer;
	}

	public Element makeElement(Element sourceElement) {
		this.item = sourceElement;

		return makeElement(true);
	}

	public Element makeErrorElement() {
		Element error = new Element("error");

		error.setAttribute("code", String.valueOf(this.errorCondition.getErrorCode()));
		error.setAttribute("type", this.errorCondition.getErrorType());
		error.addChild(new Element(this.errorCondition.getCondition(), new String[]{"xmlns"}, new String[]{xmlns}));

		return error;
	}
}
