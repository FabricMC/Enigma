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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import com.strobel.decompiler.languages.java.ast.CompilationUnit;
import cuchaz.enigma.Deobfuscator;
import cuchaz.enigma.analysis.*;
import cuchaz.enigma.api.EnigmaPlugin;
import cuchaz.enigma.config.Config;
import cuchaz.enigma.gui.dialog.ProgressDialog;
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
import java.awt.event.ItemEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

public class GuiController {

	private Deobfuscator deobfuscator;
	private Gui gui;
	private SourceIndex index;
	private ClassEntry currentObfClass;
	private Deque<EntryReference<Entry<?>, Entry<?>>> referenceStack;

	private Path loadedMappingPath;
	private MappingFormat loadedMappingFormat;

	public GuiController(Gui gui) {
		this.gui = gui;
		this.deobfuscator = null;
		this.index = null;
		this.currentObfClass = null;
		this.referenceStack = Queues.newArrayDeque();
	}

	public boolean isDirty() {
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

	public void openMappings(MappingFormat format, Path path) throws IOException, MappingParseException {
		EntryTree<EntryMapping> mappings = format.read(path);
		deobfuscator.setMappings(mappings);

		gui.setMappingsFile(path);
		loadedMappingFormat = format;

		refreshClasses();
		refreshCurrentClass();
	}

	public void saveMappings(Path path) {
		saveMappings(loadedMappingFormat, path);
	}

	public void saveMappings(MappingFormat format, Path path) {
		EntryRemapper mapper = deobfuscator.getMapper();

		MappingDelta delta = mapper.takeMappingDelta();
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
		this.deobfuscator.setMappings(null);
		this.gui.setMappingsFile(null);
		refreshClasses();
		refreshCurrentClass();
	}

	public void exportSource(final File dirOut) {
		ProgressDialog.runInThread(this.gui.getFrame(), progress -> this.deobfuscator.writeSources(dirOut, progress));
	}

	public void exportJar(final File fileOut) {
		ProgressDialog.runInThread(this.gui.getFrame(), progress -> this.deobfuscator.writeJar(fileOut, progress));
	}

	public Token getToken(int pos) {
		if (this.index == null) {
			return null;
		}
		return this.index.getReferenceToken(pos);
	}

	@Nullable
	public EntryReference<Entry<?>, Entry<?>> getDeobfReference(Token token) {
		if (this.index == null) {
			return null;
		}
		return this.index.getDeobfReference(token);
	}

	public ReadableToken getReadableToken(Token token) {
		if (this.index == null) {
			return null;
		}
		return new ReadableToken(
				this.index.getLineNumber(token.start),
				this.index.getColumnNumber(token.start),
				this.index.getColumnNumber(token.end)
		);
	}

	public boolean entryHasDeobfuscatedName(Entry<?> deobfEntry) {
		EntryResolver resolver = this.deobfuscator.getMapper().getDeobfResolver();
		Entry<?> resolvedEntry = resolver.resolveFirstEntry(deobfEntry, ResolutionStrategy.RESOLVE_ROOT);
		return this.deobfuscator.hasDeobfuscatedName(this.deobfuscator.getMapper().obfuscate(resolvedEntry));
	}

	public boolean entryIsInJar(Entry<?> deobfEntry) {
		if (deobfEntry == null) return false;
		return this.deobfuscator.isObfuscatedIdentifier(this.deobfuscator.getMapper().obfuscate(deobfEntry));
	}

	public boolean referenceIsRenameable(EntryReference<Entry<?>, Entry<?>> deobfReference) {
		if (deobfReference == null) return false;
		return this.deobfuscator.isRenameable(this.deobfuscator.getMapper().obfuscate(deobfReference));
	}

	public ClassInheritanceTreeNode getClassInheritance(ClassEntry deobfClassEntry) {
		ClassEntry obfClassEntry = this.deobfuscator.getMapper().obfuscate(deobfClassEntry);
		Translator translator = this.deobfuscator.getMapper().getDeobfuscator();
		ClassInheritanceTreeNode rootNode = this.deobfuscator.getIndexTreeBuilder().buildClassInheritance(translator, obfClassEntry);
		return ClassInheritanceTreeNode.findNode(rootNode, obfClassEntry);
	}

	public ClassImplementationsTreeNode getClassImplementations(ClassEntry deobfClassEntry) {
		ClassEntry obfClassEntry = this.deobfuscator.getMapper().obfuscate(deobfClassEntry);
		Translator translator = this.deobfuscator.getMapper().getDeobfuscator();
		return this.deobfuscator.getIndexTreeBuilder().buildClassImplementations(translator, obfClassEntry);
	}

	public MethodInheritanceTreeNode getMethodInheritance(MethodEntry deobfMethodEntry) {
		MethodEntry obfMethodEntry = this.deobfuscator.getMapper().obfuscate(deobfMethodEntry);
		Translator translator = this.deobfuscator.getMapper().getDeobfuscator();
		MethodInheritanceTreeNode rootNode = this.deobfuscator.getIndexTreeBuilder().buildMethodInheritance(translator, obfMethodEntry);
		return MethodInheritanceTreeNode.findNode(rootNode, obfMethodEntry);
	}

	public MethodImplementationsTreeNode getMethodImplementations(MethodEntry deobfMethodEntry) {
		MethodEntry obfMethodEntry = this.deobfuscator.getMapper().obfuscate(deobfMethodEntry);
		Translator translator = this.deobfuscator.getMapper().getDeobfuscator();
		List<MethodImplementationsTreeNode> rootNodes = this.deobfuscator.getIndexTreeBuilder().buildMethodImplementations(translator, obfMethodEntry);
		if (rootNodes.isEmpty()) {
			return null;
		}
		if (rootNodes.size() > 1) {
			System.err.println("WARNING: Method " + deobfMethodEntry + " implements multiple interfaces. Only showing first one.");
		}
		return MethodImplementationsTreeNode.findNode(rootNodes.get(0), obfMethodEntry);
	}

	public ClassReferenceTreeNode getClassReferences(ClassEntry deobfClassEntry) {
		ClassEntry obfClassEntry = this.deobfuscator.getMapper().obfuscate(deobfClassEntry);
		Translator deobfuscator = this.deobfuscator.getMapper().getDeobfuscator();
		ClassReferenceTreeNode rootNode = new ClassReferenceTreeNode(deobfuscator, obfClassEntry);
		rootNode.load(this.deobfuscator.getJarIndex(), true);
		return rootNode;
	}

	public FieldReferenceTreeNode getFieldReferences(FieldEntry deobfFieldEntry) {
		FieldEntry obfFieldEntry = this.deobfuscator.getMapper().obfuscate(deobfFieldEntry);
		Translator translator = this.deobfuscator.getMapper().getDeobfuscator();
		FieldReferenceTreeNode rootNode = new FieldReferenceTreeNode(translator, obfFieldEntry);
		rootNode.load(this.deobfuscator.getJarIndex(), true);
		return rootNode;
	}

	public MethodReferenceTreeNode getMethodReferences(MethodEntry deobfMethodEntry, boolean recursive) {
		MethodEntry obfMethodEntry = this.deobfuscator.getMapper().obfuscate(deobfMethodEntry);
		Translator translator = this.deobfuscator.getMapper().getDeobfuscator();
		MethodReferenceTreeNode rootNode = new MethodReferenceTreeNode(translator, obfMethodEntry);
		rootNode.load(this.deobfuscator.getJarIndex(), true, recursive);
		return rootNode;
	}

	public void rename(EntryReference<Entry<?>, Entry<?>> deobfReference, String newName, boolean refreshClassTree) {
		EntryReference<Entry<?>, Entry<?>> obfReference = this.deobfuscator.getMapper().obfuscate(deobfReference);
		this.deobfuscator.rename(obfReference.getNameableEntry(), newName);

		if (refreshClassTree && deobfReference.entry instanceof ClassEntry && !((ClassEntry) deobfReference.entry).isInnerClass())
			this.gui.moveClassTree(deobfReference, newName);
		refreshCurrentClass(obfReference);

	}

	public void removeMapping(EntryReference<Entry<?>, Entry<?>> deobfReference) {
		EntryReference<Entry<?>, Entry<?>> obfReference = this.deobfuscator.getMapper().obfuscate(deobfReference);
		this.deobfuscator.removeMapping(obfReference.getNameableEntry());
		if (deobfReference.entry instanceof ClassEntry)
			this.gui.moveClassTree(deobfReference, obfReference.entry.getName(), false, true);
		refreshCurrentClass(obfReference);
	}

	public void markAsDeobfuscated(EntryReference<Entry<?>, Entry<?>> deobfReference) {
		EntryReference<Entry<?>, Entry<?>> obfReference = this.deobfuscator.getMapper().obfuscate(deobfReference);
		this.deobfuscator.markAsDeobfuscated(obfReference.getNameableEntry());
		if (deobfReference.entry instanceof ClassEntry && !((ClassEntry) deobfReference.entry).isInnerClass())
			this.gui.moveClassTree(deobfReference, obfReference.entry.getName(), true, false);
		refreshCurrentClass(obfReference);
	}

	public void openDeclaration(Entry<?> deobfEntry) {
		if (deobfEntry == null) {
			throw new IllegalArgumentException("Entry cannot be null!");
		}
		openReference(new EntryReference<>(deobfEntry, deobfEntry.getName()));
	}

	public void openReference(EntryReference<Entry<?>, Entry<?>> deobfReference) {
		if (deobfReference == null) {
			throw new IllegalArgumentException("Reference cannot be null!");
		}

		// get the reference target class
		EntryReference<Entry<?>, Entry<?>> obfReference = this.deobfuscator.getMapper().obfuscate(deobfReference);
		ClassEntry obfClassEntry = obfReference.getLocationClassEntry();
		if (!this.deobfuscator.isObfuscatedIdentifier(obfClassEntry)) {
			throw new IllegalArgumentException("Obfuscated class " + obfClassEntry + " was not found in the jar!");
		}
		if (this.currentObfClass == null || !this.currentObfClass.equals(obfClassEntry)) {
			// deobfuscate the class, then navigate to the reference
			this.currentObfClass = obfClassEntry;
			deobfuscate(this.currentObfClass, obfReference);
		} else {
			showReference(obfReference);
		}
	}

	private void showReference(EntryReference<Entry<?>, Entry<?>> obfReference) {
		EntryRemapper mapper = this.deobfuscator.getMapper();

		Collection<Token> tokens = mapper.getObfResolver().resolveReference(obfReference, ResolutionStrategy.RESOLVE_ROOT)
				.stream()
				.map(mapper::deobfuscate)
				.flatMap(reference -> index.getReferenceTokens(reference).stream())
				.collect(Collectors.toList());

		if (tokens.isEmpty()) {
			// DEBUG
			System.err.println(String.format("WARNING: no tokens found for %s in %s", tokens, this.currentObfClass));
		} else {
			this.gui.showTokens(tokens);
		}
	}

	public void savePreviousReference(EntryReference<Entry<?>, Entry<?>> deobfReference) {
		this.referenceStack.push(this.deobfuscator.getMapper().obfuscate(deobfReference));
	}

	public void openPreviousReference() {
		if (hasPreviousLocation()) {
			openReference(this.deobfuscator.getMapper().deobfuscate(this.referenceStack.pop()));
		}
	}

	public boolean hasPreviousLocation() {
		return !this.referenceStack.isEmpty();
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

	private void refreshCurrentClass(EntryReference<Entry<?>, Entry<?>> obfReference) {
		if (this.currentObfClass != null) {
			deobfuscate(this.currentObfClass, obfReference);
		}
	}

	private void deobfuscate(final ClassEntry classEntry, final EntryReference<Entry<?>, Entry<?>> obfReference) {

		this.gui.setSource("(deobfuscating...)");

		// run the deobfuscator in a separate thread so we don't block the GUI event queue
		new Thread(() ->
		{
			// decompile,deobfuscate the bytecode
			CompilationUnit sourceTree = deobfuscator.getSourceTree(classEntry.getOutermostClass().getFullName());
			if (sourceTree == null) {
				// decompilation of this class is not supported
				gui.setSource("Unable to find class: " + classEntry);
				return;
			}
			String source = deobfuscator.getSource(sourceTree);
			index = deobfuscator.getSourceIndex(sourceTree, source);

			String sourceString = index.getSource();

			// set the highlighted tokens
			List<Token> obfuscatedTokens = Lists.newArrayList();
			List<Token> proposedTokens = Lists.newArrayList();
			List<Token> deobfuscatedTokens = Lists.newArrayList();
			List<Token> otherTokens = Lists.newArrayList();

			int offset = 0;
			Map<Token, Token> tokenRemap = new HashMap<>();
			boolean remapped = false;

			for (Token inToken : index.referenceTokens()) {
				Token token = inToken.move(offset);

				EntryReference<Entry<?>, Entry<?>> reference = index.getDeobfReference(inToken);
				if (referenceIsRenameable(reference)) {
					boolean added = false;

					if (!entryHasDeobfuscatedName(reference.getNameableEntry())) {
						Entry<?> obfEntry = deobfuscator.getMapper().obfuscate(reference.getNameableEntry());
						if (obfEntry instanceof FieldEntry) {
							for (EnigmaPlugin plugin : deobfuscator.getPlugins()) {
								String owner = obfEntry.getContainingClass().getFullName();
								String proposal = plugin.proposeFieldName(owner, obfEntry.getName(), ((FieldEntry) obfEntry).getDesc().toString());
								if (proposal != null) {
									proposedTokens.add(token);
									offset += token.getRenameOffset(proposal);
									sourceString = token.rename(sourceString, proposal);
									added = true;
									remapped = true;
									break;
								}
							}
						}
					}

					if (!added) {
						if (entryHasDeobfuscatedName(reference.getNameableEntry())) {
							deobfuscatedTokens.add(token);
						} else {
							obfuscatedTokens.add(token);
						}
					}
				} else {
					otherTokens.add(token);
				}

				tokenRemap.put(inToken, token);
			}

			if (remapped) {
				index.remap(sourceString, tokenRemap);
			}

			gui.setSource(sourceString);
			if (obfReference != null) {
				showReference(obfReference);
			}

			gui.setEditorTheme(Config.getInstance().lookAndFeel);
			gui.setHighlightedTokens(ImmutableMap.of(
					"obfuscated", obfuscatedTokens,
					"proposed", proposedTokens,
					"deobfuscated", deobfuscatedTokens,
					"other", otherTokens
			));
		}).start();
	}

	public Deobfuscator getDeobfuscator() {
		return deobfuscator;
	}

	public void modifierChange(ItemEvent event) {
		if (event.getStateChange() == ItemEvent.SELECTED) {
			deobfuscator.changeModifier(gui.reference.entry, (AccessModifier) event.getItem());
			refreshCurrentClass();
		}
	}
}
