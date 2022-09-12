package cuchaz.enigma.utils.validation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import cuchaz.enigma.utils.validation.Message.Type;

/**
 * A context for user input validation. Handles collecting error messages and
 * displaying the errors on the relevant input fields. UIs using validation
 * often have two stages of applying changes: validating all the input fields,
 * then checking if there's any errors or unconfirmed warnings, and if not,
 * then actually applying the changes. This allows for easily collecting
 * multiple errors and displaying them to the user at the same time.
 */
public class ValidationContext {
	private Validatable activeElement = null;
	private final Set<Validatable> elements = new HashSet<>();
	private final List<ParameterizedMessage> messages = new ArrayList<>();

	/**
	 * Sets the currently active element (such as an input field). Any messages
	 * raised while this is set get displayed on this element.
	 *
	 * @param v the active element to set, or {@code null} to unset
	 */
	public void setActiveElement(@Nullable Validatable v) {
		if (v != null) {
			elements.add(v);
		}

		activeElement = v;
	}

	/**
	 * Raises a message. If there's a currently active element, also notifies
	 * that element about the message.
	 *
	 * @param message the message to raise
	 * @param args the arguments used when formatting the message text
	 */
	public void raise(Message message, Object... args) {
		ParameterizedMessage pm = new ParameterizedMessage(message, args, this.activeElement);

		if (!this.messages.contains(pm)) {
			if (activeElement != null) {
				activeElement.addMessage(pm);
			}

			messages.add(pm);
		}
	}

	/**
	 * Returns whether the validation context currently has no messages that
	 * block executing actions, such as errors and unconfirmed warnings.
	 *
	 * @return whether the program can proceed executing and the UI is in a
	 * valid state
	 */
	public boolean canProceed() {
		// TODO on warnings, wait until user confirms
		return messages.stream().noneMatch(m -> m.message.type == Type.ERROR);
	}

	/**
	 * If this validation context has at least one error, throw an exception.
	 *
	 * @throws IllegalStateException if errors are present
	 */
	public void throwOnError() {
		if (!this.canProceed()) {
			for (ParameterizedMessage message : this.messages) {
				PrintValidatable.formatMessage(System.err, message);
			}

			throw new IllegalStateException("Errors encountered; cannot continue! Check error log for details.");
		}
	}

	public List<ParameterizedMessage> getMessages() {
		return Collections.unmodifiableList(messages);
	}

	/**
	 * Clears all currently pending messages. This should be called whenever the
	 * interface starts getting validated, to get rid of old messages.
	 */
	public void reset() {
		activeElement = null;
		elements.forEach(Validatable::clearMessages);
		elements.clear();
		messages.clear();
	}
}
