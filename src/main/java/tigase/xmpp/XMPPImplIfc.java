/*
 * XMPPImplIfc.java
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



package tigase.xmpp;

//~--- non-JDK imports --------------------------------------------------------

import tigase.db.TigaseDBException;

import tigase.server.Packet;

import tigase.stats.StatisticsList;

import tigase.xml.Element;

//~--- JDK imports ------------------------------------------------------------

import java.util.Map;
import java.util.Set;
import tigase.server.ComponentInfo;
import tigase.server.ServerComponent;

/**
 * This is a base interface for all session manager plugins. There are packet
 * processing plugins, pre-processing, post-processing and packet filters. They all
 * have a common basic methods which are defined here.
 *
 *
 * Created: Sat Oct 14 16:11:22 2006
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public interface XMPPImplIfc
				extends Comparable<XMPPImplIfc> {
	/** Field description */
	public static final String CLIENT_XMLNS = "jabber:client";

	//~--- methods --------------------------------------------------------------

	/**
	 * By default the method uses {@link #supElementNamePaths() } and
	 * {@link #supTypes() } method results to determine whether the plugin would process
	 * given packet. However, a plugin can implement own logic to determine packet
	 * processing capabilities or conditions. Please note, this method must be very fast
	 * and efficient. No I/O processing is recommended as it may impact performance of
	 * the whole system.
	 *
	 *
	 * @param packet is a <code>Packet</code> for processing.
	 * @param conn is a user session object or null.
	 *
	 * @return returns <code>Authorization</code> enum value or null. Null means the
	 * plugin is simply not processing the packet. {@link Authorization#AUTHORIZED} means
	 * the plugin can process the packet, any other {@link Authorization} enum value
	 * means an error which has to be returned to the sender.
	 */
	Authorization canHandle(Packet packet, XMPPResourceConnection conn);

	/**
	 * Methods returns a preferable number of threads/packets queues for the plugin.
	 * This number can be overwritten through configuration settings, however, a default
	 * value should be reasonably good for most standard installations.
	 * It is recommended to assign at least as much as twice a number of CPUs cores for I/O
	 * bound processing and number a number equal to CPUs cores for fast processing not
	 * slowed down by any I/O.
	 *
	 *
	 * @return an integer preferred number of processing threads for the plugin.
	 */
	default int concurrentQueuesNo() {
		return Runtime.getRuntime().availableProcessors();
	}

	/**
	 * Number of threads per single packets queue. Not used anymore. At the moment only
	 * one thread is always used for each queue. For a higher concurrency set more queues
	 * using {@link #concurrentQueuesNo() }.
	 *
	 *
	 *
	 *
	 * @return a value of <code>int</code>
	 */
	@Deprecated
	int concurrentThreadsPerQueue();

	/**
	 * Method <code>id</code> returns a unique ID of the plugin. Each plugin has
	 * own, unique ID which is used in the configuration file to determine whether
	 * it needs to be loaded or not. In most cases the ID can be equal to XMLNS of
	 * the packages processed by the plugin.
	 *
	 * @return a <code>String</code> value
	 */
	String id();

	/**
	 * Method <code>init</code> is called just after the plugin has been loaded
	 * into memory. The idea behind this is to allow it to initialize or check the
	 * database. This might be especially useful for plugins which want to have a
	 * database access via non-standard stored procedures or need schema upgrade.
	 *
	 * @param settings
	 *          is a Map with initial processor settings from the configuration
	 *          file.
	 * @throws TigaseDBException
	 */
	void init(Map<String, Object> settings) throws TigaseDBException;

	/**
	 * Method <code>supDiscoFeatures</code> returns an array of XML
	 * <code>Element</code>s with service discovery features which have to be
	 * returned to the client uppon request. Service discovery features returned
	 * by this method correspond to services supported by this plugin.
	 *
	 * @param session
	 *          a <code>XMPPResourceConnection</code> value
	 * @return an <code>Element[]</code> value
	 */
	Element[] supDiscoFeatures(XMPPResourceConnection session);

	/**
	 * Method <code>supElementNamePaths</code> returns an array of element
	 * names in form of a full path to the XML element for
	 * stanzas which can be processed by this plugin. Each element name path
	 * corresponds to XMLNS returned in array by <code>supNamespaces()</code>
	 * method. The element path itself is represented by a String array with each path
	 * element as a separate String.
	 *
	 * @return a <code>String[][]</code> value is an array for element paths for which
	 * the plugin offers processing capabilities. Each path is in form of a String array
	 * in order to reduce parsing overhead.
	 */
	String[][] supElementNamePaths();

	/**
	 * Method <code>supElements</code> returns an array of element names for
	 * stanzas which can be processed by this plugin. Each element name
	 * corresponds to XMLNS returned in array by <code>supNamespaces()</code>
	 * method.
	 * This method has been deprecated in favor of <code>supElementNamePaths</code>.
	 *
	 * @return a <code>String[]</code> value
	 * @see #supElementNamePaths
	 */
	@Deprecated
	String[] supElements();

	/**
	 * Method <code>supNamespaces</code> returns an array of name-spaces for
	 * stanzas which can be processed by this plugin. Each namespace
	 * corresponds to element name returned in array by
	 * <code>supElemenets()</code> method.
	 *
	 * @return a <code>String[]</code> value
	 */
	String[] supNamespaces();

	/**
	 * Method <code>supStreamFeatures</code> returns an array of XML
	 * <code>Element</code>s with stream features which have to be returned to the
	 * client uppon request. Stream features returned by this method correspond to
	 * features supported by this plugin.
	 *
	 * @param session
	 *          a <code>XMPPResourceConnection</code> value
	 * @return an <code>Element[]</code> value
	 */
	Element[] supStreamFeatures(XMPPResourceConnection session);

	/**
	 * Method returns an array of all stanza types which the plugin is able
	 * to handle. If the method returns NULL, then all stanzas of all types
	 * will be passed to the plugin for processing.
	 * Otherwise only stanzas with selected types, assuming that
	 * element names and name-spaces match as well.
	 *
	 *
	 * @return a <code>StanzaType[]</code> array of supported stanza types.
	 */
	Set<StanzaType> supTypes();

	//~--- get methods ----------------------------------------------------------

	/**
	 * Allows to obtain various informations about components
	 *
	 * @return information about particular component
	 */
	ComponentInfo getComponentInfo();
	/**
	 * The method allows to retrieve plugin own statistics if it generates any.
	 * @param list is a statistics collection to which plugins own metrics can be added.
	 */
	void getStatistics(StatisticsList list);

	/**
	 * Method <code>isSupporting</code> takes element name and name-space for this
	 * element and determines whether this element can be processed by this
	 * plugin.
	 *
	 * @param elem
	 *          a <code>String</code> value
	 * @param ns
	 *          a <code>String</code> value
	 * @return a <code>boolean</code> value
	 */
	@Deprecated
	boolean isSupporting(String elem, String ns);
}    // XMPPImplIfc


//~ Formatted in Tigase Code Convention on 13/09/21
