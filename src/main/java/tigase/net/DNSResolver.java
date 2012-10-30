/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tigase.net;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import tigase.util.DNSEntry;
import tigase.util.SimpleCache;

/**
 *
 * @author andrzej
 */
public class DNSResolver {
        
        private static final Logger log = Logger.getLogger(DNSResolver.class.getCanonicalName());
        
	private static final String OPEN_DNS_HIT_NXDOMAIN = "hit-nxdomain.opendns.com";
	private static final long DNS_CACHE_TIME = 1000 * 60;
	private static Map<String, DNSEntry[]> srv_cache = Collections
			.synchronizedMap(new SimpleCache<String, DNSEntry[]>(100, DNS_CACHE_TIME));        
        
	private static Random rand = new Random();
        
	/**
	 * Method description
	 * 
	 * 
	 * @param hostname
	 * 
	 * @return
	 * 
	 * @throws UnknownHostException
	 */
	public static DNSEntry[] getHostSRV_Entries(String hostname, String service, int defPort)
			throws UnknownHostException {
		DNSEntry[] cache_res = srv_cache.get(hostname);

		if (cache_res != null) {
			return cache_res;
		} // end of if (result != null)

		String result_host = hostname;
		int port = defPort;
		int priority = 0;
		int weight = 0;
		long ttl = 3600 * 1000;
		final ArrayList<DNSEntry> entries = new ArrayList<DNSEntry>();

		try {
			Hashtable<String, String> env = new Hashtable<String, String>(5);

			env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");

			DirContext ctx = new InitialDirContext(env);
			Attributes attrs =
					ctx.getAttributes(service + "." + hostname, new String[] { "SRV" });
			Attribute att = attrs.get("SRV");

			// System.out.println("SRV Attribute: " + att);
			if ((att != null) && (att.size() > 0)) {

				for (int i = 0; i < att.size(); i++) {
					String[] dns_resp = att.get(i).toString().split(" ");

					try {
						priority = Integer.parseInt(dns_resp[0]);
					} catch (Exception e) {
						priority = 0;
					}

					try {
						weight = Integer.parseInt(dns_resp[1]);
					} catch (Exception e) {
						weight = 0;
					}

					try {
						port = Integer.parseInt(dns_resp[2]);
					} catch (Exception e) {
						port = defPort;
					}

					result_host = dns_resp[3];

					try {
						// Jajcus is right here. If there is any problem with one of the SVR
						// host entries then none of the rest would be even considered.
						InetAddress[] all = InetAddress.getAllByName(result_host);
						String ip_address = all[0].getHostAddress();

						// if (!IPAddressUtil.isIPv4LiteralAddress(ip_address))
						// continue;

						entries.add(new DNSEntry(hostname, result_host, ip_address, port, ttl,
								priority, weight));
					} catch (Exception e) {
						// There is no more processing anyway but for the sake of clarity
						// and in case some more code is added in the future we call
						// continue here
						continue;
					}
				}
			} else {
				log.log(Level.FINER, "Empty SRV DNS records set for domain: {0}", hostname);
			}

			ctx.close();
		} catch (NamingException e) {
			result_host = hostname;

			if (log.isLoggable(Level.FINER)) {
				log.log(Level.FINER, "Problem getting SRV DNS records for domain: " + hostname, e);
			}
		} // end of try-catch

		if (entries.isEmpty()) {
			InetAddress[] all = InetAddress.getAllByName(result_host);
			String ip_address = all[0].getHostAddress();

			entries.add(new DNSEntry(hostname, ip_address, port));
		}

		// System.out.println("Adding " + hostname + " to cache DNSEntry: " +
		// entry.toString());
		DNSEntry[] result = entries.toArray(new DNSEntry[] {});
		if (result != null) {
			srv_cache.put(hostname, result);
		}

		return result;
	}

	/**
	 * Method description
	 * 
	 * 
	 * @param hostname
	 * 
	 * @return
	 * 
	 * @throws UnknownHostException
	 */
	public static DNSEntry getHostSRV_Entry(String hostname, String service, int defPort) throws UnknownHostException {
		DNSEntry[] entries = getHostSRV_Entries(hostname, service, defPort);

		if ((entries == null) || (entries.length == 0)) {
			return null;
		}

		// Let's find the entry with the highest priority
		int priority = Integer.MAX_VALUE;
		DNSEntry result = null;
		//for (DNSEntry dnsEntry : entries) {
		// We try to get random entry here, in case there are multiple results and one
		// is consistently broken
		int start = rand.nextInt(entries.length);
		int idx = 0;
		for (int i = 0; i < entries.length; ++i) {
			idx = (i+start) % entries.length;
			if (entries[idx].getPriority() < priority) {
				priority = entries[idx].getPriority();
				result = entries[idx];
			}
		}
		if (result == null) {
			// Hm this should not happen, mistake in the algorithm?
			result = entries[0];
			log.log(Level.WARNING, "No result?? should not happen, an error in the code: {0}",
					Arrays.toString(entries));

		}

		if (log.isLoggable(Level.FINEST)) {
			log.log(Level.FINEST, "Start idx: {0}, last idx: {1}, selected DNSEntry: {2}",
					new Object[] {start, idx, result});
		}
		return result;
	}
        
}
