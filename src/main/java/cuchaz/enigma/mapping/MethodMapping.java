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

import com.google.common.collect.Maps;

import java.io.Serializable;
import java.util.Map;
import java.util.Map.Entry;

public class MethodMapping implements Serializable, Comparable<MethodMapping>, MemberMapping<BehaviorEntry> {

    private static final long serialVersionUID = -4409570216084263978L;

    private String m_obfName;
    private String m_deobfName;
    private Signature m_obfSignature;
    private Map<Integer, ArgumentMapping> m_arguments;

    public MethodMapping(String obfName, Signature obfSignature) {
        this(obfName, obfSignature, null);
    }

    public MethodMapping(String obfName, Signature obfSignature, String deobfName) {
        if (obfName == null) {
            throw new IllegalArgumentException("obf name cannot be null!");
        }
        if (obfSignature == null) {
            throw new IllegalArgumentException("obf signature cannot be null!");
        }
        this.m_obfName = obfName;
        this.m_deobfName = NameValidator.validateMethodName(deobfName);
        this.m_obfSignature = obfSignature;
        this.m_arguments = Maps.newTreeMap();
    }

    public MethodMapping(MethodMapping other, ClassNameReplacer obfClassNameReplacer) {
        this.m_obfName = other.m_obfName;
        this.m_deobfName = other.m_deobfName;
        this.m_obfSignature = new Signature(other.m_obfSignature, obfClassNameReplacer);
        this.m_arguments = Maps.newTreeMap();
        for (Entry<Integer, ArgumentMapping> entry : other.m_arguments.entrySet()) {
            this.m_arguments.put(entry.getKey(), new ArgumentMapping(entry.getValue()));
        }
    }

    @Override
    public String getObfName() {
        return this.m_obfName;
    }

    public void setObfName(String val) {
        this.m_obfName = NameValidator.validateMethodName(val);
    }

    public String getDeobfName() {
        return this.m_deobfName;
    }

    public void setDeobfName(String val) {
        this.m_deobfName = NameValidator.validateMethodName(val);
    }

    public Signature getObfSignature() {
        return this.m_obfSignature;
    }

    public void setObfSignature(Signature val) {
        this.m_obfSignature = val;
    }

    public Iterable<ArgumentMapping> arguments() {
        return this.m_arguments.values();
    }

    public boolean isConstructor() {
        return this.m_obfName.startsWith("<");
    }

    public void addArgumentMapping(ArgumentMapping argumentMapping) {
        boolean wasAdded = this.m_arguments.put(argumentMapping.getIndex(), argumentMapping) == null;
        assert (wasAdded);
    }

    public String getObfArgumentName(int index) {
        ArgumentMapping argumentMapping = this.m_arguments.get(index);
        if (argumentMapping != null) {
            return argumentMapping.getName();
        }

        return null;
    }

    public String getDeobfArgumentName(int index) {
        ArgumentMapping argumentMapping = this.m_arguments.get(index);
        if (argumentMapping != null) {
            return argumentMapping.getName();
        }

        return null;
    }

    public void setArgumentName(int index, String name) {
        ArgumentMapping argumentMapping = this.m_arguments.get(index);
        if (argumentMapping == null) {
            argumentMapping = new ArgumentMapping(index, name);
            boolean wasAdded = this.m_arguments.put(index, argumentMapping) == null;
            assert (wasAdded);
        } else {
            argumentMapping.setName(name);
        }
    }

    public void removeArgumentName(int index) {
        boolean wasRemoved = this.m_arguments.remove(index) != null;
        assert (wasRemoved);
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("\t");
        buf.append(m_obfName);
        buf.append(" <-> ");
        buf.append(m_deobfName);
        buf.append("\n");
        buf.append("\t");
        buf.append(m_obfSignature);
        buf.append("\n");
        buf.append("\tArguments:\n");
        for (ArgumentMapping argumentMapping : this.m_arguments.values()) {
            buf.append("\t\t");
            buf.append(argumentMapping.getIndex());
            buf.append(" -> ");
            buf.append(argumentMapping.getName());
            buf.append("\n");
        }
        return buf.toString();
    }

    @Override
    public int compareTo(MethodMapping other) {
        return (this.m_obfName + this.m_obfSignature).compareTo(other.m_obfName + other.m_obfSignature);
    }

    public boolean renameObfClass(final String oldObfClassName, final String newObfClassName) {

        // rename obf classes in the signature
        Signature newSignature = new Signature(this.m_obfSignature, className -> {
            if (className.equals(oldObfClassName)) {
                return newObfClassName;
            }
            return null;
        });

        if (!newSignature.equals(this.m_obfSignature)) {
            this.m_obfSignature = newSignature;
            return true;
        }
        return false;
    }

    public boolean containsArgument(String name) {
        for (ArgumentMapping argumentMapping : this.m_arguments.values()) {
            if (argumentMapping.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public BehaviorEntry getObfEntry(ClassEntry classEntry) {
        if (isConstructor()) {
            return new ConstructorEntry(classEntry, this.m_obfSignature);
        } else {
            return new MethodEntry(classEntry, this.m_obfName, this.m_obfSignature);
        }
    }
}
