package cuchaz.enigma.translation.mapping;

import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nullable;

import cuchaz.enigma.EnigmaProject;
import cuchaz.enigma.source.DecompiledClassSource;
import cuchaz.enigma.translation.representation.entry.Entry;
import cuchaz.enigma.utils.TristateChange;

public final class EntryChange<E extends Entry<?>> {
	private final E target;
	private final TristateChange<String> deobfName;
	private final TristateChange<String> javadoc;
	private final TristateChange<AccessModifier> access;

	private EntryChange(E target, TristateChange<String> deobfName, TristateChange<String> javadoc, TristateChange<AccessModifier> access) {
		this.target = target;
		this.deobfName = deobfName;
		this.javadoc = javadoc;
		this.access = access;
	}

	public static <E extends Entry<?>> EntryChange<E> modify(E target) {
		return new EntryChange<>(target, TristateChange.unchanged(), TristateChange.unchanged(), TristateChange.unchanged());
	}

	public EntryChange<E> withDeobfName(String name) {
		return new EntryChange<>(this.target, TristateChange.set(name), this.javadoc, this.access);
	}

	public EntryChange<E> withDefaultDeobfName(@Nullable EnigmaProject project) {
		Optional<String> proposed = project != null ? DecompiledClassSource.proposeName(project, this.target) : Optional.empty();
		return this.withDeobfName(proposed.orElse(this.target.getName()));
	}

	public EntryChange<E> clearDeobfName() {
		return new EntryChange<>(this.target, TristateChange.reset(), this.javadoc, this.access);
	}

	public EntryChange<E> withJavadoc(String javadoc) {
		return new EntryChange<>(this.target, this.deobfName, TristateChange.set(javadoc), this.access);
	}

	public EntryChange<E> clearJavadoc() {
		return new EntryChange<>(this.target, this.deobfName, TristateChange.reset(), this.access);
	}

	public EntryChange<E> withAccess(AccessModifier access) {
		return new EntryChange<>(this.target, this.deobfName, this.javadoc, TristateChange.set(access));
	}

	public EntryChange<E> clearAccess() {
		return new EntryChange<>(this.target, this.deobfName, this.javadoc, TristateChange.reset());
	}

	public TristateChange<String> getDeobfName() {
		return this.deobfName;
	}

	public TristateChange<String> getJavadoc() {
		return this.javadoc;
	}

	public TristateChange<AccessModifier> getAccess() {
		return this.access;
	}

	public E getTarget() {
		return this.target;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		if (!(o instanceof EntryChange)) {
			return false;
		}

		EntryChange<?> that = (EntryChange<?>) o;
		return Objects.equals(this.target, that.target) && Objects.equals(this.deobfName, that.deobfName) && Objects.equals(this.javadoc, that.javadoc) && Objects.equals(this.access, that.access);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.target, this.deobfName, this.javadoc, this.access);
	}

	@Override
	public String toString() {
		return String.format("EntryChange { target: %s, deobfName: %s, javadoc: %s, access: %s }", this.target, this.deobfName, this.javadoc, this.access);
	}
}
