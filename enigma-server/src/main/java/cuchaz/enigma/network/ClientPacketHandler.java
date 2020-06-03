package cuchaz.enigma.network;

import cuchaz.enigma.analysis.EntryReference;
import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.mapping.tree.EntryTree;
import cuchaz.enigma.network.packet.Packet;
import cuchaz.enigma.translation.representation.entry.Entry;

import java.util.List;

public interface ClientPacketHandler {
    void openMappings(EntryTree<EntryMapping> mappings);

    void rename(EntryReference<Entry<?>, Entry<?>> reference, String newName, boolean refreshClassTree, boolean jumpToReference);

    void removeMapping(EntryReference<Entry<?>, Entry<?>> reference, boolean jumpToReference);

    void changeDocs(EntryReference<Entry<?>, Entry<?>> reference, String updatedDocs, boolean jumpToReference);

    void markAsDeobfuscated(EntryReference<Entry<?>, Entry<?>> reference, boolean jumpToReference);

    void disconnectIfConnected(String reason);

    void sendPacket(Packet<ServerPacketHandler> packet);

    void addMessage(Message message);

    void updateUserList(List<String> users);
}
