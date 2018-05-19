package cuchaz.enigma.bytecode.translators;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.signature.SignatureVisitor;

import java.util.Stack;
import java.util.function.Function;

public class TranslationSignatureVisitor extends SignatureVisitor {
	private final Function<String, String> remapper;

	private final SignatureVisitor sv;
	private final Stack<String> classes = new Stack<>();

	public TranslationSignatureVisitor(Function<String, String> remapper, SignatureVisitor sv) {
		super(Opcodes.ASM5);
		this.remapper = remapper;
		this.sv = sv;
	}

	@Override
	public void visitClassType(String name) {
		String translatedEntry = this.remapper.apply(name);
		this.classes.push(name);
		this.sv.visitClassType(translatedEntry);
	}

	@Override
	public void visitInnerClassType(String name) {
		String outerName = this.classes.pop();
		String className = outerName + "$" + name;
		this.classes.push(className);
		String translatedEntry = this.remapper.apply(className);
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
