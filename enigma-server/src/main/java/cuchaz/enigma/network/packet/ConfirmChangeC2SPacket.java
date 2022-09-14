package cuchaz.enigma.network.packet;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import cuchaz.enigma.network.ServerPacketHandler;

public class ConfirmChangeC2SPacket implements Packet<ServerPacketHandler> {
	private int syncId;

	ConfirmChangeC2SPacket() {
	}

	public ConfirmChangeC2SPacket(int syncId) {
		this.syncId = syncId;
	}

	@Override
	public void read(DataInput input) throws IOException {
		this.syncId = input.readUnsignedShort();
	}

	@Override
	public void write(DataOutput output) throws IOException {
		output.writeShort(syncId);
	}

	@Override
	public void handle(ServerPacketHandler handler) {
		handler.getServer().confirmChange(handler.getClient(), syncId);
	}
}
