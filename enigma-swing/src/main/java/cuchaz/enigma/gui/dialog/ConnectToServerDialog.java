package cuchaz.enigma.gui.dialog;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import javax.swing.JPasswordField;
import javax.swing.JTextField;

import cuchaz.enigma.gui.elements.ValidatableTextField;
import cuchaz.enigma.gui.util.ScaleUtil;
import cuchaz.enigma.network.EnigmaServer;
import cuchaz.enigma.utils.Pair;
import cuchaz.enigma.utils.ServerAddress;
import cuchaz.enigma.utils.validation.Message;
import cuchaz.enigma.utils.validation.StandardValidation;

public class ConnectToServerDialog extends AbstractDialog {

	private JTextField usernameField;
	private ValidatableTextField ipField;
	private JPasswordField passwordField;

	public ConnectToServerDialog(Frame owner) {
		super(owner, "prompt.connect.title", "prompt.connect.confirm", "prompt.cancel");

		Dimension preferredSize = getPreferredSize();
		preferredSize.width = ScaleUtil.scale(400);
		setPreferredSize(preferredSize);
		pack();
		setLocationRelativeTo(owner);
	}

	@Override
	protected List<Pair<String, Component>> createComponents() {
		usernameField = new JTextField(System.getProperty("user.name"));
		ipField = new ValidatableTextField();
		passwordField = new JPasswordField();

		usernameField.addActionListener(event -> confirm());
		ipField.addActionListener(event -> confirm());
		passwordField.addActionListener(event -> confirm());

		return Arrays.asList(
				new Pair<>("prompt.connect.username", usernameField),
				new Pair<>("prompt.connect.address", ipField),
				new Pair<>("prompt.password", passwordField)
		);
	}

	public void validateInputs() {
		vc.setActiveElement(ipField);
		if (StandardValidation.notBlank(vc, ipField.getText())) {
			vc.raise(Message.INVALID_IP);
		}
	}

	public Result getResult() {
		if (!isActionConfirm()) return null;
		vc.reset();
		validateInputs();
		if (!vc.canProceed()) return null;
		return new Result(
				usernameField.getText(),
				Objects.requireNonNull(ServerAddress.from(ipField.getText(), EnigmaServer.DEFAULT_PORT)),
				passwordField.getPassword()
		);
	}

	public static Result show(Frame parent) {
		ConnectToServerDialog d = new ConnectToServerDialog(parent);

		d.setVisible(true);
		Result r = d.getResult();

		d.dispose();
		return r;
	}

	public static class Result {
		private final String username;
		private final ServerAddress address;
		private final char[] password;

		public Result(String username, ServerAddress address, char[] password) {
			this.username = username;
			this.address = address;
			this.password = password;
		}

		public String getUsername() {
			return username;
		}

		public ServerAddress getAddress() {
			return address;
		}

		public char[] getPassword() {
			return password;
		}
	}

}
