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
import cuchaz.enigma.Enigma;
import cuchaz.enigma.EnigmaProfile;
import cuchaz.enigma.EnigmaProject;
import cuchaz.enigma.analysis.*;
import cuchaz.enigma.api.service.ObfuscationTestService;
import cuchaz.enigma.bytecode.translators.SourceFixVisitor;
import cuchaz.enigma.config.Config;
import cuchaz.enigma.gui.dialog.ProgressDialog;
import cuchaz.enigma.gui.stats.StatsGenerator;
import cuchaz.enigma.gui.stats.StatsMember;
import cuchaz.enigma.gui.util.History;
import cuchaz.enigma.source.*;
import cuchaz.enigma.throwables.MappingParseException;
import cuchaz.enigma.translation.Translator;
import cuchaz.enigma.translation.mapping.*;
import cuchaz.enigma.translation.mapping.serde.MappingFormat;
import cuchaz.enigma.translation.mapping.tree.EntryTree;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.Entry;
import cuchaz.enigma.translation.representation.entry.FieldEntry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;
import cuchaz.enigma.utils.I18n;
import cuchaz.enigma.utils.ReadableToken;
import cuchaz.enigma.utils.Utils;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

import javax.annotation.Nullable;
import javax.swing.JOptionPane;
import java.awt.Desktop;
import java.awt.event.ItemEvent;
import java.io.*;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GuiController {
	private static final ExecutorService DECOMPILER_SERVICE = Executors.newSingleThreadExecutor(
			new ThreadFactoryBuilder()
					.setDaemon(true)
					.setNameFormat("decompiler-thread")
					.build()
	);

	private final Gui gui;
	public final Enigma enigma;

	public EnigmaProject project;
	private DecompilerService decompilerService;
	private Decompiler decompiler;
	private IndexTreeBuilder indexTreeBuilder;

	private Path loadedMappingPath;
	private MappingFormat loadedMappingFormat;

	private DecompiledClassSource currentSource;
	private Source uncommentedSource;

	public GuiController(Gui gui, EnigmaProfile profile) {
		this.gui = gui;
		this.enigma = Enigma.builder()
				.setProfile(profile)
				.build();

		decompilerService = Config.getInstance().decompiler.service;
	}

	public boolean isDirty() {
		return project != null && project.getMapper().isDirty();
	}

	public CompletableFuture<Void> openJar(final Path jarPath) {
		this.gui.onStartOpenJar();

		return ProgressDialog.runOffThread(gui.getFrame(), progress -> {
			project = enigma.openJar(jarPath, progress);
			indexTreeBuilder = new IndexTreeBuilder(project.getJarIndex());
			decompiler = createDecompiler();
			gui.onFinishOpenJar(jarPath.getFileName().toString());
			refreshClasses();
		});
	}

	private Decompiler createDecompiler() {
		return decompilerService.create(name -> {
			ClassNode node = project.getClassCache().getClassNode(name);

			if (node == null) {
				return null;
			}

			ClassNode fixedNode = new ClassNode();
			node.accept(new SourceFixVisitor(Utils.ASM_VERSION, fixedNode, project.getJarIndex()));
			return fixedNode;
		}, new SourceSettings(true, true));
	}

	public void closeJar() {
		this.project = null;
		this.gui.onCloseJar();
	}

	public CompletableFuture<Void> openMappings(MappingFormat format, Path path) {
		if (project == null) return CompletableFuture.completedFuture(null);

		gui.setMappingsFile(path);

		return ProgressDialog.runOffThread(gui.getFrame(), progress -> {
			try {
				MappingSaveParameters saveParameters = enigma.getProfile().getMappingSaveParameters();

				EntryTree<EntryMapping> mappings = format.read(path, progress, saveParameters);
				project.setMappings(mappings);

				loadedMappingFormat = format;
				loadedMappingPath = path;

				refreshClasses();
				refreshCurrentClass();
			} catch (MappingParseException e) {
				JOptionPane.showMessageDialog(gui.getFrame(), e.getMessage());
			}
		});
	}

	public CompletableFuture<Void> saveMappings(Path path) {
		return saveMappings(path, loadedMappingFormat);
	}

	public CompletableFuture<Void> saveMappings(Path path, MappingFormat format) {
		if (project == null) return CompletableFuture.completedFuture(null);

		return ProgressDialog.runOffThread(this.gui.getFrame(), progress -> {
			EntryRemapper mapper = project.getMapper();
			MappingSaveParameters saveParameters = enigma.getProfile().getMappingSaveParameters();

			MappingDelta<EntryMapping> delta = mapper.takeMappingDelta();
			boolean saveAll = !path.equals(loadedMappingPath);

			loadedMappingFormat = format;
			loadedMappingPath = path;

			if (saveAll) {
				format.write(mapper.getObfToDeobf(), path, progress, saveParameters);
			} else {
				format.write(mapper.getObfToDeobf(), delta, path, progress, saveParameters);
			}
		});
	}

	public void closeMappings() {
		if (project == null) return;

		project.setMappings(null);

		this.gui.setMappingsFile(null);
		refreshClasses();
		refreshCurrentClass();
	}

	public CompletableFuture<Void> dropMappings() {
		if (project == null) return CompletableFuture.completedFuture(null);

		return ProgressDialog.runOffThread(this.gui.getFrame(), progress -> project.dropMappings(progress));
	}

	public CompletableFuture<Void> exportSource(final Path path) {
		if (project == null) return CompletableFuture.completedFuture(null);

		return ProgressDialog.runOffThread(this.gui.getFrame(), progress -> {
			EnigmaProject.JarExport jar = project.exportRemappedJar(progress);
			EnigmaProject.SourceExport source = jar.decompile(progress, decompilerService);

			source.write(path, progress);
		});
	}

	public CompletableFuture<Void> exportJar(final Path path) {
		if (project == null) return CompletableFuture.completedFuture(null);

		return ProgressDialog.runOffThread(this.gui.getFrame(), progress -> {
			EnigmaProject.JarExport jar = project.exportRemappedJar(progress);
			jar.write(path, progress);
		});
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

	/**
	 * Navigates to the declaration with respect to navigation history
	 *
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
	 *
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
	 *
	 * @param reference the reference
	 */
	private void setReference(EntryReference<Entry<?>, Entry<?>> reference) {
		// get the reference target class
		ClassEntry classEntry = reference.getLocationClassEntry();
		if (!project.isRenamable(classEntry)) {
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
	 *
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
		EntryRemapper mapper = this.project.getMapper();

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
		if (!project.isRenamable(entry)) {
			// entry is not in the jar. Ignore it
			return;
		}
		openDeclaration(entry);
	}

	public void navigateTo(EntryReference<Entry<?>, Entry<?>> reference) {
		if (!project.isRenamable(reference.getLocationClassEntry())) {
			return;
		}
		openReference(reference);
	}

	private void refreshClasses() {
		List<ClassEntry> obfClasses = Lists.newArrayList();
		List<ClassEntry> deobfClasses = Lists.newArrayList();
		this.addSeparatedClasses(obfClasses, deobfClasses);
		this.gui.setObfClasses(obfClasses);
		this.gui.setDeobfClasses(deobfClasses);
	}

	public void addSeparatedClasses(List<ClassEntry> obfClasses, List<ClassEntry> deobfClasses) {
		EntryRemapper mapper = project.getMapper();

		Collection<ClassEntry> classes = project.getJarIndex().getEntryIndex().getClasses();
		Stream<ClassEntry> visibleClasses = classes.stream()
				.filter(entry -> !entry.isInnerClass());

		visibleClasses.forEach(entry -> {
			ClassEntry deobfEntry = mapper.deobfuscate(entry);

			List<ObfuscationTestService> obfService = enigma.getServices().get(ObfuscationTestService.TYPE);
			boolean obfuscated = deobfEntry.equals(entry);

			if (obfuscated && !obfService.isEmpty()) {
				if (obfService.stream().anyMatch(service -> service.testDeobfuscated(entry))) {
					obfuscated = false;
				}
			}

			if (obfuscated) {
				obfClasses.add(entry);
			} else {
				deobfClasses.add(entry);
			}
		});
	}

	public void refreshCurrentClass() {
		refreshCurrentClass(null);
	}

	private void refreshCurrentClass(EntryReference<Entry<?>, Entry<?>> reference) {
		refreshCurrentClass(reference, RefreshMode.MINIMAL);
	}

	private void refreshCurrentClass(EntryReference<Entry<?>, Entry<?>> reference, RefreshMode mode) {
		if (currentSource != null) {
			loadClass(currentSource.getEntry(), () -> {
				if (reference != null) {
					showReference(reference);
				}
			}, mode);
		}
	}

	private void loadClass(ClassEntry classEntry, Runnable callback) {
		loadClass(classEntry, callback, RefreshMode.MINIMAL);
	}

	private void loadClass(ClassEntry classEntry, Runnable callback, RefreshMode mode) {
		ClassEntry targetClass = classEntry.getOutermostClass();

		boolean requiresDecompile = mode == RefreshMode.FULL || currentSource == null || !currentSource.getEntry().equals(targetClass);
		if (requiresDecompile) {
			currentSource = null; // Or the GUI may try to find a nonexistent token
			gui.setEditorText(I18n.translate("info_panel.editor.class.decompiling"));
		}

		DECOMPILER_SERVICE.submit(() -> {
			try {
				if (requiresDecompile || mode == RefreshMode.JAVADOCS) {
					currentSource = decompileSource(targetClass, mode == RefreshMode.JAVADOCS);
				}

				remapSource(project.getMapper().getDeobfuscator());
				callback.run();
			} catch (Throwable t) {
				System.err.println("An exception was thrown while decompiling class " + classEntry.getFullName());
				t.printStackTrace(System.err);
			}
		});
	}

	private DecompiledClassSource decompileSource(ClassEntry targetClass, boolean onlyRefreshJavadocs) {
		try {
			if (!onlyRefreshJavadocs || currentSource == null || !currentSource.getEntry().equals(targetClass)) {
				uncommentedSource = decompiler.getSource(targetClass.getFullName());
			}

			Source source = uncommentedSource.addJavadocs(project.getMapper());

			if (source == null) {
				gui.setEditorText(I18n.translate("info_panel.editor.class.not_found") + " " + targetClass);
				return DecompiledClassSource.text(targetClass, "Unable to find class");
			}

			SourceIndex index = source.index();
			index.resolveReferences(project.getMapper().getObfResolver());

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

		currentSource.remapSource(project, translator);

		gui.setEditorTheme(Config.getInstance().lookAndFeel);
		gui.setSource(currentSource);
	}

	public void modifierChange(ItemEvent event) {
		if (event.getStateChange() == ItemEvent.SELECTED) {
			EntryRemapper mapper = project.getMapper();
			Entry<?> entry = gui.cursorReference.entry;
			AccessModifier modifier = (AccessModifier) event.getItem();

			EntryMapping mapping = mapper.getDeobfMapping(entry);
			if (mapping != null) {
				mapper.mapFromObf(entry, new EntryMapping(mapping.getTargetName(), modifier));
			} else {
				mapper.mapFromObf(entry, new EntryMapping(entry.getName(), modifier));
			}

			refreshCurrentClass();
		}
	}

	public ClassInheritanceTreeNode getClassInheritance(ClassEntry entry) {
		Translator translator = project.getMapper().getDeobfuscator();
		ClassInheritanceTreeNode rootNode = indexTreeBuilder.buildClassInheritance(translator, entry);
		return ClassInheritanceTreeNode.findNode(rootNode, entry);
	}

	public ClassImplementationsTreeNode getClassImplementations(ClassEntry entry) {
		Translator translator = project.getMapper().getDeobfuscator();
		return this.indexTreeBuilder.buildClassImplementations(translator, entry);
	}

	public MethodInheritanceTreeNode getMethodInheritance(MethodEntry entry) {
		Translator translator = project.getMapper().getDeobfuscator();
		MethodInheritanceTreeNode rootNode = indexTreeBuilder.buildMethodInheritance(translator, entry);
		return MethodInheritanceTreeNode.findNode(rootNode, entry);
	}

	public MethodImplementationsTreeNode getMethodImplementations(MethodEntry entry) {
		Translator translator = project.getMapper().getDeobfuscator();
		List<MethodImplementationsTreeNode> rootNodes = indexTreeBuilder.buildMethodImplementations(translator, entry);
		if (rootNodes.isEmpty()) {
			return null;
		}
		if (rootNodes.size() > 1) {
			System.err.println("WARNING: Method " + entry + " implements multiple interfaces. Only showing first one.");
		}
		return MethodImplementationsTreeNode.findNode(rootNodes.get(0), entry);
	}

	public ClassReferenceTreeNode getClassReferences(ClassEntry entry) {
		Translator deobfuscator = project.getMapper().getDeobfuscator();
		ClassReferenceTreeNode rootNode = new ClassReferenceTreeNode(deobfuscator, entry);
		rootNode.load(project.getJarIndex(), true);
		return rootNode;
	}

	public FieldReferenceTreeNode getFieldReferences(FieldEntry entry) {
		Translator translator = project.getMapper().getDeobfuscator();
		FieldReferenceTreeNode rootNode = new FieldReferenceTreeNode(translator, entry);
		rootNode.load(project.getJarIndex(), true);
		return rootNode;
	}

	public MethodReferenceTreeNode getMethodReferences(MethodEntry entry, boolean recursive) {
		Translator translator = project.getMapper().getDeobfuscator();
		MethodReferenceTreeNode rootNode = new MethodReferenceTreeNode(translator, entry);
		rootNode.load(project.getJarIndex(), true, recursive);
		return rootNode;
	}

	public void rename(EntryReference<Entry<?>, Entry<?>> reference, String newName, boolean refreshClassTree) {
		Entry<?> entry = reference.getNameableEntry();
		project.getMapper().mapFromObf(entry, new EntryMapping(newName));

		if (refreshClassTree && reference.entry instanceof ClassEntry && !((ClassEntry) reference.entry).isInnerClass())
			this.gui.moveClassTree(reference, newName);

		refreshCurrentClass(reference);
	}

	public void removeMapping(EntryReference<Entry<?>, Entry<?>> reference) {
		project.getMapper().removeByObf(reference.getNameableEntry());

		if (reference.entry instanceof ClassEntry)
			this.gui.moveClassTree(reference, false, true);
		refreshCurrentClass(reference);
	}

	public void changeDocs(EntryReference<Entry<?>, Entry<?>> reference, String updatedDocs) {
		changeDoc(reference.entry, updatedDocs);

		refreshCurrentClass(reference, RefreshMode.JAVADOCS);
	}

	public void changeDoc(Entry<?> obfEntry, String newDoc) {
		EntryRemapper mapper = project.getMapper();
		if (mapper.getDeobfMapping(obfEntry) == null) {
			markAsDeobfuscated(obfEntry,false); // NPE
		}
		mapper.mapFromObf(obfEntry, mapper.getDeobfMapping(obfEntry).withDocs(newDoc), false);
	}

	public void markAsDeobfuscated(Entry<?> obfEntry, boolean renaming) {
		EntryRemapper mapper = project.getMapper();
		mapper.mapFromObf(obfEntry, new EntryMapping(mapper.deobfuscate(obfEntry).getName()), renaming);
	}

	public void markAsDeobfuscated(EntryReference<Entry<?>, Entry<?>> reference) {
		EntryRemapper mapper = project.getMapper();
		Entry<?> entry = reference.getNameableEntry();
		mapper.mapFromObf(entry, new EntryMapping(mapper.deobfuscate(entry).getName()));

		if (reference.entry instanceof ClassEntry && !((ClassEntry) reference.entry).isInnerClass())
			this.gui.moveClassTree(reference, true, false);

		refreshCurrentClass(reference);
	}

	public void openStats(Set<StatsMember> includedMembers) {
		ProgressDialog.runOffThread(gui.getFrame(), progress -> {
			String data = new StatsGenerator(project).generate(progress, includedMembers);

			try {
				File statsFile = File.createTempFile("stats", ".html");

				try (FileWriter w = new FileWriter(statsFile)) {
					w.write(
							Utils.readResourceToString("/stats.html")
								 .replace("/*data*/", data)
					);
				}

				Desktop.getDesktop().open(statsFile);
			} catch (IOException e) {
				throw new Error(e);
			}
		});
	}

	public void setDecompiler(DecompilerService service) {
		uncommentedSource = null;
		decompilerService = service;
		decompiler = createDecompiler();
		refreshCurrentClass(null, RefreshMode.FULL);
	}
}
