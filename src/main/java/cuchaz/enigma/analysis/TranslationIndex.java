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
package cuchaz.enigma.analysis;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

import java.io.*;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import cuchaz.enigma.mapping.*;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.CtField;
import javassist.bytecode.Descriptor;

public class TranslationIndex implements Serializable {

    private static final long serialVersionUID = 738687982126844179L;

    private Map<ClassEntry, ClassEntry> superclasses;
    private Multimap<ClassEntry, FieldEntry> fieldEntries;
    private Multimap<ClassEntry, BehaviorEntry> behaviorEntries;
    private Multimap<ClassEntry, ClassEntry> interfaces;

    public TranslationIndex() {
        this.superclasses = Maps.newHashMap();
        this.fieldEntries = HashMultimap.create();
        this.behaviorEntries = HashMultimap.create();
        this.interfaces = HashMultimap.create();
    }

    public TranslationIndex(TranslationIndex other, Translator translator) {

        // translate the superclasses
        this.superclasses = Maps.newHashMap();
        for (Map.Entry<ClassEntry, ClassEntry> mapEntry : other.superclasses.entrySet()) {
            this.superclasses.put(translator.translateEntry(mapEntry.getKey()), translator.translateEntry(mapEntry.getValue()));
        }

        // translate the interfaces
        this.interfaces = HashMultimap.create();
        for (Map.Entry<ClassEntry, ClassEntry> mapEntry : other.interfaces.entries()) {
            this.interfaces.put(
                    translator.translateEntry(mapEntry.getKey()),
                    translator.translateEntry(mapEntry.getValue())
            );
        }

        // translate the fields
        this.fieldEntries = HashMultimap.create();
        for (Map.Entry<ClassEntry, FieldEntry> mapEntry : other.fieldEntries.entries()) {
            this.fieldEntries.put(
                    translator.translateEntry(mapEntry.getKey()),
                    translator.translateEntry(mapEntry.getValue())
            );
        }

        this.behaviorEntries = HashMultimap.create();
        for (Map.Entry<ClassEntry, BehaviorEntry> mapEntry : other.behaviorEntries.entries()) {
            this.behaviorEntries.put(
                    translator.translateEntry(mapEntry.getKey()),
                    translator.translateEntry(mapEntry.getValue())
            );
        }
    }

    public void indexClass(CtClass c) {
        indexClass(c, true);
    }

    public void indexClass(CtClass c, boolean indexMembers) {

        ClassEntry classEntry = EntryFactory.getClassEntry(c);
        if (isJre(classEntry)) {
            return;
        }

        // add the superclass
        ClassEntry superclassEntry = EntryFactory.getSuperclassEntry(c);
        if (superclassEntry != null) {
            this.superclasses.put(classEntry, superclassEntry);
        }

        // add the interfaces
        for (String interfaceClassName : c.getClassFile().getInterfaces()) {
            ClassEntry interfaceClassEntry = new ClassEntry(Descriptor.toJvmName(interfaceClassName));
            if (!isJre(interfaceClassEntry)) {
                this.interfaces.put(classEntry, interfaceClassEntry);
            }
        }

        if (indexMembers) {
            // add fields
            for (CtField field : c.getDeclaredFields()) {
                FieldEntry fieldEntry = EntryFactory.getFieldEntry(field);
                this.fieldEntries.put(fieldEntry.getClassEntry(), fieldEntry);
            }

            // add behaviors
            for (CtBehavior behavior : c.getDeclaredBehaviors()) {
                BehaviorEntry behaviorEntry = EntryFactory.getBehaviorEntry(behavior);
                this.behaviorEntries.put(behaviorEntry.getClassEntry(), behaviorEntry);
            }
        }
    }

    public void renameClasses(Map<String, String> renames) {
        EntryRenamer.renameClassesInMap(renames, this.superclasses);
        EntryRenamer.renameClassesInMultimap(renames,this.fieldEntries);
        EntryRenamer.renameClassesInMultimap(renames, this.behaviorEntries);
    }

    public ClassEntry getSuperclass(ClassEntry classEntry) {
        return this.superclasses.get(classEntry);
    }

    public List<ClassEntry> getAncestry(ClassEntry classEntry) {
        List<ClassEntry> ancestors = Lists.newArrayList();
        while (classEntry != null) {
            classEntry = getSuperclass(classEntry);
            if (classEntry != null) {
                ancestors.add(classEntry);
            }
        }
        return ancestors;
    }

    public List<ClassEntry> getSubclass(ClassEntry classEntry) {

        // linear search is fast enough for now
        List<ClassEntry> subclasses = Lists.newArrayList();
        for (Map.Entry<ClassEntry, ClassEntry> entry : this.superclasses.entrySet()) {
            ClassEntry subclass = entry.getKey();
            ClassEntry superclass = entry.getValue();
            if (classEntry.equals(superclass)) {
                subclasses.add(subclass);
            }
        }
        return subclasses;
    }

    public void getSubclassesRecursively(Set<ClassEntry> out, ClassEntry classEntry) {
        for (ClassEntry subclassEntry : getSubclass(classEntry)) {
            out.add(subclassEntry);
            getSubclassesRecursively(out, subclassEntry);
        }
    }

    public void getSubclassNamesRecursively(Set<String> out, ClassEntry classEntry) {
        for (ClassEntry subclassEntry : getSubclass(classEntry)) {
            out.add(subclassEntry.getName());
            getSubclassNamesRecursively(out, subclassEntry);
        }
    }

    public Collection<Map.Entry<ClassEntry, ClassEntry>> getClassInterfaces() {
        return this.interfaces.entries();
    }

    public Collection<ClassEntry> getInterfaces(ClassEntry classEntry) {
        return this.interfaces.get(classEntry);
    }

    public boolean isInterface(ClassEntry classEntry) {
        return this.interfaces.containsValue(classEntry);
    }

    public boolean entryExists(Entry entry) {
        if (entry instanceof FieldEntry) {
            return fieldExists((FieldEntry) entry);
        } else if (entry instanceof BehaviorEntry) {
            return behaviorExists((BehaviorEntry) entry);
        } else if (entry instanceof ArgumentEntry) {
            return behaviorExists(((ArgumentEntry) entry).getBehaviorEntry());
        }
        throw new IllegalArgumentException("Cannot check existence for " + entry.getClass());
    }

    public boolean fieldExists(FieldEntry fieldEntry) {
        return this.fieldEntries.containsEntry(fieldEntry.getClassEntry(), fieldEntry);
    }

    public boolean behaviorExists(BehaviorEntry behaviorEntry) {
        return this.behaviorEntries.containsEntry(behaviorEntry.getClassEntry(), behaviorEntry);
    }

    public ClassEntry resolveEntryClass(Entry entry) {

        if (entry instanceof ClassEntry) {
            return (ClassEntry) entry;
        }

        ClassEntry superclassEntry = resolveSuperclass(entry);
        if (superclassEntry != null) {
            return superclassEntry;
        }

        ClassEntry interfaceEntry = resolveInterface(entry);
        if (interfaceEntry != null) {
            return interfaceEntry;
        }

        return null;
    }

    public ClassEntry resolveSuperclass(Entry entry) {

        // this entry could refer to a method on a class where the method is not actually implemented
        // travel up the inheritance tree to find the closest implementation
        while (!entryExists(entry)) {

            // is there a parent class?
            ClassEntry superclassEntry = getSuperclass(entry.getClassEntry());
            if (superclassEntry == null) {
                // this is probably a method from a class in a library
                // we can't trace the implementation up any higher unless we index the library
                return null;
            }

            // move up to the parent class
            entry = entry.cloneToNewClass(superclassEntry);
        }
        return entry.getClassEntry();
    }

    public ClassEntry resolveInterface(Entry entry) {

        // the interfaces for any class is a forest
        // so let's look at all the trees
        for (ClassEntry interfaceEntry : this.interfaces.get(entry.getClassEntry())) {
            ClassEntry resolvedClassEntry = resolveSuperclass(entry.cloneToNewClass(interfaceEntry));
            if (resolvedClassEntry != null) {
                return resolvedClassEntry;
            }
        }
        return null;
    }

    private boolean isJre(ClassEntry classEntry) {
        String packageName = classEntry.getPackageName();
        return packageName != null && (packageName.startsWith("java") || packageName.startsWith("javax"));
    }

    public void write(OutputStream out)
            throws IOException {
        GZIPOutputStream gzipout = new GZIPOutputStream(out);
        ObjectOutputStream oout = new ObjectOutputStream(gzipout);
        oout.writeObject(this.superclasses);
        oout.writeObject(this.fieldEntries);
        oout.writeObject(this.behaviorEntries);
        gzipout.finish();
    }

    @SuppressWarnings("unchecked")
    public void read(InputStream in)
            throws IOException {
        try {
            ObjectInputStream oin = new ObjectInputStream(new GZIPInputStream(in));
            this.superclasses = (HashMap<ClassEntry, ClassEntry>) oin.readObject();
            this.fieldEntries = (HashMultimap<ClassEntry, FieldEntry>) oin.readObject();
            this.behaviorEntries = (HashMultimap<ClassEntry, BehaviorEntry>) oin.readObject();
        } catch (ClassNotFoundException ex) {
            throw new Error(ex);
        }
    }
}
