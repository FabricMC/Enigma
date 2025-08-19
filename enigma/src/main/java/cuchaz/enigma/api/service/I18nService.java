package cuchaz.enigma.api.service;

import java.io.InputStream;

import org.jetbrains.annotations.Nullable;

public interface I18nService extends EnigmaService {
	EnigmaServiceType<I18nService> TYPE = EnigmaServiceType.create("translation");

	@Nullable
	InputStream getTranslationResource(String language);
}
