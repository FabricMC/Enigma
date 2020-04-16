package cuchaz.enigma.network.packet;

import cuchaz.enigma.analysis.EntryReference;
import cuchaz.enigma.gui.GuiController;
import cuchaz.enigma.translation.representation.entry.Entry;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class RenameS2CPacket implements Packet<GuiController> {
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
		this.newName = input.readUTF();
		this.refreshClassTree = input.readBoolean();
	}

	@Override
	public void write(DataOutput output) throws IOException {
		output.writeShort(syncId);
		PacketHelper.writeEntry(output, entry);
		output.writeUTF(newName);
		output.writeBoolean(refreshClassTree);
	}

	@Override
	public void handle(GuiController controller) {
		controller.rename(new EntryReference<>(entry, entry.getName()), newName, refreshClassTree);
		controller.sendPacket(new ConfirmChangeC2SPacket(syncId));
	}
}
