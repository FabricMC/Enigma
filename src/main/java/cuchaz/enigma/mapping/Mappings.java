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
import com.google.common.collect.Maps;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import cuchaz.enigma.analysis.TranslationIndex;
import cuchaz.enigma.throwables.MappingConflict;

public class Mappings {

    protected Map<String, ClassMapping> classesByObf;
    protected Map<String, ClassMapping> classesByDeobf;
    private final FormatType originMapping;

    public Mappings()
    {
        this(FormatType.ENIGMA_DIRECTORY);
    }

    public Mappings(FormatType originMapping) {
        this.originMapping = originMapping;
        this.classesByObf = Maps.newHashMap();
        this.classesByDeobf = Maps.newHashMap();
    }

    public Collection<ClassMapping> classes() {
        assert (this.classesByObf.size() >= this.classesByDeobf.size());
        return this.classesByObf.values();
    }

    public void addClassMapping(ClassMapping classMapping) throws MappingConflict {
        if (this.classesByObf.containsKey(classMapping.getObfFullName())) {
            throw new MappingConflict("class", classMapping.getObfFullName(), this.classesByObf.get(classMapping.getObfFullName()).getObfFullName());
        }
        this.classesByObf.put(classMapping.getObfFullName(), classMapping);

        if (classMapping.getDeobfName() != null) {
            if (this.classesByDeobf.containsKey(classMapping.getDeobfName())) {
                throw new MappingConflict("class", classMapping.getDeobfName(), this.classesByDeobf.get(classMapping.getDeobfName()).getDeobfName());
            }
            this.classesByDeobf.put(classMapping.getDeobfName(), classMapping);
        }
    }

    public void removeClassMapping(ClassMapping classMapping) {
        boolean obfWasRemoved = this.classesByObf.remove(classMapping.getObfFullName()) != null;
        assert (obfWasRemoved);
        if (classMapping.getDeobfName() != null) {
            boolean deobfWasRemoved = this.classesByDeobf.remove(classMapping.getDeobfName()) != null;
            assert (deobfWasRemoved);
        }
    }

    public ClassMapping getClassByObf(String obfName) {
        return this.classesByObf.get(obfName);
    }

    public void setClassDeobfName(ClassMapping classMapping, String deobfName) {
        if (classMapping.getDeobfName() != null) {
            boolean wasRemoved = this.classesByDeobf.remove(classMapping.getDeobfName()) != null;
            assert (wasRemoved);
        }
        classMapping.setDeobfName(deobfName);
        if (deobfName != null) {
            boolean wasAdded = this.classesByDeobf.put(deobfName, classMapping) == null;
            assert (wasAdded);
        }
    }

    public Translator getTranslator(TranslationDirection direction, TranslationIndex index) {
        switch (direction) {
            case Deobfuscating:

                return new Translator(direction, this.classesByObf, index);

            case Obfuscating:

                // fill in the missing deobf class entries with obf entries
                Map<String, ClassMapping> classes = Maps.newHashMap();
                for (ClassMapping classMapping : classes()) {
                    if (classMapping.getDeobfName() != null) {
                        classes.put(classMapping.getDeobfName(), classMapping);
                    } else {
                        classes.put(classMapping.getObfFullName(), classMapping);
                    }
                }

                // translate the translation index
                // NOTE: this isn't actually recursive
                TranslationIndex deobfIndex = new TranslationIndex(index, getTranslator(TranslationDirection.Deobfuscating, index));

                return new Translator(direction, classes, deobfIndex);

            default:
                throw new Error("Invalid translation direction!");
        }
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        for (ClassMapping classMapping : this.classesByObf.values()) {
            buf.append(classMapping.toString());
            buf.append("\n");
        }
        return buf.toString();
    }

    public boolean containsDeobfClass(String deobfName) {
        return this.classesByDeobf.containsKey(deobfName);
    }

    public boolean containsDeobfField(ClassEntry obfClassEntry, String deobfName, Type obfType) {
        ClassMapping classMapping = this.classesByObf.get(obfClassEntry.getName());
        return classMapping != null && classMapping.containsDeobfField(deobfName, obfType);
    }

    public boolean containsDeobfMethod(ClassEntry obfClassEntry, String deobfName, Signature deobfSignature) {
        ClassMapping classMapping = this.classesByObf.get(obfClassEntry.getName());
        return classMapping != null && classMapping.containsDeobfMethod(deobfName, deobfSignature);
    }

    public boolean containsArgument(BehaviorEntry obfBehaviorEntry, String name) {
        ClassMapping classMapping = this.classesByObf.get(obfBehaviorEntry.getClassName());
        return classMapping != null && classMapping.containsArgument(obfBehaviorEntry, name);
    }

    public List<ClassMapping> getClassMappingChain(ClassEntry obfClass) {
        List<ClassMapping> mappingChain = Lists.newArrayList();
        ClassMapping classMapping = null;
        for (ClassEntry obfClassEntry : obfClass.getClassChain()) {
            if (mappingChain.isEmpty()) {
                classMapping = this.classesByObf.get(obfClassEntry.getName());
            } else if (classMapping != null) {
                classMapping = classMapping.getInnerClassByObfSimple(obfClassEntry.getInnermostClassName());
            }
            mappingChain.add(classMapping);
        }
        return mappingChain;
    }

    public FormatType getOriginMappingFormat()
    {
        return originMapping;
    }

    public enum FormatType
    {
        JSON_DIRECTORY, ENIGMA_FILE, ENIGMA_DIRECTORY, SRG_FILE
    }
}
