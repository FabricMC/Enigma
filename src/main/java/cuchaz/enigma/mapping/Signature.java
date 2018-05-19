package cuchaz.enigma.mapping;

import cuchaz.enigma.bytecode.translators.TranslationSignatureVisitor;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureVisitor;
import org.objectweb.asm.signature.SignatureWriter;

import java.util.function.Function;
import java.util.regex.Pattern;

public class Signature {
	private static final Pattern OBJECT_PATTERN = Pattern.compile(".*:Ljava/lang/Object;:.*");

	private final String signature;
	private final boolean isType;

	private Signature(String signature, boolean isType) {
		if (signature != null && OBJECT_PATTERN.matcher(signature).matches()) {
			signature = signature.replaceAll(":Ljava/lang/Object;:", "::");
		}

		this.signature = signature;
		this.isType = isType;
	}

	public static Signature createTypedSignature(String signature) {
		if (signature != null && !signature.isEmpty()) {
			return new Signature(signature, true);
		}
		return new Signature(null, true);
	}

	public static Signature createSignature(String signature) {
		if (signature != null && !signature.isEmpty()) {
			return new Signature(signature, false);
		}
		return new Signature(null, false);
	}

	public String getSignature() {
		return signature;
	}

	public boolean isType() {
		return isType;
	}

	public Signature remap(Function<String, String> remapper) {
		if (signature == null) {
			return this;
		}
		SignatureWriter writer = new SignatureWriter();
		SignatureVisitor visitor = new TranslationSignatureVisitor(remapper, writer);
		if (isType) {
			new SignatureReader(signature).acceptType(visitor);
		} else {
			new SignatureReader(signature).accept(visitor);
		}
		return new Signature(writer.toString(), isType);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Signature) {
			Signature other = (Signature) obj;
			return (other.signature == null && signature == null || other.signature != null
					&& signature != null && other.signature.equals(signature))
					&& other.isType == this.isType;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return signature.hashCode() | (isType ? 1 : 0) << 16;
	}

	@Override
	public String toString() {
		return signature;
	}
}
