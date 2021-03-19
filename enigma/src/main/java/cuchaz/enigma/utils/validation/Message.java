package cuchaz.enigma.utils.validation;

import cuchaz.enigma.utils.I18n;

public class Message {

	public static final Message EMPTY_FIELD = create(Type.ERROR, "empty_field");
	public static final Message INVALID_IP = create(Type.ERROR, "invalid_ip");
	public static final Message NOT_INT = create(Type.ERROR, "not_int");
	public static final Message FIELD_OUT_OF_RANGE_INT = create(Type.ERROR, "field_out_of_range_int");
	public static final Message FIELD_LENGTH_OUT_OF_RANGE = create(Type.ERROR, "field_length_out_of_range");
	public static final Message NONUNIQUE_NAME_CLASS = create(Type.ERROR, "nonunique_name_class");
	public static final Message NONUNIQUE_NAME = create(Type.ERROR, "nonunique_name");
	public static final Message ILLEGAL_CLASS_NAME = create(Type.ERROR, "illegal_class_name");
	public static final Message ILLEGAL_IDENTIFIER = create(Type.ERROR, "illegal_identifier");
	public static final Message RESERVED_IDENTIFIER = create(Type.ERROR, "reserved_identifier");
	public static final Message ILLEGAL_DOC_COMMENT_END = create(Type.ERROR, "illegal_doc_comment_end");
	public static final Message UNKNOWN_RECORD_GETTER = create(Type.ERROR, "unknown_record_getter");

	public static final Message STYLE_VIOLATION = create(Type.WARNING, "style_violation");

	public final Type type;
	public final String textKey;
	public final String longTextKey;

	private Message(Type type, String textKey, String longTextKey) {
		this.type = type;
		this.textKey = textKey;
		this.longTextKey = longTextKey;
	}

	public String format(Object[] args) {
		return I18n.translateFormatted(textKey, args);
	}

	public String formatDetails(Object[] args) {
		return I18n.translateOrEmpty(longTextKey, args);
	}

	public static Message create(Type type, String name) {
		return new Message(type, String.format("validation.message.%s", name), String.format("validation.message.%s.long", name));
	}

	public enum Type {
		INFO,
		WARNING,
		ERROR,
	}

}
