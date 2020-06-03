package cuchaz.enigma.utils.validation;

import java.util.Arrays;
import java.util.Objects;

public class ParameterizedMessage {

	public final Message message;
	private final Object[] params;

	public ParameterizedMessage(Message message, Object[] params) {
		this.message = message;
		this.params = params;
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
		if (o == null || getClass() != o.getClass()) return false;
		ParameterizedMessage that = (ParameterizedMessage) o;
		return Objects.equals(message, that.message) &&
				Arrays.equals(params, that.params);
	}

	@Override
	public int hashCode() {
		int result = Objects.hash(message);
		result = 31 * result + Arrays.hashCode(params);
		return result;
	}

	@Override
	public String toString() {
		return String.format("ParameterizedMessage { message: %s, params: %s }", message, Arrays.toString(params));
	}

}
