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

import com.google.common.collect.Lists;

import java.util.List;

import cuchaz.enigma.utils.Utils;

public class Signature {

    private List<Type> argumentTypes;
    private Type returnType;

    public Signature(String signature) {
        try {
            this.argumentTypes = Lists.newArrayList();
            int i = 0;
            while (i < signature.length()) {
                char c = signature.charAt(i);
                if (c == '(') {
                    assert (this.argumentTypes.isEmpty());
                    assert (this.returnType == null);
                    i++;
                } else if (c == ')') {
                    i++;
                    break;
                } else {
                    String type = Type.parseFirst(signature.substring(i));
                    this.argumentTypes.add(new Type(type));
                    i += type.length();
                }
            }
            this.returnType = new Type(Type.parseFirst(signature.substring(i)));
        } catch (Exception ex) {
            throw new IllegalArgumentException("Unable to parse signature: " + signature, ex);
        }
    }

    public Signature(Signature other, ClassNameReplacer replacer) {
        this.argumentTypes = Lists.newArrayList(other.argumentTypes);
        for (int i = 0; i < this.argumentTypes.size(); i++) {
            this.argumentTypes.set(i, new Type(this.argumentTypes.get(i), replacer));
        }
        this.returnType = new Type(other.returnType, replacer);
    }

    public List<Type> getArgumentTypes() {
        return this.argumentTypes;
    }

    public Type getReturnType() {
        return this.returnType;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("(");
        for (Type type : this.argumentTypes) {
            buf.append(type.toString());
        }
        buf.append(")");
        buf.append(this.returnType.toString());
        return buf.toString();
    }

    public Iterable<Type> types() {
        List<Type> types = Lists.newArrayList();
        types.addAll(this.argumentTypes);
        types.add(this.returnType);
        return types;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Signature && equals((Signature) other);
    }

    public boolean equals(Signature other) {
        return this.argumentTypes.equals(other.argumentTypes) && this.returnType.equals(other.returnType);
    }

    @Override
    public int hashCode() {
        return Utils.combineHashesOrdered(this.argumentTypes.hashCode(), this.returnType.hashCode());
    }

    public boolean hasClass(ClassEntry classEntry) {
        for (Type type : types()) {
            if (type.hasClass() && type.getClassEntry().equals(classEntry)) {
                return true;
            }
        }
        return false;
    }
}
