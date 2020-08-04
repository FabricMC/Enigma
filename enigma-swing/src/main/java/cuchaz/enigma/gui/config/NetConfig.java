package cuchaz.enigma.gui.config;

import cuchaz.enigma.config.ConfigContainer;
import cuchaz.enigma.network.EnigmaServer;

public final class NetConfig {

	private NetConfig() {
	}

	private static final ConfigContainer cfg = ConfigContainer.getOrCreate("enigma/net");

	public static void save() {
		cfg.save();
	}

	public static String getUsername() {
		return cfg.data().section("User").setIfAbsentString("Username", System.getProperty("user.name", "user"));
	}

	public static void setUsername(String username) {
		cfg.data().section("User").setString("Username", username);
	}

	public static String getPassword() {
		return cfg.data().section("Remote").getString("Password").orElse("");
	}

	public static void setPassword(String password) {
		cfg.data().section("Remote").setString("Password", password);
	}

	public static String getRemoteAddress() {
		return cfg.data().section("Remote").getString("Address").orElse("");
	}

	public static void setRemoteAddress(String address) {
		cfg.data().section("Remote").setString("Address", address);
	}

	public static String getServerPassword() {
		return cfg.data().section("Server").getString("Password").orElse("");
	}

	public static void setServerPassword(String password) {
		cfg.data().section("Server").setString("Password", password);
	}

	public static int getServerPort() {
		return cfg.data().section("Server").setIfAbsentInt("Port", EnigmaServer.DEFAULT_PORT);
	}

	public static void setServerPort(int port) {
		cfg.data().section("Server").setInt("Port", port);
	}

}
