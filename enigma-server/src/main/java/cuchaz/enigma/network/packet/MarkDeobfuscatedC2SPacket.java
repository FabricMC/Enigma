package cuchaz.enigma.network.packet;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import cuchaz.enigma.network.Message;
import cuchaz.enigma.network.ServerPacketHandler;
import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.representation.entry.Entry;
import cuchaz.enigma.utils.validation.PrintValidatable;
import cuchaz.enigma.utils.validation.ValidationContext;

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
		ValidationContext vc = new ValidationContext();
		vc.setActiveElement(PrintValidatable.INSTANCE);

		boolean valid = handler.getServer().canModifyEntry(handler.getClient(), entry);

		if (!valid) {
			handler.getServer().sendCorrectMapping(handler.getClient(), entry, true);
			return;
		}

		handler.getServer().getMappings().mapFromObf(vc, entry, new EntryMapping(handler.getServer().getMappings().deobfuscate(entry).getName()));

		if (!vc.canProceed()) return;

		handler.getServer().log(handler.getServer().getUsername(handler.getClient()) + " marked " + entry + " as deobfuscated");

		int syncId = handler.getServer().lockEntry(handler.getClient(), entry);
		handler.getServer().sendToAllExcept(handler.getClient(), new MarkDeobfuscatedS2CPacket(syncId, entry));
		handler.getServer().sendMessage(Message.markDeobf(handler.getServer().getUsername(handler.getClient()), entry));
	}
}
