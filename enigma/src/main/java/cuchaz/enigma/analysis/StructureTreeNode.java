package cuchaz.enigma.analysis;

import cuchaz.enigma.EnigmaProject;
import cuchaz.enigma.api.service.NameProposalService;
import cuchaz.enigma.translation.TranslateResult;
import cuchaz.enigma.translation.mapping.EntryRemapper;
import cuchaz.enigma.translation.representation.AccessFlags;
import cuchaz.enigma.translation.representation.TypeDescriptor;
import cuchaz.enigma.translation.representation.entry.*;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.ArrayList;
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
     * Returns the parented entry represented by this tree node.
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
            if (hideDeobfuscated && !project.isObfuscated(child) && !(child instanceof ClassEntry)) {
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
        TranslateResult<ParentedEntry> translateResult = this.mapper.extendedDeobfuscate(this.entry);
        String result = translateResult.getValue().getName();

        if (translateResult.isObfuscated()) {
            if (!this.nameProposalServices.isEmpty()) {
                for (NameProposalService service : this.nameProposalServices) {
                    if (service.proposeName(this.entry, this.mapper).isPresent()) {
                        result = service.proposeName(this.entry, this.mapper).get();
                    }
                }
            }
        }

        if (this.entry instanceof FieldDefEntry) {
            FieldDefEntry field = (FieldDefEntry) translateResult.getValue();
            String returnType = this.parseDesc(field.getDesc());

            result = result + ": " + returnType;
        } else if (this.entry instanceof MethodDefEntry) {
            MethodDefEntry method = (MethodDefEntry) translateResult.getValue();
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

    public String toHtml() {
        List<String> modifiers = new ArrayList<>();

        if (this.entry instanceof DefEntry<?>) {
            AccessFlags access = ((DefEntry<?>) this.entry).getAccess();
            boolean isInterfaceMethod = false;

            if (this.entry instanceof MethodEntry && this.entry.getParent() instanceof ClassDefEntry) {
                isInterfaceMethod = ((ClassDefEntry) this.entry.getParent()).getAccess().isInterface();
            }

            if (access.isStatic() && !access.isEnum()) {
                // Static member, but not an enum constant
                modifiers.add("static");
            } else if (isInterfaceMethod && !access.isAbstract()) {
                // Non-static default interface method
                modifiers.add("default");
            }

            if (access.isAbstract() && !access.isInterface() && !isInterfaceMethod && !access.isEnum()) {
                // Abstract, but not an interface, an interface method or an enum class (abstract is the default or meaningless)
                modifiers.add("abstract");
            } else if (access.isFinal() && !access.isEnum()) {
                // Final, but not an enum or an enum constant (they're always final)
                modifiers.add("final");
            }
        }

        return "<i>" + String.join(" ", modifiers) + "</i> " + toString();
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
