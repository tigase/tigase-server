/**
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
package tigase.osgi;

import javax.script.*;
import java.io.Reader;

public class OSGiScriptEngine
		implements ScriptEngine {

	private ScriptEngine engine;
	private OSGiScriptEngineFactory factory;

	public OSGiScriptEngine(ScriptEngine engine, OSGiScriptEngineFactory factory) {
		this.engine = engine;
		this.factory = factory;
	}

	public Bindings createBindings() {
		return engine.createBindings();
	}

	public Object eval(Reader reader, Bindings n) throws ScriptException {
		return engine.eval(reader, n);
	}

	public Object eval(Reader reader, ScriptContext context) throws ScriptException {
		return engine.eval(reader, context);
	}

	public Object eval(Reader reader) throws ScriptException {
		return engine.eval(reader);
	}

	public Object eval(String script, Bindings n) throws ScriptException {
		return engine.eval(script, n);
	}

	public Object eval(String script, ScriptContext context) throws ScriptException {
		return engine.eval(script, context);
	}

	public Object eval(String script) throws ScriptException {
		return engine.eval(script);
	}

	public Object get(String key) {
		return engine.get(key);
	}

	public Bindings getBindings(int scope) {
		return engine.getBindings(scope);
	}

	public ScriptContext getContext() {
		return engine.getContext();
	}

	public void setContext(ScriptContext context) {
		engine.setContext(context);
	}

	public ScriptEngineFactory getFactory() {
		return factory;
	}

	public void put(String key, Object value) {
		engine.put(key, value);
	}

	public void setBindings(Bindings bindings, int scope) {
		engine.setBindings(bindings, scope);
	}

}