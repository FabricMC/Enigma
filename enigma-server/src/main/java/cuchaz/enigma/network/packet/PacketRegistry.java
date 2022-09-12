package cuchaz.enigma.network.packet;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import cuchaz.enigma.network.ClientPacketHandler;
import cuchaz.enigma.network.ServerPacketHandler;

public class PacketRegistry {
	private static final Map<Class<? extends Packet<ServerPacketHandler>>, Integer> c2sPacketIds = new HashMap<>();
	private static final Map<Integer, Supplier<? extends Packet<ServerPacketHandler>>> c2sPacketCreators = new HashMap<>();
	private static final Map<Class<? extends Packet<ClientPacketHandler>>, Integer> s2cPacketIds = new HashMap<>();
	private static final Map<Integer, Supplier<? extends Packet<ClientPacketHandler>>> s2cPacketCreators = new HashMap<>();

	private static <T extends Packet<ServerPacketHandler>> void registerC2S(int id, Class<T> clazz, Supplier<T> creator) {
		c2sPacketIds.put(clazz, id);
		c2sPacketCreators.put(id, creator);
	}

	private static <T extends Packet<ClientPacketHandler>> void registerS2C(int id, Class<T> clazz, Supplier<T> creator) {
		s2cPacketIds.put(clazz, id);
		s2cPacketCreators.put(id, creator);
	}

	static {
		registerC2S(0, LoginC2SPacket.class, LoginC2SPacket::new);
		registerC2S(1, ConfirmChangeC2SPacket.class, ConfirmChangeC2SPacket::new);
		registerC2S(6, MessageC2SPacket.class, MessageC2SPacket::new);
		registerC2S(7, EntryChangeC2SPacket.class, EntryChangeC2SPacket::new);

		registerS2C(0, KickS2CPacket.class, KickS2CPacket::new);
		registerS2C(1, SyncMappingsS2CPacket.class, SyncMappingsS2CPacket::new);
		registerS2C(6, MessageS2CPacket.class, MessageS2CPacket::new);
		registerS2C(7, UserListS2CPacket.class, UserListS2CPacket::new);
		registerS2C(8, EntryChangeS2CPacket.class, EntryChangeS2CPacket::new);
	}

	public static int getC2SId(Packet<ServerPacketHandler> packet) {
		return c2sPacketIds.get(packet.getClass());
	}

	public static Packet<ServerPacketHandler> createC2SPacket(int id) {
		Supplier<? extends Packet<ServerPacketHandler>> creator = c2sPacketCreators.get(id);
		return creator == null ? null : creator.get();
	}

	public static int getS2CId(Packet<ClientPacketHandler> packet) {
		return s2cPacketIds.get(packet.getClass());
	}

	public static Packet<ClientPacketHandler> createS2CPacket(int id) {
		Supplier<? extends Packet<ClientPacketHandler>> creator = s2cPacketCreators.get(id);
		return creator == null ? null : creator.get();
	}
}
