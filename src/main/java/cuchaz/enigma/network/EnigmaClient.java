package cuchaz.enigma.network;

import cuchaz.enigma.gui.GuiController;
import cuchaz.enigma.network.packet.LoginC2SPacket;
import cuchaz.enigma.network.packet.Packet;
import cuchaz.enigma.network.packet.PacketRegistry;

import javax.swing.SwingUtilities;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;

public class EnigmaClient {

	private final GuiController controller;

	private final String ip;
	private final int port;
	private Socket socket;
	private DataOutput output;

	public EnigmaClient(GuiController controller, String ip, int port) {
		this.controller = controller;
		this.ip = ip;
		this.port = port;
	}

	public void connect() throws IOException {
		socket = new Socket(ip, port);
		output = new DataOutputStream(socket.getOutputStream());
		Thread thread = new Thread(() -> {
			try {
				DataInput input = new DataInputStream(socket.getInputStream());
				while (true) {
					int packetId;
					try {
						packetId = input.readUnsignedByte();
					} catch (EOFException | SocketException e) {
						break;
					}
					Packet<GuiController> packet = PacketRegistry.createS2CPacket(packetId);
					if (packet == null) {
						throw new IOException("Received invalid packet id " + packetId);
					}
					packet.read(input);
					SwingUtilities.invokeLater(() -> packet.handle(controller));
				}
			} catch (IOException e) {
				controller.disconnectIfConnected(e.toString());
				return;
			}
			controller.disconnectIfConnected("Disconnected");
		});
		thread.setName("Client I/O thread");
		thread.setDaemon(true);
		thread.start();
	}

	public synchronized void disconnect() {
		if (!socket.isClosed()) {
			try {
				socket.close();
			} catch (IOException e1) {
				System.err.println("Failed to close socket");
				e1.printStackTrace();
			}
		}
	}


	public void sendPacket(Packet<ServerPacketHandler> packet) {
		try {
			output.writeByte(PacketRegistry.getC2SId(packet));
			packet.write(output);
		} catch (IOException e) {
			controller.disconnectIfConnected(e.toString());
		}
	}

}
