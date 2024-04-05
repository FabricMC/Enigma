package cuchaz.enigma.source.vineflower;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.Manifest;

import net.fabricmc.fernflower.api.IFabricJavadocProvider;
import org.jetbrains.java.decompiler.main.decompiler.BaseDecompiler;
import org.jetbrains.java.decompiler.main.decompiler.PrintStreamLogger;
import org.jetbrains.java.decompiler.main.extern.IContextSource;
import org.jetbrains.java.decompiler.main.extern.IFernflowerLogger;
import org.jetbrains.java.decompiler.main.extern.IFernflowerPreferences;
import org.jetbrains.java.decompiler.main.extern.IResultSaver;
import org.jetbrains.java.decompiler.main.extern.TextTokenVisitor;

import cuchaz.enigma.source.Source;
import cuchaz.enigma.source.SourceIndex;
import cuchaz.enigma.source.SourceSettings;
import cuchaz.enigma.translation.mapping.EntryRemapper;

class VineflowerSource implements Source {
	private final IContextSource contextSource;
	private final IContextSource librarySource;
	private final SourceSettings settings;
	private EntryRemapper remapper;
	private SourceIndex index;

	VineflowerSource(VineflowerContextSource contextSource, EntryRemapper remapper, SourceSettings settings) {
		this.contextSource = contextSource;
		this.librarySource = contextSource.getClasspath();
		this.remapper = remapper;
		this.settings = settings;
	}

	@Override
	public String asString() {
		ensureDecompiled();
		return index.getSource();
	}

	@Override
	public Source withJavadocs(EntryRemapper remapper) {
		this.remapper = remapper;
		this.index = null;
		return this;
	}

	@Override
	public SourceIndex index() {
		ensureDecompiled();
		return index;
	}

	private void ensureDecompiled() {
		if (index != null) {
			return;
		}

		Map<String, Object> preferences = new HashMap<>(IFernflowerPreferences.DEFAULTS);
		preferences.put(IFernflowerPreferences.INDENT_STRING, "\t");
		preferences.put(IFernflowerPreferences.LOG_LEVEL, IFernflowerLogger.Severity.WARN.name());
		preferences.put(IFernflowerPreferences.THREADS, String.valueOf(Math.max(1, Runtime.getRuntime().availableProcessors() - 2)));
		preferences.put(IFabricJavadocProvider.PROPERTY_NAME, new VineflowerJavadocProvider(remapper));

		if (settings.removeImports) {
			preferences.put(IFernflowerPreferences.REMOVE_IMPORTS, "1");
		}

		index = new SourceIndex();
		IResultSaver saver = new ResultSaver(index);
		IFernflowerLogger logger = new PrintStreamLogger(System.out);
		BaseDecompiler decompiler = new BaseDecompiler(saver, preferences, logger);

		AtomicReference<VineflowerTextTokenCollector> tokenCollector = new AtomicReference<>();
		TextTokenVisitor.addVisitor(next -> {
			tokenCollector.set(new VineflowerTextTokenCollector(next));
			return tokenCollector.get();
		});

		decompiler.addSource(contextSource);

		if (librarySource != null) {
			decompiler.addLibrary(librarySource);
		}

		decompiler.decompileContext();
		tokenCollector.get().accept(index);
	}

	private class ResultSaver implements IResultSaver {
		private final SourceIndex index;

		private ResultSaver(SourceIndex index) {
			this.index = index;
		}

		@Override
		public void saveFolder(String path) { }
		@Override
		public void copyFile(String source, String path, String entryName) { }

		@Override
		public void saveClassFile(String path, String qualifiedName, String entryName, String content, int[] mapping) {
			index.setSource(content);
		}

		@Override
		public void createArchive(String path, String archiveName, Manifest manifest) { }
		@Override
		public void saveDirEntry(String path, String archiveName, String entryName) { }
		@Override
		public void copyEntry(String source, String path, String archiveName, String entry) { }
		@Override
		public void closeArchive(String path, String archiveName) { }
		@Override
		public void saveClassEntry(String path, String archiveName, String qualifiedName, String entryName, String content) { }
	}
}
