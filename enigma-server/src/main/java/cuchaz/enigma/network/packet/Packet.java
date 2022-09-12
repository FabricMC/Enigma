package cuchaz.enigma.network.packet;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public interface Packet<H> {
	void read(DataInput input) throws IOException;

	void write(DataOutput output) throws IOException;

	void handle(H handler);
}
