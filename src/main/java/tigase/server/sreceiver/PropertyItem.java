/*  Tigase Project
 *  Copyright (C) 2001-2007
 *  "Artur Hefczyc" <artur.hefczyc@tigase.org>
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software Foundation,
 *  Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 * $Rev$
 * Last modified by $Author$
 * $Date$
 */
package tigase.server.sreceiver;

import static tigase.server.sreceiver.PropertyConstants.*;

/**
 * Describe class PropertyItem here.
 *
 *
 * Created: Fri May 18 12:14:03 2007
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class PropertyItem {

	private String name = null;
	private String display_name = null;
	private Object value = null;
	private String[] possible_values = null;
	private String description = null;

	/**
	 * Creates a new <code>PropertyItem</code> instance.
	 *
	 */
	public PropertyItem(String name, String display_name, Object def_value,
		String[] possible_values, String description) {
		this.name = name;
		this.display_name = display_name;
		this.value = def_value;
		this.possible_values = possible_values;
		this.description = description;
		if (possible_values == null && value != null) {
			guessPossibleValues();
		} // end of if (possible_values == null && value != null)
	}

	/**
	 * Creates a new <code>PropertyItem</code> instance.
	 *
	 */
	public PropertyItem(String name, Object def_value) {
		this.name = name;
		this.value = def_value;
		if (value != null) {
			guessPossibleValues();
		} // end of if (possible_values == null && value != null)
	}

	/**
	 * Creates a new <code>PropertyItem</code> instance.
	 *
	 */
	public PropertyItem(String name, String display_name, Object def_value) {
		this.name = name;
		this.display_name = display_name;
		this.value = def_value;
		if (value != null) {
			guessPossibleValues();
		} // end of if (possible_values == null && value != null)
	}

	private void guessPossibleValues() {
		if (value instanceof DefaultValues) {
			DefaultValues en_val = (DefaultValues)value;
			possible_values = en_val.strValues();
		} // end of if (value instanceof Enum)
		if (value instanceof Boolean) {
			possible_values = new String[] {"true", "false"};
		} // end of if (value instanceof Boolean)
	}

	/**
	 * Gets the value of name
	 *
	 * @return the value of name
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Sets the value of name
	 *
	 * @param argName Value to assign to this.name
	 */
	public void setName(final String argName) {
		this.name = argName;
	}

	/**
	 * Gets the value of display_name
	 *
	 * @return the value of display_name
	 */
	public String getDisplay_name() {
		return (this.display_name != null ? this.display_name : this.name);
	}

	/**
	 * Sets the value of display_name
	 *
	 * @param argDisplay_name Value to assign to this.display_name
	 */
	public void setDisplay_name(final String argDisplay_name) {
		this.display_name = argDisplay_name;
	}

	/**
	 * Gets the value of the property
	 *
	 * @return the value of value
	 */
	public Object getValue() {
		return this.value;
	}

	/**
	 * Sets the value of the property
	 *
	 * @param argValue Value to assign to this.value
	 */
	public void setValue(final Object argValue) {
		this.value = argValue;
	}

	/**
	 * Gets the value of possible_values
	 *
	 * @return the value of possible_values
	 */
	public String[] getPossible_values() {
		return this.possible_values;
	}

	/**
	 * Sets the value of possible_values
	 *
	 * @param argPossible_values Value to assign to this.possible_values
	 */
	public void setPossible_values(final String[] argPossible_values) {
		this.possible_values = argPossible_values;
	}

	/**
	 * Gets the value of description
	 *
	 * @return the value of description
	 */
	public String getDescription() {
		return this.description;
	}

	/**
	 * Sets the value of description
	 *
	 * @param argDescription Value to assign to this.description
	 */
	public void setDescription(final String argDescription) {
		this.description = argDescription;
	}

	public String toString() {
		return value != null ? value.toString() : null;
	}

} // PropertyItem
