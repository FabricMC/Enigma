/*******************************************************************************
 * Copyright (c) 2015 Jeff Martin.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public
 * License v3.0 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * 
 * Contributors:
 *     Jeff Martin - initial API and implementation
 ******************************************************************************/
package cuchaz.enigma.gui;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.jar.JarFile;

import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import com.strobel.decompiler.languages.java.ast.CompilationUnit;

import cuchaz.enigma.Deobfuscator;
import cuchaz.enigma.Deobfuscator.ProgressListener;
import cuchaz.enigma.analysis.BehaviorReferenceTreeNode;
import cuchaz.enigma.analysis.ClassImplementationsTreeNode;
import cuchaz.enigma.analysis.ClassInheritanceTreeNode;
import cuchaz.enigma.analysis.EntryReference;
import cuchaz.enigma.analysis.FieldReferenceTreeNode;
import cuchaz.enigma.analysis.MethodImplementationsTreeNode;
import cuchaz.enigma.analysis.MethodInheritanceTreeNode;
import cuchaz.enigma.analysis.SourceIndex;
import cuchaz.enigma.analysis.Token;
import cuchaz.enigma.gui.ProgressDialog.ProgressRunnable;
import cuchaz.enigma.mapping.BehaviorEntry;
import cuchaz.enigma.mapping.ClassEntry;
import cuchaz.enigma.mapping.Entry;
import cuchaz.enigma.mapping.FieldEntry;
import cuchaz.enigma.mapping.MappingParseException;
import cuchaz.enigma.mapping.MappingsReader;
import cuchaz.enigma.mapping.MappingsWriter;
import cuchaz.enigma.mapping.MethodEntry;
import cuchaz.enigma.mapping.TranslationDirection;

public class GuiController {
	
	private Deobfuscator m_deobfuscator;
	private Gui m_gui;
	private SourceIndex m_index;
	private ClassEntry m_currentObfClass;
	private boolean m_isDirty;
	private Deque<EntryReference<Entry,Entry>> m_referenceStack;
	
	public GuiController(Gui gui) {
		m_gui = gui;
		m_deobfuscator = null;
		m_index = null;
		m_currentObfClass = null;
		m_isDirty = false;
		m_referenceStack = Queues.newArrayDeque();
	}
	
	public boolean isDirty() {
		return m_isDirty;
	}
	
	public void openJar(final JarFile jar) throws IOException {
		m_gui.onStartOpenJar();
		m_deobfuscator = new Deobfuscator(jar);
		m_gui.onFinishOpenJar(m_deobfuscator.getJarName());
		refreshClasses();
	}
	
	public void closeJar() {
		m_deobfuscator = null;
		m_gui.onCloseJar();
	}
	
	public void openMappings(File file) throws IOException, MappingParseException {
		FileReader in = new FileReader(file);
		m_deobfuscator.setMappings(new MappingsReader().read(in));
		in.close();
		m_isDirty = false;
		m_gui.setMappingsFile(file);
		refreshClasses();
		refreshCurrentClass();
	}
	
	public void saveMappings(File file) throws IOException {
		FileWriter out = new FileWriter(file);
		new MappingsWriter().write(out, m_deobfuscator.getMappings());
		out.close();
		m_isDirty = false;
	}
	
	public void closeMappings() {
		m_deobfuscator.setMappings(null);
		m_gui.setMappingsFile(null);
		refreshClasses();
		refreshCurrentClass();
	}
	
	public void exportSource(final File dirOut) {
		ProgressDialog.runInThread(m_gui.getFrame(), new ProgressRunnable() {
			@Override
			public void run(ProgressListener progress) throws Exception {
				m_deobfuscator.writeSources(dirOut, progress);
			}
		});
	}
	
	public void exportJar(final File fileOut) {
		ProgressDialog.runInThread(m_gui.getFrame(), new ProgressRunnable() {
			@Override
			public void run(ProgressListener progress) {
				m_deobfuscator.writeJar(fileOut, progress);
			}
		});
	}
	
	public Token getToken(int pos) {
		if (m_index == null) {
			return null;
		}
		return m_index.getReferenceToken(pos);
	}
	
	public EntryReference<Entry,Entry> getDeobfReference(Token token) {
		if (m_index == null) {
			return null;
		}
		return m_index.getDeobfReference(token);
	}
	
	public ReadableToken getReadableToken(Token token) {
		if (m_index == null) {
			return null;
		}
		return new ReadableToken(
			m_index.getLineNumber(token.start),
			m_index.getColumnNumber(token.start),
			m_index.getColumnNumber(token.end)
		);
	}
	
	public boolean entryHasDeobfuscatedName(Entry deobfEntry) {
		return m_deobfuscator.hasDeobfuscatedName(m_deobfuscator.obfuscateEntry(deobfEntry));
	}
	
	public boolean entryIsInJar(Entry deobfEntry) {
		return m_deobfuscator.isObfuscatedIdentifier(m_deobfuscator.obfuscateEntry(deobfEntry));
	}
	
	public boolean referenceIsRenameable(EntryReference<Entry,Entry> deobfReference) {
		return m_deobfuscator.isRenameable(m_deobfuscator.obfuscateReference(deobfReference));
	}
	
	public ClassInheritanceTreeNode getClassInheritance(ClassEntry deobfClassEntry) {
		ClassEntry obfClassEntry = m_deobfuscator.obfuscateEntry(deobfClassEntry);
		ClassInheritanceTreeNode rootNode = m_deobfuscator.getJarIndex().getClassInheritance(
			m_deobfuscator.getTranslator(TranslationDirection.Deobfuscating),
			obfClassEntry
		);
		return ClassInheritanceTreeNode.findNode(rootNode, obfClassEntry);
	}
	
	public ClassImplementationsTreeNode getClassImplementations(ClassEntry deobfClassEntry) {
		ClassEntry obfClassEntry = m_deobfuscator.obfuscateEntry(deobfClassEntry);
		return m_deobfuscator.getJarIndex().getClassImplementations(
			m_deobfuscator.getTranslator(TranslationDirection.Deobfuscating),
			obfClassEntry
		);
	}
	
	public MethodInheritanceTreeNode getMethodInheritance(MethodEntry deobfMethodEntry) {
		MethodEntry obfMethodEntry = m_deobfuscator.obfuscateEntry(deobfMethodEntry);
		MethodInheritanceTreeNode rootNode = m_deobfuscator.getJarIndex().getMethodInheritance(
			m_deobfuscator.getTranslator(TranslationDirection.Deobfuscating),
			obfMethodEntry
		);
		return MethodInheritanceTreeNode.findNode(rootNode, obfMethodEntry);
	}
	
	public MethodImplementationsTreeNode getMethodImplementations(MethodEntry deobfMethodEntry) {
		MethodEntry obfMethodEntry = m_deobfuscator.obfuscateEntry(deobfMethodEntry);
		List<MethodImplementationsTreeNode> rootNodes = m_deobfuscator.getJarIndex().getMethodImplementations(
			m_deobfuscator.getTranslator(TranslationDirection.Deobfuscating),
			obfMethodEntry
		);
		if (rootNodes.isEmpty()) {
			return null;
		}
		if (rootNodes.size() > 1) {
			System.err.println("WARNING: Method " + deobfMethodEntry + " implements multiple interfaces. Only showing first one.");
		}
		return MethodImplementationsTreeNode.findNode(rootNodes.get(0), obfMethodEntry);
	}
	
	public FieldReferenceTreeNode getFieldReferences(FieldEntry deobfFieldEntry) {
		FieldEntry obfFieldEntry = m_deobfuscator.obfuscateEntry(deobfFieldEntry);
		FieldReferenceTreeNode rootNode = new FieldReferenceTreeNode(
			m_deobfuscator.getTranslator(TranslationDirection.Deobfuscating),
			obfFieldEntry
		);
		rootNode.load(m_deobfuscator.getJarIndex(), true);
		return rootNode;
	}
	
	public BehaviorReferenceTreeNode getMethodReferences(BehaviorEntry deobfBehaviorEntry) {
		BehaviorEntry obfBehaviorEntry = m_deobfuscator.obfuscateEntry(deobfBehaviorEntry);
		BehaviorReferenceTreeNode rootNode = new BehaviorReferenceTreeNode(
			m_deobfuscator.getTranslator(TranslationDirection.Deobfuscating),
			obfBehaviorEntry
		);
		rootNode.load(m_deobfuscator.getJarIndex(), true);
		return rootNode;
	}
	
	public void rename(EntryReference<Entry,Entry> deobfReference, String newName) {
		EntryReference<Entry,Entry> obfReference = m_deobfuscator.obfuscateReference(deobfReference);
		m_deobfuscator.rename(obfReference.getNameableEntry(), newName);
		m_isDirty = true;
		refreshClasses();
		refreshCurrentClass(obfReference);
	}
	
	public void removeMapping(EntryReference<Entry,Entry> deobfReference) {
		EntryReference<Entry,Entry> obfReference = m_deobfuscator.obfuscateReference(deobfReference);
		m_deobfuscator.removeMapping(obfReference.getNameableEntry());
		m_isDirty = true;
		refreshClasses();
		refreshCurrentClass(obfReference);
	}
	
	public void markAsDeobfuscated(EntryReference<Entry,Entry> deobfReference) {
		EntryReference<Entry,Entry> obfReference = m_deobfuscator.obfuscateReference(deobfReference);
		m_deobfuscator.markAsDeobfuscated(obfReference.getNameableEntry());
		m_isDirty = true;
		refreshClasses();
		refreshCurrentClass(obfReference);
	}
	
	public void openDeclaration(Entry deobfEntry) {
		if (deobfEntry == null) {
			throw new IllegalArgumentException("Entry cannot be null!");
		}
		openReference(new EntryReference<Entry,Entry>(deobfEntry, deobfEntry.getName()));
	}
	
	public void openReference(EntryReference<Entry,Entry> deobfReference) {
		if (deobfReference == null) {
			throw new IllegalArgumentException("Reference cannot be null!");
		}
		
		// get the reference target class
		EntryReference<Entry,Entry> obfReference = m_deobfuscator.obfuscateReference(deobfReference);
		ClassEntry obfClassEntry = obfReference.getLocationClassEntry().getOutermostClassEntry();
		if (!m_deobfuscator.isObfuscatedIdentifier(obfClassEntry)) {
			throw new IllegalArgumentException("Obfuscated class " + obfClassEntry + " was not found in the jar!");
		}
		if (m_currentObfClass == null || !m_currentObfClass.equals(obfClassEntry)) {
			// deobfuscate the class, then navigate to the reference
			m_currentObfClass = obfClassEntry;
			deobfuscate(m_currentObfClass, obfReference);
		} else {
			showReference(obfReference);
		}
	}
	
	private void showReference(EntryReference<Entry,Entry> obfReference) {
		EntryReference<Entry,Entry> deobfReference = m_deobfuscator.deobfuscateReference(obfReference);
		Collection<Token> tokens = m_index.getReferenceTokens(deobfReference);
		if (tokens.isEmpty()) {
			// DEBUG
			System.err.println(String.format("WARNING: no tokens found for %s in %s", deobfReference, m_currentObfClass));
		} else {
			m_gui.showTokens(tokens);
		}
	}
	
	public void savePreviousReference(EntryReference<Entry,Entry> deobfReference) {
		m_referenceStack.push(m_deobfuscator.obfuscateReference(deobfReference));
	}
	
	public void openPreviousReference() {
		if (hasPreviousLocation()) {
			openReference(m_deobfuscator.deobfuscateReference(m_referenceStack.pop()));
		}
	}
	
	public boolean hasPreviousLocation() {
		return !m_referenceStack.isEmpty();
	}
	
	private void refreshClasses() {
		List<ClassEntry> obfClasses = Lists.newArrayList();
		List<ClassEntry> deobfClasses = Lists.newArrayList();
		m_deobfuscator.getSeparatedClasses(obfClasses, deobfClasses);
		m_gui.setObfClasses(obfClasses);
		m_gui.setDeobfClasses(deobfClasses);
	}
	
	private void refreshCurrentClass() {
		refreshCurrentClass(null);
	}
	
	private void refreshCurrentClass(EntryReference<Entry,Entry> obfReference) {
		if (m_currentObfClass != null) {
			deobfuscate(m_currentObfClass, obfReference);
		}
	}
	
	private void deobfuscate(final ClassEntry classEntry, final EntryReference<Entry,Entry> obfReference) {
		
		m_gui.setSource("(deobfuscating...)");
		
		// run the deobfuscator in a separate thread so we don't block the GUI event queue
		new Thread() {
			@Override
			public void run() {
				// decompile,deobfuscate the bytecode
				CompilationUnit sourceTree = m_deobfuscator.getSourceTree(classEntry.getClassName());
				if (sourceTree == null) {
					// decompilation of this class is not supported
					m_gui.setSource("Unable to find class: " + classEntry);
					return;
				}
				String source = m_deobfuscator.getSource(sourceTree);
				m_index = m_deobfuscator.getSourceIndex(sourceTree, source);
				m_gui.setSource(m_index.getSource());
				if (obfReference != null) {
					showReference(obfReference);
				}
				
				// set the highlighted tokens
				List<Token> obfuscatedTokens = Lists.newArrayList();
				List<Token> deobfuscatedTokens = Lists.newArrayList();
				List<Token> otherTokens = Lists.newArrayList();
				for (Token token : m_index.referenceTokens()) {
					EntryReference<Entry,Entry> reference = m_index.getDeobfReference(token);
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
				m_gui.setHighlightedTokens(obfuscatedTokens, deobfuscatedTokens, otherTokens);
			}
		}.start();
	}
}
