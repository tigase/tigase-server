/*
 * RosterFactory.java
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2013 "Tigase, Inc." <office@tigase.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License,
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
 *
 */
package tigase.xmpp.impl.roster;

/**
 * {@link RosterFactory} is an factory that is responsible for creation
 * appropriate instance of {@link RosterAbstract} class
 *
 * Created: Thu Sep 4 18:33:11 2008
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public abstract class RosterFactory {

	/** Key used to configure class name holding roster implementation */
	public static final String ROSTER_IMPL_PROP_KEY = "roster-implementation";
	/** Default roster implementation class - {@link RosterFactory} */
	public static final String ROSTER_IMPL_PROP_VAL = RosterFlat.class
			.getCanonicalName();
	/** Holds shared implementation of {@link RosterAbstract} */
	private static RosterAbstract shared = null;

	//~--- get methods ----------------------------------------------------------
	/**
	 * Creates new instance of class implementing {@link RosterAbstract} - either
	 * default one ({@link RosterFlat}) or the one configured with
	 * <em>"roster-implementation"</em> property.
	 *
	 * @param shared_impl determines whether to returns shared or non shared
	 *                    implementation
	 *
	 * @return new instance of class implementing {@link RosterAbstract}
	 */
	public static RosterAbstract getRosterImplementation( boolean shared_impl ) {
		try {
			String cls_name = System.getProperty( ROSTER_IMPL_PROP_KEY, ROSTER_IMPL_PROP_VAL );

			return getRosterImplementation( cls_name, shared_impl );
		} catch ( ClassNotFoundException | InstantiationException | IllegalAccessException e ) {
			return null;
		}
	}

	/**
	 * Creates new instance of class implementing {@link RosterAbstract} from
	 * provided
	 * <code>class_name</code>
	 *
	 * @param class_name  full qualified name of the class
	 * @param shared_impl determines whether to returns shared or non shared
	 *                    implementation
	 *
	 * @return new instance of class implementing {@link RosterAbstract}
	 *
	 * @throws ClassNotFoundException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 */
	public static RosterAbstract getRosterImplementation( String class_name,
																												boolean shared_impl )
			throws ClassNotFoundException, InstantiationException, IllegalAccessException {
		if ( shared_impl ){
			if ( shared == null ){
				shared = (RosterAbstract) Class.forName( class_name ).newInstance();
			}
			return shared;
		}

		return (RosterAbstract) Class.forName( class_name ).newInstance();
	}
}

//~ Formatted in Tigase Code Convention on 13/04/24
