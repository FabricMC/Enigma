package cuchaz.enigma.gui.dialog;

import java.awt.*;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.*;

import cuchaz.enigma.gui.elements.ValidatablePasswordField;
import cuchaz.enigma.gui.elements.ValidatableTextField;
import cuchaz.enigma.network.EnigmaServer;
import cuchaz.enigma.utils.I18n;
import cuchaz.enigma.utils.validation.Message;
import cuchaz.enigma.utils.validation.StandardValidation;
import cuchaz.enigma.utils.validation.ValidationContext;

public class CreateServerDialog extends JDialog {

	private final ValidationContext vc = new ValidationContext();

	private final ValidatableTextField portField;
	private final ValidatablePasswordField passwordField;
	private boolean actionConfirm = false;

	public CreateServerDialog(Frame owner) {
		super(owner, I18n.translate("prompt.create_server.title"), true);

		Container contentPane = getContentPane();
		contentPane.setLayout(new BorderLayout());
		Container inputContainer = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		portField = new ValidatableTextField(Integer.toString(EnigmaServer.DEFAULT_PORT));
		passwordField = new ValidatablePasswordField();

		java.util.List<JLabel> labels = Stream.of("prompt.create_server.port", "prompt.password")
				.map(I18n::translate)
				.map(JLabel::new)
				.collect(Collectors.toList());
		List<JTextField> inputs = Arrays.asList(portField, passwordField);

		for (int i = 0; i < inputs.size(); i += 1) {
			c.gridy = i;
			c.insets = new Insets(4, 4, 4, 4);

			c.gridx = 0;
			c.weightx = 0.0;
			c.anchor = GridBagConstraints.LINE_END;
			c.fill = GridBagConstraints.NONE;
			inputContainer.add(labels.get(i), c);

			c.gridx = 1;
			c.weightx = 1.0;
			c.anchor = GridBagConstraints.LINE_START;
			c.fill = GridBagConstraints.HORIZONTAL;
			inputs.get(i).addActionListener(event -> confirm());
			inputContainer.add(inputs.get(i), c);
		}
		contentPane.add(inputContainer, BorderLayout.CENTER);
		Container buttonContainer = new JPanel(new GridBagLayout());
		c = new GridBagConstraints();
		c.weightx = 1.0;
		c.insets = new Insets(4, 4, 4, 4);
		c.anchor = GridBagConstraints.LINE_END;
		JButton connectButton = new JButton(I18n.translate("prompt.create_server.confirm"));
		connectButton.addActionListener(event -> confirm());
		buttonContainer.add(connectButton, c);
		c.weightx = 0.0;
		c.anchor = GridBagConstraints.CENTER;
		JButton abortButton = new JButton(I18n.translate("prompt.cancel"));
		abortButton.addActionListener(event -> cancel());
		buttonContainer.add(abortButton, c);
		contentPane.add(buttonContainer, BorderLayout.SOUTH);

		setLocationRelativeTo(owner);
		setSize(new Dimension(400, 150));
	}

	private void confirm() {
		vc.reset();
		validateInputs();
		if (vc.canProceed()) {
			actionConfirm = true;
			setVisible(false);
		}
	}

	private void cancel() {
		actionConfirm = false;
		setVisible(false);
	}

	public void validateInputs() {
		vc.setActiveElement(portField);
		StandardValidation.isIntInRange(vc, portField.getText(), 0, 65535);
		vc.setActiveElement(passwordField);
		if (passwordField.getPassword().length > EnigmaServer.MAX_PASSWORD_LENGTH) {
			vc.raise(Message.FIELD_LENGTH_OUT_OF_RANGE, EnigmaServer.MAX_PASSWORD_LENGTH);
		}
	}

	public Result getResult() {
		if (!actionConfirm) return null;
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
