package cuchaz.enigma.translation.representation;

import java.util.function.Function;
import java.util.regex.Pattern;

import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureVisitor;
import org.objectweb.asm.signature.SignatureWriter;

import cuchaz.enigma.bytecode.translators.TranslationSignatureVisitor;
import cuchaz.enigma.translation.Translatable;
import cuchaz.enigma.translation.TranslateResult;
import cuchaz.enigma.translation.Translator;
import cuchaz.enigma.translation.mapping.EntryMap;
import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.mapping.EntryResolver;
import cuchaz.enigma.translation.representation.entry.ClassEntry;

public class Signature implements Translatable {
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
			return (other.signature == null && signature == null || other.signature != null && signature != null && other.signature.equals(signature)) && other.isType == this.isType;
		}

		return false;
	}

	@Override
	public int hashCode() {
		int hash = (isType ? 1 : 0) << 16;

		if (signature != null) {
			hash |= signature.hashCode();
		}

		return hash;
	}

	@Override
	public String toString() {
		return signature;
	}

	@Override
	public TranslateResult<Signature> extendedTranslate(Translator translator, EntryResolver resolver, EntryMap<EntryMapping> mappings) {
		return TranslateResult.ungrouped(this.remap(name -> translator.translate(new ClassEntry(name)).getFullName()));
	}
}
