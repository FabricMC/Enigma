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
import cuchaz.enigma.mapping.MethodEntry;
import cuchaz.enigma.mapping.Translator;

public class MethodImplementationsTreeNode extends DefaultMutableTreeNode {

    private static final long serialVersionUID = 3781080657461899915L;

    private Translator deobfuscatingTranslator;
    private MethodEntry entry;

    public MethodImplementationsTreeNode(Translator deobfuscatingTranslator, MethodEntry entry) {
        if (entry == null) {
            throw new IllegalArgumentException("Entry cannot be null!");
        }

        this.deobfuscatingTranslator = deobfuscatingTranslator;
        this.entry = entry;
    }

    public MethodEntry getMethodEntry() {
        return this.entry;
    }

    public String getDeobfClassName() {
        return this.deobfuscatingTranslator.translateClass(this.entry.getClassName());
    }

    public String getDeobfMethodName() {
        return this.deobfuscatingTranslator.translate(this.entry);
    }

    @Override
    public String toString() {
        String className = getDeobfClassName();
        if (className == null) {
            className = this.entry.getClassName();
        }

        String methodName = getDeobfMethodName();
        if (methodName == null) {
            methodName = this.entry.getName();
        }
        return className + "." + methodName + "()";
    }

    public void load(JarIndex index) {

        // get all method implementations
        List<MethodImplementationsTreeNode> nodes = Lists.newArrayList();
        for (String implementingClassName : index.getImplementingClasses(this.entry.getClassName())) {
            MethodEntry methodEntry = new MethodEntry(new ClassEntry(implementingClassName), this.entry.getName(), this.entry.getSignature()
            );
            if (index.containsObfBehavior(methodEntry)) {
                nodes.add(new MethodImplementationsTreeNode(this.deobfuscatingTranslator, methodEntry));
            }
        }

        // add them to this node
        nodes.forEach(this::add);
    }

    public static MethodImplementationsTreeNode findNode(MethodImplementationsTreeNode node, MethodEntry entry) {
        // is this the node?
        if (node.getMethodEntry().equals(entry)) {
            return node;
        }

        // recurse
        for (int i = 0; i < node.getChildCount(); i++) {
            MethodImplementationsTreeNode foundNode = findNode((MethodImplementationsTreeNode) node.getChildAt(i), entry);
            if (foundNode != null) {
                return foundNode;
            }
        }
        return null;
    }
}
