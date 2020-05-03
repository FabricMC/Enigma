package cuchaz.enigma.network.packet;

import cuchaz.enigma.network.EnigmaServer;
import cuchaz.enigma.network.ServerPacketHandler;
import cuchaz.enigma.utils.Message;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;

public class LoginC2SPacket implements Packet<ServerPacketHandler> {
	private byte[] jarChecksum;
	private char[] password;
	private String username;

	LoginC2SPacket() {
	}

	public LoginC2SPacket(byte[] jarChecksum, char[] password, String username) {
		this.jarChecksum = jarChecksum;
		this.password = password;
		this.username = username;
	}

	@Override
	public void read(DataInput input) throws IOException {
		if (input.readUnsignedShort() != EnigmaServer.PROTOCOL_VERSION) {
			throw new IOException("Mismatching protocol");
		}
		this.jarChecksum = new byte[EnigmaServer.CHECKSUM_SIZE];
		input.readFully(jarChecksum);
		this.password = new char[input.readUnsignedByte()];
		for (int i = 0; i < password.length; i++) {
			password[i] = input.readChar();
		}
		this.username = PacketHelper.readString(input);
	}

	@Override
	public void write(DataOutput output) throws IOException {
		output.writeShort(EnigmaServer.PROTOCOL_VERSION);
		output.write(jarChecksum);
		output.writeByte(password.length);
		for (char c : password) {
			output.writeChar(c);
		}
		PacketHelper.writeString(output, username);
	}

	@Override
	public void handle(ServerPacketHandler handler) {
		boolean usernameTaken = handler.getServer().isUsernameTaken(username);
		handler.getServer().setUsername(handler.getClient(), username);
		handler.getServer().log(username + " logged in with IP " + handler.getClient().getInetAddress().toString() + ":" + handler.getClient().getPort());

		if (!Arrays.equals(password, handler.getServer().getPassword())) {
			handler.getServer().kick(handler.getClient(), "disconnect.wrong_password");
			return;
		}

		if (usernameTaken) {
			handler.getServer().kick(handler.getClient(), "disconnect.username_taken");
			return;
		}

		if (!Arrays.equals(jarChecksum, handler.getServer().getJarChecksum())) {
			handler.getServer().kick(handler.getClient(), "disconnect.wrong_jar");
			return;
		}

		handler.getServer().sendPacket(handler.getClient(), new SyncMappingsS2CPacket(handler.getServer().getMappings().getObfToDeobf()));
		handler.getServer().sendMessage(Message.connect(username));
	}
}
