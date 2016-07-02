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

import java.io.Serializable;

public class FieldMapping implements Serializable, Comparable<FieldMapping>, MemberMapping<FieldEntry> {

    private static final long serialVersionUID = 8610742471440861315L;

    private String obfName;
    private String deobfName;
    private Type obfType;

    public FieldMapping(String obfName, Type obfType, String deobfName) {
        this.obfName = obfName;
        this.deobfName = NameValidator.validateFieldName(deobfName);
        this.obfType = obfType;
    }

    public FieldMapping(FieldMapping other, ClassNameReplacer obfClassNameReplacer) {
        this.obfName = other.obfName;
        this.deobfName = other.deobfName;
        this.obfType = new Type(other.obfType, obfClassNameReplacer);
    }

    @Override
    public String getObfName() {
        return this.obfName;
    }

    public void setObfName(String val) {
        this.obfName = NameValidator.validateFieldName(val);
    }

    public String getDeobfName() {
        return this.deobfName;
    }

    public void setDeobfName(String val) {
        this.deobfName = NameValidator.validateFieldName(val);
    }

    public Type getObfType() {
        return this.obfType;
    }

    public void setObfType(Type val) {
        this.obfType = val;
    }

    @Override
    public int compareTo(FieldMapping other) {
        return (this.obfName + this.obfType).compareTo(other.obfName + other.obfType);
    }

    public boolean renameObfClass(final String oldObfClassName, final String newObfClassName) {

        // rename obf classes in the type
        Type newType = new Type(this.obfType, className -> {
            if (className.equals(oldObfClassName)) {
                return newObfClassName;
            }
            return null;
        });

        if (!newType.equals(this.obfType)) {
            this.obfType = newType;
            return true;
        }
        return false;
    }

    @Override
    public FieldEntry getObfEntry(ClassEntry classEntry) {
        return new FieldEntry(classEntry, this.obfName, new Type(this.obfType));
    }
}
