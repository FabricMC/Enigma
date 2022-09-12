package cuchaz.enigma.utils.validation;

public class StandardValidation {
	public static boolean notBlank(ValidationContext vc, String value) {
		if (value.trim().isEmpty()) {
			vc.raise(Message.EMPTY_FIELD);
			return false;
		}

		return true;
	}

	public static boolean isInt(ValidationContext vc, String value) {
		if (!notBlank(vc, value)) {
			return false;
		}

		try {
			Integer.parseInt(value);
			return true;
		} catch (NumberFormatException e) {
			vc.raise(Message.NOT_INT);
			return false;
		}
	}

	public static boolean isIntInRange(ValidationContext vc, String value, int min, int max) {
		if (!isInt(vc, value)) {
			return false;
		}

		int intVal = Integer.parseInt(value);

		if (intVal < min || intVal > max) {
			vc.raise(Message.FIELD_OUT_OF_RANGE_INT, min, max);
			return false;
		}

		return true;
	}
}
