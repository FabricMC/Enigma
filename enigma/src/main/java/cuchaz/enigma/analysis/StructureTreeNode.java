package cuchaz.enigma.analysis;

import cuchaz.enigma.EnigmaProject;
import cuchaz.enigma.api.service.NameProposalService;
import cuchaz.enigma.api.service.ObfuscationTestService;
import cuchaz.enigma.translation.mapping.EntryRemapper;
import cuchaz.enigma.translation.representation.TypeDescriptor;
import cuchaz.enigma.translation.representation.entry.*;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.List;

public class StructureTreeNode extends DefaultMutableTreeNode {
    private final List<NameProposalService> nameProposalServices;
    private final EntryRemapper mapper;
    private final ClassEntry parentEntry;
    private final ParentedEntry entry;

    public StructureTreeNode(EnigmaProject project, ClassEntry parentEntry, ParentedEntry entry) {
        this.nameProposalServices = project.getEnigma().getServices().get(NameProposalService.TYPE);
        this.mapper = project.getMapper();
        this.parentEntry = parentEntry;
        this.entry = entry;
    }

    /**
     * Returns the parented entry corresponding to this tree node.
     */
    public ParentedEntry getEntry() {
        return this.entry;
    }

    public void load(EnigmaProject project, boolean hideDeobfuscated) {
        List<ParentedEntry> children = project.getJarIndex().getChildrenByClass().get(this.parentEntry);

        for (ParentedEntry child : children) {
            StructureTreeNode childNode = new StructureTreeNode(project, this.parentEntry, child);

            if (child instanceof ClassEntry) {
                childNode = new StructureTreeNode(project, (ClassEntry) child, child);
                childNode.load(project, hideDeobfuscated);
            }

            // don't add deobfuscated members if hideDeobfuscated is true, unless it's an inner class
            if (hideDeobfuscated && this.isDeobfuscated(project, child) && !(child instanceof ClassEntry)) {
                continue;
            }

            // don't add constructor methods if hideDeobfuscated is true
            if (hideDeobfuscated && (child instanceof MethodEntry) && ((MethodEntry) child).isConstructor()) {
                continue;
            }

            this.add(childNode);
        }
    }

    private boolean isDeobfuscated(EnigmaProject project, ParentedEntry child) {
        List<ObfuscationTestService> obfuscationTestServices = project.getEnigma().getServices().get(ObfuscationTestService.TYPE);

        if (!obfuscationTestServices.isEmpty()) {
            for (ObfuscationTestService service : obfuscationTestServices) {
                if (service.testDeobfuscated(child)) {
                    return true;
                }
            }
        }

        if (!this.nameProposalServices.isEmpty()) {
            for (NameProposalService service : this.nameProposalServices) {
                if (service.proposeName(child, this.mapper).isPresent()) {
                    return true;
                }
            }
        }

        String mappedName = project.getMapper().deobfuscate(child).getName();
        if (mappedName != null && !mappedName.isEmpty() && !mappedName.equals(child.getName())) {
            return true;
        }

        return false;
    }

    @Override
    public String toString() {
        ParentedEntry translatedEntry = this.mapper.deobfuscate(this.entry);
        String result = translatedEntry.getName();

        if (!this.nameProposalServices.isEmpty()) {
            for (NameProposalService service : this.nameProposalServices) {
                if (service.proposeName(this.entry, this.mapper).isPresent()) {
                    result = service.proposeName(this.entry, this.mapper).get();
                }
            }
        }

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
