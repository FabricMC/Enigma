package cuchaz.enigma.network.packet;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import cuchaz.enigma.gui.GuiController;

public class UserListS2CPacket implements Packet<GuiController> {

	private List<String> users;

	UserListS2CPacket() {
	}

	public UserListS2CPacket(List<String> users) {
		this.users = users;
	}

	@Override
	public void read(DataInput input) throws IOException {
		int len = input.readUnsignedShort();
		users = new ArrayList<>(len);
		for (int i = 0; i < len; i++) {
			users.add(input.readUTF());
		}
	}

	@Override
	public void write(DataOutput output) throws IOException {
		output.writeShort(users.size());
		for (String user : users) {
			output.writeUTF(user);
		}
	}

	@Override
	public void handle(GuiController handler) {
		handler.updateUserList(users);
	}

}
