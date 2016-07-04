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

import java.util.Map;

import cuchaz.enigma.analysis.JarIndex;


public class MappingsChecker {

    private JarIndex index;
    private Map<ClassEntry, ClassMapping> droppedClassMappings;
    private Map<ClassEntry, ClassMapping> droppedInnerClassMappings;
    private Map<FieldEntry, FieldMapping> droppedFieldMappings;
    private Map<BehaviorEntry, MethodMapping> droppedMethodMappings;

    public MappingsChecker(JarIndex index) {
        this.index = index;
        this.droppedClassMappings = Maps.newHashMap();
        this.droppedInnerClassMappings = Maps.newHashMap();
        this.droppedFieldMappings = Maps.newHashMap();
        this.droppedMethodMappings = Maps.newHashMap();
    }

    public Map<ClassEntry, ClassMapping> getDroppedClassMappings() {
        return this.droppedClassMappings;
    }

    public Map<ClassEntry, ClassMapping> getDroppedInnerClassMappings() {
        return this.droppedInnerClassMappings;
    }

    public Map<FieldEntry, FieldMapping> getDroppedFieldMappings() {
        return this.droppedFieldMappings;
    }

    public Map<BehaviorEntry, MethodMapping> getDroppedMethodMappings() {
        return this.droppedMethodMappings;
    }

    public void dropBrokenMappings(Mappings mappings) {
        for (ClassMapping classMapping : Lists.newArrayList(mappings.classes())) {
            if (!checkClassMapping(classMapping)) {
                mappings.removeClassMapping(classMapping);
                this.droppedClassMappings.put(EntryFactory.getObfClassEntry(this.index, classMapping), classMapping);
            }
        }
    }

    private boolean checkClassMapping(ClassMapping classMapping) {

        // check the class
        ClassEntry classEntry = EntryFactory.getObfClassEntry(this.index, classMapping);
        if (!this.index.getObfClassEntries().contains(classEntry)) {
            return false;
        }

        // check the fields
        for (FieldMapping fieldMapping : Lists.newArrayList(classMapping.fields())) {
            FieldEntry obfFieldEntry = EntryFactory.getObfFieldEntry(classMapping, fieldMapping);
            if (!this.index.containsObfField(obfFieldEntry)) {
                classMapping.removeFieldMapping(fieldMapping);
                this.droppedFieldMappings.put(obfFieldEntry, fieldMapping);
            }
        }

        // check methods
        for (MethodMapping methodMapping : Lists.newArrayList(classMapping.methods())) {
            BehaviorEntry obfBehaviorEntry = EntryFactory.getObfBehaviorEntry(classEntry, methodMapping);
            if (!this.index.containsObfBehavior(obfBehaviorEntry)) {
                classMapping.removeMethodMapping(methodMapping);
                this.droppedMethodMappings.put(obfBehaviorEntry, methodMapping);
            }
        }

        // check inner classes
        for (ClassMapping innerClassMapping : Lists.newArrayList(classMapping.innerClasses())) {
            if (!checkClassMapping(innerClassMapping)) {
                classMapping.removeInnerClassMapping(innerClassMapping);
                this.droppedInnerClassMappings.put(EntryFactory.getObfClassEntry(this.index, innerClassMapping), innerClassMapping);
            }
        }

        return true;
    }
}
