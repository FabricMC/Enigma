package cuchaz.enigma.utils;

import java.util.Objects;

public final class TristateChange<T> {
	private static final TristateChange<?> UNCHANGED = new TristateChange<>(Type.UNCHANGED, null);
	private static final TristateChange<?> RESET = new TristateChange<>(Type.RESET, null);

	private final Type type;
	private final T val;

	@SuppressWarnings("unchecked")
	public static <T> TristateChange<T> unchanged() {
		return (TristateChange<T>) TristateChange.UNCHANGED;
	}

	@SuppressWarnings("unchecked")
	public static <T> TristateChange<T> reset() {
		return (TristateChange<T>) TristateChange.RESET;
	}

	public static <T> TristateChange<T> set(T value) {
		return new TristateChange<>(Type.SET, value);
	}

	private TristateChange(Type type, T val) {
		this.type = type;
		this.val = val;
	}

	public Type getType() {
		return this.type;
	}

	public boolean isUnchanged() {
		return this.type == Type.UNCHANGED;
	}

	public boolean isReset() {
		return this.type == Type.RESET;
	}

	public boolean isSet() {
		return this.type == Type.SET;
	}

	public T getNewValue() {
		if (this.type != Type.SET) {
			throw new IllegalStateException(String.format("No concrete value in %s", this));
		}

		return this.val;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		TristateChange<?> that = (TristateChange<?>) o;
		return type == that.type && Objects.equals(val, that.val);
	}

	@Override
	public int hashCode() {
		return Objects.hash(type, val);
	}

	@Override
	public String toString() {
		return String.format("TristateChange { type: %s, val: %s }", type, val);
	}

	public enum Type {
		UNCHANGED,
		RESET,
		SET,
	}
}
