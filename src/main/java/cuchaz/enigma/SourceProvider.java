package cuchaz.enigma;

import com.strobel.assembler.metadata.ITypeLoader;
import com.strobel.assembler.metadata.MetadataSystem;
import com.strobel.assembler.metadata.TypeDefinition;
import com.strobel.assembler.metadata.TypeReference;
import com.strobel.decompiler.DecompilerContext;
import com.strobel.decompiler.DecompilerSettings;
import com.strobel.decompiler.PlainTextOutput;
import com.strobel.decompiler.languages.java.JavaOutputVisitor;
import com.strobel.decompiler.languages.java.ast.AstBuilder;
import com.strobel.decompiler.languages.java.ast.CompilationUnit;
import com.strobel.decompiler.languages.java.ast.InsertParenthesesVisitor;
import com.strobel.decompiler.languages.java.ast.transforms.IAstTransform;
import cuchaz.enigma.utils.Utils;
import oml.ast.transformers.*;

import java.io.StringWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.List;

public class SourceProvider {
	private final DecompilerSettings settings;

	private final ITypeLoader typeLoader;
	private final MetadataSystem metadataSystem;

	public SourceProvider(DecompilerSettings settings, ITypeLoader typeLoader, MetadataSystem metadataSystem) {
		this.settings = settings;
		this.typeLoader = typeLoader;
		this.metadataSystem = metadataSystem;
	}

	public SourceProvider(DecompilerSettings settings, ITypeLoader typeLoader) {
		this(settings, typeLoader, new Deobfuscator.NoRetryMetadataSystem(typeLoader));
	}

	public static DecompilerSettings createSettings() {
		DecompilerSettings settings = DecompilerSettings.javaDefaults();
		settings.setMergeVariables(Utils.getSystemPropertyAsBoolean("enigma.mergeVariables", true));
		settings.setForceExplicitImports(Utils.getSystemPropertyAsBoolean("enigma.forceExplicitImports", true));
		settings.setForceExplicitTypeArguments(Utils.getSystemPropertyAsBoolean("enigma.forceExplicitTypeArguments", true));
		settings.setShowDebugLineNumbers(Utils.getSystemPropertyAsBoolean("enigma.showDebugLineNumbers", false));
		settings.setShowSyntheticMembers(Utils.getSystemPropertyAsBoolean("enigma.showSyntheticMembers", false));

		return settings;
	}

	public CompilationUnit getSources(String name) {
		TypeReference type = metadataSystem.lookupType(name);
		if (type == null) {
			throw new Error(String.format("Unable to find desc: %s", name));
		}

		TypeDefinition resolvedType = type.resolve();

		settings.setTypeLoader(typeLoader);

		// decompile it!
		DecompilerContext context = new DecompilerContext();
		context.setCurrentType(resolvedType);
		context.setSettings(settings);

		AstBuilder builder = new AstBuilder(context);
		builder.addType(resolvedType);
		builder.runTransformations(null);
		runCustomTransforms(builder, context);

		return builder.getCompilationUnit();
	}

	public void writeSource(Writer writer, CompilationUnit sourceTree) {
		// render the AST into source
		sourceTree.acceptVisitor(new InsertParenthesesVisitor(), null);
		sourceTree.acceptVisitor(new JavaOutputVisitor(new PlainTextOutput(writer), settings), null);
	}

	public String writeSourceToString(CompilationUnit sourceTree) {
		StringWriter writer = new StringWriter();
		writeSource(writer, sourceTree);
		return writer.toString();
	}

	private static void runCustomTransforms(AstBuilder builder, DecompilerContext context) {
		List<IAstTransform> transformers = Arrays.asList(
				new ObfuscatedEnumSwitchRewriterTransform(context),
				new VarargsFixer(context),
				new RemoveObjectCasts(context),
				new Java8Generics(),
				new InvalidIdentifierFix()
		);
		for (IAstTransform transform : transformers) {
			transform.run(builder.getCompilationUnit());
		}
	}
}
