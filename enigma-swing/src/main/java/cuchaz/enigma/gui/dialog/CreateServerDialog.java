package cuchaz.enigma.gui.dialog;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.util.Arrays;
import java.util.List;

import cuchaz.enigma.gui.elements.ValidatablePasswordField;
import cuchaz.enigma.gui.elements.ValidatableTextField;
import cuchaz.enigma.network.EnigmaServer;
import cuchaz.enigma.utils.Pair;
import cuchaz.enigma.utils.validation.Message;
import cuchaz.enigma.utils.validation.StandardValidation;

public class CreateServerDialog extends AbstractDialog {

	private ValidatableTextField portField;
	private ValidatablePasswordField passwordField;

	public CreateServerDialog(Frame owner) {
		super(owner, "prompt.create_server.title", "prompt.create_server.confirm", "prompt.cancel");

		setSize(new Dimension(400, 150));
		setLocationRelativeTo(owner);
	}

	@Override
	protected List<Pair<String, Component>> createComponents() {
		portField = new ValidatableTextField(Integer.toString(EnigmaServer.DEFAULT_PORT));
		passwordField = new ValidatablePasswordField();

		portField.addActionListener(event -> confirm());
		passwordField.addActionListener(event -> confirm());

		return Arrays.asList(
				new Pair<>("prompt.create_server.port", portField),
				new Pair<>("prompt.password", passwordField)
		);
	}

	@Override
	public void validateInputs() {
		vc.setActiveElement(portField);
		StandardValidation.isIntInRange(vc, portField.getText(), 0, 65535);
		vc.setActiveElement(passwordField);
		if (passwordField.getPassword().length > EnigmaServer.MAX_PASSWORD_LENGTH) {
			vc.raise(Message.FIELD_LENGTH_OUT_OF_RANGE, EnigmaServer.MAX_PASSWORD_LENGTH);
		}
	}

	public Result getResult() {
		if (!isActionConfirm()) return null;
		vc.reset();
		validateInputs();
		if (!vc.canProceed()) return null;
		return new Result(
				Integer.parseInt(portField.getText()),
				passwordField.getPassword()
		);
	}

	public static Result show(Frame parent) {
		CreateServerDialog d = new CreateServerDialog(parent);

		d.setVisible(true);
		Result r = d.getResult();

		d.dispose();
		return r;
	}

	public static class Result {
		private final int port;
		private final char[] password;

		public Result(int port, char[] password) {
			this.port = port;
			this.password = password;
		}

		public int getPort() {
			return port;
		}

		public char[] getPassword() {
			return password;
		}
	}

}
