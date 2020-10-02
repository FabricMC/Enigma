package cuchaz.enigma.translation.representation;

import cuchaz.enigma.analysis.Access;
import org.objectweb.asm.Opcodes;

import java.lang.reflect.Modifier;

public class AccessFlags {
	public static final AccessFlags PRIVATE = new AccessFlags(Opcodes.ACC_PRIVATE);
	public static final AccessFlags PUBLIC = new AccessFlags(Opcodes.ACC_PUBLIC);
	public static final int ACCESS_LEVEL_PUBLIC = 4;
	public static final int ACCESS_LEVEL_PROTECTED = 3;
	public static final int ACCESS_LEVEL_PACKAGE_LOCAL = 2;
	public static final int ACCESS_LEVEL_PRIVATE = 1;

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

	public boolean isBridge() {
		return (flags & Opcodes.ACC_BRIDGE) != 0;
	}

	public boolean isFinal() {
		return (flags & Opcodes.ACC_FINAL) != 0;
	}

	public boolean isInterface() {
		return (flags & Opcodes.ACC_INTERFACE) != 0;
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

	public AccessFlags setBridge() {
		flags |= Opcodes.ACC_BRIDGE;
		return this;
	}

	@Deprecated
	public AccessFlags setBridged() {
		return setBridge();
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

	/**
	 * Adapted from https://github.com/JetBrains/intellij-community/blob/6472c347db91d11bbf02895a767198f9d884b119/java/java-psi-api/src/com/intellij/psi/util/PsiUtil.java#L389
	 * @return visibility access level on a 'weakness scale'
	 */
	public int getAccessLevel() {
		if (isPrivate()) {
			return ACCESS_LEVEL_PRIVATE;
		}
		if (isProtected()) {
			return ACCESS_LEVEL_PROTECTED;
		}
		if (isPublic()) {
			return ACCESS_LEVEL_PUBLIC;
		}
		return ACCESS_LEVEL_PACKAGE_LOCAL;
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof AccessFlags && ((AccessFlags) obj).flags == flags;
	}

	@Override
	public int hashCode() {
		return flags;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(Access.get(this).toString().toLowerCase());
		if (isStatic()) {
			builder.append(" static");
		}
		if (isSynthetic()) {
			builder.append(" synthetic");
		}
		if (isBridge()) {
			builder.append(" bridge");
		}
		return builder.toString();
	}
}
