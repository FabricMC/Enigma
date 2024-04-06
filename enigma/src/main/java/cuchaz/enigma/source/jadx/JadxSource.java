package cuchaz.enigma.source.jadx;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.objectweb.asm.tree.ClassNode;
import com.google.common.base.Strings;
import jadx.api.ICodeInfo;
import jadx.api.JadxArgs;
import jadx.api.JadxDecompiler;
import jadx.api.JavaClass;
import jadx.api.JavaField;
import jadx.api.JavaMethod;
import jadx.api.impl.InMemoryCodeCache;
import jadx.api.metadata.ICodeAnnotation;
import jadx.api.metadata.annotations.NodeDeclareRef;
import jadx.api.metadata.annotations.VarNode;
import jadx.api.metadata.annotations.VarRef;
import jadx.api.utils.CodeUtils;
import jadx.core.codegen.TypeGen;
import jadx.core.dex.info.MethodInfo;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.plugins.input.java.JavaInputPlugin;

import cuchaz.enigma.source.Source;
import cuchaz.enigma.source.SourceIndex;
import cuchaz.enigma.source.SourceSettings;
import cuchaz.enigma.source.Token;
import cuchaz.enigma.translation.mapping.EntryRemapper;
import cuchaz.enigma.translation.representation.MethodDescriptor;
import cuchaz.enigma.translation.representation.TypeDescriptor;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.FieldEntry;
import cuchaz.enigma.translation.representation.entry.LocalVariableEntry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;
import cuchaz.enigma.utils.AsmUtil;

public class JadxSource implements Source {
	private final SourceSettings settings;
	private final Supplier<JadxArgs> jadxArgsSupplier;
	private final ClassNode classNode;
	private final EntryRemapper mapper;
	private SourceIndex index;

	public JadxSource(SourceSettings settings, Supplier<JadxArgs> jadxArgsSupplier, ClassNode classNode, @Nullable EntryRemapper mapper) {
		this.settings = settings;
		this.jadxArgsSupplier = jadxArgsSupplier;
		this.classNode = classNode;
		this.mapper = mapper;
	}

	@Override
	public Source withJavadocs(EntryRemapper mapper) {
		return new JadxSource(settings, jadxArgsSupplier, classNode, mapper);
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

		try (JadxDecompiler jadx = new JadxDecompiler(jadxArgsSupplier.get())) {
			jadx.addCustomCodeLoader(JavaInputPlugin.loadSingleClass(AsmUtil.nodeToBytes(classNode), classNode.name));
			jadx.load();
			JavaClass cls = jadx.getClasses().get(0);

			runWithFixedLineSeparator(() -> {
				try {
					// Javadocs
					// TODO: Make this less hacky
					if (mapper != null) {
						int reload = 0;
						String comment;

						for (JavaField fld : cls.getFields()) {
							if ((comment = Strings.emptyToNull(mapper.getDeobfMapping(fieldEntryOf(fld.getFieldNode())).javadoc())) != null) {
								fld.getFieldNode().addCodeComment(comment);
								reload = 1;
							}
						}

						for (JavaMethod mth : cls.getMethods()) {
							if ((comment = Strings.emptyToNull(mapper.getDeobfMapping(methodEntryOf(mth.getMethodNode())).javadoc())) != null) {
								mth.getMethodNode().addCodeComment(comment);
								reload = 1;
							}
						}

						if (reload == 1) {
							jadx.getArgs().getCodeCache().close();
							jadx.getArgs().setCodeCache(new InMemoryCodeCache());
							reload = 2;
						}

						if ((comment = Strings.emptyToNull(mapper.getDeobfMapping(classEntryOf(cls.getClassNode())).javadoc())) != null) {
							cls.getClassNode().addCodeComment(comment);
							if (reload != 2) reload = 1;
						}

						if (reload == 1) {
							jadx.getArgs().getCodeCache().close();
							jadx.getArgs().setCodeCache(new InMemoryCodeCache());
						}
					}

					index = new SourceIndex(cls.getCode());
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			});

			// Tokens
			cls.getCodeInfo().getCodeMetadata().searchDown(0, (pos, ann) -> {
				processAnnotatedElement(pos, ann, cls.getCodeInfo());
				return null;
			});
		} catch (Exception e) {
			throw new RuntimeException(e);
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

	private Map<jadx.core.dex.nodes.ClassNode, String> internalNames = new HashMap<>();
	private Map<jadx.core.dex.nodes.ClassNode, ClassEntry> classMap = new HashMap<>();
	private Map<FieldNode, FieldEntry> fieldMap = new HashMap<>();
	private Map<MethodNode, MethodEntry> methodMap = new HashMap<>();
	private Map<VarNode, LocalVariableEntry> varMap = new HashMap<>();
	private Map<MethodNode, List<VarNode>> argMap = new HashMap<>();

	private String internalNameOf(jadx.core.dex.nodes.ClassNode cls) {
		return internalNames.computeIfAbsent(cls, (unused) -> cls.getClassInfo().makeRawFullName().replace('.', '/'));
	}

	private ClassEntry classEntryOf(jadx.core.dex.nodes.ClassNode cls) {
		if (cls == null) return null;
		return classMap.computeIfAbsent(cls, (unused) -> new ClassEntry(internalNameOf(cls)));
	}

	private FieldEntry fieldEntryOf(FieldNode fld) {
		return fieldMap.computeIfAbsent(fld, (unused) ->
				new FieldEntry(classEntryOf(fld.getParentClass()), fld.getName(), new TypeDescriptor(TypeGen.signature(fld.getType()))));
	}

	private MethodEntry methodEntryOf(MethodNode mth) {
		return methodMap.computeIfAbsent(mth, (unused) -> {
			MethodInfo mthInfo = mth.getMethodInfo();
			MethodDescriptor desc = new MethodDescriptor(mthInfo.getShortId().substring(mthInfo.getName().length()));
			return new MethodEntry(classEntryOf(mth.getParentClass()), mthInfo.getName(), desc);
		});
	}

	private LocalVariableEntry paramEntryOf(VarNode param, ICodeInfo codeInfo) {
		return varMap.computeIfAbsent(param, (unused) -> {
			MethodEntry owner = methodEntryOf(param.getMth());
			int index = getMethodArgs(param.getMth(), codeInfo).indexOf(param);
			return new LocalVariableEntry(owner, index, param.getName(), true, null);
		});
	}

	private List<VarNode> getMethodArgs(MethodNode mth, ICodeInfo codeInfo) {
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
}
