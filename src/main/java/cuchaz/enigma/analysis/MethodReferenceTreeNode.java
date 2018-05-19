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
import cuchaz.enigma.mapping.*;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import java.util.Set;

public class MethodReferenceTreeNode extends DefaultMutableTreeNode
	implements ReferenceTreeNode<MethodEntry, MethodDefEntry> {

	private Translator deobfuscatingTranslator;
	private MethodEntry entry;
	private EntryReference<MethodEntry, MethodDefEntry> reference;
	private Access access;

	public MethodReferenceTreeNode(Translator deobfuscatingTranslator, MethodEntry entry) {
		this.deobfuscatingTranslator = deobfuscatingTranslator;
		this.entry = entry;
		this.reference = null;
	}

	public MethodReferenceTreeNode(Translator deobfuscatingTranslator,
								   EntryReference<MethodEntry, MethodDefEntry> reference, Access access) {
		this.deobfuscatingTranslator = deobfuscatingTranslator;
		this.entry = reference.entry;
		this.reference = reference;
		this.access = access;
	}

	@Override
	public MethodEntry getEntry() {
		return this.entry;
	}

	@Override
	public EntryReference<MethodEntry, MethodDefEntry> getReference() {
		return this.reference;
	}

	@Override
	public String toString() {
		if (this.reference != null) {
			return String.format("%s (%s)", this.deobfuscatingTranslator.getTranslatedMethodDef(this.reference.context),
				this.access);
		}
		return this.deobfuscatingTranslator.getTranslatedMethod(this.entry).getName();
	}

	public void load(JarIndex index, boolean recurse) {
		// get all the child nodes
		for (EntryReference<MethodEntry, MethodDefEntry> reference : index.getMethodReferences(this.entry)) {
			add(new MethodReferenceTreeNode(this.deobfuscatingTranslator, reference, index.getAccess(this.entry)));
		}

		if (recurse && this.children != null) {
			for (Object child : this.children) {
				if (child instanceof MethodReferenceTreeNode) {
					MethodReferenceTreeNode node = (MethodReferenceTreeNode) child;

					// don't recurse into ancestor
					Set<Entry> ancestors = Sets.newHashSet();
					TreeNode n = node;
					while (n.getParent() != null) {
						n = n.getParent();
						if (n instanceof MethodReferenceTreeNode) {
							ancestors.add(((MethodReferenceTreeNode) n).getEntry());
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
