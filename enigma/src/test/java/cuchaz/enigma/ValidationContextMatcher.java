package cuchaz.enigma;

import cuchaz.enigma.utils.validation.ParameterizedMessage;
import cuchaz.enigma.utils.validation.ValidationContext;
import org.hamcrest.CustomMatcher;
import org.hamcrest.Description;

class ValidationContextMatcher extends CustomMatcher<ValidationContext> {
	public static final ValidationContextMatcher INSTANCE = new ValidationContextMatcher();

	private ValidationContextMatcher() {
		super("ValidationContext can proceed");
	}

	@Override
	public boolean matches(Object item) {
		return item instanceof ValidationContext && ((ValidationContext) item).canProceed();
	}

	@Override
	public void describeMismatch(Object item, Description description) {
		if (!(item instanceof ValidationContext)) {
			description.appendText("expected ValidationContext, was").appendValue(item);
			return;
		}
		ValidationContext vc = (ValidationContext) item;
		for (ParameterizedMessage message : vc.getMessages()) {
			description.appendText(message.getText());
			String longMessage = message.getLongText();
			if (longMessage != null && !longMessage.trim().isEmpty()){
				description.appendText(longMessage);
			}
		}
	}
}
