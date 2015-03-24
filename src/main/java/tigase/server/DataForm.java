/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2015 "Tigase, Inc." <office@tigase.com>
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3 of the License,
 * or (at your option) any later version.
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
package tigase.server;

import tigase.server.Command.DataType;

import tigase.xml.Element;
import tigase.xml.XMLUtils;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

/**
 *
 * @author Wojciech Kapcia
 */
public class DataForm {

	public static final String FIELD_EL = "field";
	public static final String VALUE_EL = "value";
	protected static final String[] FIELD_VALUE_PATH = { FIELD_EL, VALUE_EL };

	private static final Logger log = Logger.getLogger( DataForm.class.getName() );

	/**
	 * Data form-types as defined in the XEP-0050.
	 */

	public static void addCheckBoxField(final Element el, String f_name, boolean f_value) {
		DataForm.addFieldValue(el, f_name, Boolean.toString(f_value), "boolean");
	}

	public static Element addDataForm( Element el, DataType data_type ) {
		Element x = new Element( "x", new String[] { "xmlns", "type" },
														 new String[] { "jabber:x:data",
																						data_type.name() } );

		el.addChild( x );

		return x;
	}

	public static void addFieldMultiValue( final Element el, final String f_name,
																				 final List<String> f_value ) {
		Element x = el.getChild( "x", "jabber:x:data" );

		if ( x == null ){
			x = addDataForm( el, DataType.result );
		}
		if ( f_value != null ){
			Element field = new Element( FIELD_EL, new String[] { "var", "type" },
																	 new String[] { XMLUtils.escape( f_name ),
																									"text-multi" } );

			for ( String val : f_value ) {
				if ( val != null ){
					Element value = new Element( VALUE_EL, XMLUtils.escape( val ) );

					field.addChild( value );
				}
			}
			x.addChild( field );
		}
	}

	public static void addFieldMultiValue( final Element el, final String f_name,
																				 final Throwable ex ) {
		Element x = el.getChild( "x", "jabber:x:data" );

		if ( x == null ){
			x = addDataForm( el, DataType.submit );
		}

		List<String> f_value = null;

		if ( ex != null ){
			f_value = new ArrayList<String>( 100 );
			f_value.add( ex.getLocalizedMessage() );
			for ( StackTraceElement ste : ex.getStackTrace() ) {
				f_value.add( "  " + ste.toString() );
			}
		}
		if ( f_value != null ){
			Element field = new Element( FIELD_EL, new String[] { "var", "type" },
																	 new String[] { XMLUtils.escape( f_name ),
																									"text-multi" } );

			for ( String val : f_value ) {
				if ( val != null ){
					Element value = new Element( VALUE_EL, XMLUtils.escape( val ) );

					field.addChild( value );
				}
			}
			x.addChild( field );
		}
	}

	public static void addField( final Element el, final String f_name,
																		final String f_label, final String type ) {
		Element x = el.getChild( "x", "jabber:x:data" );

		if ( x == null ){
			x = addDataForm( el, DataType.submit );
		}

		Element field = new Element( FIELD_EL,
																 new String[] { "var", "type", "label" },
																 new String[] { XMLUtils.escape( f_name ), type, f_label } );

		x.addChild( field );
	}

	public static void addFieldValue( final Element el, final String f_name,
																		final String f_value ) {
		Element x = el.getChild( "x", "jabber:x:data" );

		if ( x == null ){
			x = addDataForm( el, DataType.submit );
		}

		Element field = new Element( FIELD_EL,
																 new Element[] {
																	 new Element( VALUE_EL, XMLUtils.escape( f_value ) ) },
																 new String[] { "var" },
																 new String[] { XMLUtils.escape( f_name ) } );

		x.addChild( field );
	}

	public static void addFieldValue( final Element el, final String f_name,
																		final String f_value, final String label,
																		final String[] labels, final String[] options ) {
		Element x = el.getChild( "x", "jabber:x:data" );

		if ( x == null ){
			x = addDataForm( el, DataType.submit );
		}

		Element field = new Element( FIELD_EL,
																 new Element[] {
																	 new Element( VALUE_EL, XMLUtils.escape( f_value ) ) },
																 new String[] { "var", "type", "label" },
																 new String[] { XMLUtils.escape( f_name ), "list-single",
																								XMLUtils.escape( label ) } );

		for ( int i = 0 ; i < labels.length ; i++ ) {
			field.addChild( new Element( "option",
																	 new Element[] {
																		 new Element( VALUE_EL, XMLUtils.escape( options[i] ) ) },
																	 new String[] { "label" },
																	 new String[] { XMLUtils.escape( labels[i] ) } ) );
		}
		x.addChild( field );
	}

	public static void addFieldValue( final Element el, final String f_name,
																		final String[] f_values, final String label,
																		final String[] labels, final String[] options ) {
		Element x = el.getChild( "x", "jabber:x:data" );

		if ( x == null ){
			x = addDataForm( el, DataType.submit );
		}

		Element field = new Element( FIELD_EL, new String[] { "var", "type", "label" },
																 new String[] { XMLUtils.escape( f_name ),
																								"list-multi", XMLUtils.escape( label ) } );

		for ( int i = 0 ; i < labels.length ; i++ ) {
			field.addChild( new Element( "option",
																	 new Element[] {
																		 new Element( VALUE_EL, XMLUtils.escape( options[i] ) ) },
																	 new String[] { "label" },
																	 new String[] { XMLUtils.escape( labels[i] ) } ) );
		}
		for ( int i = 0 ; i < f_values.length ; i++ ) {
			field.addChild( new Element( VALUE_EL, XMLUtils.escape( f_values[i] ) ) );
		}
		x.addChild( field );
	}

	public static void addFieldValue( final Element el, final String f_name,
																		final String f_value, final String label,
																		final String[] labels, final String[] options,
																		final String type ) {
		Element x = el.getChild( "x", "jabber:x:data" );

		if ( x == null ){
			x = addDataForm( el, DataType.submit );
		}

		Element field = new Element( FIELD_EL,
																 new Element[] {
																	 new Element( VALUE_EL, XMLUtils.escape( f_value ) ) },
																 new String[] { "var", "type", "label" },
																 new String[] { XMLUtils.escape( f_name ), type,
																								XMLUtils.escape( label ) } );

		for ( int i = 0 ; i < labels.length ; i++ ) {
			field.addChild( new Element( "option",
																	 new Element[] {
																		 new Element( VALUE_EL, XMLUtils.escape( options[i] ) ) },
																	 new String[] { "label" },
																	 new String[] { XMLUtils.escape( labels[i] ) } ) );
		}
		x.addChild( field );
	}

	public static void addFieldValue( final Element el, final String f_name,
																		final String f_value, final String type ) {
		Element x = el.getChild( "x", "jabber:x:data" );

		if ( x == null ){
			x = addDataForm( el, DataType.submit );
		}

		Element field = new Element( FIELD_EL,
																 new Element[] {
																	 new Element( VALUE_EL, XMLUtils.escape( f_value ) ) },
																 new String[] { "var", "type" },
																 new String[] { XMLUtils.escape( f_name ), type } );

		x.addChild( field );
	}

	public static void addFieldValue( final Element el, final String f_name,
																		final String f_value, final String type,
																		final String label ) {
		Element x = el.getChild( "x", "jabber:x:data" );

		if ( x == null ){
			x = addDataForm( el, DataType.submit );
		}

		Element field = new Element( FIELD_EL,
																 new Element[] {
																	 new Element( VALUE_EL, XMLUtils.escape( f_value ) ) },
																 new String[] { "var", "type", "label" },
																 new String[] { XMLUtils.escape( f_name ), type, XMLUtils.escape( label ) } );

		x.addChild( field );
	}

	public static void addHiddenField( final Element el, String f_name, String f_value ) {
		addFieldValue( el, f_name, f_value, "hidden" );
	}

	public static void addInstructions( final Element el, final String instructions ) {
		Element x = el.getChild( "x", "jabber:x:data" );

		if ( x == null ){
			x = addDataForm( el, DataType.submit );
		}
		x.addChild( new Element( "instructions", instructions ) );
	}


	public static void addTextField( final Element el, String f_name, String f_value ) {
		addFieldValue( el, f_name, f_value, "fixed" );
	}

	public static void addTitle( final Element el, final String title ) {
		Element x = el.getChild( "x", "jabber:x:data" );

		if ( x == null ){
			x = addDataForm( el, DataType.submit );
		}
		x.addChild( new Element( "title", title ) );
	}

	public static String getFieldValue( final Element el, String f_name ) {
		Element x = el.getChild( "x", "jabber:x:data" );

		if ( x != null ){
			List<Element> children = x.getChildren();

			if ( children != null ){
				for ( Element child : children ) {
					if ( child.getName().equals( FIELD_EL )
							 && child.getAttributeStaticStr( "var" ).equals( f_name ) ){
						String value = child.getChildCDataStaticStr( FIELD_VALUE_PATH );

						if ( value != null ){
							return XMLUtils.unescape( value );
						}
					}
				}
			}
		}

		return null;
	}

	public static String[] getFieldValues( final Element el, final String f_name ) {
		Element x = el.getChild( "x", "jabber:x:data" );

		if ( x != null ){
			List<Element> children = x.getChildren();

			if ( children != null ){
				for ( Element child : children ) {
					if ( child.getName().equals( FIELD_EL )
							 && child.getAttributeStaticStr( "var" ).equals( f_name ) ){
						List<String> values = new LinkedList<String>();
						List<Element> val_children = child.getChildren();

						if ( val_children != null ){
							for ( Element val_child : val_children ) {
								if ( val_child.getName().equals( VALUE_EL ) ){
									String value = val_child.getCData();

									if ( value != null ){
										values.add( XMLUtils.unescape( value ) );
									}
								}
							}
						}

						return values.toArray( new String[ 0 ] );
					}
				}
			}
		}

		return null;
	}

	public static boolean removeFieldValue( final Element el, final String f_name ) {
		Element x = el.getChild( "x", "jabber:x:data" );

		if ( x != null ){
			List<Element> children = x.getChildren();

			if ( children != null ){
				for ( Element child : children ) {
					if ( child.getName().equals( FIELD_EL )
							 && child.getAttributeStaticStr( "var" ).equals( f_name ) ){
						return x.removeChild( child );
					}
				}
			}
		}

		return false;
	}

	public static String getFieldKeyStartingWith( final Element el, String f_name ) {
		Element x = el.getChild( "x", "jabber:x:data" );

		if ( x != null ){
			List<Element> children = x.getChildren();

			if ( children != null ){
				for ( Element child : children ) {
					if ( child.getName().equals( FIELD_EL )
							 && child.getAttributeStaticStr( "var" ).startsWith( f_name ) ){
						return child.getAttributeStaticStr( "var" );
					}
				}
			}
		}

		return null;
	}

}
