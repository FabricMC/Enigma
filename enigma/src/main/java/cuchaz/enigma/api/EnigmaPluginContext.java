package cuchaz.enigma.api;

import org.jetbrains.annotations.ApiStatus;

import cuchaz.enigma.api.service.EnigmaService;
import cuchaz.enigma.api.service.EnigmaServiceFactory;
import cuchaz.enigma.api.service.EnigmaServiceType;

@ApiStatus.NonExtendable
public interface EnigmaPluginContext {
	<T extends EnigmaService> void registerService(String id, EnigmaServiceType<T> serviceType, EnigmaServiceFactory<T> factory, Ordering... ordering);
	void disableService(String id, EnigmaServiceType<?> serviceType);
}
