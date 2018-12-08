package cuchaz.enigma.analysis;

import cuchaz.enigma.mapping.entry.ClassDefEntry;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;

public class IndexClassVisitor extends ClassVisitor {
	private final JarIndex index;
	private ClassDefEntry classEntry;

	public IndexClassVisitor(JarIndex index, int api) {
		super(api);
		this.index = index;
	}

	public IndexClassVisitor(JarIndex index, int api, ClassVisitor cv) {
		super(api, cv);
		this.index = index;
	}

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		this.classEntry = this.index.indexClass(access, name, signature, superName, interfaces);
		super.visit(version, access, name, signature, superName, interfaces);
	}

	@Override
	public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
		if (this.classEntry != null) {
			this.index.indexField(this.classEntry, access, name, desc, signature);
		}
		return super.visitField(access, name, desc, signature, value);
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		if (this.classEntry != null) {
			this.index.indexMethod(this.classEntry, access, name, desc, signature);
		}
		return super.visitMethod(access, name, desc, signature, exceptions);
	}
}
