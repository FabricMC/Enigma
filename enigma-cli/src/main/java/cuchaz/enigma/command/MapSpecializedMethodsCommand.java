package cuchaz.enigma.command;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import cuchaz.enigma.ProgressListener;
import cuchaz.enigma.analysis.IndexTreeBuilder;
import cuchaz.enigma.analysis.index.BridgeMethodIndex;
import cuchaz.enigma.analysis.index.JarIndex;
import cuchaz.enigma.classprovider.CachingClassProvider;
import cuchaz.enigma.classprovider.JarClassProvider;
import cuchaz.enigma.translation.MappingTranslator;
import cuchaz.enigma.translation.Translator;
import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.mapping.serde.MappingFileNameFormat;
import cuchaz.enigma.translation.mapping.serde.MappingParseException;
import cuchaz.enigma.translation.mapping.serde.MappingSaveParameters;
import cuchaz.enigma.translation.mapping.tree.EntryTree;
import cuchaz.enigma.translation.mapping.tree.EntryTreeNode;
import cuchaz.enigma.translation.mapping.tree.HashEntryTree;
import cuchaz.enigma.translation.representation.entry.MethodEntry;
import cuchaz.enigma.utils.Utils;

public class MapSpecializedMethodsCommand extends Command {
	public MapSpecializedMethodsCommand() {
		super("map-specialized-methods");
	}

	@Override
	public String getUsage() {
		return "<jar> <source-format> <source> <result-format> <result>";
	}

	@Override
	public boolean isValidArgument(int length) {
		return length == 5;
	}

	@Override
	public void run(String... args) throws IOException, MappingParseException {
		run(Paths.get(args[0]), args[1], Paths.get(args[2]), args[3], Paths.get(args[4]));
	}

	public static void run(Path jar, String sourceFormat, Path sourcePath, String resultFormat, Path output) throws IOException, MappingParseException {
		MappingSaveParameters saveParameters = new MappingSaveParameters(MappingFileNameFormat.BY_DEOBF);
		EntryTree<EntryMapping> source = MappingCommandsUtil.read(sourceFormat, sourcePath, saveParameters);
		EntryTree<EntryMapping> result = new HashEntryTree<>();

		JarClassProvider jcp = new JarClassProvider(jar);
		JarIndex jarIndex = JarIndex.empty();
		jarIndex.indexJar(jcp.getClassNames(), new CachingClassProvider(jcp), ProgressListener.none());

		BridgeMethodIndex bridgeMethodIndex = jarIndex.getBridgeMethodIndex();
		Translator translator = new MappingTranslator(source, jarIndex.getEntryResolver());
		IndexTreeBuilder indexTreeBuilder = new IndexTreeBuilder(jarIndex);

		// Copy all non-specialized methods
		for (EntryTreeNode<EntryMapping> node : source) {
			if (!(node.getEntry() instanceof MethodEntry) || !bridgeMethodIndex.isSpecializedMethod((MethodEntry) node.getEntry())) {
				result.insert(node.getEntry(), node.getValue());
			}
		}

		// Add correct mappings for specialized methods
		for (Map.Entry<MethodEntry, MethodEntry> entry : bridgeMethodIndex.getBridgeToSpecialized().entrySet()) {
			MethodEntry bridge = entry.getKey();
			MethodEntry specialized = indexTreeBuilder.buildMethodInheritance(translator, entry.getValue()).getMethodEntry();
			String name = translator.translate(bridge).getName();
			result.insert(specialized, new EntryMapping(name));
		}

		Utils.delete(output);
		MappingCommandsUtil.write(result, resultFormat, output, saveParameters);
	}
}
