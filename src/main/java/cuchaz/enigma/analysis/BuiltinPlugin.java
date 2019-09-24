package cuchaz.enigma.analysis;

import com.strobel.core.Pair;
import cuchaz.enigma.api.EnigmaPlugin;
import cuchaz.enigma.api.EnigmaPluginContext;
import cuchaz.enigma.api.service.JarIndexerService;
import cuchaz.enigma.api.service.NameProposalService;
import cuchaz.enigma.translation.mapping.ResolutionStrategy;
import cuchaz.enigma.translation.representation.TypeDescriptor;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.Entry;
import cuchaz.enigma.translation.representation.entry.FieldEntry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.SourceInterpreter;
import org.objectweb.asm.tree.analysis.SourceValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.UnaryOperator;

public final class BuiltinPlugin implements EnigmaPlugin {

	private UnaryOperator<MethodEntry> specializedToBridge;

	public BuiltinPlugin() {
	}

	@Override
	public void init(EnigmaPluginContext ctx) {
		registerEnumNamingService(ctx);
	}

	private void registerEnumNamingService(EnigmaPluginContext ctx) {
		final Map<Entry<?>, String> names = new HashMap<>();
		final EnumFieldNameFindingVisitor visitor = new EnumFieldNameFindingVisitor(names);

		ctx.registerService("enigma:enum_initializer_indexer", JarIndexerService.TYPE, ctx1 -> (classCache, jarIndex) -> classCache.visit(() -> visitor, ClassReader.SKIP_FRAMES));
		ctx.registerService("enigma:enum_name_proposer", NameProposalService.TYPE, ctx1 -> (obfEntry, remapper) -> Optional.ofNullable(names.get(obfEntry)));
	}

	// TODO make this actually work; currently the resolver does not resolve specific methods that implement bridge methods
	private void registerBridgeNamingService(EnigmaPluginContext ctx) {
		ctx.registerService("enigma:specialized_bridge_method_indexer", JarIndexerService.TYPE, ctx1 -> (classCache, jarIndex) -> {BuiltinPlugin.this.specializedToBridge = jarIndex.getBridgeMethodIndex()::getBridgeFromSpecialized;
			jarIndex.getBridgeMethodIndex().getSpecializedToBridge().entrySet().forEach(System.out::println);});
		ctx.registerService("enigma:specialized_method_name_proposer", NameProposalService.TYPE, ctx1 -> (obfEntry, remapper) -> {
			System.out.println(obfEntry);
			if (!(obfEntry instanceof MethodEntry))
				return Optional.empty();
			MethodEntry bridge = BuiltinPlugin.this.specializedToBridge.apply((MethodEntry) obfEntry);
			if (bridge == null)
				return Optional.empty();
			return Optional.of(remapper.deobfuscate(remapper.getObfResolver().resolveFirstEntry((MethodEntry) obfEntry, ResolutionStrategy.RESOLVE_ROOT)).getName()); // People never name bridge methods!
		});
	}

	private static final class EnumFieldNameFindingVisitor extends ClassVisitor {

		private ClassEntry clazz;
		private String className;
		private final Map<Entry<?>, String> mappings;
		private final Set<Pair<String, String>> enumFields = new HashSet<>();
		private final List<MethodNode> classInits = new ArrayList<>();

		EnumFieldNameFindingVisitor(Map<Entry<?>, String> mappings) {
			super(Opcodes.ASM7);
			this.mappings = mappings;
		}

		@Override
		public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
			super.visit(version, access, name, signature, superName, interfaces);
			this.className = name;
			this.clazz = new ClassEntry(name);
			this.enumFields.clear();
			this.classInits.clear();
		}

		@Override
		public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
			if ((access & Opcodes.ACC_ENUM) != 0) {
				if (!enumFields.add(new Pair<>(name, descriptor))) {
					throw new IllegalArgumentException("Found two enum fields with the same name \"" + name + "\" and desc \"" + descriptor + "\"!");
				}
			}
			return super.visitField(access, name, descriptor, signature, value);
		}

		@Override
		public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
			if ("<clinit>".equals(name)) {
				MethodNode node = new MethodNode(api, access, name, descriptor, signature, exceptions);
				classInits.add(node);
				return node;
			}
			return super.visitMethod(access, name, descriptor, signature, exceptions);
		}

		@Override
		public void visitEnd() {
			super.visitEnd();
			try {
				collectResults();
			} catch (Exception ex) {
				throw new RuntimeException(ex);
			}
		}

		private void collectResults() throws Exception {
			String owner = className;
			Analyzer<SourceValue> analyzer = new Analyzer<>(new SourceInterpreter());

			for (MethodNode mn : classInits) {
				Frame<SourceValue>[] frames = analyzer.analyze(className, mn);

				InsnList instrs = mn.instructions;
				for (int i = 1; i < instrs.size(); i++) {
					AbstractInsnNode instr1 = instrs.get(i - 1);
					AbstractInsnNode instr2 = instrs.get(i);
					String s = null;

					if (instr2.getOpcode() == Opcodes.PUTSTATIC
							&& ((FieldInsnNode) instr2).owner.equals(owner)
							&& enumFields.contains(new Pair<>(((FieldInsnNode) instr2).name, ((FieldInsnNode) instr2).desc))
							&& instr1.getOpcode() == Opcodes.INVOKESPECIAL
							&& "<init>".equals(((MethodInsnNode) instr1).name)) {

						for (int j = 0; j < frames[i - 1].getStackSize(); j++) {
							SourceValue sv = frames[i - 1].getStack(j);
							for (AbstractInsnNode ci : sv.insns) {
								if (ci instanceof LdcInsnNode && ((LdcInsnNode) ci).cst instanceof String) {
									//if (s == null || !s.equals(((LdcInsnNode) ci).cst)) {
									if (s == null) {
										s = (String) (((LdcInsnNode) ci).cst);
										// stringsFound++;
									}
								}
							}
						}
					}

					if (s != null) {
						mappings.put(new FieldEntry(clazz, ((FieldInsnNode) instr2).name, new TypeDescriptor(((FieldInsnNode) instr2).desc)), s);
					}
					// report otherwise?
				}
			}
		}
	}
}
