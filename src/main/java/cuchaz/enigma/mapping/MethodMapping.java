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

import java.util.Map;

import cuchaz.enigma.throwables.MappingConflict;

public class MethodMapping implements Comparable<MethodMapping>, MemberMapping<BehaviorEntry> {

    private String obfName;
    private String deobfName;
    private Signature obfSignature;
    private Map<Integer, ArgumentMapping> arguments;
    private Mappings.EntryModifier modifier;

    public MethodMapping(String obfName, Signature obfSignature) {
        this(obfName, obfSignature, null,Mappings.EntryModifier.UNCHANGED);
    }

    public MethodMapping(String obfName, Signature obfSignature, String deobfName) {
        this(obfName, obfSignature, deobfName, Mappings.EntryModifier.UNCHANGED);
    }

    public MethodMapping(String obfName, Signature obfSignature, String deobfName, Mappings.EntryModifier modifier) {
        if (obfName == null) {
            throw new IllegalArgumentException("obf name cannot be null!");
        }
        if (obfSignature == null) {
            throw new IllegalArgumentException("obf signature cannot be null!");
        }
        this.obfName = obfName;
        this.deobfName = NameValidator.validateMethodName(deobfName);
        this.obfSignature = obfSignature;
        this.arguments = Maps.newTreeMap();
        this.modifier = modifier;
    }
    
    public MethodMapping(MethodMapping other, ClassNameReplacer obfClassNameReplacer) {
        this.obfName = other.obfName;
        this.deobfName = other.deobfName;
        this.modifier = other.modifier;
        this.obfSignature = new Signature(other.obfSignature, obfClassNameReplacer);
        this.arguments = Maps.newTreeMap();
        for (Map.Entry<Integer,ArgumentMapping> entry : other.arguments.entrySet()) {
            this.arguments.put(entry.getKey(), new ArgumentMapping(entry.getValue()));
        }
    }

    @Override
    public String getObfName() {
        return this.obfName;
    }

    public String getDeobfName() {
        return this.deobfName;
    }

    public void setDeobfName(String val) {
        this.deobfName = NameValidator.validateMethodName(val);
    }

    public Signature getObfSignature() {
        return this.obfSignature;
    }

    public void setObfName(String name) {
        this.obfName = NameValidator.validateMethodName(name);
    }

    public void setObfSignature(Signature val) {
        this.obfSignature = val;
    }

    public Iterable<ArgumentMapping> arguments() {
        return this.arguments.values();
    }

    public void addArgumentMapping(ArgumentMapping argumentMapping) throws MappingConflict {
        if (this.arguments.containsKey(argumentMapping.getIndex())) {
            throw new MappingConflict("argument", argumentMapping.getName(), this.arguments.get(argumentMapping.getIndex()).getName());
        }
        this.arguments.put(argumentMapping.getIndex(), argumentMapping);
    }

    public String getObfArgumentName(int index) {
        ArgumentMapping argumentMapping = this.arguments.get(index);
        if (argumentMapping != null) {
            return argumentMapping.getName();
        }

        return null;
    }

    public String getDeobfArgumentName(int index) {
        ArgumentMapping argumentMapping = this.arguments.get(index);
        if (argumentMapping != null) {
            return argumentMapping.getName();
        }

        return null;
    }

    public void setArgumentName(int index, String name) {
        ArgumentMapping argumentMapping = this.arguments.get(index);
        if (argumentMapping == null) {
            argumentMapping = new ArgumentMapping(index, name);
            boolean wasAdded = this.arguments.put(index, argumentMapping) == null;
            assert (wasAdded);
        } else {
            argumentMapping.setName(name);
        }
    }

    public void removeArgumentName(int index) {
        boolean wasRemoved = this.arguments.remove(index) != null;
        assert (wasRemoved);
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("\t");
        buf.append(this.obfName);
        buf.append(" <-> ");
        buf.append(this.deobfName);
        buf.append("\n");
        buf.append("\t");
        buf.append(this.obfSignature);
        buf.append("\n");
        buf.append("\tArguments:\n");
        for (ArgumentMapping argumentMapping : this.arguments.values()) {
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
        return (this.obfName + this.obfSignature).compareTo(other.obfName + other.obfSignature);
    }

    public boolean containsArgument(String name) {
        for (ArgumentMapping argumentMapping : this.arguments.values()) {
            if (argumentMapping.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    public boolean renameObfClass(final String oldObfClassName, final String newObfClassName) {
        // rename obf classes in the signature
        Signature newSignature = new Signature(this.obfSignature, new ClassNameReplacer() {
            @Override
            public String replace(String className) {
                if (className.equals(oldObfClassName)) {
                    return newObfClassName;
                }
                return null;
            }
        });

        if (!newSignature.equals(this.obfSignature)) {
            this.obfSignature = newSignature;
            return true;
        }
        return false;
    }

    public boolean isConstructor() {
        return this.obfName.startsWith("<");
    }

    @Override
    public BehaviorEntry getObfEntry(ClassEntry classEntry) {
        if (isConstructor()) {
            return new ConstructorEntry(classEntry, this.obfSignature);
        } else {
            return new MethodEntry(classEntry, this.obfName, this.obfSignature);
        }
    }

    public Mappings.EntryModifier getModifier()
    {
        return modifier;
    }

    public void setModifier(Mappings.EntryModifier modifier)
    {
        this.modifier = modifier;
    }
}
