/*******************************************************************************
 * Copyright (c) 2015 Jeff Martin.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public
 * License v3.0 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * <p>
 * Contributors:
 * Jeff Martin - initial API and implementation
 ******************************************************************************/

package cuchaz.enigma;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import cuchaz.enigma.analysis.ClassCache;
import cuchaz.enigma.analysis.index.JarIndex;
import cuchaz.enigma.api.EnigmaPlugin;
import cuchaz.enigma.api.EnigmaPluginContext;
import cuchaz.enigma.api.service.EnigmaService;
import cuchaz.enigma.api.service.EnigmaServiceFactory;
import cuchaz.enigma.api.service.EnigmaServiceType;
import cuchaz.enigma.api.service.JarIndexerService;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ServiceLoader;

public class Enigma {
	private final EnigmaProfile profile;
	private final EnigmaServices services;

	private Enigma(EnigmaProfile profile, EnigmaServices services) {
		this.profile = profile;
		this.services = services;
	}

	public static Enigma create() {
		return new Builder().build();
	}

	public static Builder builder() {
		return new Builder();
	}

	public EnigmaProject openJar(Path path, ProgressListener progress) throws IOException {
		ClassCache classCache = ClassCache.of(path);
		JarIndex jarIndex = classCache.index(progress);

		services.get(JarIndexerService.TYPE).ifPresent(indexer -> {
			indexer.acceptJar(classCache, jarIndex);
		});

		return new EnigmaProject(this, classCache, jarIndex);
	}

	public EnigmaProfile getProfile() {
		return profile;
	}

	public EnigmaServices getServices() {
		return services;
	}

	public static class Builder {
		private EnigmaProfile profile = EnigmaProfile.EMPTY;
		private Iterable<EnigmaPlugin> plugins = ServiceLoader.load(EnigmaPlugin.class);

		private Builder() {
		}

		public Builder setProfile(EnigmaProfile profile) {
			Preconditions.checkNotNull(profile, "profile cannot be null");
			this.profile = profile;
			return this;
		}

		public Builder setPlugins(Iterable<EnigmaPlugin> plugins) {
			Preconditions.checkNotNull(plugins, "plugins cannot be null");
			this.plugins = plugins;
			return this;
		}

		public Enigma build() {
			PluginContext pluginContext = new PluginContext(profile);
			for (EnigmaPlugin plugin : plugins) {
				plugin.init(pluginContext);
			}

			EnigmaServices services = pluginContext.buildServices();
			return new Enigma(profile, services);
		}
	}

	private static class PluginContext implements EnigmaPluginContext {
		private final EnigmaProfile profile;

		private final ImmutableMap.Builder<EnigmaServiceType<?>, EnigmaService> services = ImmutableMap.builder();

		PluginContext(EnigmaProfile profile) {
			this.profile = profile;
		}

		@Override
		public <T extends EnigmaService> void registerService(String id, EnigmaServiceType<T> serviceType, EnigmaServiceFactory<T> factory) {
			EnigmaProfile.Service serviceProfile = profile.getServiceProfile(serviceType);

			// if this service type is not configured, or it is configured to use a different service id, skip
			if (serviceProfile == null || !serviceProfile.matches(id)) return;

			T service = factory.create(serviceProfile::getArgument);
			services.put(serviceType, service);
		}

		EnigmaServices buildServices() {
			return new EnigmaServices(services.build());
		}
	}
}
