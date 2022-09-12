package cuchaz.enigma.network;

import java.util.List;

import cuchaz.enigma.network.packet.Packet;
import cuchaz.enigma.translation.mapping.EntryChange;
import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.mapping.tree.EntryTree;

public interface ClientPacketHandler {
	void openMappings(EntryTree<EntryMapping> mappings);

	boolean applyChangeFromServer(EntryChange<?> change);

	void disconnectIfConnected(String reason);

	void sendPacket(Packet<ServerPacketHandler> packet);

	void addMessage(Message message);

	void updateUserList(List<String> users);
}
