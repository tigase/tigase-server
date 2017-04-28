/*
 * VHostJDBCRepository.java
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



package tigase.vhosts;

//~--- non-JDK imports --------------------------------------------------------

import tigase.db.DBInitException;
import tigase.db.DataSource;
import tigase.db.Repository;
import tigase.db.comp.ComponentRepositoryDataSourceAware;
import tigase.db.comp.UserRepoRepository;
import tigase.kernel.beans.config.ConfigField;
import tigase.util.DNSEntry;
import tigase.util.DNSResolverFactory;
import tigase.util.DNSResolverIfc;
import tigase.xmpp.BareJID;

import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Map;
import java.util.logging.Logger;

/**
 * This implementation stores virtual domains in the UserRepository database. It
 * loads initial settings and virtual hosts from the configuration file and then
 * loads more vhosts from the database. Virtual domains from the database can
 * overwrite (disable) vhosts loaded from the configuration file.
 *
 * This implementation keeps all virtual hosts and their parameters in a single
 * database field. This might not be very efficient if you want to manager big
 * number of virtual domains. It is sufficient for hundreds of vhosts. If you
 * need thousands of VHosts support I advice to implement this storage in more
 * efficient way using separate database tables instead of UserRepository.
 * Please note there is a limit of about 300 vhosts if you use Derby database.
 *
 *
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @since Nov 29, 2008 2:32:48 PM
 */
@Repository.Meta( supportedUris = { ".*" }, isDefault = true)
public class VHostJDBCRepository
				extends UserRepoRepository<VHostItem>
				implements ComponentRepositoryDataSourceAware<VHostItem,DataSource> {

	private static final Logger log                         =
		Logger.getLogger(VHostJDBCRepository.class.getName());

	//~--- fields ---------------------------------------------------------------
	@ConfigField(desc = "Default IP to which VHost should resolve", alias = "dns-def-ip")
	private String def_ip_address    = null;
	@ConfigField(desc = "Default hostname to which VHost should resolve", alias = "dns-srv-def-addr")
	private String def_srv_address   = null;
	@ConfigField(desc = "Max allowed number of domains per user", alias = "domains-per-user-limit")
	private int max_domains_per_user = 25;

	public VHostJDBCRepository() {
		DNSResolverIfc resolver = DNSResolverFactory.getInstance();
		def_srv_address = resolver.getDefaultHost();
		try {
			def_ip_address = resolver.getHostIP(resolver.getDefaultHost());
		} catch (Exception e) {
			def_ip_address = resolver.getDefaultHost();
		}

		autoReloadInterval = 60;
	}

	@Override
	public void destroy() {
		// Nothing to do
	}
	
	//~--- get methods ----------------------------------------------------------

	@Override
	public String getConfigKey() {
		return VHostRepoDefaults.getConfigKey();
	}

	@Override
	public String[] getDefaultPropetyItems() {
		return VHostRepoDefaults.getDefaultPropetyItems();
	}

	@Override
	public VHostItem getItemInstance() {
		return VHostRepoDefaults.getItemInstance();
	}

	@Override
	public String getItemsListPKey() {
		return VHostRepoDefaults.getItemsListPKey();
	}

	@Override
	public String getPropertyKey() {
		return VHostRepoDefaults.getPropertyKey();
	}

	@Override
	public BareJID getRepoUser() {
		return VHostRepoDefaults.getRepoUser();
	}

	@Deprecated
	@Override
	public void initRepository(String resource_uri, Map<String, String> params) throws DBInitException {
		// Nothing to do
	}
	
	//~--- set methods ----------------------------------------------------------

	public void setDef_srv_address(String address) {
		def_srv_address = address;
		if (def_srv_address != null && !def_srv_address.endsWith(".")) {
			def_srv_address = def_srv_address + ".";
		}
	}

	//~--- methods --------------------------------------------------------------

	@Override
	public String validateItem(VHostItem item) {
		if ((item.getVhost() == null) || (item.getVhost().getDomain() == null) ||
				item.getVhost().getDomain().isEmpty()) {
			return "Domain name not specified";
		}

		int vhost_count = 0;

		for (VHostItem it : allItems()) {
			if (it.isOwner(item.getOwner())) {
				++vhost_count;
			}
		}
		if (vhost_count >= max_domains_per_user) {
			return "Maximum number of domains exceeded for the user! Current number is: " +
						 vhost_count;
		}
		
		if (item.getS2sSecret() == null) {
			return "S2S Secret is required";
		}
		
		if (System.getProperty("vhost-disable-dns-check") != null) {
			return null;
		}

		// verify all SRV DNS records
		try {
			DNSEntry[] entries = DNSResolverFactory.getInstance().getHostSRV_Entries(item.getKey());

			if (entries != null) {
				for (DNSEntry dNSEntry : entries) {
					log.finest("Validating DNS SRV settings ('" + dNSEntry +
										 "') for the given hostname: " + item.getKey() + " (defaults: " +
										 def_ip_address + ", " + def_srv_address);
					if (Arrays.asList(dNSEntry.getIps()).contains(def_ip_address) ||
							def_srv_address.equals(dNSEntry.getDnsResultHost())) {

						// configuration is OK
						return null;
					}
				}

				return "Incorrect DNS SRV settings" + Arrays.asList(entries);
			}
		} catch (UnknownHostException ex) {

			// Ignore, maybe simply IP address is set in DNS
		}

		// verify DNS records
		try {
			String[] ipAddress = DNSResolverFactory.getInstance().getHostIPs(item.getKey());

			if (ipAddress != null) {
				if (Arrays.asList(ipAddress).contains(def_ip_address)) {
					return null;
				} else {
					return "Incorrect IP address: '" + Arrays.asList(ipAddress) +
								 "' found in DNS for the given host: " + item.getKey();
				}
			} else {
				return "No DNS settings found for given host: " + item.getKey();
			}
		} catch (UnknownHostException ex1) {
			return "There is no DNS settings for given host: " + item.getKey();
		}
	}

	@Override
	public void setDataSource(DataSource dataSource) {
		// this is needed as it is required by interface
	}

}


//~ Formatted in Tigase Code Convention on 13/03/09
