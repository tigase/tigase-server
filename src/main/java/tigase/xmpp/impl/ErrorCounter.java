/*
 * ErrorCoutner.java
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


package tigase.xmpp.impl;

import tigase.db.NonAuthUserRepository;
import tigase.server.Packet;
import tigase.stats.CounterValue;
import tigase.stats.StatisticsList;
import tigase.xmpp.Authorization;
import tigase.xmpp.StanzaType;
import tigase.xmpp.XMPPPacketFilterIfc;
import tigase.xmpp.XMPPResourceConnection;
import tigase.xmpp.impl.annotation.AnnotatedXMPPProcessor;
import tigase.xmpp.impl.annotation.HandleStanzaTypes;
import tigase.xmpp.impl.annotation.Id;

import java.util.Queue;
import java.util.logging.Level;

/**
 * ErrorCounter class is implementation of XMPPProcessor responsible for counting 
 * packets with type=error which value is added to Tigase XMPP Server statistics.
 * 
 * @author andrzej
 */
@Id("error-counter")
@HandleStanzaTypes(StanzaType.error)
public class ErrorCounter extends AnnotatedXMPPProcessor implements XMPPPacketFilterIfc {

	private static final String SM_COMP = "sess-man";
	
	private final ErrorStatisticsHolder holder = new ErrorStatisticsHolder();
	
	@Override
	public void getStatistics(StatisticsList list) {
		super.getStatistics(list);
		holder.getStatistics(list);
	}

	@Override
	public String[][] supElementNamePaths() {
		return ALL_PATHS;
	}

	@Override
	public void filter(Packet packet, XMPPResourceConnection session, NonAuthUserRepository repo, Queue<Packet> results) {
		//process(packet, session);
		for (Packet r : results) {
			process(r, session);
		}
	}

	protected void process(Packet packet, XMPPResourceConnection session) {
		if (super.canHandle(packet, session) == Authorization.AUTHORIZED) {
			holder.count(packet);
		}
	}

	public static class ErrorStatisticsHolder {
		
		private static final String[] ERROR_NAMES;
		
		private final CounterValue[] counters;
		
		static {
			int counters = Authorization.values().length+1;
			ERROR_NAMES = new String[counters];
			Authorization[] vals = Authorization.values();
			for (int i=0; i<vals.length; i++) {
				String name = vals[i].getCondition();
				if (name == null) name = vals[i].name().toLowerCase();
				StringBuilder sb = new StringBuilder();
				sb.append(vals[i].getErrorCode());
				for (String part : name.split("-")) {
					sb.append(Character.toUpperCase(part.charAt(0)));
					sb.append(part.substring(1));
				}
				ERROR_NAMES[i] = sb.toString();
			}
			ERROR_NAMES[vals.length] = "0Unknown";
		}
		
		public static String[] getErrorNames() {
			return ERROR_NAMES;
		}
		
		public ErrorStatisticsHolder() {
			counters = new CounterValue[ERROR_NAMES.length];
			for (int i=0; i<ERROR_NAMES.length; i++) {
				counters[i] = new CounterValue(ERROR_NAMES[i], Level.FINER);
			}
		}
		
		public void count(Packet packet) {
			String condition = packet.getErrorCondition();
			Authorization val = Authorization.getByCondition(condition);
			if (val != null)
				counters[val.ordinal()].inc();
			else
				counters[counters.length-1].inc();
		}

	
		public void getStatistics(StatisticsList list) {
			for (CounterValue c : counters) {
				list.add(SM_COMP, "ErrorStats/" + c.getName() + "ErrorsNumber", c.getValue(), c.getLevel());
			}
		}
	}
	
}
