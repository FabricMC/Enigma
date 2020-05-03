package cuchaz.enigma.network;

import java.net.Socket;

public class ServerPacketHandler {

	private final Socket client;
	private final EnigmaServer server;

	public ServerPacketHandler(Socket client, EnigmaServer server) {
		this.client = client;
		this.server = server;
	}

	public Socket getClient() {
		return client;
	}

	public EnigmaServer getServer() {
		return server;
	}
}
