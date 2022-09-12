package cuchaz.enigma.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class ServerAddressTest {
	@Test
	public void validAddresses() {
		assertEquals(ServerAddress.of("127.0.0.1", 22), ServerAddress.from("127.0.0.1", 22));
		assertEquals(ServerAddress.of("::1", 80), ServerAddress.from("[::1]:80", 22));
		assertEquals(ServerAddress.of("dblsaiko.net", 22), ServerAddress.from("dblsaiko.net", 22));
		assertEquals(ServerAddress.of("f00f:efee::127.0.0.1", 724), ServerAddress.from("[f00f:efee::127.0.0.1]:724", 22));
		assertEquals(ServerAddress.of("aaaa:aaaa:aaaa:aaaa:aaaa:aaaa:aaaa:70", 22), ServerAddress.from("aaaa:aaaa:aaaa:aaaa:aaaa:aaaa:aaaa:70", 22));
		assertEquals(ServerAddress.of("::1", 22), ServerAddress.from("::1", 22));
		assertEquals(ServerAddress.of("0", 22), ServerAddress.from("0", 22));
	}

	@Test
	public void invalidAddresses() {
		assertNull(ServerAddress.from("127.0.0.1:-72", 22));
		assertNull(ServerAddress.from("127.0.0.1:100000000", 22));
		assertNull(ServerAddress.from("127.0.0.1:lmao", 22));
	}
}
