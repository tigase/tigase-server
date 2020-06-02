/*
 * Tigase XMPP Server - The instant messaging server
 * Copyright (C) 2004 Tigase, Inc. (office@tigase.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 */

package tigase.server;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import tigase.component.DSLBeanConfiguratorWithBackwardCompatibility;
import tigase.kernel.DefaultTypesConverter;
import tigase.kernel.beans.Bean;
import tigase.kernel.core.Kernel;
import tigase.server.script.AddScriptCommand;
import tigase.server.script.Script;
import tigase.util.log.LogFormatter;
import tigase.xml.Element;
import tigase.xmpp.StanzaType;
import tigase.xmpp.jid.JID;

import javax.script.*;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BasicComponentTest {

	static Logger log;
	private static BasicComponent instance;
	private static Kernel kernel;

	static void configureLogger(Logger log, Level level) {
		log.setUseParentHandlers(false);
		log.setLevel(level);
		final Handler[] handlers = log.getHandlers();
		if (Arrays.stream(handlers).noneMatch(ConsoleHandler.class::isInstance)) {
			ConsoleHandler ch = new ConsoleHandler();
			ch.setLevel(level);
			ch.setFormatter(new LogFormatter());
			log.addHandler(ch);
		}
		for (Handler logHandler : handlers) {
			logHandler.setLevel(level);
		}
	}

	private static Script getTestScript() throws ScriptException {
		final AddScriptCommand addScriptCommand = new AddScriptCommand();
		final InputStream resourceAsStream = BasicComponentTest.class.getResourceAsStream("/TestScript.groovy");
		final BufferedReader br = new BufferedReader(new InputStreamReader(resourceAsStream));
		String scriptTxt = br.lines().collect(Collectors.joining("\n"));

		final Bindings bindings = instance.scriptEngineManager.getBindings();
		return addScriptCommand.addAdminScript("test-cmd", "Test script", "tigase", scriptTxt, "groovy", "groovy",
											   bindings);
	}

	@BeforeClass
	public static void setup() {
		log = Logger.getLogger("tigase");
		configureLogger(log, Level.WARNING);

		Map<String, Object> props = new HashMap<>();
		props.put("name", "basic");

		kernel = new Kernel();
		kernel.setName("basic");
		kernel.setForceAllowNull(true);
		kernel.registerBean(DefaultTypesConverter.class).exec();
		kernel.registerBean(DSLBeanConfiguratorWithBackwardCompatibility.class).exportable().exec();
		final DSLBeanConfiguratorWithBackwardCompatibility config = kernel.getInstance(
				DSLBeanConfiguratorWithBackwardCompatibility.class);
		config.setProperties(props);
		kernel.registerBean("service").asClass(TestBasicComponent.class).setActive(true).exec();
		try {
			instance = kernel.getInstance(TestBasicComponent.class);
			final Script script = getTestScript();
			instance.scriptCommands.put("test-cmd", script);
		} catch (Exception ex) {
			log.log(Level.WARNING, ex, () -> "There was an error setting up test");
		}

	}

	@Test
	public void paralelScriptExecution() {
		runTest(true);
	}

	@Test
	public void serialScriptExecution() {
		runTest(false);
	}

	public void runTest(boolean paralel) {
		final Queue<Packet> results = new ConcurrentLinkedQueue<>();
		final Set<Iq> iqPackets = generatePackets(1000);
		final Set<JID> JIDs = (paralel ? iqPackets.parallelStream() : iqPackets.stream()).map(iqPacket -> {
			final JID from = iqPacket.getFrom();
			instance.processScriptCommand(iqPacket, results);
			return from;
		}).collect(Collectors.toSet());

		final Set<JID> resultJIDs = results.stream()
				.map(packet -> JID.jidInstanceNS(Command.getFieldValue(packet, "Note")))
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());

		Assert.assertEquals(JIDs, resultJIDs);

	}

	@Test
	public void simpleTest() throws ScriptException {
		final ScriptEngineManager scriptEngineManager = new ScriptEngineManager();
		final ScriptEngine scriptEngine = scriptEngineManager.getEngineByExtension("groovy");
		final String script = "return key";
		final CompiledScript compiled = ((Compilable) scriptEngine).compile(script);
		final Set<String> input = Stream.generate(() -> UUID.randomUUID().toString())
				.limit(10)
				.collect(Collectors.toSet());

		final Set<String> output = input.stream().parallel().map(uuid -> {
//			final ScriptContext context = scriptEngine.getContext();
			final ScriptContext context = new SimpleScriptContext();
			final Bindings bindings = scriptEngine.createBindings();
			context.setBindings(bindings, ScriptContext.ENGINE_SCOPE);
			bindings.put("key", uuid);
			Object eval = null;
			try {
				eval = compiled.eval(context);
			} catch (ScriptException e) {
				e.printStackTrace();
			}
			return (String) eval;
		}).filter(Objects::nonNull).collect(Collectors.toSet());
		Assert.assertEquals(input, output);
	}

	private Set<Iq> generatePackets(int maxSize) {
		return Stream.generate(() -> {
			JID stanzaAddress = JID.jidInstanceNS(UUID.randomUUID().toString(), "domain.com");
			final Element iq = Command.createIqCommand(stanzaAddress, stanzaAddress, StanzaType.set,
													   stanzaAddress.toString(), "test-cmd", Command.DataType.submit);
			final Iq iqPacket = new Iq(iq, stanzaAddress, stanzaAddress);
			Command.addFieldValue(iqPacket, "accountjid", stanzaAddress.toString());
			iqPacket.setXMLNS(Packet.CLIENT_XMLNS);
			iqPacket.setPermissions(Permissions.ADMIN);
			return iqPacket;
		}).limit(maxSize).collect(Collectors.toSet());
	}

	@Bean(name = "TestBasicComponent", parent = Kernel.class, exportable = true, active = true)
	public static class TestBasicComponent
			extends BasicComponent {

		public TestBasicComponent() {
		}

		@Override
		public boolean canCallCommand(JID jid, String commandId) {
			return true;
		}

		@Override
		public boolean canCallCommand(JID jid, String domain, String commandId) {
			return true;
		}
	}
}