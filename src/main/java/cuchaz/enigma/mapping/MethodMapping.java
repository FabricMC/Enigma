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
        this.obfName = obfName;
        this.deobfName = NameValidator.validateMethodName(deobfName);
        this.obfSignature = obfSignature;
        this.arguments = Maps.newTreeMap();
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
}
