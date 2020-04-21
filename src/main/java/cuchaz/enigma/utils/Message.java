package cuchaz.enigma.utils;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Objects;

import cuchaz.enigma.network.packet.PacketHelper;
import cuchaz.enigma.translation.representation.entry.Entry;

public interface Message {

	static Chat chat(String user, String message) {
		return new Chat(user, message);
	}

	static Connect connect(String user) {
		return new Connect(user);
	}

	static Disconnect disconnect(String user) {
		return new Disconnect(user);
	}

	static EditDocs editDocs(String user, Entry<?> entry) {
		return new EditDocs(user, entry);
	}

	static MarkDeobf markDeobf(String user, Entry<?> entry) {
		return new MarkDeobf(user, entry);
	}

	static RemoveMapping removeMapping(String user, Entry<?> entry) {
		return new RemoveMapping(user, entry);
	}

	static Rename rename(String user, Entry<?> entry, String newName) {
		return new Rename(user, entry, newName);
	}

	String translate();

	Type getType();

	static Message read(DataInput input) throws IOException {
		byte typeId = input.readByte();
		if (typeId < 0 || typeId >= Type.values().length) throw new IOException(String.format("Invalid message type ID %d", typeId));
		Type type = Type.values()[typeId];
		switch (type) {
			case CHAT:
				return Chat.read(input);
			case CONNECT:
				return Connect.read(input);
			case DISCONNECT:
				return Disconnect.read(input);
			case EDIT_DOCS:
				return EditDocs.read(input);
			case MARK_DEOBF:
				return MarkDeobf.read(input);
			case REMOVE_MAPPING:
				return RemoveMapping.read(input);
			case RENAME:
				return Rename.read(input);
			default:
				throw new IllegalStateException("unreachable");
		}
	}

	void write(DataOutput output) throws IOException;

	enum Type {
		CHAT,
		CONNECT,
		DISCONNECT,
		EDIT_DOCS,
		MARK_DEOBF,
		REMOVE_MAPPING,
		RENAME,
	}

	final class Chat implements Message {

		public final String user;
		public final String message;

		private Chat(String user, String message) {
			this.user = user;
			this.message = message;
		}

		private static Chat read(DataInput input) throws IOException {
			String user = input.readUTF();
			String message = input.readUTF();
			return Message.chat(user, message);
		}

		@Override
		public void write(DataOutput output) throws IOException {
			output.write(getType().ordinal());
			output.writeUTF(user);
			output.writeUTF(message);
		}

		@Override
		public String translate() {
			return String.format(I18n.translate("message.chat.text"), user, message);
		}

		@Override
		public Type getType() {
			return Type.CHAT;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			Chat chat = (Chat) o;
			return Objects.equals(user, chat.user) &&
					Objects.equals(message, chat.message);
		}

		@Override
		public int hashCode() {
			return Objects.hash(user, message);
		}

		@Override
		public String toString() {
			return String.format("Message.Chat { user: '%s', message: '%s'}", user, message);
		}

	}

	final class Connect implements Message {

		public final String user;

		private Connect(String user) {
			this.user = user;
		}

		private static Connect read(DataInput input) throws IOException {
			String user = input.readUTF();
			return Message.connect(user);
		}

		@Override
		public void write(DataOutput output) throws IOException {
			output.write(getType().ordinal());
			output.writeUTF(user);
		}

		@Override
		public String translate() {
			return String.format(I18n.translate("message.connect.text"), user);
		}

		@Override
		public Type getType() {
			return Type.CONNECT;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			Connect connect = (Connect) o;
			return Objects.equals(user, connect.user);
		}

		@Override
		public int hashCode() {
			return Objects.hash(user);
		}

		@Override
		public String toString() {
			return String.format("Message.Connect { user: '%s' }", user);
		}

	}

	final class Disconnect implements Message {

		public final String user;

		private Disconnect(String user) {
			this.user = user;
		}

		private static Disconnect read(DataInput input) throws IOException {
			String user = input.readUTF();
			return Message.disconnect(user);
		}

		@Override
		public void write(DataOutput output) throws IOException {
			output.write(getType().ordinal());
			output.writeUTF(user);
		}

		@Override
		public String translate() {
			return String.format(I18n.translate("message.disconnect.text"), user);
		}

		@Override
		public Type getType() {
			return Type.DISCONNECT;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			Disconnect that = (Disconnect) o;
			return Objects.equals(user, that.user);
		}

		@Override
		public int hashCode() {
			return Objects.hash(user);
		}

		@Override
		public String toString() {
			return String.format("Message.Disconnect { user: '%s' }", user);
		}

	}

	final class EditDocs implements Message {

		public final String user;
		public final Entry<?> entry;

		private EditDocs(String user, Entry<?> entry) {
			this.user = user;
			this.entry = entry;
		}

		private static EditDocs read(DataInput input) throws IOException {
			String user = input.readUTF();
			Entry<?> entry = PacketHelper.readEntry(input);
			return Message.editDocs(user, entry);
		}

		@Override
		public void write(DataOutput output) throws IOException {
			output.write(getType().ordinal());
			output.writeUTF(user);
			PacketHelper.writeEntry(output, entry);
		}

		@Override
		public String translate() {
			return String.format(I18n.translate("message.edit_docs.text"), user, entry);
		}

		@Override
		public Type getType() {
			return Type.EDIT_DOCS;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			EditDocs editDocs = (EditDocs) o;
			return Objects.equals(user, editDocs.user) &&
					Objects.equals(entry, editDocs.entry);
		}

		@Override
		public int hashCode() {
			return Objects.hash(user, entry);
		}

		@Override
		public String toString() {
			return String.format("Message.EditDocs { user: '%s', entry: %s }", user, entry);
		}

	}

	final class MarkDeobf implements Message {

		public final String user;
		public final Entry<?> entry;

		private MarkDeobf(String user, Entry<?> entry) {
			this.user = user;
			this.entry = entry;
		}

		private static MarkDeobf read(DataInput input) throws IOException {
			String user = input.readUTF();
			Entry<?> entry = PacketHelper.readEntry(input);
			return Message.markDeobf(user, entry);
		}

		@Override
		public void write(DataOutput output) throws IOException {
			output.write(getType().ordinal());
			output.writeUTF(user);
			PacketHelper.writeEntry(output, entry);
		}

		@Override
		public String translate() {
			return String.format(I18n.translate("message.mark_deobf.text"), user, entry);
		}

		@Override
		public Type getType() {
			return Type.MARK_DEOBF;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			MarkDeobf markDeobf = (MarkDeobf) o;
			return Objects.equals(user, markDeobf.user) &&
					Objects.equals(entry, markDeobf.entry);
		}

		@Override
		public int hashCode() {
			return Objects.hash(user, entry);
		}

		@Override
		public String toString() {
			return String.format("Message.MarkDeobf { user: '%s', entry: %s }", user, entry);
		}

	}

	final class RemoveMapping implements Message {

		public final String user;
		public final Entry<?> entry;

		private RemoveMapping(String user, Entry<?> entry) {
			this.user = user;
			this.entry = entry;
		}

		private static RemoveMapping read(DataInput input) throws IOException {
			String user = input.readUTF();
			Entry<?> entry = PacketHelper.readEntry(input);
			return Message.removeMapping(user, entry);
		}

		@Override
		public void write(DataOutput output) throws IOException {
			output.write(getType().ordinal());
			output.writeUTF(user);
			PacketHelper.writeEntry(output, entry);
		}

		@Override
		public String translate() {
			return String.format(I18n.translate("message.remove_mapping.text"), user, entry);
		}

		@Override
		public Type getType() {
			return Type.REMOVE_MAPPING;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			RemoveMapping removeMapping = (RemoveMapping) o;
			return Objects.equals(user, removeMapping.user) &&
					Objects.equals(entry, removeMapping.entry);
		}

		@Override
		public int hashCode() {
			return Objects.hash(user, entry);
		}

		@Override
		public String toString() {
			return String.format("Message.RemoveMapping { user: '%s', entry: %s }", user, entry);
		}

	}

	final class Rename implements Message {

		public final String user;
		public final Entry<?> entry;
		public final String newName;

		private Rename(String user, Entry<?> entry, String newName) {
			this.user = user;
			this.entry = entry;
			this.newName = newName;
		}

		private static Rename read(DataInput input) throws IOException {
			String user = input.readUTF();
			Entry<?> entry = PacketHelper.readEntry(input);
			String newName = input.readUTF();
			return Message.rename(user, entry, newName);
		}

		@Override
		public void write(DataOutput output) throws IOException {
			output.write(getType().ordinal());
			output.writeUTF(user);
			PacketHelper.writeEntry(output, entry);
			output.writeUTF(newName);
		}

		@Override
		public String translate() {
			return String.format(I18n.translate("message.rename.text"), user, entry, newName);
		}

		@Override
		public Type getType() {
			return Type.RENAME;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			Rename rename = (Rename) o;
			return Objects.equals(user, rename.user) &&
					Objects.equals(entry, rename.entry) &&
					Objects.equals(newName, rename.newName);
		}

		@Override
		public int hashCode() {
			return Objects.hash(user, entry, newName);
		}

		@Override
		public String toString() {
			return String.format("Message.Rename { user: '%s', entry: %s, newName: '%s' }", user, entry, newName);
		}

	}

}