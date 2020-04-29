package cuchaz.enigma.utils.validation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

public class ValidationContext {

	private Validatable activeElement = null;
	private Set<Validatable> elements = new HashSet<>();
	private List<Entry> messages = new ArrayList<>();

	public void setActiveElement(@Nullable Validatable v) {
		if (v != null) {
			elements.add(v);
		}
		activeElement = v;
	}

	public void raise(Message message, Object... args) {
		if (activeElement != null) {
			activeElement.addMessage(message, args);
		}
		messages.add(new Entry(message, args));
	}

	public boolean canProceed() {
		return messages.isEmpty();
	}

	public void reset() {
		activeElement = null;
		elements.forEach(Validatable::clearMessages);
		elements.clear();
		messages.clear();
	}

	private static final class Entry {
		public final Message message;
		public final Object[] args;

		private Entry(Message message, Object[] args) {
			this.message = message;
			this.args = args;
		}
	}

}
