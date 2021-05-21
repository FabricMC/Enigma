package cuchaz.enigma.utils.validation;

import java.util.Arrays;
import java.util.Objects;

public final class ParameterizedMessage {

	public final Message message;
	private final Object[] params;
	private final Validatable target;

	public ParameterizedMessage(Message message, Object[] params, Validatable target) {
		this.message = message;
		this.params = params;
		this.target = target;
	}

	public String getText() {
		return message.format(params);
	}

	public String getLongText() {
		return message.formatDetails(params);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof ParameterizedMessage that)) return false;
		return Objects.equals(message, that.message) &&
				Arrays.equals(params, that.params) &&
				Objects.equals(target, that.target);
	}

	@Override
	public int hashCode() {
		int result = Objects.hash(message, target);
		result = 31 * result + Arrays.hashCode(params);
		return result;
	}

	@Override
	public String toString() {
		return String.format("ParameterizedMessage { message: %s, params: %s, target: %s }", message, Arrays.toString(params), target);
	}

}
