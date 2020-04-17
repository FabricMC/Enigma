package cuchaz.enigma.network.packet;

import cuchaz.enigma.network.ServerPacketHandler;
import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.representation.entry.Entry;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class MarkDeobfuscatedC2SPacket implements Packet<ServerPacketHandler> {
	private Entry<?> entry;

	MarkDeobfuscatedC2SPacket() {
	}

	public MarkDeobfuscatedC2SPacket(Entry<?> entry) {
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
		if (!valid) {
			handler.getServer().sendCorrectMapping(handler.getClient(), entry, true);
			return;
		}

		handler.getServer().getMappings().mapFromObf(entry, new EntryMapping(handler.getServer().getMappings().deobfuscate(entry).getName()));
		System.out.println(handler.getServer().getUsername(handler.getClient()) + " marked " + entry + " as deobfuscated");

		int syncId = handler.getServer().lockEntry(handler.getClient(), entry);
		handler.getServer().sendToAllExcept(handler.getClient(), new MarkDeobfuscatedS2CPacket(syncId, entry));
	}
}
