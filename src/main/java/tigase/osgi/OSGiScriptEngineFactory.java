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

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import java.util.List;

/**
 * This is a wrapper class for the ScriptEngineFactory class that deals with context class loader issues It is necessary
 * because engines (at least ruby) use the context classloader to find their resources (i.e., their "native" classes)
 */
public class OSGiScriptEngineFactory
		implements ScriptEngineFactory {

	private ClassLoader contextClassLoader;
	private ScriptEngineFactory factory;

	public OSGiScriptEngineFactory(ScriptEngineFactory factory, ClassLoader contextClassLoader) {
		this.factory = factory;
		this.contextClassLoader = contextClassLoader;
	}

	public String getEngineName() {
		return factory.getEngineName();
	}

	public String getEngineVersion() {
		return factory.getEngineVersion();
	}

	public List<String> getExtensions() {
		return factory.getExtensions();
	}

	public String getLanguageName() {
		return factory.getLanguageName();
	}

	public String getLanguageVersion() {
		return factory.getLanguageVersion();
	}

	public String getMethodCallSyntax(String obj, String m, String... args) {
		return factory.getMethodCallSyntax(obj, m, args);
	}

	public List<String> getMimeTypes() {
		return factory.getMimeTypes();
	}

	public List<String> getNames() {
		return factory.getNames();
	}

	public String getOutputStatement(String toDisplay) {
		return factory.getOutputStatement(toDisplay);
	}

	public Object getParameter(String key) {
		return factory.getParameter(key);
	}

	public String getProgram(String... statements) {
		return factory.getProgram(statements);
	}

	public ScriptEngine getScriptEngine() {
		ScriptEngine engine = null;
		if (contextClassLoader != null) {
			ClassLoader old = Thread.currentThread().getContextClassLoader();
			Thread.currentThread().setContextClassLoader(contextClassLoader);
			engine = factory.getScriptEngine();
			Thread.currentThread().setContextClassLoader(old);
		} else {
			engine = factory.getScriptEngine();
		}
		return engine;
	}

}