package cuchaz.enigma.gui.dialog;

import cuchaz.enigma.network.EnigmaServer;
import cuchaz.enigma.utils.I18n;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.Frame;

public class ConnectToServerDialog {

	public static Result show(Frame parentComponent) {
		JTextField usernameField = new JTextField(System.getProperty("user.name"), 20);
		JPanel usernameRow = new JPanel();
		usernameRow.add(new JLabel(I18n.translate("prompt.connect.username")));
		usernameRow.add(usernameField);
		JTextField ipField = new JTextField(20);
		JPanel ipRow = new JPanel();
		ipRow.add(new JLabel(I18n.translate("prompt.connect.ip")));
		ipRow.add(ipField);
		JTextField portField = new JTextField(String.valueOf(EnigmaServer.DEFAULT_PORT), 10);
		JPanel portRow = new JPanel();
		portRow.add(new JLabel(I18n.translate("prompt.port")));
		portRow.add(portField);

		int response = JOptionPane.showConfirmDialog(parentComponent, new Object[]{usernameRow, ipRow, portRow}, I18n.translate("prompt.connect.title"), JOptionPane.OK_CANCEL_OPTION);
		if (response != JOptionPane.OK_OPTION) {
			return null;
		}

		String username = usernameField.getText();
		String ip = ipField.getText();
		int port;
		try {
			port = Integer.parseInt(portField.getText());
		} catch (NumberFormatException e) {
			JOptionPane.showMessageDialog(parentComponent, I18n.translate("prompt.port.nan"), I18n.translate("prompt.connect.title"), JOptionPane.ERROR_MESSAGE);
			return null;
		}
		if (port < 1024 || port >= 65536) {
			JOptionPane.showMessageDialog(parentComponent, I18n.translate("prompt.port.invalid"), I18n.translate("prompt.connect.title"), JOptionPane.ERROR_MESSAGE);
			return null;
		}

		return new Result(username, ip, port);
	}

	public static class Result {
		private final String username;
		private final String ip;
		private final int port;

		public Result(String username, String ip, int port) {
			this.username = username;
			this.ip = ip;
			this.port = port;
		}

		public String getUsername() {
			return username;
		}

		public String getIp() {
			return ip;
		}

		public int getPort() {
			return port;
		}
	}

}
