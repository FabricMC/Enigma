package cuchaz.enigma.utils;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

public final class Result<T, E> {

	private final T ok;
	private final E err;

	private Result(T ok, E err) {
		this.ok = ok;
		this.err = err;
	}

	public static <T, E> Result<T, E> ok(T ok) {
		return new Result<>(Objects.requireNonNull(ok), null);
	}

	public static <T, E> Result<T, E> err(E err) {
		return new Result<>(null, Objects.requireNonNull(err));
	}

	public boolean isOk() {
		return this.ok != null;
	}

	public boolean isErr() {
		return this.err != null;
	}

	public Optional<T> ok() {
		return Optional.ofNullable(this.ok);
	}

	public Optional<E> err() {
		return Optional.ofNullable(this.err);
	}

	public T unwrap() {
		if (this.isOk()) return this.ok;
		throw new IllegalStateException(String.format("Called Result.unwrap on an Err value: %s", this.err));
	}

	public E unwrapErr() {
		if (this.isErr()) return this.err;
		throw new IllegalStateException(String.format("Called Result.unwrapErr on an Ok value: %s", this.ok));
	}

	public T unwrapOr(T fallback) {
		if (this.isOk()) return this.ok;
		return fallback;
	}

	public T unwrapOrElse(Function<E, T> fn) {
		if (this.isOk()) return this.ok;
		return fn.apply(this.err);
	}

	@SuppressWarnings("unchecked")
	public <U> Result<U, E> map(Function<T, U> op) {
		if (!this.isOk()) return (Result<U, E>) this;
		return Result.ok(op.apply(this.ok));
	}

	@SuppressWarnings("unchecked")
	public <F> Result<T, F> mapErr(Function<E, F> op) {
		if (!this.isErr()) return (Result<T, F>) this;
		return Result.err(op.apply(this.err));
	}

	@SuppressWarnings("unchecked")
	public <U> Result<U, E> and(Result<U, E> next) {
		if (this.isErr()) return (Result<U, E>) this;
		return next;
	}

	@SuppressWarnings("unchecked")
	public <U> Result<U, E> andThen(Function<T, Result<U, E>> op) {
		if (this.isErr()) return (Result<U, E>) this;
		return op.apply(this.ok);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Result<?, ?> result = (Result<?, ?>) o;
		return Objects.equals(ok, result.ok) &&
				Objects.equals(err, result.err);
	}

	@Override
	public int hashCode() {
		return Objects.hash(ok, err);
	}

	@Override
	public String toString() {
		if (this.isOk()) {
			return String.format("Result.Ok(%s)", this.ok);
		} else {
			return String.format("Result.Err(%s)", this.err);
		}
	}

}
