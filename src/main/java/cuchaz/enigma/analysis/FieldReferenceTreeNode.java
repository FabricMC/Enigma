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

import javax.swing.tree.DefaultMutableTreeNode;

import cuchaz.enigma.mapping.BehaviorEntry;
import cuchaz.enigma.mapping.FieldEntry;
import cuchaz.enigma.mapping.Translator;

public class FieldReferenceTreeNode extends DefaultMutableTreeNode implements ReferenceTreeNode<FieldEntry, BehaviorEntry> {

    private Translator deobfuscatingTranslator;
    private FieldEntry entry;
    private EntryReference<FieldEntry, BehaviorEntry> reference;
    private Access access;

    public FieldReferenceTreeNode(Translator deobfuscatingTranslator, FieldEntry entry) {
        this.deobfuscatingTranslator = deobfuscatingTranslator;
        this.entry = entry;
        this.reference = null;
    }

    private FieldReferenceTreeNode(Translator deobfuscatingTranslator, EntryReference<FieldEntry, BehaviorEntry> reference, Access access) {
        this.deobfuscatingTranslator = deobfuscatingTranslator;
        this.entry = reference.entry;
        this.reference = reference;
        this.access = access;
    }

    @Override
    public FieldEntry getEntry() {
        return this.entry;
    }

    @Override
    public EntryReference<FieldEntry, BehaviorEntry> getReference() {
        return this.reference;
    }

    @Override
    public String toString() {
        if (this.reference != null) {
            return String.format("%s (%s)", this.deobfuscatingTranslator.translateEntry(this.reference.context), this.access);
        }
        return this.deobfuscatingTranslator.translateEntry(this.entry).toString();
    }

    public void load(JarIndex index, boolean recurse) {
        // get all the child nodes
        if (this.reference == null) {
            for (EntryReference<FieldEntry, BehaviorEntry> reference : index.getFieldReferences(this.entry)) {
                add(new FieldReferenceTreeNode(this.deobfuscatingTranslator, reference, index.getAccess(this.entry)));
            }
        } else {
            for (EntryReference<BehaviorEntry, BehaviorEntry> reference : index.getBehaviorReferences(this.reference.context)) {
                add(new BehaviorReferenceTreeNode(this.deobfuscatingTranslator, reference, index.getAccess(this.reference.context)));
            }
        }

        if (recurse && children != null) {
            for (Object node : children) {
                if (node instanceof BehaviorReferenceTreeNode) {
                    ((BehaviorReferenceTreeNode) node).load(index, true);
                } else if (node instanceof FieldReferenceTreeNode) {
                    ((FieldReferenceTreeNode) node).load(index, true);
                }
            }
        }
    }
}
