package cuchaz.enigma.network.packet;

import cuchaz.enigma.network.EnigmaServer;
import cuchaz.enigma.network.ServerPacketHandler;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;

public class LoginC2SPacket implements Packet<ServerPacketHandler> {
	private byte[] jarChecksum;
	private String username;

	LoginC2SPacket() {
	}

	public LoginC2SPacket(byte[] jarChecksum, String username) {
		this.jarChecksum = jarChecksum;
		this.username = username;
	}

	@Override
	public void read(DataInput input) throws IOException {
		if (input.readUnsignedShort() != EnigmaServer.PROTOCOL_VERSION) {
			throw new IOException("Mismatching protocol");
		}
		this.jarChecksum = new byte[EnigmaServer.CHECKSUM_SIZE];
		input.readFully(jarChecksum);
		this.username = input.readUTF();
	}

	@Override
	public void write(DataOutput output) throws IOException {
		output.writeShort(EnigmaServer.PROTOCOL_VERSION);
		output.write(jarChecksum);
		output.writeUTF(username);
	}

	@Override
	public void handle(ServerPacketHandler handler) {
		if (!Arrays.equals(jarChecksum, handler.getServer().getJarChecksum())) {
			handler.getServer().kick(handler.getClient(), "disconnect.wrong_jar");
			return;
		}

		if (handler.getServer().isUsernameTaken(username)) {
			handler.getServer().kick(handler.getClient(), "disconnect.username_taken");
			return;
		}

		handler.getServer().setUsername(handler.getClient(), username);
		System.out.println(username + " logged in with IP " + handler.getClient().getInetAddress().toString() + ":" + handler.getClient().getPort());

		handler.getServer().sendPacket(handler.getClient(), new SyncMappingsS2CPacket(handler.getServer().getMappings().getObfToDeobf()));
	}
}
