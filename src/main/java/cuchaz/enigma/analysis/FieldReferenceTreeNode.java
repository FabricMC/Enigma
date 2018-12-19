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

import cuchaz.enigma.translation.representation.AccessFlags;
import cuchaz.enigma.translation.representation.entry.FieldEntry;
import cuchaz.enigma.translation.representation.entry.MethodDefEntry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;

import javax.swing.tree.DefaultMutableTreeNode;

public class FieldReferenceTreeNode extends DefaultMutableTreeNode implements ReferenceTreeNode<FieldEntry, MethodDefEntry> {

	private FieldEntry entry;
	private EntryReference<FieldEntry, MethodDefEntry> reference;
	private AccessFlags access;

	public FieldReferenceTreeNode(FieldEntry entry) {
		this.entry = entry;
		this.reference = null;
	}

	private FieldReferenceTreeNode(EntryReference<FieldEntry, MethodDefEntry> reference, AccessFlags access) {
		this.entry = reference.entry;
		this.reference = reference;
		this.access = access;
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
			return String.format("%s (%s)", this.reference.context, this.access);
		}
		return entry.toString();
	}

	public void load(JarIndex index, boolean recurse) {
		// get all the child nodes
		if (this.reference == null) {
			for (EntryReference<FieldEntry, MethodDefEntry> reference : index.getFieldReferences(this.entry)) {
				add(new FieldReferenceTreeNode(reference, index.getAccessFlags(this.entry)));
			}
		} else {
			for (EntryReference<MethodEntry, MethodDefEntry> reference : index.getMethodsReferencing(this.reference.context, false)) {
				add(new MethodReferenceTreeNode(reference, index.getAccessFlags(this.reference.context)));
			}
		}

		if (recurse && children != null) {
			for (Object node : children) {
				if (node instanceof MethodReferenceTreeNode) {
					((MethodReferenceTreeNode) node).load(index, true);
				} else if (node instanceof FieldReferenceTreeNode) {
					((FieldReferenceTreeNode) node).load(index, true);
				}
			}
		}
	}
}
