package cuchaz.enigma.network.packet;

import cuchaz.enigma.analysis.EntryReference;
import cuchaz.enigma.gui.GuiController;
import cuchaz.enigma.translation.representation.entry.Entry;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class RemoveMappingS2CPacket implements Packet<GuiController> {
	private int syncId;
	private Entry<?> entry;

	RemoveMappingS2CPacket() {
	}

	public RemoveMappingS2CPacket(int syncId, Entry<?> entry) {
		this.syncId = syncId;
		this.entry = entry;
	}

	@Override
	public void read(DataInput input) throws IOException {
		this.syncId = input.readUnsignedShort();
		this.entry = PacketHelper.readEntry(input);
	}

	@Override
	public void write(DataOutput output) throws IOException {
		output.writeShort(syncId);
		PacketHelper.writeEntry(output, entry);
	}

	@Override
	public void handle(GuiController controller) {
		controller.removeMapping(new EntryReference<>(entry, entry.getName()), false);
		controller.sendPacket(new ConfirmChangeC2SPacket(syncId));
	}
}
