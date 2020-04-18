package cuchaz.enigma.network.packet;

import cuchaz.enigma.gui.GuiController;
import cuchaz.enigma.network.EnigmaServer;
import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.mapping.tree.EntryTree;
import cuchaz.enigma.translation.mapping.tree.EntryTreeNode;
import cuchaz.enigma.translation.mapping.tree.HashEntryTree;
import cuchaz.enigma.translation.representation.entry.Entry;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class SyncMappingsS2CPacket implements Packet<GuiController> {
	private EntryTree<EntryMapping> mappings;

	SyncMappingsS2CPacket() {
	}

	public SyncMappingsS2CPacket(EntryTree<EntryMapping> mappings) {
		this.mappings = mappings;
	}

	@Override
	public void read(DataInput input) throws IOException {
		mappings = new HashEntryTree<>();
		int size = input.readInt();
		for (int i = 0; i < size; i++) {
			readEntryTreeNode(input, null);
		}
	}

	private void readEntryTreeNode(DataInput input, Entry<?> parent) throws IOException {
		Entry<?> entry = PacketHelper.readEntry(input, parent, false);
		EntryMapping mapping = null;
		if (input.readBoolean()) {
			String name = input.readUTF();
			if (input.readBoolean()) {
				String javadoc = input.readUTF();
				mapping = new EntryMapping(name, javadoc);
			} else {
				mapping = new EntryMapping(name);
			}
		}
		mappings.insert(entry, mapping);
		int size = input.readUnsignedShort();
		for (int i = 0; i < size; i++) {
			readEntryTreeNode(input, entry);
		}
	}

	@Override
	public void write(DataOutput output) throws IOException {
		List<EntryTreeNode<EntryMapping>> roots = mappings.getRootNodes().collect(Collectors.toList());
		output.writeInt(roots.size());
		for (EntryTreeNode<EntryMapping> node : roots) {
			writeEntryTreeNode(output, node);
		}
	}

	private static void writeEntryTreeNode(DataOutput output, EntryTreeNode<EntryMapping> node) throws IOException {
		PacketHelper.writeEntry(output, node.getEntry(), false);
		EntryMapping value = node.getValue();
		output.writeBoolean(value != null);
		if (value != null) {
			output.writeUTF(value.getTargetName());
			output.writeBoolean(value.getJavadoc() != null);
			if (value.getJavadoc() != null) {
				output.writeUTF(value.getJavadoc());
			}
		}
		Collection<? extends EntryTreeNode<EntryMapping>> children = node.getChildNodes();
		output.writeShort(children.size());
		for (EntryTreeNode<EntryMapping> child : children) {
			writeEntryTreeNode(output, child);
		}
	}

	@Override
	public void handle(GuiController controller) {
		controller.openMappings(mappings);
		controller.sendPacket(new ConfirmChangeC2SPacket(EnigmaServer.DUMMY_SYNC_ID));
	}
}
