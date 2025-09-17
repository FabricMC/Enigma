package cuchaz.enigma.translation.mapping;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import cuchaz.enigma.EnigmaProject;
import cuchaz.enigma.translation.representation.entry.Entry;
import cuchaz.enigma.utils.validation.ValidationContext;

public class EntryUtil {
	public static EntryMapping applyChange(ValidationContext vc, @Nullable EnigmaProject project, EntryRemapper remapper, EntryChange<?> change) {
		Entry<?> target = change.getTarget();
		EntryMapping prev = remapper.getDeobfMapping(target);
		EntryMapping mapping = EntryUtil.applyChange(prev, change);

		if (remapper.putMapping(vc, target, mapping)) {
			if (project != null) {
				project.onEntryChange(target, prev, change);
			}
		}

		return mapping;
	}

	public static EntryMapping applyChange(@NotNull EntryMapping self, EntryChange<?> change) {
		if (change.getDeobfName().isSet()) {
			self = self.withName(change.getDeobfName().getNewValue());
		} else if (change.getDeobfName().isReset()) {
			self = self.withName(null);
		}

		if (change.getJavadoc().isSet()) {
			self = self.withDocs(change.getJavadoc().getNewValue());
		} else if (change.getJavadoc().isReset()) {
			self = self.withDocs(null);
		}

		if (change.getAccess().isSet()) {
			self = self.withModifier(change.getAccess().getNewValue());
		} else if (change.getAccess().isReset()) {
			self = self.withModifier(AccessModifier.UNCHANGED);
		}

		return self;
	}
}
