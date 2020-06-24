package cuchaz.enigma.translation;

import java.util.Objects;
import java.util.function.Function;

import cuchaz.enigma.source.RenamableTokenType;

public final class TranslateResult<T> {

	private final RenamableTokenType type;
	private final T value;

	private TranslateResult(RenamableTokenType type, T value) {
		this.type = type;
		this.value = value;
	}

	public static <T> TranslateResult<T> of(RenamableTokenType type, T value) {
		Objects.requireNonNull(type, "type must not be null");
		return new TranslateResult<>(type, value);
	}

	// Used for translatables that don't have a concept of
	// obfuscated/deobfuscated (e.g. method descriptors) for example because
	// they don't have an identifier attached to them
	public static <T> TranslateResult<T> ungrouped(T value) {
		return TranslateResult.obfuscated(value);
	}

	public static <T> TranslateResult<T> obfuscated(T value) {
		return TranslateResult.of(RenamableTokenType.OBFUSCATED, value);
	}

	public static <T> TranslateResult<T> deobfuscated(T value) {
		return TranslateResult.of(RenamableTokenType.DEOBFUSCATED, value);
	}

	public static <T> TranslateResult<T> proposed(T value) {
		return TranslateResult.of(RenamableTokenType.PROPOSED, value);
	}

	public RenamableTokenType getType() {
		return type;
	}

	public T getValue() {
		return value;
	}

	public <R> TranslateResult<R> map(Function<T, R> op) {
		return TranslateResult.of(this.type, op.apply(this.value));
	}

	public boolean isObfuscated() {
		return this.type == RenamableTokenType.OBFUSCATED;
	}

	public boolean isDeobfuscated() {
		return this.type == RenamableTokenType.DEOBFUSCATED;
	}

	public boolean isProposed() {
		return this.type == RenamableTokenType.PROPOSED;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		TranslateResult<?> that = (TranslateResult<?>) o;
		return type == that.type &&
				Objects.equals(value, that.value);
	}

	@Override
	public int hashCode() {
		return Objects.hash(type, value);
	}

	@Override
	public String toString() {
		return String.format("TranslateResult { type: %s, value: %s }", type, value);
	}

}
