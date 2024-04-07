package cuchaz.enigma.source.jadx;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import jadx.api.ICodeInfo;
import jadx.api.metadata.annotations.NodeDeclareRef;
import jadx.api.metadata.annotations.VarNode;
import jadx.api.utils.CodeUtils;
import jadx.core.codegen.TypeGen;
import jadx.core.dex.info.MethodInfo;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.MethodNode;

import cuchaz.enigma.translation.representation.MethodDescriptor;
import cuchaz.enigma.translation.representation.TypeDescriptor;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.FieldEntry;
import cuchaz.enigma.translation.representation.entry.LocalVariableEntry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;

class JadxHelper {
	private Map<jadx.core.dex.nodes.ClassNode, String> internalNames = new HashMap<>();
	private Map<jadx.core.dex.nodes.ClassNode, ClassEntry> classMap = new HashMap<>();
	private Map<FieldNode, FieldEntry> fieldMap = new HashMap<>();
	private Map<MethodNode, MethodEntry> methodMap = new HashMap<>();
	private Map<VarNode, LocalVariableEntry> varMap = new HashMap<>();
	private Map<MethodNode, List<VarNode>> argMap = new HashMap<>();

	private String internalNameOf(jadx.core.dex.nodes.ClassNode cls) {
		return internalNames.computeIfAbsent(cls, (unused) -> cls.getClassInfo().makeRawFullName().replace('.', '/'));
	}

	ClassEntry classEntryOf(jadx.core.dex.nodes.ClassNode cls) {
		if (cls == null) return null;
		return classMap.computeIfAbsent(cls, (unused) -> new ClassEntry(internalNameOf(cls)));
	}

	FieldEntry fieldEntryOf(FieldNode fld) {
		return fieldMap.computeIfAbsent(fld, (unused) ->
				new FieldEntry(classEntryOf(fld.getParentClass()), fld.getName(), new TypeDescriptor(TypeGen.signature(fld.getType()))));
	}

	MethodEntry methodEntryOf(MethodNode mth) {
		return methodMap.computeIfAbsent(mth, (unused) -> {
			MethodInfo mthInfo = mth.getMethodInfo();
			MethodDescriptor desc = new MethodDescriptor(mthInfo.getShortId().substring(mthInfo.getName().length()));
			return new MethodEntry(classEntryOf(mth.getParentClass()), mthInfo.getName(), desc);
		});
	}

	LocalVariableEntry paramEntryOf(VarNode param, ICodeInfo codeInfo) {
		return varMap.computeIfAbsent(param, (unused) -> {
			MethodEntry owner = methodEntryOf(param.getMth());
			int index = getMethodArgs(param.getMth(), codeInfo).indexOf(param);
			return new LocalVariableEntry(owner, index, param.getName(), true, null);
		});
	}

	List<VarNode> getMethodArgs(MethodNode mth, ICodeInfo codeInfo) {
		return argMap.computeIfAbsent(mth, (unused) -> {
			int mthDefPos = mth.getDefPosition();
			int lineEndPos = CodeUtils.getLineEndForPos(codeInfo.getCodeStr(), mthDefPos);
			List<VarNode> args = new ArrayList<>();
			codeInfo.getCodeMetadata().searchDown(mthDefPos, (pos, ann) -> {
				if (pos > lineEndPos) {
					// Stop at line end
					return Boolean.TRUE;
				}

				if (ann instanceof NodeDeclareRef ref && ref.getNode() instanceof VarNode varNode) {
					if (!varNode.getMth().equals(mth)) {
						// Stop if we've gone too far and have entered a different method
						return Boolean.TRUE;
					}

					args.add(varNode);
				}

				return null;
			});

			return args;
		});
	}

	boolean isRecord(jadx.core.dex.nodes.ClassNode cls) {
		if (cls.getSuperClass() == null || !cls.getSuperClass().isObject()) {
			return false;
		}

		return Objects.equals(cls.getSuperClass().getObject(), "java/lang/Record");
	}
}
