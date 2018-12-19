package cuchaz.enigma.translation.representation.entry;

import cuchaz.enigma.translation.Translator;
import cuchaz.enigma.translation.mapping.EntryMapping;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public interface ChildEntry<P extends Entry> extends Entry {
	@Nonnull
	P getParent();

	ChildEntry<P> withParent(P parent);

	default String getParentName() {
		return getParent().getName();
	}

	@Override
	default ClassEntry getContainingClass() {
		return getParent().getContainingClass();
	}

	@Override
	default List<Entry> getAncestry() {
		List<Entry> ancestry = getParent().getAncestry();
		ancestry.add(this);
		return ancestry;
	}

	@Override
	@SuppressWarnings("unchecked")
	default <E extends Entry> Entry replaceAncestor(E target, E replacement) {
		if (equals(target)) {
			return replacement;
		}
		return withParent((P) getParent().replaceAncestor(target, replacement));
	}

	@Override
	default Entry translate(Translator translator, @Nullable EntryMapping mapping) {
		ChildEntry<?> parentTranslated = withParent(translator.translate(getParent()));
		return parentTranslated.translateSelf(translator, mapping);
	}

	Entry translateSelf(Translator translator, @Nullable EntryMapping mapping);
}
