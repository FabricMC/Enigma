package cuchaz.enigma.network;

import java.util.Objects;

import javax.annotation.Nullable;

public class ServerAddress {
	public final String address;
	public final int port;

	private ServerAddress(String address, int port) {
		this.address = address;
		this.port = port;
	}

	@Nullable
	public static ServerAddress of(String address, int port) {
		if (port < 0 || port > 65535) {
			return null;
		}

		if (address == null) {
			return null;
		}

		if (address.equals("")) {
			return null;
		}

		if (!address.matches("[a-zA-Z0-9.:-]+")) {
			return null;
		}

		if (address.startsWith("-") || address.endsWith("-")) {
			return null;
		}

		return new ServerAddress(address, port);
	}

	@Nullable
	public static ServerAddress from(String s, int defaultPort) {
		String address;
		int idx = s.indexOf(']');

		if (s.startsWith("[") && idx != -1) {
			address = s.substring(1, idx);
			s = s.substring(idx + 1);
		} else if (s.chars().filter(c -> c == ':').count() == 1) {
			idx = s.indexOf(':');
			address = s.substring(0, idx);
			s = s.substring(idx);
		} else {
			address = s;
			s = "";
		}

		int port;

		if (s.isEmpty()) {
			port = defaultPort;
		} else if (s.startsWith(":")) {
			s = s.substring(1);

			try {
				port = Integer.parseInt(s);
			} catch (NumberFormatException e) {
				return null;
			}
		} else {
			return null;
		}

		return ServerAddress.of(address, port);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		ServerAddress that = (ServerAddress) o;
		return port == that.port && Objects.equals(address, that.address);
	}

	@Override
	public int hashCode() {
		return Objects.hash(address, port);
	}

	@Override
	public String toString() {
		return String.format("ServerAddress { address: '%s', port: %d }", address, port);
	}
}
