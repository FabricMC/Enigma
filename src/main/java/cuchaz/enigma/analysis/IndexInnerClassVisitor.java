package cuchaz.enigma.analysis;

import cuchaz.enigma.translation.representation.ClassEntry;
import org.objectweb.asm.ClassVisitor;

public class IndexInnerClassVisitor extends ClassVisitor {
	private final JarIndex index;

	public IndexInnerClassVisitor(JarIndex index, int api) {
		super(api);
		this.index = index;
	}

	public IndexInnerClassVisitor(JarIndex index, int api, ClassVisitor cv) {
		super(api, cv);
		this.index = index;
	}

	@Override
	public void visitInnerClass(String name, String outerName, String innerName, int access) {
		ClassEntry entry = new ClassEntry(name);
		// Ignore anonymous classes
		if (innerName != null && outerName != null) {
			ClassEntry outerEntry = new ClassEntry(outerName);
			index.indexInnerClass(entry, outerEntry);
		}
		super.visitInnerClass(name, outerName, innerName, access);
	}
}
