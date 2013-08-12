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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import tigase.server.ConnectionManager;

/**
 *
 * @author andrzej
 */
public class XMPPIOProcessorsFactory {

	private static final Logger log = Logger.getLogger(XMPPIOProcessorsFactory.class.getCanonicalName());
	
	private static final String IO_PROCESSORS_PROP_KEY = "processors";
	
	private static final Map<String,Class<? extends XMPPIOProcessor>> PROCESSORS = new HashMap<String,Class<? extends XMPPIOProcessor>>();
	
	static {
		PROCESSORS.put(StreamManagementIOProcessor.XMLNS, StreamManagementIOProcessor.class);
	}
	
	public static XMPPIOProcessor[] updateIOProcessors(ConnectionManager connectionManager,
			XMPPIOProcessor[] activeProcessors, Map<String,Object> props) {
		
		if (props.containsKey(IO_PROCESSORS_PROP_KEY)) {
			String[] processorsArr = (String[]) props.get(IO_PROCESSORS_PROP_KEY);
			List<XMPPIOProcessor> processors = new ArrayList<XMPPIOProcessor>();
			
			if (processorsArr != null) {
				
				for (String procId : processorsArr) {
					XMPPIOProcessor proc = findProcessor(activeProcessors, procId);
					
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
		
		String keyPrefix = IO_PROCESSORS_PROP_KEY + "/" + procId;
				
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
	
	public static XMPPIOProcessor findProcessor(XMPPIOProcessor[] activeProcessors, String procId) {
		for (XMPPIOProcessor proc : activeProcessors) {
			if (procId.equals(proc.getId())) {
				return proc;
			}
		}
		
		try {
			return PROCESSORS.get(procId).newInstance() ;
		}
		catch (Exception ex) {
			
			return null;
		}
	}
}
