package cuchaz.enigma.analysis;

import cuchaz.enigma.analysis.index.JarIndex;
import cuchaz.enigma.translation.Translator;
import cuchaz.enigma.translation.representation.TypeDescriptor;
import cuchaz.enigma.translation.representation.entry.*;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.List;

public class StructureTreeNode extends DefaultMutableTreeNode {
    private final Translator translator;
    private final ClassEntry parentEntry;
    private final ParentedEntry entry;

    public StructureTreeNode(Translator translator, ClassEntry parentEntry, ParentedEntry entry) {
        this.translator = translator;
        this.parentEntry = parentEntry;
        this.entry = entry;
    }

    /**
     * Returns the parented entry corresponding to this tree node.
     */
    public ParentedEntry getEntry() {
        return this.entry;
    }

    public void load(JarIndex jarIndex, boolean hideDeobfuscated) {
        List<ParentedEntry> children = jarIndex.getChildrenByClass().get(this.parentEntry);

        for (ParentedEntry child : children) {
            StructureTreeNode childNode = new StructureTreeNode(this.translator, this.parentEntry, child);

            if (child instanceof ClassEntry) {
                childNode = new StructureTreeNode(this.translator, (ClassEntry) child, child);
                childNode.load(jarIndex, hideDeobfuscated);
            }

            // don't add deobfuscated members if hideDeobfuscated is true, unless it's an inner class
            if (hideDeobfuscated && this.translator.extendedTranslate(child).isDeobfuscated() && !(child instanceof ClassEntry)) {
                continue;
            }

            // don't add constructor methods if hideDeobfuscated is true
            if (hideDeobfuscated && (child instanceof MethodEntry) && ((MethodEntry) child).isConstructor()) {
                continue;
            }

            this.add(childNode);
        }
    }

    @Override
    public String toString() {
        ParentedEntry translatedEntry = this.translator.extendedTranslate(this.entry).getValue();
        String result = translatedEntry.getName();

        if (this.entry instanceof FieldDefEntry) {
            FieldDefEntry field = (FieldDefEntry) translatedEntry;
            String returnType = this.parseDesc(field.getDesc());

            result = result + ": " + returnType;
        } else if (this.entry instanceof MethodDefEntry) {
            MethodDefEntry method = (MethodDefEntry) translatedEntry;
            String args = this.parseArgs(method.getDesc().getArgumentDescs());
            String returnType = this.parseDesc(method.getDesc().getReturnDesc());

            if (method.isConstructor()) {
                result = method.getParent().getSimpleName() + args;
            } else {
                result = result + args + ": " + returnType;
            }
        }

        return result;
    }

    private String parseArgs(List<TypeDescriptor> args) {
        if (args.size() > 0) {
            String result = "(";

            for (int i = 0; i < args.size(); i++) {
                if (i > 0) {
                    result += ", ";
                }

                result += this.parseDesc(args.get(i));
            }

            return result + ")";
        }

        return "()";
    }

    private String parseDesc(TypeDescriptor desc) {
        if (desc.isVoid()) return "void";
        if (desc.isPrimitive()) return desc.getPrimitive().getKeyword();
        if (desc.isType()) return desc.getTypeEntry().getSimpleName();

        if (desc.isArray()) {
            if (desc.getArrayType().isPrimitive()) return desc.getArrayType().getPrimitive().getKeyword() + "[]";
            if (desc.getArrayType().isType()) return desc.getArrayType().getTypeEntry().getSimpleName() + "[]";
        }

        return null;
    }
}
