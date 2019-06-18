package cuchaz.enigma.api.service;

import com.google.common.base.Strings;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.Entry;

public interface ObfuscationTestService extends EnigmaService {
	EnigmaServiceType<ObfuscationTestService> TYPE = EnigmaServiceType.create("obfuscation_test");

	boolean testDeobfuscated(Entry<?> entry);

	final class Default implements ObfuscationTestService {
		Default INSTANCE = new Default();

		Default() {
		}

		@Override
		public boolean testDeobfuscated(Entry<?> entry) {
			if (entry instanceof ClassEntry) {
				String packageName = ((ClassEntry) entry).getPackageName();
				return Strings.isNullOrEmpty(packageName);
			}
			return false;
		}
	}
}
