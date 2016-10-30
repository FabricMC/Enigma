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

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.List;
import java.util.Set;
import java.util.zip.GZIPOutputStream;

import com.google.common.collect.Lists;
import cuchaz.enigma.analysis.JarIndex;
import cuchaz.enigma.analysis.MethodImplementationsTreeNode;
import cuchaz.enigma.throwables.IllegalNameException;
import cuchaz.enigma.throwables.MappingConflict;

public class MappingsRenamer {

    private JarIndex m_index;
    private Mappings m_mappings;

    public MappingsRenamer(JarIndex index, Mappings mappings) {
        m_index = index;
        m_mappings = mappings;
    }

    public void setMappings(Mappings mappings)
    {
        this.m_mappings = mappings;
    }

    public void setClassName(ClassEntry obf, String deobfName) {

        deobfName = NameValidator.validateClassName(deobfName, !obf.isInnerClass());

        List<ClassMapping> mappingChain = getOrCreateClassMappingChain(obf);
        if (mappingChain.size() == 1) {

            if (deobfName != null) {
                // make sure we don't rename to an existing obf or deobf class
                if (m_mappings.containsDeobfClass(deobfName) || m_index.containsObfClass(new ClassEntry(deobfName))) {
                    throw new IllegalNameException(deobfName, "There is already a class with that name");
                }
            }

            ClassMapping classMapping = mappingChain.get(0);
            m_mappings.setClassDeobfName(classMapping, deobfName);

        } else {

            ClassMapping outerClassMapping = mappingChain.get(mappingChain.size() - 2);

            if (deobfName != null) {
                // make sure we don't rename to an existing obf or deobf inner class
                if (outerClassMapping.hasInnerClassByDeobf(deobfName) || outerClassMapping.hasInnerClassByObfSimple(deobfName)) {
                    throw new IllegalNameException(deobfName, "There is already a class with that name");
                }
            }

            outerClassMapping.setInnerClassName(obf, deobfName);
        }
    }

    public void removeClassMapping(ClassEntry obf) {
        setClassName(obf, null);
    }

    public void markClassAsDeobfuscated(ClassEntry obf) {
        String deobfName = obf.isInnerClass() ? obf.getInnermostClassName() : obf.getName();
        List<ClassMapping> mappingChain = getOrCreateClassMappingChain(obf);
        if (mappingChain.size() == 1) {
            ClassMapping classMapping = mappingChain.get(0);
            m_mappings.setClassDeobfName(classMapping, deobfName);
        } else {
            ClassMapping outerClassMapping = mappingChain.get(mappingChain.size() - 2);
            outerClassMapping.setInnerClassName(obf, deobfName);
        }
    }

    public void setFieldName(FieldEntry obf, String deobfName) {
        deobfName = NameValidator.validateFieldName(deobfName);
        FieldEntry targetEntry = new FieldEntry(obf.getClassEntry(), deobfName, obf.getType());
        ClassEntry definedClass = null;
        if (m_mappings.containsDeobfField(obf.getClassEntry(), deobfName) || m_index.containsEntryWithSameName(targetEntry))
            definedClass = obf.getClassEntry();
        else {
            for (ClassEntry ancestorEntry : this.m_index.getTranslationIndex().getAncestry(obf.getClassEntry())) {
                if (m_mappings.containsDeobfField(ancestorEntry, deobfName) || m_index.containsEntryWithSameName(targetEntry.cloneToNewClass(ancestorEntry))) {
                    definedClass = ancestorEntry;
                    break;
                }
            }
        }

        if (definedClass != null) {
            String className = m_mappings.getTranslator(TranslationDirection.Deobfuscating, m_index.getTranslationIndex()).translateClass(definedClass.getClassName());
            if (className == null)
                className = definedClass.getClassName();
            throw new IllegalNameException(deobfName, "There is already a field with that name in " + className);
        }

        ClassMapping classMapping = getOrCreateClassMapping(obf.getClassEntry());
        classMapping.setFieldName(obf.getName(), obf.getType(), deobfName);
    }

    public void removeFieldMapping(FieldEntry obf) {
        ClassMapping classMapping = getOrCreateClassMapping(obf.getClassEntry());
        classMapping.removeFieldMapping(classMapping.getFieldByObf(obf.getName(), obf.getType()));
    }

    public void markFieldAsDeobfuscated(FieldEntry obf) {
        ClassMapping classMapping = getOrCreateClassMapping(obf.getClassEntry());
        classMapping.setFieldName(obf.getName(), obf.getType(), obf.getName());
    }

    private void validateMethodTreeName(MethodEntry entry, String deobfName) {
        MethodEntry targetEntry = new MethodEntry(entry.getClassEntry(), deobfName, entry.getSignature());

        // TODO: Verify if I don't break things
        ClassMapping classMapping = m_mappings.getClassByObf(entry.getClassEntry());
        if ((classMapping != null && classMapping.containsDeobfMethod(deobfName, entry.getSignature()) && classMapping.getMethodByObf(entry.getName(), entry.getSignature()) != classMapping.getMethodByDeobf(deobfName, entry.getSignature()))
                || m_index.containsObfBehavior(targetEntry)) {
            String deobfClassName = m_mappings.getTranslator(TranslationDirection.Deobfuscating, m_index.getTranslationIndex()).translateClass(entry.getClassName());
            if (deobfClassName == null) {
                deobfClassName = entry.getClassName();
            }
            throw new IllegalNameException(deobfName, "There is already a method with that name and signature in class " + deobfClassName);
        }

        for (ClassEntry child : m_index.getTranslationIndex().getSubclass(entry.getClassEntry())) {
            validateMethodTreeName(entry.cloneToNewClass(child), deobfName);
        }
    }

    public void setMethodTreeName(MethodEntry obf, String deobfName) {
        Set<MethodEntry> implementations = m_index.getRelatedMethodImplementations(obf);

        deobfName = NameValidator.validateMethodName(deobfName);
        for (MethodEntry entry : implementations) {
            validateMethodTreeName(entry, deobfName);
        }

        for (MethodEntry entry : implementations) {
            setMethodName(entry, deobfName);
        }
    }

    public void setMethodName(MethodEntry obf, String deobfName) {
        deobfName = NameValidator.validateMethodName(deobfName);
        MethodEntry targetEntry = new MethodEntry(obf.getClassEntry(), deobfName, obf.getSignature());
        ClassMapping classMapping = getOrCreateClassMapping(obf.getClassEntry());

        // TODO: Verify if I don't break things
        if ((m_mappings.containsDeobfMethod(obf.getClassEntry(), deobfName, obf.getSignature()) && classMapping.getMethodByObf(obf.getName(), obf.getSignature()) != classMapping.getMethodByDeobf(deobfName, obf.getSignature()))
                || m_index.containsObfBehavior(targetEntry)) {
            String deobfClassName = m_mappings.getTranslator(TranslationDirection.Deobfuscating, m_index.getTranslationIndex()).translateClass(obf.getClassName());
            if (deobfClassName == null) {
                deobfClassName = obf.getClassName();
            }
            throw new IllegalNameException(deobfName, "There is already a method with that name and signature in class " + deobfClassName);
        }

        classMapping.setMethodName(obf.getName(), obf.getSignature(), deobfName);
    }

    public void removeMethodTreeMapping(MethodEntry obf) {
        m_index.getRelatedMethodImplementations(obf).forEach(this::removeMethodMapping);
    }

    public void removeMethodMapping(MethodEntry obf) {
        ClassMapping classMapping = getOrCreateClassMapping(obf.getClassEntry());
        classMapping.setMethodName(obf.getName(), obf.getSignature(), null);
    }

    public void markMethodTreeAsDeobfuscated(MethodEntry obf) {
        m_index.getRelatedMethodImplementations(obf).forEach(this::markMethodAsDeobfuscated);
    }

    public void markMethodAsDeobfuscated(MethodEntry obf) {
        ClassMapping classMapping = getOrCreateClassMapping(obf.getClassEntry());
        classMapping.setMethodName(obf.getName(), obf.getSignature(), obf.getName());
    }

    public void setArgumentTreeName(ArgumentEntry obf, String deobfName) {
        if (!(obf.getBehaviorEntry() instanceof MethodEntry)) {
            setArgumentName(obf, deobfName);
            return;
        }

        MethodEntry obfMethod = (MethodEntry) obf.getBehaviorEntry();

        Set<MethodEntry> implementations = m_index.getRelatedMethodImplementations(obfMethod);
        for (MethodEntry entry : implementations) {
            ClassMapping classMapping = m_mappings.getClassByObf(entry.getClassEntry());
            if (classMapping != null) {
                MethodMapping mapping = classMapping.getMethodByObf(entry.getName(), entry.getSignature());
                // NOTE: don't need to check arguments for name collisions with names determined by Procyon
                // TODO: Verify if I don't break things
                if (mapping != null) {
                    for (ArgumentMapping argumentMapping : Lists.newArrayList(mapping.arguments())) {
                        if (argumentMapping.getIndex() != obf.getIndex()) {
                            if (mapping.getDeobfArgumentName(argumentMapping.getIndex()).equals(deobfName)
                                    || argumentMapping.getName().equals(deobfName)) {
                                throw new IllegalNameException(deobfName, "There is already an argument with that name");
                            }
                        }
                    }
                }
            }
        }

        for (MethodEntry entry : implementations) {
            setArgumentName(new ArgumentEntry(obf, entry), deobfName);
        }
    }

    public void setArgumentName(ArgumentEntry obf, String deobfName) {
        deobfName = NameValidator.validateArgumentName(deobfName);
        ClassMapping classMapping = getOrCreateClassMapping(obf.getClassEntry());
        MethodMapping mapping = classMapping.getMethodByObf(obf.getMethodName(), obf.getMethodSignature());
        // NOTE: don't need to check arguments for name collisions with names determined by Procyon
        // TODO: Verify if I don't break things
        if (mapping != null) {
            for (ArgumentMapping argumentMapping : Lists.newArrayList(mapping.arguments())) {
                if (argumentMapping.getIndex() != obf.getIndex()) {
                    if (mapping.getDeobfArgumentName(argumentMapping.getIndex()).equals(deobfName)
                            || argumentMapping.getName().equals(deobfName)) {
                        throw new IllegalNameException(deobfName, "There is already an argument with that name");
                    }
                }
            }
        }

        classMapping.setArgumentName(obf.getMethodName(), obf.getMethodSignature(), obf.getIndex(), deobfName);
    }

    public void removeArgumentMapping(ArgumentEntry obf) {
        ClassMapping classMapping = getOrCreateClassMapping(obf.getClassEntry());
        classMapping.removeArgumentName(obf.getMethodName(), obf.getMethodSignature(), obf.getIndex());
    }

    public void markArgumentAsDeobfuscated(ArgumentEntry obf) {
        ClassMapping classMapping = getOrCreateClassMapping(obf.getClassEntry());
        classMapping.setArgumentName(obf.getMethodName(), obf.getMethodSignature(), obf.getIndex(), obf.getName());
    }

    public boolean moveFieldToObfClass(ClassMapping classMapping, FieldMapping fieldMapping, ClassEntry obfClass) {
        classMapping.removeFieldMapping(fieldMapping);
        ClassMapping targetClassMapping = getOrCreateClassMapping(obfClass);
        if (!targetClassMapping.containsObfField(fieldMapping.getObfName(), fieldMapping.getObfType())) {
            if (!targetClassMapping.containsDeobfField(fieldMapping.getDeobfName(), fieldMapping.getObfType())) {
                targetClassMapping.addFieldMapping(fieldMapping);
                return true;
            } else {
                System.err.println("WARNING: deobf field was already there: " + obfClass + "." + fieldMapping.getDeobfName());
            }
        }
        return false;
    }

    public boolean moveMethodToObfClass(ClassMapping classMapping, MethodMapping methodMapping, ClassEntry obfClass) {
        classMapping.removeMethodMapping(methodMapping);
        ClassMapping targetClassMapping = getOrCreateClassMapping(obfClass);
        if (!targetClassMapping.containsObfMethod(methodMapping.getObfName(), methodMapping.getObfSignature())) {
            if (!targetClassMapping.containsDeobfMethod(methodMapping.getDeobfName(), methodMapping.getObfSignature())) {
                targetClassMapping.addMethodMapping(methodMapping);
                return true;
            } else {
                System.err.println("WARNING: deobf method was already there: " + obfClass + "." + methodMapping.getDeobfName() + methodMapping.getObfSignature());
            }
        }
        return false;
    }

    public void write(OutputStream out) throws IOException {
        // TEMP: just use the object output for now. We can find a more efficient storage format later
        GZIPOutputStream gzipout = new GZIPOutputStream(out);
        ObjectOutputStream oout = new ObjectOutputStream(gzipout);
        oout.writeObject(this);
        gzipout.finish();
    }

    private ClassMapping getOrCreateClassMapping(ClassEntry obfClassEntry) {
        List<ClassMapping> mappingChain = getOrCreateClassMappingChain(obfClassEntry);
        return mappingChain.get(mappingChain.size() - 1);
    }

    private List<ClassMapping> getOrCreateClassMappingChain(ClassEntry obfClassEntry) {
        List<ClassEntry> classChain = obfClassEntry.getClassChain();
        List<ClassMapping> mappingChain = m_mappings.getClassMappingChain(obfClassEntry);
        for (int i = 0; i < classChain.size(); i++) {
            ClassEntry classEntry = classChain.get(i);
            ClassMapping classMapping = mappingChain.get(i);
            if (classMapping == null) {

                // create it
                classMapping = new ClassMapping(classEntry.getName());
                mappingChain.set(i, classMapping);

                // add it to the right parent
                try {
                    if (i == 0) {
                        m_mappings.addClassMapping(classMapping);
                    } else {
                        mappingChain.get(i - 1).addInnerClassMapping(classMapping);
                    }
                } catch (MappingConflict mappingConflict) {
                    mappingConflict.printStackTrace();
                }
            }
        }
        return mappingChain;
    }

    public void setClassModifier(ClassEntry obEntry, Mappings.EntryModifier modifier)
    {
        ClassMapping classMapping = getOrCreateClassMapping(obEntry);
        classMapping.setModifier(modifier);
    }

    public void setFieldModifier(FieldEntry obEntry, Mappings.EntryModifier modifier)
    {
        ClassMapping classMapping = getOrCreateClassMapping(obEntry.getClassEntry());
        classMapping.setFieldModifier(obEntry.getName(), obEntry.getType(), modifier);
    }

    public void setMethodModifier(BehaviorEntry obEntry, Mappings.EntryModifier modifier)
    {
        ClassMapping classMapping = getOrCreateClassMapping(obEntry.getClassEntry());
        classMapping.setMethodModifier(obEntry.getName(), obEntry.getSignature(), modifier);
    }
}
