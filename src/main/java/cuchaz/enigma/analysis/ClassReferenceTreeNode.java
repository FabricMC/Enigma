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

package cuchaz.enigma.analysis;

import com.google.common.collect.Sets;
import cuchaz.enigma.translation.Translator;
import cuchaz.enigma.translation.representation.AccessFlags;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.Entry;
import cuchaz.enigma.translation.representation.entry.MethodDefEntry;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import java.util.Set;

public class ClassReferenceTreeNode extends DefaultMutableTreeNode
	implements ReferenceTreeNode<ClassEntry, MethodDefEntry> {

	private Translator deobfuscatingTranslator;
	private ClassEntry entry;
	private EntryReference<ClassEntry, MethodDefEntry> reference;
	private AccessFlags access;

	public ClassReferenceTreeNode(Translator deobfuscatingTranslator, ClassEntry entry) {
		this.deobfuscatingTranslator = deobfuscatingTranslator;
		this.entry = entry;
		this.reference = null;
	}

	public ClassReferenceTreeNode(Translator deobfuscatingTranslator,
                                  EntryReference<ClassEntry, MethodDefEntry> reference, AccessFlags access) {
		this.deobfuscatingTranslator = deobfuscatingTranslator;
		this.entry = reference.entry;
		this.reference = reference;
		this.access = access;
	}

	@Override
	public ClassEntry getEntry() {
		return this.entry;
	}

	@Override
	public EntryReference<ClassEntry, MethodDefEntry> getReference() {
		return this.reference;
	}

	@Override
	public String toString() {
		if (this.reference != null) {
			return String.format("%s (%s)", this.deobfuscatingTranslator.translate(this.reference.context),
				this.access);
		}
		return this.deobfuscatingTranslator.translate(this.entry).getName();
	}

	public void load(JarIndex index, boolean recurse) {
		// get all the child nodes
		for (EntryReference<ClassEntry, MethodDefEntry> reference : index.getMethodsReferencing(this.entry)) {
			add(new ClassReferenceTreeNode(this.deobfuscatingTranslator, reference, index.getAccessFlags(this.entry)));
		}

		if (recurse && this.children != null) {
			for (Object child : this.children) {
				if (child instanceof ClassReferenceTreeNode) {
					ClassReferenceTreeNode node = (ClassReferenceTreeNode) child;

					// don't recurse into ancestor
					Set<Entry> ancestors = Sets.newHashSet();
					TreeNode n = node;
					while (n.getParent() != null) {
						n = n.getParent();
						if (n instanceof ClassReferenceTreeNode) {
							ancestors.add(((ClassReferenceTreeNode) n).getEntry());
						}
					}
					if (ancestors.contains(node.getEntry())) {
						continue;
					}

					node.load(index, true);
				}
			}
		}
	}
}
