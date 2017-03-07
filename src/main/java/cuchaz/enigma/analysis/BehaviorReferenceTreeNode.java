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
import cuchaz.enigma.mapping.BehaviorEntry;
import cuchaz.enigma.mapping.Entry;
import cuchaz.enigma.mapping.Translator;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import java.util.Set;

public class BehaviorReferenceTreeNode extends DefaultMutableTreeNode
        implements ReferenceTreeNode<BehaviorEntry, BehaviorEntry>
{

    private Translator                                   deobfuscatingTranslator;
    private BehaviorEntry                                entry;
    private EntryReference<BehaviorEntry, BehaviorEntry> reference;
    private Access                                       access;

    public BehaviorReferenceTreeNode(Translator deobfuscatingTranslator, BehaviorEntry entry)
    {
        this.deobfuscatingTranslator = deobfuscatingTranslator;
        this.entry = entry;
        this.reference = null;
    }

    public BehaviorReferenceTreeNode(Translator deobfuscatingTranslator,
            EntryReference<BehaviorEntry, BehaviorEntry> reference, Access access)
    {
        this.deobfuscatingTranslator = deobfuscatingTranslator;
        this.entry = reference.entry;
        this.reference = reference;
        this.access = access;
    }

    @Override public BehaviorEntry getEntry()
    {
        return this.entry;
    }

    @Override public EntryReference<BehaviorEntry, BehaviorEntry> getReference()
    {
        return this.reference;
    }

    @Override public String toString()
    {
        if (this.reference != null)
        {
            return String.format("%s (%s)", this.deobfuscatingTranslator.translateEntry(this.reference.context),
                    this.access);
        }
        return this.deobfuscatingTranslator.translateEntry(this.entry).toString();
    }

    public void load(JarIndex index, boolean recurse)
    {
        // get all the child nodes
        for (EntryReference<BehaviorEntry, BehaviorEntry> reference : index.getBehaviorReferences(this.entry))
        {
            add(new BehaviorReferenceTreeNode(this.deobfuscatingTranslator, reference, index.getAccess(this.entry)));
        }

        if (recurse && this.children != null)
        {
            for (Object child : this.children)
            {
                if (child instanceof BehaviorReferenceTreeNode)
                {
                    BehaviorReferenceTreeNode node = (BehaviorReferenceTreeNode) child;

                    // don't recurse into ancestor
                    Set<Entry> ancestors = Sets.newHashSet();
                    TreeNode n = node;
                    while (n.getParent() != null)
                    {
                        n = n.getParent();
                        if (n instanceof BehaviorReferenceTreeNode)
                        {
                            ancestors.add(((BehaviorReferenceTreeNode) n).getEntry());
                        }
                    }
                    if (ancestors.contains(node.getEntry()))
                    {
                        continue;
                    }

                    node.load(index, true);
                }
            }
        }
    }
}
