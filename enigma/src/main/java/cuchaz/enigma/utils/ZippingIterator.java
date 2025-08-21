package cuchaz.enigma.utils;

import java.util.Iterator;
import java.util.function.BiFunction;

public record ZippingIterator<T1, T2, R>(Iterator<T1> left, Iterator<T2> right, BiFunction<T1, T2, R> join) implements Iterator<R> {
	@Override
	public boolean hasNext() {
		return left.hasNext() && right.hasNext();
	}

	@Override
	public R next() {
		return join.apply(left.next(), right.next());
	}
}
