package cuchaz.enigma.source.procyon;

import com.strobel.assembler.metadata.ITypeLoader;
import com.strobel.assembler.metadata.MetadataSystem;
import com.strobel.assembler.metadata.TypeDefinition;
import com.strobel.assembler.metadata.TypeReference;
import com.strobel.decompiler.DecompilerContext;
import com.strobel.decompiler.DecompilerSettings;
import com.strobel.decompiler.languages.java.BraceStyle;
import com.strobel.decompiler.languages.java.JavaFormattingOptions;
import com.strobel.decompiler.languages.java.ast.AstBuilder;
import com.strobel.decompiler.languages.java.ast.CompilationUnit;
import com.strobel.decompiler.languages.java.ast.InsertParenthesesVisitor;
import cuchaz.enigma.ClassProvider;
import cuchaz.enigma.source.Source;
import cuchaz.enigma.source.Decompiler;
import cuchaz.enigma.source.SourceSettings;
import cuchaz.enigma.source.procyon.transformers.*;
import cuchaz.enigma.source.procyon.typeloader.CompiledSourceTypeLoader;
import cuchaz.enigma.source.procyon.typeloader.NoRetryMetadataSystem;
import cuchaz.enigma.source.procyon.typeloader.SynchronizedTypeLoader;

public class ProcyonDecompiler implements Decompiler {
	private final SourceSettings settings;
	private final DecompilerSettings decompilerSettings;
	private final MetadataSystem metadataSystem;

	public ProcyonDecompiler(ClassProvider classProvider, SourceSettings settings) {
		ITypeLoader typeLoader = new SynchronizedTypeLoader(new CompiledSourceTypeLoader(classProvider));

		metadataSystem = new NoRetryMetadataSystem(typeLoader);
		metadataSystem.setEagerMethodLoadingEnabled(true);

		decompilerSettings = DecompilerSettings.javaDefaults();
		decompilerSettings.setMergeVariables(getSystemPropertyAsBoolean("enigma.mergeVariables", true));
		decompilerSettings.setForceExplicitImports(getSystemPropertyAsBoolean("enigma.forceExplicitImports", true));
		decompilerSettings.setForceExplicitTypeArguments(getSystemPropertyAsBoolean("enigma.forceExplicitTypeArguments", true));
		decompilerSettings.setShowDebugLineNumbers(getSystemPropertyAsBoolean("enigma.showDebugLineNumbers", false));
		decompilerSettings.setShowSyntheticMembers(getSystemPropertyAsBoolean("enigma.showSyntheticMembers", false));
		decompilerSettings.setTypeLoader(typeLoader);

		JavaFormattingOptions formattingOptions = decompilerSettings.getJavaFormattingOptions();
		formattingOptions.ClassBraceStyle = BraceStyle.EndOfLine;
		formattingOptions.InterfaceBraceStyle = BraceStyle.EndOfLine;
		formattingOptions.EnumBraceStyle = BraceStyle.EndOfLine;

		this.settings = settings;
	}

	@Override
	public Source getSource(String className) {
		TypeReference type = metadataSystem.lookupType(className);
		if (type == null) {
			throw new Error(String.format("Unable to find desc: %s", className));
		}

		TypeDefinition resolvedType = type.resolve();

		DecompilerContext context = new DecompilerContext();
		context.setCurrentType(resolvedType);
		context.setSettings(decompilerSettings);

		AstBuilder builder = new AstBuilder(context);
		builder.addType(resolvedType);
		builder.runTransformations(null);
		CompilationUnit source = builder.getCompilationUnit();

		new ObfuscatedEnumSwitchRewriterTransform(context).run(source);
		new VarargsFixer(context).run(source);
		new RemoveObjectCasts(context).run(source);
		new Java8Generics().run(source);
		new InvalidIdentifierFix().run(source);
		if (settings.removeImports) DropImportAstTransform.INSTANCE.run(source);
		if (settings.removeVariableFinal) DropVarModifiersAstTransform.INSTANCE.run(source);
		source.acceptVisitor(new InsertParenthesesVisitor(), null);

		return new ProcyonSource(source, decompilerSettings);
	}

	private static boolean getSystemPropertyAsBoolean(String property, boolean defValue) {
		String value = System.getProperty(property);
		return value == null ? defValue : Boolean.parseBoolean(value);
	}
}
