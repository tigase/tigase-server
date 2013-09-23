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

/**
 *
 * @author Wojciech Kapcia <wojciech.kapcia@tigase.org>
 */
public class ComponentInfo {
	private String name = null;
	private String title;
	private String version;
	private String cls;
	private HashMap<String, Object> cmpData;

	public ComponentInfo( String cmpTitle, String cmpVersion, String cmpCls ) {
		this( null, cmpTitle, cmpVersion, cmpCls);
	}

	public ComponentInfo( String cmpName, String cmpTitle, String cmpVersion, String cmpCls ) {
		this(null, cmpTitle, cmpVersion, cmpCls, new HashMap<String, Object>() );
	}

	public ComponentInfo( String cmpName, String cmpTitle, String cmpVersion, String cmpCls, HashMap<String, Object> cmpData ) {
		this.name = cmpName;
		this.title = cmpTitle;
		this.version = cmpVersion;
		this.cls = cmpCls;
		this.cmpData = cmpData;
	}

	public ComponentInfo( Class<?> c ) {
		this( null, c);
	}

	public ComponentInfo(String cmpName, Class<?> c ) {
		this.name = cmpName;
		this.title = getImplementationTitle( c );
		this.version = getImplementationVersion( c );
		this.cls = c.getName();
		this.cmpData = new HashMap<>();
	}

	public String getName() {
		return name;
	}

	public String getComponentTitle() {
		return title;
	}

	public String getComponentVersion() {
		return version;
	}

	public String getComponentClass() {
		return cls;
	}

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

	public static Package getImplementation( Class<?> c ) {
		return c.getPackage() == null ? XMPPServer.class.getPackage() : c.getPackage();
	}

	public static String getImplementationVersion( Class<?> c ) {
		Package p = getImplementation( c );

		String version = p == null ? null : p.getImplementationVersion();

		return ( version == null ) ? "" : version;
	}

	public static String getImplementationTitle( Class<?> c ) {
		Package p = getImplementation( c );

		String title = p == null ? null : p.getImplementationTitle();

		return title == null ? "" : title;
	}

	public static String getImplementationInfo( Class<?> c ) {
		return getImplementationTitle( c ) + ", version: " + getImplementationVersion( c );
	}
}
