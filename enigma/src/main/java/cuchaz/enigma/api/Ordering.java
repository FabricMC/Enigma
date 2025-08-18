package cuchaz.enigma.api;

import org.jetbrains.annotations.ApiStatus;

import cuchaz.enigma.utils.OrderingImpl;

@ApiStatus.NonExtendable
public interface Ordering {
	static Ordering first() {
		return OrderingImpl.First.INSTANCE;
	}

	static Ordering last() {
		return OrderingImpl.Last.INSTANCE;
	}

	static Ordering before(String id) {
		return new OrderingImpl.Before(id);
	}

	static Ordering after(String id) {
		return new OrderingImpl.After(id);
	}
}
