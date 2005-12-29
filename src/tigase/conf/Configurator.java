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
import java.util.TreeMap;
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
		Map<String, ?> prop = repository.getProperties(compId);
		component.setProperties(prop);
	}

  /**
   * Returns defualt configuration settings in case if there is no
   * config file.
   */
	public Map<String, ?> getDefaults() {
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
    console.setLevel(Level.ALL);
    log.addHandler(console);
		log.setLevel(Level.ALL);

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

		ConfigRepository testRep = ConfigRepository.getConfigRepository(testConfig);
		testRep.putProperties(compName1, props);
		testRep.putProperties(compName2, props);
		testRep.sync();
	}

}
