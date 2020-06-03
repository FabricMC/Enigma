package cuchaz.enigma.gui.util;

import com.google.common.collect.Queues;

import java.util.Deque;

public class History<T> {
	private final Deque<T> previous = Queues.newArrayDeque();
	private final Deque<T> next = Queues.newArrayDeque();
	private T current;

	public History(T initial) {
		current = initial;
	}

	public T getCurrent() {
		return current;
	}

	public void push(T value) {
		previous.addLast(current);
		current = value;
		next.clear();
	}

	public void replace(T value) {
		current = value;
	}

	public boolean canGoBack() {
		return !previous.isEmpty();
	}

	public T goBack() {
		next.addFirst(current);
		current = previous.removeLast();
		return current;
	}

	public boolean canGoForward() {
		return !next.isEmpty();
	}

	public T goForward() {
		previous.addLast(current);
		current = next.removeFirst();
		return current;
	}
}
