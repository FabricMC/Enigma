package cuchaz.enigma.gui.dialog;

import cuchaz.enigma.network.EnigmaServer;
import cuchaz.enigma.utils.I18n;

import javax.swing.*;
import java.awt.*;

public class CreateServerDialog {

	public static Result show(Frame parentComponent) {
		JTextField portField = new JTextField(String.valueOf(EnigmaServer.DEFAULT_PORT), 10);
		JPanel portRow = new JPanel();
		portRow.add(new JLabel(I18n.translate("prompt.port")));
		portRow.add(portField);
		JPasswordField passwordField = new JPasswordField(20);
		JPanel passwordRow = new JPanel();
		passwordRow.add(new JLabel(I18n.translate("prompt.password")));
		passwordRow.add(passwordField);

		int response = JOptionPane.showConfirmDialog(parentComponent, new Object[]{portRow, passwordRow}, I18n.translate("prompt.create_server.title"), JOptionPane.OK_CANCEL_OPTION);
		if (response != JOptionPane.OK_OPTION) {
			return null;
		}

		int port;
		try {
			port = Integer.parseInt(portField.getText());
		} catch (NumberFormatException e) {
			JOptionPane.showMessageDialog(parentComponent, I18n.translate("prompt.port.nan"), I18n.translate("prompt.create_server.title"), JOptionPane.ERROR_MESSAGE);
			return null;
		}
		if (port < 0 || port >= 65536) {
			JOptionPane.showMessageDialog(parentComponent, I18n.translate("prompt.port.invalid"), I18n.translate("prompt.create_server.title"), JOptionPane.ERROR_MESSAGE);
			return null;
		}

		char[] password = passwordField.getPassword();
		if (password.length > EnigmaServer.MAX_PASSWORD_LENGTH) {
			JOptionPane.showMessageDialog(parentComponent, I18n.translate("prompt.password.too_long"), I18n.translate("prompt.create_server.title"), JOptionPane.ERROR_MESSAGE);
			return null;
		}

		return new Result(port, password);
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
