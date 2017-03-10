/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2012 "Artur Hefczyc" <artur.hefczyc@tigase.org>
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
package tigase.server;

import java.util.HashMap;
import tigase.cluster.api.ClusteredComponentIfc;
import tigase.xml.Element;

/**
 * Helper class for storing and handling additional informations about components
 *
 * @author Wojciech Kapcia
 */
public class ComponentInfo {
	private String name = null;
	private String title;
	private String version;
	private String cls;
	private HashMap<String, Object> cmpData;

	/**
	 * Creates ComponentInfo object with initial data
	 *
	 * @param cmpTitle title of the component
	 * @param cmpVersion version of the component
	 * @param cmpCls class of the component
	 */
	public ComponentInfo( String cmpTitle, String cmpVersion, String cmpCls ) {
		this( null, cmpTitle, cmpVersion, cmpCls);
	}

	/**
	 * Creates ComponentInfo object with initial data
	 *
	 * @param cmpName name of the component
	 * @param cmpTitle title of the component
	 * @param cmpVersion version of the component
	 * @param cmpCls class of the component
	 */
	public ComponentInfo( String cmpName, String cmpTitle, String cmpVersion, String cmpCls ) {
		this(null, cmpTitle, cmpVersion, cmpCls, new HashMap<String, Object>() );
	}

	/**
	 * Creates ComponentInfo object with initial data
	 *
	 * @param cmpName name of the component
	 * @param cmpTitle title of the component
	 * @param cmpVersion version of the component
	 * @param cmpCls class of the component
	 * @param cmpData additional information about component
	 */
	public ComponentInfo( String cmpName, String cmpTitle, String cmpVersion, String cmpCls, HashMap<String, Object> cmpData ) {
		this.name = cmpName;
		this.title = cmpTitle;
		this.version = cmpVersion;
		this.cls = cmpCls;
		this.cmpData = cmpData;
	}

	/**
	 * Creates ComponentInfo object with initial data
	 *
	 * @param c class of the component
	 */
	public ComponentInfo( Class<?> c ) {
		this( null, c);
	}

	/**
	 * Creates ComponentInfo object with initial data
	 *
	 * @param cmpName name of the component
	 * @param c class of the component
	 */
	public ComponentInfo(String cmpName, Class<?> c ) {
		this.name = cmpName;
		this.title = getImplementationTitle( c );
		this.version = getImplementationVersion( c );
		this.cls = c.getName();
		this.cmpData = new HashMap<>();
	}

	/**
	 * Allows retrieving of component's name
	 *
	 * @return component name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Allows retrieving of component's title
	 *
	 * @return component title
	 */
	public String getComponentTitle() {
		return title;
	}

	/**
	 * Allows retrieving of component's version
	 *
	 * @return component version
	 */
	public String getComponentVersion() {
		return version;
	}

	/**
	 * Allows retrieving of component's class
	 *
	 * @return component class
	 */
	public String getComponentClass() {
		return cls;
	}

	/**
	 * Allows retrieving of component's additional data
	 *
	 * @return  component additional data
	 */
	public HashMap<String, Object> getComponentData() {
		return cmpData;
	}

	@Override
	public String toString() {
		return (name == null ? "" : name + " :: ") + "componentInfo{"
					 + (title.isEmpty() ? "" : "Title=" + title + ", ")
					 + (version.isEmpty() ? "" : "Version=" + version + ", ")
					 + "Class=" + cls
					 +  ( cmpData.isEmpty() ? "" : ", componentData=" + cmpData) + '}';
	}

	/**
	 * Allows retrieving of component's information as Element
	 *
	 * @return component information as Element
	 */
	public Element toElement() {
		Element cmpInfo = new Element( "cmpInfo" );
		if ( name != null ){
			cmpInfo.addChild(new Element( "name", name ) );
		}
		if ( !title.isEmpty() ){
			cmpInfo.addChild( new Element( "title", title ) );
		}
		if ( !version.isEmpty() ){
			cmpInfo.addChild( new Element( "version", version ) );
		}
		if ( !cls.isEmpty() ){
			cmpInfo.addChild( new Element( "class", cls ) );
		}
		if ( !cmpData.isEmpty() ){
			Element data = new Element( "cmpData" );
			for ( String key : cmpData.keySet() ) {
				data.addChild( new Element( key, cmpData.get( key ).toString() ) );
			}
			cmpInfo.addChild( data );
		}
		return cmpInfo;
	}

	/**
	 * Allows retrieving implementation package (obtained from jar package)
	 * for a given class
	 *
	 * @param c class for which package is to be retrieved
	 * @return package containing given class
	 */
	public static Package getImplementation( Class<?> c ) {
		return c.getPackage() == null ? XMPPServer.class.getPackage() : c.getPackage();
	}

	/**
	 * Allows retrieving implementation version (obtained from jar package)
	 * for a given class
	 *
	 * @param c class for which Package version is to be retrieved
	 * @return Package version of the given class
	 */
	public static String getImplementationVersion( Class<?> c ) {
		Package p = getImplementation( c );

		String version = p == null ? null : p.getImplementationVersion();

		if (ClusteredComponentIfc.class.isAssignableFrom(c)) {
			Class<?> superClass = c.getSuperclass();
			Package superPackage = getImplementation( superClass );
			if (p != superPackage && superPackage != null && !p.equals(superPackage)) {
				String superVersion  = superPackage.getImplementationVersion();
				if (superVersion != null && version != null && !version.equals(superVersion)) {
					version += "-" + superVersion;
				}
			}
		}
		
		return ( version == null ) ? "" : version;
	}

	/**
	 * Allows retrieving implementation title (obtained from jar package)
	 * for a given class
	 *
	 * @param c class for which Package title is to be retrieved
	 * @return Package title of the given class
	 */
	public static String getImplementationTitle( Class<?> c ) {
		Package p = getImplementation( c );

		String title = p == null ? null : p.getImplementationTitle();

		return title == null ? "" : title;
	}

	/**
	 * Allows retrieving implementation information (obtained from jar package)
	 * for a given class
	 *
	 * @param c class for which Package information is to be retrieved
	 * @return title and version of the Package holding class
	 */
	public static String getImplementationInfo( Class<?> c ) {
		return getImplementationTitle( c ) + ", version: " + getImplementationVersion( c );
	}
}
