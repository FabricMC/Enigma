package cuchaz.enigma.network.packet;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import cuchaz.enigma.network.ServerPacketHandler;
import cuchaz.enigma.utils.Message;

public class MessageC2SPacket implements Packet<ServerPacketHandler> {

	private String message;

	MessageC2SPacket() {
	}

	public MessageC2SPacket(String message) {
		this.message = message;
	}

	@Override
	public void read(DataInput input) throws IOException {
		message = input.readUTF();
	}

	@Override
	public void write(DataOutput output) throws IOException {
		output.writeUTF(message);
	}

	@Override
	public void handle(ServerPacketHandler handler) {
		String message = this.message.trim();
		if (!message.isEmpty()) {
			handler.getServer().sendMessage(Message.chat(handler.getServer().getUsername(handler.getClient()), message));
		}
	}

}
