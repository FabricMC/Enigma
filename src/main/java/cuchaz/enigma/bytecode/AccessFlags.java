package cuchaz.enigma.bytecode;

import org.objectweb.asm.Opcodes;

import java.lang.reflect.Modifier;

public class AccessFlags {
	private int flags;

	public AccessFlags(int flags) {
		this.flags = flags;
	}

	public boolean isPrivate() {
		return Modifier.isPrivate(this.flags);
	}

	public boolean isProtected() {
		return Modifier.isProtected(this.flags);
	}

	public boolean isPublic() {
		return Modifier.isPublic(this.flags);
	}

	public boolean isSynthetic() {
		return (this.flags & Opcodes.ACC_SYNTHETIC) != 0;
	}

	public boolean isStatic() {
		return Modifier.isStatic(this.flags);
	}

	public boolean isEnum() {
		return (flags & Opcodes.ACC_ENUM) != 0;
	}

	public AccessFlags setPrivate() {
		this.setVisibility(Opcodes.ACC_PRIVATE);
		return this;
	}

	public AccessFlags setProtected() {
		this.setVisibility(Opcodes.ACC_PROTECTED);
		return this;
	}

	public AccessFlags setPublic() {
		this.setVisibility(Opcodes.ACC_PUBLIC);
		return this;
	}

	public AccessFlags setBridged() {
		this.setVisibility(Opcodes.ACC_BRIDGE);
		return this;
	}

	public void setVisibility(int visibility) {
		this.resetVisibility();
		this.flags |= visibility;
	}

	private void resetVisibility() {
		this.flags &= ~(Opcodes.ACC_PRIVATE | Opcodes.ACC_PROTECTED | Opcodes.ACC_PUBLIC);
	}

	public int getFlags() {
		return this.flags;
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof AccessFlags && ((AccessFlags) obj).flags == flags;
	}

	@Override
	public int hashCode() {
		return flags;
	}
}
