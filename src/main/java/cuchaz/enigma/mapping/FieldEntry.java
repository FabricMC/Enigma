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
package cuchaz.enigma.mapping;

import cuchaz.enigma.utils.Utils;

public class FieldEntry implements Entry {

    private ClassEntry classEntry;
    private String name;
    private Type type;

    // NOTE: this argument order is important for the MethodReader/MethodWriter
    public FieldEntry(ClassEntry classEntry, String name, Type type) {
        if (classEntry == null) {
            throw new IllegalArgumentException("Class cannot be null!");
        }
        if (name == null) {
            throw new IllegalArgumentException("Field name cannot be null!");
        }
        if (type == null) {
            throw new IllegalArgumentException("Field type cannot be null!");
        }

        this.classEntry = classEntry;
        this.name = name;
        this.type = type;
    }

    public FieldEntry(FieldEntry other, ClassEntry newClassEntry) {
        this.classEntry = newClassEntry;
        this.name = other.name;
        this.type = other.type;
    }

    @Override
    public ClassEntry getClassEntry() {
        return this.classEntry;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getClassName() {
        return this.classEntry.getName();
    }

    public Type getType() {
        return this.type;
    }

    @Override
    public FieldEntry cloneToNewClass(ClassEntry classEntry) {
        return new FieldEntry(this, classEntry);
    }

    @Override
    public int hashCode() {
        return Utils.combineHashesOrdered(this.classEntry, this.name, this.type);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof FieldEntry && equals((FieldEntry) other);
    }

    public boolean equals(FieldEntry other) {
        return this.classEntry.equals(other.classEntry) && this.name.equals(other.name) && this.type.equals(other.type);
    }

    @Override
    public String toString() {
        return this.classEntry.getName() + "." + this.name + ":" + this.type;
    }
}
