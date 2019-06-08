/*******************************************************************************
 * Copyright (c) 2015 Jeff Martin.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public
 * License v3.0 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * <p>
 * Contributors:
 * Jeff Martin - initial API and implementation
 ******************************************************************************/

package cuchaz.enigma.gui;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.strobel.decompiler.languages.java.ast.CompilationUnit;
import cuchaz.enigma.Deobfuscator;
import cuchaz.enigma.SourceProvider;
import cuchaz.enigma.analysis.*;
import cuchaz.enigma.config.Config;
import cuchaz.enigma.gui.dialog.ProgressDialog;
import cuchaz.enigma.gui.util.History;
import cuchaz.enigma.throwables.MappingParseException;
import cuchaz.enigma.translation.Translator;
import cuchaz.enigma.translation.mapping.*;
import cuchaz.enigma.translation.mapping.serde.MappingFormat;
import cuchaz.enigma.translation.mapping.tree.EntryTree;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.Entry;
import cuchaz.enigma.translation.representation.entry.FieldEntry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;
import cuchaz.enigma.utils.ReadableToken;

import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.event.ItemEvent;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

public class GuiController {
	private static final ExecutorService DECOMPILER_SERVICE = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setDaemon(true).setNameFormat("decompiler-thread").build());

	private final Gui gui;
	private Deobfuscator deobfuscator;
	private DecompiledClassSource currentSource;


	private Path loadedMappingPath;
	private MappingFormat loadedMappingFormat;

	public GuiController(Gui gui) {
		this.gui = gui;
	}

	public boolean isDirty() {
		if (deobfuscator == null) {
			return false;
		}
		return deobfuscator.getMapper().isDirty();
	}

	public void openJar(final JarFile jar) throws IOException {
		this.gui.onStartOpenJar("Loading JAR...");
		this.deobfuscator = new Deobfuscator(jar, this.gui::onStartOpenJar);
		this.gui.onFinishOpenJar(jar.getName());
		refreshClasses();
	}

	public void closeJar() {
		this.deobfuscator = null;
		this.gui.onCloseJar();
	}

	public void openMappings(MappingFormat format, Path path) {
		if (deobfuscator == null) return;
		ProgressDialog.runInThread(this.gui.getFrame(), progress -> {
			try {
				EntryTree<EntryMapping> mappings = format.read(path, progress);
				deobfuscator.setMappings(mappings, progress);

				gui.setMappingsFile(path);
				loadedMappingFormat = format;
				loadedMappingPath = path;

				refreshClasses();
				refreshCurrentClass();
			} catch (MappingParseException e) {
				JOptionPane.showMessageDialog(this.gui.getFrame(), e.getMessage());
			}
		});
	}

	public void saveMappings(Path path) {
		saveMappings(loadedMappingFormat, path);
	}

	public void saveMappings(MappingFormat format, Path path) {
		if (deobfuscator == null) return;
		EntryRemapper mapper = deobfuscator.getMapper();

		MappingDelta<EntryMapping> delta = mapper.takeMappingDelta();
		boolean saveAll = !path.equals(loadedMappingPath);

		ProgressDialog.runInThread(this.gui.getFrame(), progress -> {
			if (saveAll) {
				format.write(mapper.getObfToDeobf(), path, progress);
			} else {
				format.write(mapper.getObfToDeobf(), delta, path, progress);
			}
		});

		loadedMappingFormat = format;
		loadedMappingPath = path;
	}

	public void closeMappings() {
		if (deobfuscator == null) return;
		this.deobfuscator.setMappings(null);
		this.gui.setMappingsFile(null);
		refreshClasses();
		refreshCurrentClass();
	}

	public void exportSource(final File dirOut) {
		if (deobfuscator == null) return;
		ProgressDialog.runInThread(this.gui.getFrame(), progress -> this.deobfuscator.writeSources(dirOut.toPath(), progress));
	}

	public void exportJar(final File fileOut) {
		if (deobfuscator == null) return;
		ProgressDialog.runInThread(this.gui.getFrame(), progress -> this.deobfuscator.writeTransformedJar(fileOut, progress));
	}

	public Token getToken(int pos) {
		if (this.currentSource == null) {
			return null;
		}
		return this.currentSource.getIndex().getReferenceToken(pos);
	}

	@Nullable
	public EntryReference<Entry<?>, Entry<?>> getReference(Token token) {
		if (this.currentSource == null) {
			return null;
		}
		return this.currentSource.getIndex().getReference(token);
	}

	public ReadableToken getReadableToken(Token token) {
		if (this.currentSource == null) {
			return null;
		}
		SourceIndex index = this.currentSource.getIndex();
		return new ReadableToken(
				index.getLineNumber(token.start),
				index.getColumnNumber(token.start),
				index.getColumnNumber(token.end)
		);
	}

	public boolean entryIsInJar(Entry<?> entry) {
		if (entry == null || deobfuscator == null) return false;
		return this.deobfuscator.isRenamable(entry);
	}

	public ClassInheritanceTreeNode getClassInheritance(ClassEntry entry) {
		Translator translator = this.deobfuscator.getMapper().getDeobfuscator();
		ClassInheritanceTreeNode rootNode = this.deobfuscator.getIndexTreeBuilder().buildClassInheritance(translator, entry);
		return ClassInheritanceTreeNode.findNode(rootNode, entry);
	}

	public ClassImplementationsTreeNode getClassImplementations(ClassEntry entry) {
		Translator translator = this.deobfuscator.getMapper().getDeobfuscator();
		return this.deobfuscator.getIndexTreeBuilder().buildClassImplementations(translator, entry);
	}

	public MethodInheritanceTreeNode getMethodInheritance(MethodEntry entry) {
		Translator translator = this.deobfuscator.getMapper().getDeobfuscator();
		MethodInheritanceTreeNode rootNode = this.deobfuscator.getIndexTreeBuilder().buildMethodInheritance(translator, entry);
		return MethodInheritanceTreeNode.findNode(rootNode, entry);
	}

	public MethodImplementationsTreeNode getMethodImplementations(MethodEntry entry) {
		Translator translator = this.deobfuscator.getMapper().getDeobfuscator();
		List<MethodImplementationsTreeNode> rootNodes = this.deobfuscator.getIndexTreeBuilder().buildMethodImplementations(translator, entry);
		if (rootNodes.isEmpty()) {
			return null;
		}
		if (rootNodes.size() > 1) {
			System.err.println("WARNING: Method " + entry + " implements multiple interfaces. Only showing first one.");
		}
		return MethodImplementationsTreeNode.findNode(rootNodes.get(0), entry);
	}

	public ClassReferenceTreeNode getClassReferences(ClassEntry entry) {
		Translator deobfuscator = this.deobfuscator.getMapper().getDeobfuscator();
		ClassReferenceTreeNode rootNode = new ClassReferenceTreeNode(deobfuscator, entry);
		rootNode.load(this.deobfuscator.getJarIndex(), true);
		return rootNode;
	}

	public FieldReferenceTreeNode getFieldReferences(FieldEntry entry) {
		Translator translator = this.deobfuscator.getMapper().getDeobfuscator();
		FieldReferenceTreeNode rootNode = new FieldReferenceTreeNode(translator, entry);
		rootNode.load(this.deobfuscator.getJarIndex(), true);
		return rootNode;
	}

	public MethodReferenceTreeNode getMethodReferences(MethodEntry entry, boolean recursive) {
		Translator translator = this.deobfuscator.getMapper().getDeobfuscator();
		MethodReferenceTreeNode rootNode = new MethodReferenceTreeNode(translator, entry);
		rootNode.load(this.deobfuscator.getJarIndex(), true, recursive);
		return rootNode;
	}

	public void rename(EntryReference<Entry<?>, Entry<?>> reference, String newName, boolean refreshClassTree) {
		this.deobfuscator.rename(reference.getNameableEntry(), newName);

		if (refreshClassTree && reference.entry instanceof ClassEntry && !((ClassEntry) reference.entry).isInnerClass())
			this.gui.moveClassTree(reference, newName);
		refreshCurrentClass(reference);
	}

	public void changeDocs(EntryReference<Entry<?>, Entry<?>> reference, String updatedDocs) {
		this.deobfuscator.changeDoc(reference.getNameableEntry(), updatedDocs);

		refreshCurrentClass(reference, true);
	}

	public void removeMapping(EntryReference<Entry<?>, Entry<?>> reference) {
		this.deobfuscator.removeMapping(reference.getNameableEntry());
		if (reference.entry instanceof ClassEntry)
			this.gui.moveClassTree(reference, false, true);
		refreshCurrentClass(reference);
	}

	public void markAsDeobfuscated(EntryReference<Entry<?>, Entry<?>> reference) {
		this.deobfuscator.markAsDeobfuscated(reference.getNameableEntry());
		if (reference.entry instanceof ClassEntry && !((ClassEntry) reference.entry).isInnerClass())
			this.gui.moveClassTree(reference, true, false);
		refreshCurrentClass(reference);
	}

	/**
	 * Navigates to the declaration with respect to navigation history
	 * @param entry the entry whose declaration will be navigated to
	 */
	public void openDeclaration(Entry<?> entry) {
		if (entry == null) {
			throw new IllegalArgumentException("Entry cannot be null!");
		}
		openReference(new EntryReference<>(entry, entry.getName()));
	}

	/**
	 * Navigates to the reference with respect to navigation history
	 * @param reference the reference
	 */
	public void openReference(EntryReference<Entry<?>, Entry<?>> reference) {
		if (reference == null) {
			throw new IllegalArgumentException("Reference cannot be null!");
		}
		if (this.gui.referenceHistory == null) {
			this.gui.referenceHistory = new History<>(reference);
		} else {
			if (!reference.equals(this.gui.referenceHistory.getCurrent())) {
				this.gui.referenceHistory.push(reference);
			}
		}
		setReference(reference);
	}

	/**
	 * Navigates to the reference without modifying history. If the class is not currently loaded, it will be loaded.
	 * @param reference the reference
	 */
	private void setReference(EntryReference<Entry<?>, Entry<?>> reference) {
		// get the reference target class
		ClassEntry classEntry = reference.getLocationClassEntry();
		if (!this.deobfuscator.isRenamable(classEntry)) {
			throw new IllegalArgumentException("Obfuscated class " + classEntry + " was not found in the jar!");
		}

		if (this.currentSource == null || !this.currentSource.getEntry().equals(classEntry)) {
			// deobfuscate the class, then navigate to the reference
			loadClass(classEntry, () -> showReference(reference));
		} else {
			showReference(reference);
		}
	}

	/**
	 * Navigates to the reference without modifying history. Assumes the class is loaded.
	 * @param reference
	 */
	private void showReference(EntryReference<Entry<?>, Entry<?>> reference) {
		Collection<Token> tokens = getTokensForReference(reference);
		if (tokens.isEmpty()) {
			// DEBUG
			System.err.println(String.format("WARNING: no tokens found for %s in %s", reference, this.currentSource.getEntry()));
		} else {
			this.gui.showTokens(tokens);
		}
	}

	public Collection<Token> getTokensForReference(EntryReference<Entry<?>, Entry<?>> reference) {
		EntryRemapper mapper = this.deobfuscator.getMapper();

		SourceIndex index = this.currentSource.getIndex();
		return mapper.getObfResolver().resolveReference(reference, ResolutionStrategy.RESOLVE_CLOSEST)
				.stream()
				.flatMap(r -> index.getReferenceTokens(r).stream())
				.collect(Collectors.toList());
	}

	public void openPreviousReference() {
		if (hasPreviousReference()) {
			setReference(gui.referenceHistory.goBack());
		}
	}

	public boolean hasPreviousReference() {
		return gui.referenceHistory != null && gui.referenceHistory.canGoBack();
	}

	public void openNextReference() {
		if (hasNextReference()) {
			setReference(gui.referenceHistory.goForward());
		}
	}

	public boolean hasNextReference() {
		return gui.referenceHistory != null && gui.referenceHistory.canGoForward();
	}

	public void navigateTo(Entry<?> entry) {
		if (!entryIsInJar(entry)) {
			// entry is not in the jar. Ignore it
			return;
		}
		openDeclaration(entry);
	}

	public void navigateTo(EntryReference<Entry<?>, Entry<?>> reference) {
		if (!entryIsInJar(reference.getLocationClassEntry())) {
			return;
		}
		openReference(reference);
	}

	private void refreshClasses() {
		List<ClassEntry> obfClasses = Lists.newArrayList();
		List<ClassEntry> deobfClasses = Lists.newArrayList();
		this.deobfuscator.getSeparatedClasses(obfClasses, deobfClasses);
		this.gui.setObfClasses(obfClasses);
		this.gui.setDeobfClasses(deobfClasses);
	}

	public void refreshCurrentClass() {
		refreshCurrentClass(null);
	}

	private void refreshCurrentClass(EntryReference<Entry<?>, Entry<?>> reference) {
		refreshCurrentClass(reference, false);
	}

	private void refreshCurrentClass(EntryReference<Entry<?>, Entry<?>> reference, boolean forceDecomp) {
		if (currentSource != null) {
			loadClass(currentSource.getEntry(), () -> {
				if (reference != null) {
					showReference(reference);
				}
			}, forceDecomp);
		}
	}

	private void loadClass(ClassEntry classEntry, Runnable callback) {
		loadClass(classEntry, callback, false);
	}

	private void loadClass(ClassEntry classEntry, Runnable callback, boolean forceDecomp) {
		ClassEntry targetClass = classEntry.getOutermostClass();

		boolean requiresDecompile = forceDecomp || currentSource == null || !currentSource.getEntry().equals(targetClass);
		if (requiresDecompile) {
			currentSource = null; // Or the GUI may try to find a nonexistent token
			gui.setEditorText("(decompiling...)");
		}

		DECOMPILER_SERVICE.submit(() -> {
			try {
				if (requiresDecompile) {
					currentSource = decompileSource(targetClass, deobfuscator.getObfSourceProvider());
				}

				remapSource(deobfuscator.getMapper().getDeobfuscator());
				callback.run();
			} catch (Throwable t) {
				System.err.println("An exception was thrown while decompiling class " + classEntry.getFullName());
				t.printStackTrace(System.err);
			}
		});
	}

	private DecompiledClassSource decompileSource(ClassEntry targetClass, SourceProvider sourceProvider) {
		try {
			CompilationUnit sourceTree = sourceProvider.getSources(targetClass.getFullName());
			if (sourceTree == null) {
				gui.setEditorText("Unable to find class: " + targetClass);
				return DecompiledClassSource.text(targetClass, "Unable to find class");
			}

			DropImportAstTransform.INSTANCE.run(sourceTree);
			DropVarModifiersAstTransform.INSTANCE.run(sourceTree);
			new AddJavadocsAstTransform(deobfuscator.getMapper()).run(sourceTree);

			String sourceString = sourceProvider.writeSourceToString(sourceTree);

			SourceIndex index = SourceIndex.buildIndex(sourceString, sourceTree, true);
			index.resolveReferences(deobfuscator.getMapper().getObfResolver());

			return new DecompiledClassSource(targetClass, index);
		} catch (Throwable t) {
			StringWriter traceWriter = new StringWriter();
			t.printStackTrace(new PrintWriter(traceWriter));

			return DecompiledClassSource.text(targetClass, traceWriter.toString());
		}
	}

	private void remapSource(Translator translator) {
		if (currentSource == null) {
			return;
		}

		currentSource.remapSource(deobfuscator, translator);

		gui.setEditorTheme(Config.getInstance().lookAndFeel);
		gui.setSource(currentSource);
	}

	public Deobfuscator getDeobfuscator() {
		return deobfuscator;
	}

	public void modifierChange(ItemEvent event) {
		if (event.getStateChange() == ItemEvent.SELECTED) {
			deobfuscator.changeModifier(gui.cursorReference.entry, (AccessModifier) event.getItem());
			refreshCurrentClass();
		}
	}
}
