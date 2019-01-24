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

import cuchaz.enigma.analysis.index.JarIndex;
import cuchaz.enigma.analysis.index.ReferenceIndex;
import cuchaz.enigma.translation.Translator;
import cuchaz.enigma.translation.representation.entry.FieldEntry;
import cuchaz.enigma.translation.representation.entry.MethodDefEntry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;

import javax.swing.tree.DefaultMutableTreeNode;

public class FieldReferenceTreeNode extends DefaultMutableTreeNode implements ReferenceTreeNode<FieldEntry, MethodDefEntry> {

	private final Translator translator;
	private FieldEntry entry;
	private EntryReference<FieldEntry, MethodDefEntry> reference;

	public FieldReferenceTreeNode(Translator translator, FieldEntry entry) {
		this.translator = translator;
		this.entry = entry;
		this.reference = null;
	}

	private FieldReferenceTreeNode(Translator translator, EntryReference<FieldEntry, MethodDefEntry> reference) {
		this.translator = translator;
		this.entry = reference.entry;
		this.reference = reference;
	}

	@Override
	public FieldEntry getEntry() {
		return this.entry;
	}

	@Override
	public EntryReference<FieldEntry, MethodDefEntry> getReference() {
		return this.reference;
	}

	@Override
	public String toString() {
		if (this.reference != null) {
			return String.format("%s", translator.translate(this.reference.context));
		}
		return translator.translate(entry).toString();
	}

	public void load(JarIndex index, boolean recurse) {
		ReferenceIndex referenceIndex = index.getReferenceIndex();

		// get all the child nodes
		if (this.reference == null) {
			for (EntryReference<FieldEntry, MethodDefEntry> reference : referenceIndex.getReferencesToField(this.entry)) {
				add(new FieldReferenceTreeNode(translator, reference));
			}
		} else {
			for (EntryReference<MethodEntry, MethodDefEntry> reference : referenceIndex.getReferencesToMethod(this.reference.context)) {
				add(new MethodReferenceTreeNode(translator, reference));
			}
		}

		if (recurse && children != null) {
			for (Object node : children) {
				if (node instanceof MethodReferenceTreeNode) {
					((MethodReferenceTreeNode) node).load(index, true, false);
				} else if (node instanceof FieldReferenceTreeNode) {
					((FieldReferenceTreeNode) node).load(index, true);
				}
			}
		}
	}
}
