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
package tigase.vhosts;

import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Bean(name = "extension-manager", parent = VHostManager.class, active = true, exportable = true)
public class VHostItemExtensionManager {

	private static final Logger log = Logger.getLogger(VHostItemExtensionManager.class.getCanonicalName());

	@Inject(nullAllowed = true)
	private VHostItemExtensionProvider[] providers = new VHostItemExtensionProvider[0];
	private Map<Class<VHostItemExtension>, VHostItemExtensionProvider<?>> providersByClass = new ConcurrentHashMap<>();

	public void setProviders(VHostItemExtensionProvider[] providers) {
		this.providers = Optional.ofNullable(providers).orElseGet(() -> new VHostItemExtensionProvider[0]);
		Set<VHostItemExtensionProvider> newProviders = new HashSet<>(Arrays.asList(this.providers));
		this.providersByClass.values().removeIf(provider -> !newProviders.contains(provider));
		Arrays.stream(this.providers).forEach(provider -> providersByClass.put(provider.getExtensionClazz(), provider));
	}
	
	public <T extends VHostItemExtension> T newExtensionInstanceForClass(Class<T> extensionClass) {
		return Optional.ofNullable((VHostItemExtensionProvider<T>) providersByClass.get(extensionClass))
				.map(this::newExtensionInstance)
				.orElse(null);
	}

	public <T extends VHostItemExtension> Stream<T> newExtensionInstances() {
		return newExtensionInstances(Arrays.stream(providers));
	}

	public <T extends VHostItemExtension> Stream<T> newExtensionInstances(Stream<VHostItemExtensionProvider> providerStream) {
		return (Stream<T>) providerStream.map(this::newExtensionInstance).filter(Objects::nonNull);
	}

	protected  <T extends VHostItemExtension> T newExtensionInstance(VHostItemExtensionProvider<T> provider) {
		Class<T> extensionClass = provider.getExtensionClazz();
		try {
			return extensionClass.newInstance();
		} catch (Throwable ex) {
			log.log(Level.WARNING, "Could not create extension " + extensionClass + " returned by " +
					provider.getClass().getCanonicalName(), ex);
			return null;
		}

	}

	public Stream<VHostItemExtension> addMissingExtensions(Collection<VHostItemExtension> extensions) {
		Set<String> existingExtensions = extensions.stream().map(ext -> ext.getId()).collect(Collectors.toSet());
		return Stream.concat(extensions.stream(), newExtensionInstances(Arrays.stream(providers).filter(provider -> !existingExtensions.contains(provider.getId()))));
	}
}
