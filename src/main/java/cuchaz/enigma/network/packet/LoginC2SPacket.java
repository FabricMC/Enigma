package cuchaz.enigma.network.packet;

import cuchaz.enigma.network.EnigmaServer;
import cuchaz.enigma.network.ServerPacketHandler;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;

public class LoginC2SPacket implements Packet<ServerPacketHandler> {
	private int protocolVersion;
	private byte[] jarChecksum;
	private String username;

	LoginC2SPacket() {
	}

	public LoginC2SPacket(byte[] jarChecksum, String username) {
		this.protocolVersion = EnigmaServer.PROTOCOL_VERSION;
		this.jarChecksum = jarChecksum;
		this.username = username;
	}

	@Override
	public void read(DataInput input) throws IOException {
		this.protocolVersion = input.readUnsignedShort();
		this.jarChecksum = new byte[16];
		input.readFully(jarChecksum);
		this.username = input.readUTF();
	}

	@Override
	public void write(DataOutput output) throws IOException {
		output.writeShort(protocolVersion);
		output.write(jarChecksum);
		output.writeUTF(username);
	}

	@Override
	public void handle(ServerPacketHandler handler) {
		if (protocolVersion != EnigmaServer.PROTOCOL_VERSION) {
			handler.getServer().kick(handler.getClient(), "Mismatching protocol version");
			return;
		}

		if (!Arrays.equals(jarChecksum, handler.getServer().getJarChecksum())) {
			handler.getServer().kick(handler.getClient(), "Jar checksums don't match (you have the wrong jar)!");
			return;
		}

		if (handler.getServer().isUsernameTaken(username)) {
			handler.getServer().kick(handler.getClient(), "Username is taken");
			return;
		}

		handler.getServer().setUsername(handler.getClient(), username);
		System.out.println(username + " logged in with IP " + handler.getClient().getInetAddress().toString());

		handler.getServer().sendPacket(handler.getClient(), new SyncMappingsS2CPacket(handler.getServer().getMappings().getObfToDeobf()));
	}
}
