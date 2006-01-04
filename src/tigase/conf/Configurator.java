/*
 *  Package Tigase XMPP/Jabber Server
 *  Copyright (C) 2004, 2005, 2006
 *  "Artur Hefczyc" <artur.hefczyc@gmail.com>
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software Foundation,
 *  Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 * $Rev$
 * Last modified by $Author$
 * $Date$
 */

package tigase.conf;

import tigase.server.AbstractComponentRegistrator;
import tigase.server.XMPPService;
import tigase.server.ServerComponent;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Arrays;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * Class Configurator
 *
 * Created: Tue Nov 22 07:07:11 2005
 *
 * @author <a href="mailto:artur.hefczyc@gmail.com">Artur Hefczyc</a>
 * @version $Rev$
 */
public class Configurator extends AbstractComponentRegistrator
	implements XMPPService {

	private ConfigRepository repository = null;

	public Configurator(String fileName) {
		repository = ConfigRepository.getConfigRepository(fileName);
	}

	public void componentAdded(ServerComponent component) {
		if (component instanceof Configurable) {
			setup((Configurable)component);
		} // end of if (component instanceof Configurable)
	}

	public void componentRemoved(ServerComponent component) {}


	public void setup(Configurable component) {
		String compId = component.getName();
		Map<String, Object> prop = repository.getProperties(compId);
		Map<String, Object> defs = component.getDefaults();
		Set<Map.Entry<String, Object>> defs_entries = defs.entrySet();
		boolean modified = false;
		for (Map.Entry<String, Object> entry : defs_entries) {
			if (!prop.containsKey(entry.getKey())) {
				prop.put(entry.getKey(), entry.getValue());
				modified = true;
			} // end of if ()
		} // end of for ()
		if (modified) {
			repository.putProperties(compId, prop);
			try {
				repository.sync();
			} // end of try
			catch (Exception e) {
				e.printStackTrace();
			} // end of try-catch
		} // end of if (modified)
		component.setProperties(prop);
	}

  /**
   * Returns defualt configuration settings in case if there is no
   * config file.
   */
	public Map<String, Object> getDefaults() {
		Map<String, Object> defaults = new TreeMap<String, Object>();
		defaults.put("tigase.message-router.id", "router");
		defaults.put("tigase.message-router.class",
			"tigase.server.MessageRouter");
		return defaults;
	}

	/**
	 * Describe <code>main</code> method here.
	 *
	 * @param args a <code>String[]</code> value
	 */
	public static void main(final String[] args) throws Exception {

		Logger log = Logger.global.getParent();
		ConsoleHandler console = new ConsoleHandler();
    console.setLevel(Level.WARNING);
    log.addHandler(console);
		log.setLevel(Level.WARNING);

		String testConfig = "tests/data/test_config.xml";
		String compName1 = "test_1_Component";
		String compName2 = "test_2_Component.node1.node2";

		Map<String, Object> props = new TreeMap<String, Object>();

		props.put("", "muc");
		props.put("component.accept", true);
		props.put("component.ports", new int[] {1, 22, 333, 4444});
		props.put("component.treshold", 12.34);

		props.put("server.params.int_12345", 12345);
		props.put("server.params.ints_12345", new int[] {1, 2, 3, 4, 5});
		props.put("server.params.bool_true", true);
		props.put("server.params.bool_false", false);
		props.put("server.params.bools", new boolean[] {true, false});
		props.put("server.params.string", "Just a string.");
		props.put("server.params.strings",
			new String[] {"str_1", "str_2", "str_3"});
		props.put("server.params.double", 12.34);
		props.put("server.params.doubles", new double[] {1.2, 2.3, 3.4, 4.5});

		props.put("int_12345", 12345);
		props.put("ints_12345", new int[] {1, 2, 3, 4, 5});
		props.put("bool_true", true);
		props.put("bool_false", false);
		props.put("bools", new boolean[] {true, false});
		props.put("string", "Just a string.");
		props.put("strings", new String[] {"str_1", "str_2", "str_3"});
		props.put("double", 12.34);
		props.put("doubles", new double[] {1.2, 2.3, 3.4, 4.5});

		ConfigRepository testRep1 =
			ConfigRepository.getConfigRepository(testConfig);
		testRep1.putProperties(compName1, props);
		// 		testRep1.putProperties(compName2, props);
		testRep1.sync();

		ConfigRepository testRep2 =
			ConfigRepository.getConfigRepository(testConfig);
		Map<String, ?> props_r = testRep2.getProperties(compName1);

		pr_eq("", "muc", props_r);
		pr_eq("component.accept", true, props_r);
		pr_eq("component.ports", new int[] {1, 22, 333, 4444}, props_r);
		pr_eq("component.treshold", 12.34, props_r);
		pr_eq("int_12345", 12345, props_r);
		pr_eq("ints_12345", new int[] {1, 2, 3, 4, 5}, props_r);
		pr_eq("bool_true", true, props_r);
		pr_eq("bool_false", false, props_r);
		pr_eq("bools", new boolean[] {true, false}, props_r);
		pr_eq("string", "Just a string.", props_r);
		pr_eq("strings", new String[] {"str_1", "str_2", "str_3"}, props_r);
		pr_eq("double", 12.34, props_r);
		pr_eq("doubles", new double[] {1.2, 2.3, 3.4, 4.5}, props_r);
		pr_eq("server.params.int_12345", 12345, props_r);
		pr_eq("server.params.ints_12345", new int[] {1, 2, 3, 4, 5}, props_r);
		pr_eq("server.params.bool_true", true, props_r);
		pr_eq("server.params.bool_false", false, props_r);
		pr_eq("server.params.bools", new boolean[] {true, false}, props_r);
		pr_eq("server.params.string", "Just a string.", props_r);
		pr_eq("server.params.strings",
			new String[] {"str_1", "str_2", "str_3"}, props_r);
		pr_eq("server.params.double", 12.34, props_r);
		pr_eq("server.params.doubles", new double[] {1.2, 2.3, 3.4, 4.5}, props_r);
	}

	private static void pr_eq(String key, String val, Map<String, ?> props) {
		String val_t = (String)props.get(key);
		System.out.println("'" + key + "'=" + val_t + ", " +
			(val.equals(val_t) ? "OK" : "ERR, should be: " + val));
	}

	private static void pr_eq(String key, int val, Map<String, ?> props) {
		int val_t = (Integer)props.get(key);
		System.out.println("'" + key + "'=" + val_t + ", " +
			(val == val_t ? "OK" : "ERR, should be: " + val));
	}

	private static void pr_eq(String key, double val, Map<String, ?> props) {
		double val_t = (Double)props.get(key);
		System.out.println("'" + key + "'=" + val_t + ", " +
			(val == val_t ? "OK" : "ERR, should be: " + val));
	}

	private static void pr_eq(String key, boolean val, Map<String, ?> props) {
		boolean val_t = (Boolean)props.get(key);
		System.out.println("'" + key + "'=" + val_t + ", " +
			(val == val_t ? "OK" : "ERR, should be: " + val));
	}

	private static void pr_eq(String key, String[] val, Map<String, ?> props) {
		String[] val_t = (String[])props.get(key);
		System.out.println("'" + key + "'=" + Arrays.toString(val_t) + ", " +
			(Arrays.equals(val, val_t) ? "OK" : "ERR, should be: " +
				Arrays.toString(val)));
	}

	private static void pr_eq(String key, int[] val, Map<String, ?> props) {
		int[] val_t = (int[])props.get(key);
		System.out.println("'" + key + "'=" + Arrays.toString(val_t) + ", " +
			(Arrays.equals(val, val_t) ? "OK" : "ERR, should be: " +
				Arrays.toString(val)));
	}

	private static void pr_eq(String key, double[] val, Map<String, ?> props) {
		double[] val_t = (double[])props.get(key);
		System.out.println("'" + key + "'=" + Arrays.toString(val_t) + ", " +
			(Arrays.equals(val, val_t) ? "OK" : "ERR, should be: " +
				Arrays.toString(val)));
	}

	private static void pr_eq(String key, boolean[] val, Map<String, ?> props) {
		boolean[] val_t = (boolean[])props.get(key);
		System.out.println("'" + key + "'=" + Arrays.toString(val_t) + ", " +
			(Arrays.equals(val, val_t) ? "OK" : "ERR, should be: " +
				Arrays.toString(val)));
	}

}
