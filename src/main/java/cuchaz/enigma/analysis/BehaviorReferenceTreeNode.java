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

import java.util.Set;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;

import cuchaz.enigma.mapping.BehaviorEntry;
import cuchaz.enigma.mapping.Entry;
import cuchaz.enigma.mapping.Translator;

public class BehaviorReferenceTreeNode extends DefaultMutableTreeNode implements ReferenceTreeNode<BehaviorEntry, BehaviorEntry> {

    private Translator m_deobfuscatingTranslator;
    private BehaviorEntry m_entry;
    private EntryReference<BehaviorEntry, BehaviorEntry> m_reference;
    private Access m_access;

    public BehaviorReferenceTreeNode(Translator deobfuscatingTranslator, BehaviorEntry entry) {
        this.m_deobfuscatingTranslator = deobfuscatingTranslator;
        this.m_entry = entry;
        this.m_reference = null;
    }

    public BehaviorReferenceTreeNode(Translator deobfuscatingTranslator, EntryReference<BehaviorEntry, BehaviorEntry> reference, Access access) {
        this.m_deobfuscatingTranslator = deobfuscatingTranslator;
        this.m_entry = reference.entry;
        this.m_reference = reference;
        this.m_access = access;
    }

    @Override
    public BehaviorEntry getEntry() {
        return this.m_entry;
    }

    @Override
    public EntryReference<BehaviorEntry, BehaviorEntry> getReference() {
        return this.m_reference;
    }

    @Override
    public String toString() {
        if (this.m_reference != null) {
            return String.format("%s (%s)", this.m_deobfuscatingTranslator.translateEntry(this.m_reference.context), this.m_access);
        }
        return this.m_deobfuscatingTranslator.translateEntry(this.m_entry).toString();
    }

    public void load(JarIndex index, boolean recurse) {
        // get all the child nodes
        for (EntryReference<BehaviorEntry, BehaviorEntry> reference : index.getBehaviorReferences(this.m_entry)) {
            add(new BehaviorReferenceTreeNode(this.m_deobfuscatingTranslator, reference, index.getAccess(this.m_entry)));
        }

        if (recurse && this.children != null) {
            for (Object child : this.children) {
                if (child instanceof BehaviorReferenceTreeNode) {
                    BehaviorReferenceTreeNode node = (BehaviorReferenceTreeNode) child;

                    // don't recurse into ancestor
                    Set<Entry> ancestors = Sets.newHashSet();
                    TreeNode n = node;
                    while (n.getParent() != null) {
                        n = n.getParent();
                        if (n instanceof BehaviorReferenceTreeNode) {
                            ancestors.add(((BehaviorReferenceTreeNode) n).getEntry());
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
