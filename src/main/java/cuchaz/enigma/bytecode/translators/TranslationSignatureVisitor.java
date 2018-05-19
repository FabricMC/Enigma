package cuchaz.enigma.bytecode.translators;

import cuchaz.enigma.mapping.Translator;
import cuchaz.enigma.mapping.entry.ClassEntry;
import org.objectweb.asm.signature.SignatureVisitor;

import java.util.Stack;

public class TranslationSignatureVisitor extends SignatureVisitor {
	private final Translator translator;

	private final SignatureVisitor sv;
	private final Stack<ClassEntry> classes = new Stack<>();

	public TranslationSignatureVisitor(Translator translator, int api, SignatureVisitor sv) {
		super(api);
		this.translator = translator;
		this.sv = sv;
	}

	@Override
	public void visitClassType(String name) {
		ClassEntry entry = new ClassEntry(name);
		ClassEntry translatedEntry = this.translator.getTranslatedClass(entry);
		this.classes.push(entry);
		this.sv.visitClassType(translatedEntry.getName());
	}

	@Override
	public void visitInnerClassType(String name) {
		ClassEntry outerEntry = this.classes.pop();
		ClassEntry entry = new ClassEntry(outerEntry + "$" + name);
		this.classes.push(entry);
		String translatedEntry = this.translator.getTranslatedClass(entry).getName();
		this.sv.visitInnerClassType(translatedEntry.substring(translatedEntry.lastIndexOf('$') + 1));
	}

	@Override
	public void visitFormalTypeParameter(String name) {
		this.sv.visitFormalTypeParameter(name);
	}

	@Override
	public void visitTypeVariable(String name) {
		this.sv.visitTypeVariable(name);
	}

	@Override
	public SignatureVisitor visitArrayType() {
		this.sv.visitArrayType();
		return this;
	}

	@Override
	public void visitBaseType(char descriptor) {
		this.sv.visitBaseType(descriptor);
	}

	@Override
	public SignatureVisitor visitClassBound() {
		this.sv.visitClassBound();
		return this;
	}

	@Override
	public SignatureVisitor visitExceptionType() {
		this.sv.visitExceptionType();
		return this;
	}

	@Override
	public SignatureVisitor visitInterface() {
		this.sv.visitInterface();
		return this;
	}

	@Override
	public SignatureVisitor visitInterfaceBound() {
		this.sv.visitInterfaceBound();
		return this;
	}

	@Override
	public SignatureVisitor visitParameterType() {
		this.sv.visitParameterType();
		return this;
	}

	@Override
	public SignatureVisitor visitReturnType() {
		this.sv.visitReturnType();
		return this;
	}

	@Override
	public SignatureVisitor visitSuperclass() {
		this.sv.visitSuperclass();
		return this;
	}

	@Override
	public void visitTypeArgument() {
		this.sv.visitTypeArgument();
	}

	@Override
	public SignatureVisitor visitTypeArgument(char wildcard) {
		this.sv.visitTypeArgument(wildcard);
		return this;
	}

	@Override
	public void visitEnd() {
		this.sv.visitEnd();
		this.classes.pop();
	}

	@Override
	public String toString() {
		return this.sv.toString();
	}
}
