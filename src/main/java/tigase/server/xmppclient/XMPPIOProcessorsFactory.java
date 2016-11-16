/*
 * XMPPIOProcessorsFactory.java
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
package tigase.server.xmppclient;

import tigase.osgi.ModulesManagerImpl;
import tigase.server.ConnectionManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author andrzej
 */
public class XMPPIOProcessorsFactory {

	private static final Logger log = Logger.getLogger(XMPPIOProcessorsFactory.class.getCanonicalName());
	
	private static final String IO_PROCESSORS_PROP_KEY = "processors";
	
	private static final Map<String,String> DEF_PROCESSORS = new HashMap<String,String>();
	
	static {
		DEF_PROCESSORS.put(StreamManagementIOProcessor.XMLNS, StreamManagementIOProcessor.class.getCanonicalName());
		DEF_PROCESSORS.put(StreamErrorCounterIOProcessor.ID, StreamErrorCounterIOProcessor.class.getCanonicalName());
		DEF_PROCESSORS.put(RegistrationThrottlingProcessor.ID, RegistrationThrottlingProcessor.class.getCanonicalName());
	}
	
	public static XMPPIOProcessor[] updateIOProcessors(ConnectionManager connectionManager,
			XMPPIOProcessor[] activeProcessors, Map<String,Object> props) {
		
		if (props.containsKey(IO_PROCESSORS_PROP_KEY)) {
			String[] processorsArr = (String[]) props.get(IO_PROCESSORS_PROP_KEY);
			List<XMPPIOProcessor> processors = new ArrayList<XMPPIOProcessor>();
			
			if (processorsArr != null) {			
				for (String procStr : processorsArr) {
					String[] procStrArr = procStr.split("=");
					String procId = procStrArr[0];
					String procClass = procStrArr.length > 1 ? procStrArr[1] : DEF_PROCESSORS.get(procId);
					
					XMPPIOProcessor proc = findProcessor(activeProcessors, procId, procClass);
					
					if (proc != null) {
						proc.setConnectionManager(connectionManager);
						
						Map<String,Object> procSettings = getProcSettings(props, procId);
						proc.setProperties(procSettings);
						
						processors.add(proc);
					}
				}

			}
			
			return processors.toArray(new XMPPIOProcessor[processors.size()]);
		}
		
		return activeProcessors;
	}
	
	public static Map<String,Object> getProcSettings(Map<String,Object> props, String procId) {
		
		Map<String,Object> results = new HashMap<String,Object>();
		
		String keyPrefix = IO_PROCESSORS_PROP_KEY + "/" + procId + "/";
				
		for (Map.Entry<String,Object> entry : props.entrySet()) {
			if (entry.getKey().startsWith(keyPrefix)) {
				String key = entry.getKey().replace(keyPrefix, "");
				if (key != null && !key.isEmpty()) {
					results.put(key, entry.getValue());
				}
			}
		}
		
		return results;
	}
	
	public static XMPPIOProcessor findProcessor(XMPPIOProcessor[] activeProcessors, String procId, String procClassName) {
		Class<? extends XMPPIOProcessor> procCls = null;
		try {
			procCls = (Class<? extends XMPPIOProcessor>) ModulesManagerImpl.getInstance().forName(procClassName);
		} catch (ClassNotFoundException ex) {
			// we ignore this exception
		}
		
		for (XMPPIOProcessor proc : activeProcessors) {
			if (procId.equals(proc.getId()) && proc.getClass().equals(procCls)) {
				return proc;
			}
		}
		
		try {
			if (log.isLoggable(Level.FINEST)) {
				log.log(Level.FINEST, "looking for XMPP processors of id = {0} of class {1} and found {2}", 
						new Object[]{procId, procClassName, procCls != null ? procCls.toString() : "null"});
			}
			return procCls.newInstance() ;
		}
		catch (Exception ex) {
			return null;
		}
	}
}
