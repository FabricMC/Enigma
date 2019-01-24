package cuchaz.enigma;

import com.strobel.assembler.metadata.ITypeLoader;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import java.util.List;

/**
 * For delegation of TranslatingTypeLoader without needing the subclass the whole thing
 */
public interface ITranslatingTypeLoader extends ITypeLoader {
	List<String> getClassNamesToTry(String className);

	List<String> getClassNamesToTry(ClassEntry obfClassEntry);

	String transformInto(ClassNode node, ClassWriter writer);
}
