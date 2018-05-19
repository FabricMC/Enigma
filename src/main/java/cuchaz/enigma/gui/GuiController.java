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
import com.google.common.collect.Queues;
import com.strobel.decompiler.languages.java.ast.CompilationUnit;
import cuchaz.enigma.Deobfuscator;
import cuchaz.enigma.analysis.*;
import cuchaz.enigma.gui.dialog.ProgressDialog;
import cuchaz.enigma.mapping.*;
import cuchaz.enigma.throwables.MappingParseException;
import cuchaz.enigma.utils.ReadableToken;

import java.awt.event.ItemEvent;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.jar.JarFile;

public class GuiController {

	private Deobfuscator deobfuscator;
	private Gui gui;
	private SourceIndex index;
	private ClassEntry currentObfClass;
	private boolean isDirty;
	private Deque<EntryReference<Entry, Entry>> referenceStack;

	public GuiController(Gui gui) {
		this.gui = gui;
		this.deobfuscator = null;
		this.index = null;
		this.currentObfClass = null;
		this.isDirty = false;
		this.referenceStack = Queues.newArrayDeque();
	}

	public boolean isDirty() {
		return this.isDirty;
	}

	public void openJar(final JarFile jar) throws IOException {
		this.gui.onStartOpenJar();
		this.deobfuscator = new Deobfuscator(jar);
		this.gui.onFinishOpenJar(this.deobfuscator.getJarName());
		refreshClasses();
	}

	public void closeJar() {
		this.deobfuscator = null;
		this.gui.onCloseJar();
	}

	public void openEnigmaMappings(File file) throws IOException, MappingParseException {
		this.deobfuscator.setMappings(new MappingsEnigmaReader().read(file));
		this.isDirty = false;
		this.gui.setMappingsFile(file);
		refreshClasses();
		refreshCurrentClass();
	}

	public void openTinyMappings(File file) throws IOException, MappingParseException {
		this.deobfuscator.setMappings(new MappingsTinyReader().read(file));
		this.isDirty = false;
		this.gui.setMappingsFile(file);
		refreshClasses();
		refreshCurrentClass();
	}

	public void saveMappings(File file) throws IOException {
		Mappings mappings = this.deobfuscator.getMappings();
		switch (mappings.getOriginMappingFormat()) {
			case SRG_FILE:
				saveSRGMappings(file);
				break;
			default:
				saveEnigmaMappings(file, Mappings.FormatType.ENIGMA_FILE != mappings.getOriginMappingFormat());
				break;
		}

	}

	public void saveEnigmaMappings(File file, boolean isDirectoryFormat) throws IOException {
		this.deobfuscator.getMappings().saveEnigmaMappings(file, isDirectoryFormat);
		this.isDirty = false;
	}

	public void saveSRGMappings(File file) throws IOException {
		this.deobfuscator.getMappings().saveSRGMappings(file);
		this.isDirty = false;
	}

	public void closeMappings() {
		this.deobfuscator.setMappings(null);
		this.gui.setMappingsFile(null);
		refreshClasses();
		refreshCurrentClass();
	}

	public void rebuildMethodNames() {
		ProgressDialog.runInThread(this.gui.getFrame(), progress -> this.deobfuscator.rebuildMethodNames(progress));
		this.isDirty = true;
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

	public EntryReference<Entry, Entry> getDeobfReference(Token token) {
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

	public boolean entryHasDeobfuscatedName(Entry deobfEntry) {
		return this.deobfuscator.hasDeobfuscatedName(this.deobfuscator.obfuscateEntry(deobfEntry));
	}

	public boolean entryIsInJar(Entry deobfEntry) {
		return this.deobfuscator.isObfuscatedIdentifier(this.deobfuscator.obfuscateEntry(deobfEntry));
	}

	public boolean referenceIsRenameable(EntryReference<Entry, Entry> deobfReference) {
		return this.deobfuscator.isRenameable(this.deobfuscator.obfuscateReference(deobfReference), true);
	}

	public ClassInheritanceTreeNode getClassInheritance(ClassEntry deobfClassEntry) {
		ClassEntry obfClassEntry = this.deobfuscator.obfuscateEntry(deobfClassEntry);
		ClassInheritanceTreeNode rootNode = this.deobfuscator.getJarIndex().getClassInheritance(this.deobfuscator.getTranslator(TranslationDirection.DEOBFUSCATING), obfClassEntry);
		return ClassInheritanceTreeNode.findNode(rootNode, obfClassEntry);
	}

	public ClassImplementationsTreeNode getClassImplementations(ClassEntry deobfClassEntry) {
		ClassEntry obfClassEntry = this.deobfuscator.obfuscateEntry(deobfClassEntry);
		return this.deobfuscator.getJarIndex().getClassImplementations(this.deobfuscator.getTranslator(TranslationDirection.DEOBFUSCATING), obfClassEntry);
	}

	public MethodInheritanceTreeNode getMethodInheritance(MethodEntry deobfMethodEntry) {
		MethodEntry obfMethodEntry = this.deobfuscator.obfuscateEntry(deobfMethodEntry);
		MethodInheritanceTreeNode rootNode = this.deobfuscator.getJarIndex().getMethodInheritance(this.deobfuscator.getTranslator(TranslationDirection.DEOBFUSCATING), obfMethodEntry);
		return MethodInheritanceTreeNode.findNode(rootNode, obfMethodEntry);
	}

	public MethodImplementationsTreeNode getMethodImplementations(MethodEntry deobfMethodEntry) {
		MethodEntry obfMethodEntry = this.deobfuscator.obfuscateEntry(deobfMethodEntry);
		List<MethodImplementationsTreeNode> rootNodes = this.deobfuscator.getJarIndex().getMethodImplementations(this.deobfuscator.getTranslator(TranslationDirection.DEOBFUSCATING), obfMethodEntry);
		if (rootNodes.isEmpty()) {
			return null;
		}
		if (rootNodes.size() > 1) {
			System.err.println("WARNING: Method " + deobfMethodEntry + " implements multiple interfaces. Only showing first one.");
		}
		return MethodImplementationsTreeNode.findNode(rootNodes.get(0), obfMethodEntry);
	}

	public FieldReferenceTreeNode getFieldReferences(FieldEntry deobfFieldEntry) {
		FieldEntry obfFieldEntry = this.deobfuscator.obfuscateEntry(deobfFieldEntry);
		FieldReferenceTreeNode rootNode = new FieldReferenceTreeNode(this.deobfuscator.getTranslator(TranslationDirection.DEOBFUSCATING), obfFieldEntry);
		rootNode.load(this.deobfuscator.getJarIndex(), true);
		return rootNode;
	}

	public MethodReferenceTreeNode getMethodReferences(MethodEntry deobfMethodEntry) {
		MethodEntry obfMethodEntry = this.deobfuscator.obfuscateEntry(deobfMethodEntry);
		MethodReferenceTreeNode rootNode = new MethodReferenceTreeNode(this.deobfuscator.getTranslator(TranslationDirection.DEOBFUSCATING), obfMethodEntry);
		rootNode.load(this.deobfuscator.getJarIndex(), true);
		return rootNode;
	}

	public void rename(EntryReference<Entry, Entry> deobfReference, String newName) {
		rename(deobfReference, newName, true, true);
	}

	public void rename(EntryReference<Entry, Entry> deobfReference, String newName, boolean refreshClassTree, boolean clearTranslationCache) {
		EntryReference<Entry, Entry> obfReference = this.deobfuscator.obfuscateReference(deobfReference);
		this.deobfuscator.rename(obfReference.getNameableEntry(), newName, clearTranslationCache);
		this.isDirty = true;

		if (refreshClassTree && deobfReference.entry instanceof ClassEntry && !((ClassEntry) deobfReference.entry).isInnerClass())
			this.gui.moveClassTree(deobfReference, newName);
		refreshCurrentClass(obfReference);

	}

	public void removeMapping(EntryReference<Entry, Entry> deobfReference) {
		EntryReference<Entry, Entry> obfReference = this.deobfuscator.obfuscateReference(deobfReference);
		this.deobfuscator.removeMapping(obfReference.getNameableEntry());
		this.isDirty = true;
		if (deobfReference.entry instanceof ClassEntry)
			this.gui.moveClassTree(deobfReference, obfReference.entry.getName(), false, true);
		refreshCurrentClass(obfReference);
	}

	public void markAsDeobfuscated(EntryReference<Entry, Entry> deobfReference) {
		EntryReference<Entry, Entry> obfReference = this.deobfuscator.obfuscateReference(deobfReference);
		this.deobfuscator.markAsDeobfuscated(obfReference.getNameableEntry());
		this.isDirty = true;
		if (deobfReference.entry instanceof ClassEntry && !((ClassEntry) deobfReference.entry).isInnerClass())
			this.gui.moveClassTree(deobfReference, obfReference.entry.getName(), true, false);
		refreshCurrentClass(obfReference);
	}

	public void openDeclaration(Entry deobfEntry) {
		if (deobfEntry == null) {
			throw new IllegalArgumentException("Entry cannot be null!");
		}
		openReference(new EntryReference<>(deobfEntry, deobfEntry.getName()));
	}

	public void openReference(EntryReference<Entry, Entry> deobfReference) {
		if (deobfReference == null) {
			throw new IllegalArgumentException("Reference cannot be null!");
		}

		// get the reference target class
		EntryReference<Entry, Entry> obfReference = this.deobfuscator.obfuscateReference(deobfReference);
		ClassEntry obfClassEntry = obfReference.getLocationClassEntry().getOutermostClassEntry();
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

	private void showReference(EntryReference<Entry, Entry> obfReference) {
		EntryReference<Entry, Entry> deobfReference = this.deobfuscator.deobfuscateReference(obfReference);
		Collection<Token> tokens = this.index.getReferenceTokens(deobfReference);
		if (tokens.isEmpty()) {
			// DEBUG
			System.err.println(String.format("WARNING: no tokens found for %s in %s", deobfReference, this.currentObfClass));
		} else {
			this.gui.showTokens(tokens);
		}
	}

	public void savePreviousReference(EntryReference<Entry, Entry> deobfReference) {
		this.referenceStack.push(this.deobfuscator.obfuscateReference(deobfReference));
	}

	public void openPreviousReference() {
		if (hasPreviousLocation()) {
			openReference(this.deobfuscator.deobfuscateReference(this.referenceStack.pop()));
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

	private void refreshCurrentClass(EntryReference<Entry, Entry> obfReference) {
		if (this.currentObfClass != null) {
			deobfuscate(this.currentObfClass, obfReference);
		}
	}

	private void deobfuscate(final ClassEntry classEntry, final EntryReference<Entry, Entry> obfReference) {

		this.gui.setSource("(deobfuscating...)");

		// run the deobfuscator in a separate thread so we don't block the GUI event queue
		new Thread(() ->
		{
			// decompile,deobfuscate the bytecode
			CompilationUnit sourceTree = deobfuscator.getSourceTree(classEntry.getClassName());
			if (sourceTree == null) {
				// decompilation of this class is not supported
				gui.setSource("Unable to find class: " + classEntry);
				return;
			}
			String source = deobfuscator.getSource(sourceTree);
			index = deobfuscator.getSourceIndex(sourceTree, source);
			gui.setSource(index.getSource());
			if (obfReference != null) {
				showReference(obfReference);
			}

			// set the highlighted tokens
			List<Token> obfuscatedTokens = Lists.newArrayList();
			List<Token> deobfuscatedTokens = Lists.newArrayList();
			List<Token> otherTokens = Lists.newArrayList();
			for (Token token : index.referenceTokens()) {
				EntryReference<Entry, Entry> reference = index.getDeobfReference(token);
				if (referenceIsRenameable(reference)) {
					if (entryHasDeobfuscatedName(reference.getNameableEntry())) {
						deobfuscatedTokens.add(token);
					} else {
						obfuscatedTokens.add(token);
					}
				} else {
					otherTokens.add(token);
				}
			}
			gui.setHighlightedTokens(obfuscatedTokens, deobfuscatedTokens, otherTokens);
		}).start();
	}

	public Deobfuscator getDeobfuscator() {
		return deobfuscator;
	}

	public void modifierChange(ItemEvent event) {
		if (event.getStateChange() == ItemEvent.SELECTED) {
			deobfuscator.changeModifier(gui.reference.entry, (Mappings.EntryModifier) event.getItem());
			this.isDirty = true;
			refreshCurrentClass();
		}
	}
}
