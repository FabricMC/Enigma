package cuchaz.enigma.utils;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

public class AsmUtil {
	public static byte[] nodeToBytes(ClassNode node) {
		ClassWriter w = new ClassWriter(0);
		node.accept(w);
		return w.toByteArray();
	}

	public static ClassNode bytesToNode(byte[] bytes) {
		ClassReader r = new ClassReader(bytes);
		ClassNode node = new ClassNode();
		r.accept(node, 0);
		return node;
	}
}
