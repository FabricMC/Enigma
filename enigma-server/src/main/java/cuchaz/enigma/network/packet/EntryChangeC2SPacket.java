package cuchaz.enigma.network.packet;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import cuchaz.enigma.network.Message;
import cuchaz.enigma.network.ServerPacketHandler;
import cuchaz.enigma.translation.mapping.EntryChange;
import cuchaz.enigma.translation.mapping.EntryUtil;
import cuchaz.enigma.utils.validation.PrintValidatable;
import cuchaz.enigma.utils.validation.ValidationContext;

public class EntryChangeC2SPacket implements Packet<ServerPacketHandler> {
	private EntryChange<?> change;

	EntryChangeC2SPacket() {
	}

	public EntryChangeC2SPacket(EntryChange<?> change) {
		this.change = change;
	}

	@Override
	public void read(DataInput input) throws IOException {
		this.change = PacketHelper.readEntryChange(input);
	}

	@Override
	public void write(DataOutput output) throws IOException {
		PacketHelper.writeEntryChange(output, change);
	}

	@Override
	public void handle(ServerPacketHandler handler) {
		ValidationContext vc = new ValidationContext();
		vc.setActiveElement(PrintValidatable.INSTANCE);

		boolean valid = handler.getServer().canModifyEntry(handler.getClient(), this.change.getTarget());

		if (valid) {
			EntryUtil.applyChange(vc, null, handler.getServer().getMappings(), this.change);
			valid = vc.canProceed();
		}

		if (!valid) {
			handler.getServer().sendCorrectMapping(handler.getClient(), this.change.getTarget(), true);
			return;
		}

		int syncId = handler.getServer().lockEntry(handler.getClient(), this.change.getTarget());
		handler.getServer().sendToAllExcept(handler.getClient(), new EntryChangeS2CPacket(syncId, this.change));

		if (this.change.getDeobfName().isSet()) {
			handler.getServer().sendMessage(Message.rename(handler.getServer().getUsername(handler.getClient()), this.change.getTarget(), this.change.getDeobfName().getNewValue()));
		} else if (this.change.getDeobfName().isReset()) {
			handler.getServer().sendMessage(Message.removeMapping(handler.getServer().getUsername(handler.getClient()), this.change.getTarget()));
		}

		if (!this.change.getJavadoc().isUnchanged()) {
			handler.getServer().sendMessage(Message.editDocs(handler.getServer().getUsername(handler.getClient()), this.change.getTarget()));
		}
	}
}
