package cuchaz.enigma.network.packet;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import cuchaz.enigma.analysis.EntryReference;
import cuchaz.enigma.network.ClientPacketHandler;
import cuchaz.enigma.translation.representation.entry.Entry;
import cuchaz.enigma.utils.validation.PrintValidatable;
import cuchaz.enigma.utils.validation.ValidationContext;

public class RenameS2CPacket implements Packet<ClientPacketHandler> {
	private int syncId;
	private Entry<?> entry;
	private String newName;
	private boolean refreshClassTree;

	RenameS2CPacket() {
	}

	public RenameS2CPacket(int syncId, Entry<?> entry, String newName, boolean refreshClassTree) {
		this.syncId = syncId;
		this.entry = entry;
		this.newName = newName;
		this.refreshClassTree = refreshClassTree;
	}

	@Override
	public void read(DataInput input) throws IOException {
		this.syncId = input.readUnsignedShort();
		this.entry = PacketHelper.readEntry(input);
		this.newName = PacketHelper.readString(input);
		this.refreshClassTree = input.readBoolean();
	}

	@Override
	public void write(DataOutput output) throws IOException {
		output.writeShort(syncId);
		PacketHelper.writeEntry(output, entry);
		PacketHelper.writeString(output, newName);
		output.writeBoolean(refreshClassTree);
	}

	@Override
	public void handle(ClientPacketHandler controller) {
		ValidationContext vc = new ValidationContext();
		vc.setActiveElement(PrintValidatable.INSTANCE);

		controller.rename(vc, new EntryReference<>(entry, entry.getName()), newName, refreshClassTree);

		if (!vc.canProceed()) return;
		controller.sendPacket(new ConfirmChangeC2SPacket(syncId));
	}
}
