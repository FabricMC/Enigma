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
import com.google.common.collect.ImmutableListMultimap;
import cuchaz.enigma.analysis.index.JarIndex;
import cuchaz.enigma.api.EnigmaPlugin;
import cuchaz.enigma.api.EnigmaPluginContext;
import cuchaz.enigma.api.service.EnigmaService;
import cuchaz.enigma.api.service.EnigmaServiceFactory;
import cuchaz.enigma.api.service.EnigmaServiceType;
import cuchaz.enigma.api.service.JarIndexerService;
import cuchaz.enigma.classprovider.CachingClassProvider;
import cuchaz.enigma.classprovider.ClassProvider;
import cuchaz.enigma.classprovider.CombiningClassProvider;
import cuchaz.enigma.classprovider.JarClassProvider;
import cuchaz.enigma.utils.Utils;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;

public class Enigma {
    public static final String NAME = "Enigma";
	public static final String VERSION;
	public static final String URL = "https://fabricmc.net";
    public static final int ASM_VERSION = Opcodes.ASM8;

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

	public EnigmaProject openJar(Path path, ClassProvider libraryClassProvider, ProgressListener progress) throws IOException {
		JarClassProvider jarClassProvider = new JarClassProvider(path);
		ClassProvider classProvider = new CachingClassProvider(new CombiningClassProvider(jarClassProvider, libraryClassProvider));
		Set<String> scope = jarClassProvider.getClassNames();

		JarIndex index = JarIndex.empty();
		index.indexJar(scope, classProvider, progress);
		services.get(JarIndexerService.TYPE).forEach(indexer -> indexer.acceptJar(scope, classProvider, index));

		return new EnigmaProject(this, classProvider, index, Utils.zipSha1(path));
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

		private final ImmutableListMultimap.Builder<EnigmaServiceType<?>, EnigmaService> services = ImmutableListMultimap.builder();

		PluginContext(EnigmaProfile profile) {
			this.profile = profile;
		}

		@Override
		public <T extends EnigmaService> void registerService(String id, EnigmaServiceType<T> serviceType, EnigmaServiceFactory<T> factory) {
			List<EnigmaProfile.Service> serviceProfiles = profile.getServiceProfiles(serviceType);

			for (EnigmaProfile.Service serviceProfile : serviceProfiles) {
				if (serviceProfile.matches(id)) {
					T service = factory.create(serviceProfile::getArgument);
					services.put(serviceType, service);
					break;
				}
			}
		}

		EnigmaServices buildServices() {
			return new EnigmaServices(services.build());
		}
	}

	static {
		String version = null;

		try {
			version = Utils.readResourceToString("/version.txt");
		} catch (Throwable t) {
			version = "Unknown Version";
		}

		VERSION = version;
	}
}
