package cuchaz.enigma.classhandle;

import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public final class ClassHandleError {

	public final Type type;
	public final Throwable cause;

	private ClassHandleError(Type type, Throwable cause) {
		this.type = type;
		this.cause = cause;
	}

	@Nullable
	public String getStackTrace() {
		if (cause == null) return null;
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		PrintStream ps = new PrintStream(os);
		cause.printStackTrace(ps);
		return os.toString();
	}

	public static ClassHandleError decompile(Throwable cause) {
		return new ClassHandleError(Type.DECOMPILE, cause);
	}

	public static ClassHandleError remap(Throwable cause) {
		return new ClassHandleError(Type.REMAP, cause);
	}

	public enum Type {
		DECOMPILE,
		REMAP,
	}

}