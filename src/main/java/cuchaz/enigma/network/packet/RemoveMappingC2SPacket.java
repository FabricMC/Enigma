package cuchaz.enigma.network.packet;

import cuchaz.enigma.network.ServerPacketHandler;
import cuchaz.enigma.throwables.IllegalNameException;
import cuchaz.enigma.translation.representation.entry.Entry;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class RemoveMappingC2SPacket implements Packet<ServerPacketHandler> {
	private Entry<?> entry;

	RemoveMappingC2SPacket() {
	}

	public RemoveMappingC2SPacket(Entry<?> entry) {
		this.entry = entry;
	}

	@Override
	public void read(DataInput input) throws IOException {
		this.entry = PacketHelper.readEntry(input);
	}

	@Override
	public void write(DataOutput output) throws IOException {
		PacketHelper.writeEntry(output, entry);
	}

	@Override
	public void handle(ServerPacketHandler handler) {
		boolean valid = handler.getServer().canModifyEntry(handler.getClient(), entry);

		if (valid) {
			try {
				handler.getServer().getMappings().removeByObf(entry);
			} catch (IllegalNameException e) {
				valid = false;
			}
		}

		if (!valid) {
			handler.getServer().sendCorrectMapping(handler.getClient(), entry, true);
			return;
		}

		int syncId = handler.getServer().lockEntry(handler.getClient(), entry);
		handler.getServer().sendToAllExcept(handler.getClient(), new RemoveMappingS2CPacket(syncId, entry));
	}
}
