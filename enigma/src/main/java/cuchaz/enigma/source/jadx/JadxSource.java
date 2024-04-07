package cuchaz.enigma.source.jadx;

import java.util.List;
import java.util.function.Function;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.objectweb.asm.tree.ClassNode;
import jadx.api.ICodeInfo;
import jadx.api.JadxArgs;
import jadx.api.JadxDecompiler;
import jadx.api.JavaClass;
import jadx.api.metadata.ICodeAnnotation;
import jadx.api.metadata.annotations.NodeDeclareRef;
import jadx.api.metadata.annotations.VarNode;
import jadx.api.metadata.annotations.VarRef;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.plugins.input.java.JavaInputPlugin;

import cuchaz.enigma.source.Source;
import cuchaz.enigma.source.SourceIndex;
import cuchaz.enigma.source.SourceSettings;
import cuchaz.enigma.source.Token;
import cuchaz.enigma.translation.mapping.EntryRemapper;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.FieldEntry;
import cuchaz.enigma.translation.representation.entry.LocalVariableEntry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;
import cuchaz.enigma.utils.AsmUtil;

public class JadxSource implements Source {
	private final SourceSettings settings;
	private final Function<EntryRemapper, JadxArgs> jadxArgsFactory;
	private final ClassNode classNode;
	private final EntryRemapper mapper;
	private final JadxHelper jadxHelper;
	private SourceIndex index;

	public JadxSource(SourceSettings settings, Function<EntryRemapper, JadxArgs> jadxArgsFactory, ClassNode classNode, @Nullable EntryRemapper mapper, JadxHelper jadxHelper) {
		this.settings = settings;
		this.jadxArgsFactory = jadxArgsFactory;
		this.classNode = classNode;
		this.mapper = mapper;
		this.jadxHelper = jadxHelper;
	}

	@Override
	public Source withJavadocs(EntryRemapper mapper) {
		return new JadxSource(settings, jadxArgsFactory, classNode, mapper, jadxHelper);
	}

	@Override
	public SourceIndex index() {
		ensureDecompiled();
		return index;
	}

	@Override
	public String asString() {
		ensureDecompiled();
		return index.getSource();
	}

	private void ensureDecompiled() {
		if (index != null) {
			return;
		}

		try (JadxDecompiler jadx = new JadxDecompiler(jadxArgsFactory.apply(mapper))) {
			jadx.addCustomCodeLoader(JavaInputPlugin.loadSingleClass(AsmUtil.nodeToBytes(classNode), classNode.name));
			jadx.load();
			JavaClass cls = jadx.getClasses().get(0);

			runWithFixedLineSeparator(() -> {
				index = new SourceIndex(cls.getCode());
			});

			// Tokens
			cls.getCodeInfo().getCodeMetadata().searchDown(0, (pos, ann) -> {
				processAnnotatedElement(pos, ann, cls.getCodeInfo());
				return null;
			});
		}
	}

	/**
	 * JADX uses the system default line ending, but JEditorPane does not (seems to be hardcoded to \n).
	 * This causes tokens to be offset by one char per preceding line, since Windows' \r\n is one char longer than plain \r or \n.
	 * Unfortunately, the only way of making JADX use a different value is by changing the system property, which may cause issues
	 * elsewhere in the program. That's why we immediately reset it to the default after the runnable has been executed.
	 * TODO: Remove once https://github.com/skylot/jadx/issues/1948 is addressed.
	 */
	private void runWithFixedLineSeparator(Runnable runnable) {
		String propertyKey = "line.separator";
		String oldLineSeparator = System.getProperty(propertyKey);
		System.setProperty(propertyKey, "\n");

		runnable.run();

		System.getProperties().setProperty(propertyKey, oldLineSeparator);
	}

	private void processAnnotatedElement(int pos, ICodeAnnotation ann, ICodeInfo codeInfo) {
		if (ann == null) return;

		if (ann instanceof NodeDeclareRef ref) {
			processAnnotatedElement(pos, ref.getNode(), codeInfo);
		} else if (ann instanceof jadx.core.dex.nodes.ClassNode cls) {
			Token token = new Token(pos, pos + cls.getShortName().length(), cls.getShortName());

			if (pos == cls.getDefPosition()) {
				index.addDeclaration(token, classEntryOf(cls));
			} else {
				index.addReference(token, classEntryOf(cls), classEntryOf(cls.getParentClass()));
			}
		} else if (ann instanceof FieldNode fld) {
			Token token = new Token(pos, pos + fld.getName().length(), fld.getName());

			if (pos == fld.getDefPosition()) {
				index.addDeclaration(token, fieldEntryOf(fld));
			} else {
				index.addReference(token, fieldEntryOf(fld), classEntryOf(fld.getParentClass()));
			}
		} else if (ann instanceof MethodNode mth) {
			if (mth.getName().equals("<clinit>")) return;
			Token token = new Token(pos, pos + mth.getName().length(), mth.getName());

			if (mth.isConstructor()) {
				processAnnotatedElement(pos, mth.getTopParentClass(), codeInfo);
			} else if (pos == mth.getDefPosition()) {
				index.addDeclaration(token, methodEntryOf(mth));
			} else {
				index.addReference(token, methodEntryOf(mth), classEntryOf(mth.getParentClass()));
			}
		} else if (ann instanceof VarNode var) {
			if (!getMethodArgs(var.getMth(), codeInfo).contains(var)) return;
			Token token = new Token(pos, pos + var.getName().length(), var.getName());

			if (pos == var.getDefPosition()) {
				index.addDeclaration(token, paramEntryOf(var, codeInfo));
			} else {
				index.addReference(token, paramEntryOf(var, codeInfo), methodEntryOf(var.getMth()));
			}
		} else if (ann instanceof VarRef varRef) {
			processAnnotatedElement(pos, codeInfo.getCodeMetadata().getAt(varRef.getRefPos()), codeInfo);
		}
	}

	private ClassEntry classEntryOf(jadx.core.dex.nodes.ClassNode cls) {
		return jadxHelper.classEntryOf(cls);
	}

	private FieldEntry fieldEntryOf(FieldNode fld) {
		return jadxHelper.fieldEntryOf(fld);
	}

	private MethodEntry methodEntryOf(MethodNode mth) {
		return jadxHelper.methodEntryOf(mth);
	}

	private LocalVariableEntry paramEntryOf(VarNode param, ICodeInfo codeInfo) {
		return jadxHelper.paramEntryOf(param, codeInfo);
	}

	private List<VarNode> getMethodArgs(MethodNode mth, ICodeInfo codeInfo) {
		return jadxHelper.getMethodArgs(mth, codeInfo);
	}
}
