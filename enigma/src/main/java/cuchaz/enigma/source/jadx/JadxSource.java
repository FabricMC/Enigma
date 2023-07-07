package cuchaz.enigma.source.jadx;

import java.io.IOException;
import java.util.function.Consumer;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.objectweb.asm.tree.ClassNode;
import jadx.api.JadxArgs;
import jadx.api.JadxDecompiler;
import jadx.api.JavaClass;
import jadx.api.plugins.input.data.IClassData;
import jadx.api.plugins.input.data.ILoadResult;
import jadx.api.plugins.input.data.IResourceData;
import jadx.plugins.input.java.JavaClassReader;
import jadx.plugins.input.java.data.JavaClassData;

import cuchaz.enigma.source.Source;
import cuchaz.enigma.source.SourceIndex;
import cuchaz.enigma.source.SourceSettings;
import cuchaz.enigma.source.Token;
import cuchaz.enigma.translation.mapping.EntryRemapper;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.utils.AsmUtil;

public class JadxSource implements Source {
	private final SourceSettings settings;
	private final JadxArgs jadxArgs;
	private final ClassNode classNode;
	private final EntryRemapper mapper;
	private SourceIndex index;

	public JadxSource(SourceSettings settings, JadxArgs jadxArgs, ClassNode classNode, @Nullable EntryRemapper mapper) {
		this.settings = settings;
		this.jadxArgs = jadxArgs;
		this.classNode = classNode;
		this.mapper = mapper;
	}

	@Override
	public Source withJavadocs(EntryRemapper mapper) {
		return new JadxSource(settings, jadxArgs, classNode, mapper);
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

		try (JadxDecompiler jadx = new JadxDecompiler(jadxArgs)) {
			jadx.addCustomLoad(new ILoadResult() {
				@Override
				public void close() throws IOException {
					return;
				}

				@Override
				public void visitClasses(Consumer<IClassData> consumer) {
					consumer.accept(new JavaClassData(new JavaClassReader(0, classNode.name + ".class", AsmUtil.nodeToBytes(classNode))));
				}

				@Override
				public void visitResources(Consumer<IResourceData> consumer) {
					return;
				}

				@Override
				public boolean isEmpty() {
					return false;
				}
			});
			jadx.load();

			JavaClass decompiledClass = jadx.getClasses().get(0);
			SourceIndex index = new SourceIndex(decompiledClass.getCode());
			int pos = decompiledClass.getDefPos() - 2;
			
			Token token = new Token(pos, pos + decompiledClass.getName().length(), decompiledClass.getName());
			index.addDeclaration(token, parse(decompiledClass));
			this.index = index;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private ClassEntry parse(JavaClass javaClass) {
		return new ClassEntry(javaClass.getRawName());
	}
}
