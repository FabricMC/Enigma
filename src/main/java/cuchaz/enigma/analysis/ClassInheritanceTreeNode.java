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

import com.google.common.collect.Lists;

import java.util.List;

import javax.swing.tree.DefaultMutableTreeNode;

import cuchaz.enigma.mapping.ClassEntry;
import cuchaz.enigma.mapping.Translator;

public class ClassInheritanceTreeNode extends DefaultMutableTreeNode {

    private Translator deobfuscatingTranslator;
    private String obfClassName;

    public ClassInheritanceTreeNode(Translator deobfuscatingTranslator, String obfClassName) {
        this.deobfuscatingTranslator = deobfuscatingTranslator;
        this.obfClassName = obfClassName;
    }

    public String getObfClassName() {
        return this.obfClassName;
    }

    public String getDeobfClassName() {
        return this.deobfuscatingTranslator.translateClass(this.obfClassName);
    }

    @Override
    public String toString() {
        String deobfClassName = getDeobfClassName();
        if (deobfClassName != null) {
            return deobfClassName;
        }
        return this.obfClassName;
    }

    public void load(TranslationIndex ancestries, boolean recurse) {
        // get all the child nodes
        List<ClassInheritanceTreeNode> nodes = Lists.newArrayList();
        for (ClassEntry subclassEntry : ancestries.getSubclass(new ClassEntry(this.obfClassName))) {
            nodes.add(new ClassInheritanceTreeNode(this.deobfuscatingTranslator, subclassEntry.getName()));
        }

        // add them to this node
        nodes.forEach(this::add);

        if (recurse) {
            for (ClassInheritanceTreeNode node : nodes) {
                node.load(ancestries, true);
            }
        }
    }

    public static ClassInheritanceTreeNode findNode(ClassInheritanceTreeNode node, ClassEntry entry) {
        // is this the node?
        if (node.getObfClassName().equals(entry.getName())) {
            return node;
        }

        // recurse
        for (int i = 0; i < node.getChildCount(); i++) {
            ClassInheritanceTreeNode foundNode = findNode((ClassInheritanceTreeNode) node.getChildAt(i), entry);
            if (foundNode != null) {
                return foundNode;
            }
        }
        return null;
    }
}
