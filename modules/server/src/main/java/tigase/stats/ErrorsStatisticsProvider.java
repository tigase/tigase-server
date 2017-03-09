/*
 * ErrorsStatisticsProvider.java
 *
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2016 "Tigase, Inc." <office@tigase.com>
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
package tigase.stats;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanConstructorInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.ReflectionException;
import javax.management.RuntimeOperationsException;
import tigase.server.xmppclient.StreamErrorCounterIOProcessor;
import tigase.xmpp.Authorization;
import tigase.xmpp.StreamError;
import tigase.xmpp.impl.ErrorCounter;

/**
 *
 * @author andrzej
 */
public class ErrorsStatisticsProvider
		implements DynamicMBean {

	private static final String ERRORS_NUMBER = "ErrorsNumber";
	private static final String PER_SECOND = "PerSecond";
	private static final String TOTAL = "Total";
	
	private static final String TYPE_FLOAT = "java.lang.Float";
	private static final String TYPE_LONG = "java.lang.Long";
	
	// internal variables describing the MBean
	private final String dClassName = this.getClass().getName();
	private final String dDescription = "Error statistics MBean";
	private MBeanAttributeInfo[] dAttributes;
	private final MBeanConstructorInfo[] dConstructors = new MBeanConstructorInfo[0];
	private final MBeanOperationInfo[] dOperations = new MBeanOperationInfo[1];
	private MBeanInfo dMBeanInfo = null;

	private String[] statsKeys;
	private final Map<String,Holder> stats = new ConcurrentHashMap<>();
	
	private final Map<String,String> statsKeyToKey = new HashMap<>();
	private final Map<String,String> attrToKey = new HashMap<>();

	public ErrorsStatisticsProvider() {
		buildDynamicMBeanInfo();
	}

	public void update(StatisticsProvider sp) {
		// retrieval of statistics data for keys listed in statsKeys
		Map<String,Object> data = sp.getCurStats(statsKeys);
		
		// refreshing totals and per second values using retrieved data
		for (String statKey : statsKeys) {
			String key = statsKeyToKey.get(statKey);
			long value = (long) data.getOrDefault(statKey, 0);
			
			Holder holder = stats.get(key);
			if (holder == null) {
				holder = new Holder();
				stats.put(key, holder);
			}
			
			holder.updateTotal(value);
		}
		
		for (String errorName : StreamErrorCounterIOProcessor.ErrorStatisticsHolder.getErrorNames()) {
			long total = 0;
			for (String compName : sp.getCompNames()) {
				String key = "StreamErrorStats/" + errorName  + "ErrorsNumber";
				total += sp.getStats(compName, key, 0L);
			}
			
			Holder holder = stats.get(errorName);
			if (holder == null) {
				holder = new Holder();
				stats.put(errorName, holder);
			}
			
			holder.updateTotal(total);
		}
	}

	@Override
	public Object getAttribute(String attribute) throws AttributeNotFoundException, MBeanException, ReflectionException {
		boolean total = attribute.endsWith(TOTAL);
		boolean perSec = attribute.endsWith(PER_SECOND);
		
		attribute = attrToKey.get(attribute);
		Holder holder = stats.get(attribute);
		
		if (holder != null) { 
			if (total)
				return holder.getTotal();
			else if (perSec)
				return holder.getPerSecond();
		}
		
		throw new RuntimeOperationsException(
				new IllegalArgumentException(
					"Unknown attribute name " + attribute),
				"Cannot invoke a getter of " + dClassName);
	}

	@Override
	public void setAttribute(Attribute attribute) throws AttributeNotFoundException, InvalidAttributeValueException, MBeanException, ReflectionException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public AttributeList getAttributes(String[] attributeNames) {
		if (attributeNames == null) {
			throw new RuntimeOperationsException(
					new IllegalArgumentException(
							"attributeNames[] cannot be null"),
					"Cannot invoke a getter of " + dClassName);
		}
		AttributeList resultList = new AttributeList();

		if (attributeNames.length == 0) {
			return resultList;
		}

		for (String attributeName : attributeNames) {
			try {
				Object value = getAttribute((String) attributeName);
				resultList.add(new Attribute(attributeName, value));
			}catch (Exception e) {
				// ignoring this exception
			}
		}
		return resultList;
	}

	@Override
	public AttributeList setAttributes(AttributeList attributes) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public Object invoke(String actionName, Object[] params, String[] signature) throws MBeanException, ReflectionException {
		switch (actionName) {
			case "getAllStats":
				return getAllStats();
			default:
				throw new RuntimeOperationsException(
					new IllegalArgumentException(
							"Unknown method " + actionName),
					"Cannot invoke a method " + actionName);
		}
	}

	public Map<String,String> getAllStats() throws MBeanException, ReflectionException {
		LinkedHashMap<String,String> result = new LinkedHashMap<>();
		for (String attr : attrToKey.keySet()) {
			try {
				Object value = getAttribute(attr);
				if (value != null)
					result.put(attr, String.valueOf(value));
			} catch (AttributeNotFoundException ex) {
				// should not happens
			}
		}
		return result;
	}
	
	@Override
	public MBeanInfo getMBeanInfo() {
		return dMBeanInfo;
	}
	
	private void buildDynamicMBeanInfo() {
		List<MBeanAttributeInfo> attrs = new ArrayList<MBeanAttributeInfo>();
		
		for (String errorName : ErrorCounter.ErrorStatisticsHolder.getErrorNames()) {
			String attrName = errorName + ERRORS_NUMBER + PER_SECOND;
			attrs.add(new MBeanAttributeInfo(attrName, TYPE_FLOAT, "Number of errors " + errorName + " per second", true, false, false));
			attrToKey.put(attrName, errorName);
			attrName = errorName + ERRORS_NUMBER + TOTAL;
			attrs.add(new MBeanAttributeInfo(attrName, TYPE_LONG, "Total number of errors " + errorName, true, false, false));
			attrToKey.put(attrName, errorName);
		}
		
		for (String errorName : StreamErrorCounterIOProcessor.ErrorStatisticsHolder.getErrorNames()) {
			String attrName = errorName + ERRORS_NUMBER + PER_SECOND;
			attrs.add(new MBeanAttributeInfo(attrName, TYPE_FLOAT, "Number of errors " + errorName + " per second", true, false, false));
			attrToKey.put(attrName, errorName);
			attrName = errorName + ERRORS_NUMBER + TOTAL;
			attrs.add(new MBeanAttributeInfo(attrName, TYPE_LONG, "Total number of errors " + errorName, true, false, false));
			attrToKey.put(attrName, errorName);
		}
		
		dAttributes = attrs.toArray(new MBeanAttributeInfo[attrs.size()]);
		
		dOperations[0] = new MBeanOperationInfo("getAllStats", "Provides errors statistics", new MBeanParameterInfo[0], "java.util.Map", MBeanOperationInfo.INFO);
		
		dMBeanInfo = new MBeanInfo(dClassName,
                               dDescription,
                               dAttributes,
                               dConstructors,
                               dOperations,
                               new MBeanNotificationInfo[0]);
	
		String[] errorNames = ErrorCounter.ErrorStatisticsHolder.getErrorNames();
		statsKeys = new String[errorNames.length];
		for (int i=0; i<errorNames.length; i++) {
			String key = "sess-man/ErrorStats/" + errorNames[i]  + "ErrorsNumber[L]";
			statsKeys[i] = key;
			statsKeyToKey.put(key, errorNames[i]);
		}
	}
	
	private static class Holder {
		private long total = 0;
		private float perSec = 0;
		private float prevPerSec = 0;
		
		public void updateTotal(long newValue) {
			float temp = perSec;
			long prevTotal = total;
			total = newValue;
			perSec = (prevPerSec + (temp * 2f) + (total - prevTotal)) / 4f;
			prevPerSec = temp;			
		}
		
		public long getTotal() {
			return total;
		}
		
		public float getPerSecond() {
			return perSec;
		}
	}
}
