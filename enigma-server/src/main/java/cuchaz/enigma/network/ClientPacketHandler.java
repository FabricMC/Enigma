package cuchaz.enigma.network;

import cuchaz.enigma.analysis.EntryReference;
import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.mapping.tree.EntryTree;
import cuchaz.enigma.network.packet.Packet;
import cuchaz.enigma.translation.representation.entry.Entry;
import cuchaz.enigma.utils.validation.ValidationContext;

import java.util.List;

public interface ClientPacketHandler {
    void openMappings(EntryTree<EntryMapping> mappings);

    void rename(ValidationContext vc, EntryReference<Entry<?>, Entry<?>> reference, String newName, boolean refreshClassTree);

    void removeMapping(ValidationContext vc, EntryReference<Entry<?>, Entry<?>> reference);

    void changeDocs(ValidationContext vc, EntryReference<Entry<?>, Entry<?>> reference, String updatedDocs);

    void markAsDeobfuscated(ValidationContext vc, EntryReference<Entry<?>, Entry<?>> reference);

    void disconnectIfConnected(String reason);

    void sendPacket(Packet<ServerPacketHandler> packet);

    void addMessage(Message message);

    void updateUserList(List<String> users);
}
