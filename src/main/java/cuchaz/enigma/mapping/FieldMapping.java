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

public class FieldMapping implements Comparable<FieldMapping>, MemberMapping<FieldEntry> {

    private String obfName;
    private String deobfName;
    private Type obfType;

    public FieldMapping(String obfName, Type obfType, String deobfName) {
        this.obfName = obfName;
        this.deobfName = NameValidator.validateFieldName(deobfName);
        this.obfType = obfType;
    }

    @Override
    public String getObfName() {
        return this.obfName;
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

    @Override
    public int compareTo(FieldMapping other) {
        return (this.obfName + this.obfType).compareTo(other.obfName + other.obfType);
    }
}
