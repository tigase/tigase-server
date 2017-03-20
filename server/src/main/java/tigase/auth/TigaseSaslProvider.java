/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2012 "Artur Hefczyc" <artur.hefczyc@tigase.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License.
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
 * $Rev$
 * Last modified by $Author$
 * $Date$
 */
package tigase.auth;

import tigase.auth.callbacks.CallbackHandlerFactoryIfc;
import tigase.db.NonAuthUserRepository;
import tigase.kernel.beans.*;
import tigase.kernel.core.Kernel;
import tigase.server.xmppsession.SessionManager;
import tigase.xmpp.XMPPResourceConnection;

import javax.security.auth.callback.CallbackHandler;
import javax.security.sasl.SaslServerFactory;
import java.security.Provider;
import java.security.Security;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Describe class TigaseSaslProvider here.
 * 
 * Created: Sun Nov 5 22:31:20 2006
 * 
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 */
@Bean(name = "sasl-provider", parent = SessionManager.class, active = true)
public class TigaseSaslProvider
		extends Provider
		implements Initializable, UnregisterAware, RegistrarBean {

	public static final String FACTORY_KEY = "factory";

	private static final String INFO = "This is tigase provider (provides Tigase server specific mechanisms)";

	private static final Logger log = Logger.getLogger(TigaseSaslProvider.class.getName());

	private static final String MY_NAME = "tigase.sasl";

	private static final long serialVersionUID = 1L;

	private static final double VERSION = 1.0;

	@Inject
	private CallbackHandlerFactoryIfc callbackHandlerFactory;

	@Inject
	private MechanismSelector mechanismSelector;

	@Inject(nullAllowed = true)
	private CopyOnWriteArraySet<SaslServerFactory> saslServerFactories = new CopyOnWriteArraySet<>();

	private ConcurrentHashMap<SaslServerFactory, List<Service>> saslServerFactoriesServices = new ConcurrentHashMap<>();

	@SuppressWarnings("unchecked")
	public TigaseSaslProvider() {
		super(MY_NAME, VERSION, INFO);
	}

	public void setSaslServerFactories(CopyOnWriteArraySet<SaslServerFactory> saslServerFactories) {
		this.saslServerFactories.stream().filter(factory -> saslServerFactories == null || !saslServerFactories.contains(factory)).forEach(this::unregisterFactory);
		if (saslServerFactories != null) {
			saslServerFactories.stream().filter(factory -> !this.saslServerFactories.contains(factory)).forEach(this::registerFactory);
		}
		this.saslServerFactories = saslServerFactories == null ? new CopyOnWriteArraySet<>() : saslServerFactories;
	}

	@Override
	public void beforeUnregister() {
		Security.removeProvider(MY_NAME);
	}

	@Override
	public void initialize() {
		Security.insertProviderAt(this, 1);
	}

	public CallbackHandler create(String mechanismName, XMPPResourceConnection session, NonAuthUserRepository repo,
								  Map<String, Object> settings)
			throws ClassNotFoundException, InstantiationException, IllegalAccessException {
		return callbackHandlerFactory.create(mechanismName, session, repo, settings);
	}

	public Collection<String> filterMechanisms(Enumeration<SaslServerFactory> serverFactories,
											   XMPPResourceConnection session) {
		return mechanismSelector.filterMechanisms(serverFactories, session);
	}

	@Override
	public void register(Kernel kernel) {
	}

	@Override
	public void unregister(Kernel kernel) {
	}

	private void registerFactory(SaslServerFactory factory) {
		String factoryClassName = factory.getClass().getName();

		List<Service> services = Arrays.stream(factory.getMechanismNames(new HashMap<>()))
				.map(name -> new Provider.Service(this, "SaslServerFactory", name, factoryClassName, null, null))
				.collect(Collectors.toList());

		services.forEach(this::putService);
		saslServerFactoriesServices.put(factory,services);
	}

	private void unregisterFactory(SaslServerFactory factory) {
		List<Service> services = saslServerFactoriesServices.remove(factory);
		if (services != null) {
			services.forEach(this::removeService);
		}
	}

	@Override
	protected synchronized void putService(Service s) {
		log.config("Registering SASL mechanism '" + s.getAlgorithm() + "' with factory " + s.getClassName());
		super.putService(s);
	}

	@Override
	protected synchronized void removeService(Service s) {
		log.config("Unregistering SASL mechanism '" + s.getAlgorithm() + "' with factory " + s.getClassName());
		super.removeService(s);
	}
}
