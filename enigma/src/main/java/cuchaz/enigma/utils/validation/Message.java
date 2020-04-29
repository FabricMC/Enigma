package cuchaz.enigma.utils.validation;

import cuchaz.enigma.utils.I18n;

public class Message {

	public static final Message EMPTY_FIELD = create("empty_field");
	public static final Message NOT_INT = create("not_int");
	public static final Message FIELD_OUT_OF_RANGE_INT = create("field_out_of_range_int");
	public static final Message FIELD_LENGTH_OUT_OF_RANGE = create("field_length_out_of_range");

	public final String translationKey;

	private Message(String translationKey) {
		this.translationKey = translationKey;
	}

	public String format(Object[] args) {
		return String.format(I18n.translate(translationKey), args);
	}

	public static Message create(String translationKey) {
		return new Message(String.format("validation.message.%s", translationKey));
	}

}
