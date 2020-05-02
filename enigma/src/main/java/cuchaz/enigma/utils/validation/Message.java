package cuchaz.enigma.utils.validation;

import cuchaz.enigma.utils.I18n;

public class Message {

	public static final Message EMPTY_FIELD = create("empty_field");
	public static final Message NOT_INT = create("not_int");
	public static final Message FIELD_OUT_OF_RANGE_INT = create("field_out_of_range_int");
	public static final Message FIELD_LENGTH_OUT_OF_RANGE = create("field_length_out_of_range");
	public static final Message INVALID_NAME = create("invalid_name");

	public final String textKey;
	public final String longTextKey;

	private Message(String textKey, String longTextKey) {
		this.textKey = textKey;
		this.longTextKey = longTextKey;
	}

	public String format(Object[] args) {
		return I18n.translateFormatted(textKey, args);
	}

	public String formatDetails(Object[] args) {
		return I18n.translateOrEmpty(longTextKey, args);
	}

	public static Message create(String name) {
		return new Message(String.format("validation.message.%s", name), String.format("validation.message.%s.long", name));
	}

}
