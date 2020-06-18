package cuchaz.enigma.network.packet;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import cuchaz.enigma.network.Message;
import cuchaz.enigma.network.ServerPacketHandler;
import cuchaz.enigma.newabstraction.EntryChange;
import cuchaz.enigma.newabstraction.EntryUtil;
import cuchaz.enigma.translation.representation.entry.Entry;
import cuchaz.enigma.utils.validation.PrintValidatable;
import cuchaz.enigma.utils.validation.ValidationContext;

public class RenameC2SPacket implements Packet<ServerPacketHandler> {
	private Entry<?> entry;
	private String newName;
	private boolean refreshClassTree;

	RenameC2SPacket() {
	}

	public RenameC2SPacket(Entry<?> entry, String newName, boolean refreshClassTree) {
		this.entry = entry;
		this.newName = newName;
		this.refreshClassTree = refreshClassTree;
	}

	@Override
	public void read(DataInput input) throws IOException {
		this.entry = PacketHelper.readEntry(input);
		this.newName = PacketHelper.readString(input);
		this.refreshClassTree = input.readBoolean();
	}

	@Override
	public void write(DataOutput output) throws IOException {
		PacketHelper.writeEntry(output, entry);
		PacketHelper.writeString(output, newName);
		output.writeBoolean(refreshClassTree);
	}

	@Override
	public void handle(ServerPacketHandler handler) {
		ValidationContext vc = new ValidationContext();
		vc.setActiveElement(PrintValidatable.INSTANCE);

		boolean valid = handler.getServer().canModifyEntry(handler.getClient(), entry);

		if (valid) {
			EntryUtil.applyChange(vc, handler.getServer().getMappings(), EntryChange.modify(entry).withDeobfName(this.newName));
			valid = vc.canProceed();
		}

		if (!valid) {
			handler.getServer().sendCorrectMapping(handler.getClient(), entry, refreshClassTree);
			return;
		}

		handler.getServer().log(handler.getServer().getUsername(handler.getClient()) + " renamed " + entry + " to " + newName);

		int syncId = handler.getServer().lockEntry(handler.getClient(), entry);
		handler.getServer().sendToAllExcept(handler.getClient(), new RenameS2CPacket(syncId, entry, newName, refreshClassTree));
		handler.getServer().sendMessage(Message.rename(handler.getServer().getUsername(handler.getClient()), entry, newName));
	}
}
