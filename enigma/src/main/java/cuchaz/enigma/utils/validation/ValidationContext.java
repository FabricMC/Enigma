package cuchaz.enigma.utils.validation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

public class ValidationContext {

	private Validatable activeElement = null;
	private final Set<Validatable> elements = new HashSet<>();
	private final List<ParameterizedMessage> messages = new ArrayList<>();

	public void setActiveElement(@Nullable Validatable v) {
		if (v != null) {
			elements.add(v);
		}
		activeElement = v;
	}

	public void raise(Message message, Object... args) {
		ParameterizedMessage pm = new ParameterizedMessage(message, args);
		if (activeElement != null) {
			activeElement.addMessage(pm);
		}
		messages.add(pm);
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

}
