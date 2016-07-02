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
package cuchaz.enigma.convert;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import java.util.Collection;

import cuchaz.enigma.mapping.ClassEntry;


public class ClassForest {

    private ClassIdentifier identifier;
    private Multimap<ClassIdentity, ClassEntry> forest;

    public ClassForest(ClassIdentifier identifier) {
        this.identifier = identifier;
        this.forest = HashMultimap.create();
    }

    public void add(ClassEntry entry) {
        try {
            this.forest.put(this.identifier.identify(entry), entry);
        } catch (ClassNotFoundException ex) {
            throw new Error("Unable to find class " + entry.getName());
        }
    }

    public Collection<ClassIdentity> identities() {
        return this.forest.keySet();
    }

    public Collection<ClassEntry> classes() {
        return this.forest.values();
    }

    public Collection<ClassEntry> getClasses(ClassIdentity identity) {
        return this.forest.get(identity);
    }

    public boolean containsIdentity(ClassIdentity identity) {
        return this.forest.containsKey(identity);
    }
}
