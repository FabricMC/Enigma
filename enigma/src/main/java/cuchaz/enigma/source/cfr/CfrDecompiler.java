package cuchaz.enigma.source.cfr;

import java.util.Collection;
import java.util.Map;

import org.benf.cfr.reader.apiunreleased.ClassFileSource2;
import org.benf.cfr.reader.apiunreleased.JarContent;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.Pair;
import org.benf.cfr.reader.util.AnalysisType;
import org.benf.cfr.reader.util.getopt.Options;
import org.benf.cfr.reader.util.getopt.OptionsImpl;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.objectweb.asm.tree.ClassNode;

import cuchaz.enigma.classprovider.ClassProvider;
import cuchaz.enigma.source.Decompiler;
import cuchaz.enigma.source.Source;
import cuchaz.enigma.source.SourceSettings;
import cuchaz.enigma.translation.mapping.EntryRemapper;
import cuchaz.enigma.utils.AsmUtil;

public class CfrDecompiler implements Decompiler {
	// cfr doesn't add final on params so final setting is ignored
	private final SourceSettings settings;
	private final Options options;
	private final ClassFileSource2 classFileSource;

	public CfrDecompiler(ClassProvider classProvider, SourceSettings sourceSettings) {
		this.options = OptionsImpl.getFactory().create(Map.of("trackbytecodeloc", "true"));
		this.settings = sourceSettings;
		this.classFileSource = new ClassFileSource(classProvider);
	}

	@Override
	public Source getSource(String className, @Nullable EntryRemapper mapper) {
		return new CfrSource(className, settings, this.options, this.classFileSource, mapper);
	}

	private record ClassFileSource(ClassProvider classProvider) implements ClassFileSource2 {
		@Override
		public JarContent addJarContent(String s, AnalysisType analysisType) {
			return null;
		}

		@Override
		public void informAnalysisRelativePathDetail(String usePath, String classFilePath) {
		}

		@Override
		public Collection<String> addJar(String jarPath) {
			return null;
		}

		@Override
		public String getPossiblyRenamedPath(String path) {
			return path;
		}

		@Override
		public Pair<byte[], String> getClassFileContent(String path) {
			ClassNode node = classProvider.get(path.substring(0, path.lastIndexOf('.')));

			if (node == null) {
				return null;
			}

			return new Pair<>(AsmUtil.nodeToBytes(node), path);
		}
	}
}
