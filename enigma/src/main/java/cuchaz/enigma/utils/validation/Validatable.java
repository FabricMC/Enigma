package cuchaz.enigma.utils.validation;

public interface Validatable {

	void addMessage(Message message, Object[] args);

	void clearMessages();

}
