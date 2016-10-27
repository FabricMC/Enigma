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

import com.google.common.collect.*;

import java.lang.reflect.Modifier;
import java.util.*;
import java.util.jar.JarFile;

import cuchaz.enigma.Constants;
import cuchaz.enigma.bytecode.ClassRenamer;
import cuchaz.enigma.mapping.*;
import cuchaz.enigma.mapping.Translator;
import javassist.*;
import javassist.bytecode.*;
import javassist.expr.*;

public class JarIndex {

    private Set<ClassEntry> obfClassEntries;
    private TranslationIndex translationIndex;
    private Map<Entry, Access> access;
    private Multimap<ClassEntry, FieldEntry> fields;
    private Multimap<ClassEntry, BehaviorEntry> behaviors;
    private Multimap<String, MethodEntry> methodImplementations;
    private Multimap<BehaviorEntry, EntryReference<BehaviorEntry, BehaviorEntry>> behaviorReferences;
    private Multimap<FieldEntry, EntryReference<FieldEntry, BehaviorEntry>> fieldReferences;
    private Multimap<ClassEntry, ClassEntry> innerClassesByOuter;
    private Map<ClassEntry, ClassEntry> outerClassesByInner;
    private Map<ClassEntry, BehaviorEntry> anonymousClasses;
    private Map<MethodEntry, MethodEntry> bridgedMethods;
    private Set<MethodEntry> syntheticMethods;

    public JarIndex() {
        this.obfClassEntries = Sets.newHashSet();
        this.translationIndex = new TranslationIndex();
        this.access = Maps.newHashMap();
        this.fields = HashMultimap.create();
        this.behaviors = HashMultimap.create();
        this.methodImplementations = HashMultimap.create();
        this.behaviorReferences = HashMultimap.create();
        this.fieldReferences = HashMultimap.create();
        this.innerClassesByOuter = HashMultimap.create();
        this.outerClassesByInner = Maps.newHashMap();
        this.anonymousClasses = Maps.newHashMap();
        this.bridgedMethods = Maps.newHashMap();
        this.syntheticMethods = Sets.newHashSet();
    }

    public void indexJar(JarFile jar, boolean buildInnerClasses) {

        // step 1: read the class names
        this.obfClassEntries.addAll(JarClassIterator.getClassEntries(jar));

        // step 2: index field/method/constructor access
        for (CtClass c : JarClassIterator.classes(jar)) {
            for (CtField field : c.getDeclaredFields()) {
                FieldEntry fieldEntry = EntryFactory.getFieldEntry(field);
                this.access.put(fieldEntry, Access.get(field));
                this.fields.put(fieldEntry.getClassEntry(), fieldEntry);
            }
            for (CtBehavior behavior : c.getDeclaredBehaviors()) {
                BehaviorEntry behaviorEntry = EntryFactory.getBehaviorEntry(behavior);
                this.access.put(behaviorEntry, Access.get(behavior));
                this.behaviors.put(behaviorEntry.getClassEntry(), behaviorEntry);
            }
        }

        // step 3: index extends, implements, fields, and methods
        for (CtClass c : JarClassIterator.classes(jar)) {
            this.translationIndex.indexClass(c);
            String className = Descriptor.toJvmName(c.getName());
            for (String interfaceName : c.getClassFile().getInterfaces()) {
                className = Descriptor.toJvmName(className);
                interfaceName = Descriptor.toJvmName(interfaceName);
                if (className.equals(interfaceName)) {
                    throw new IllegalArgumentException("Class cannot be its own interface! " + className);
                }
            }
            for (CtBehavior behavior : c.getDeclaredBehaviors()) {
                indexBehavior(behavior);
            }
        }

        // step 4: index field, method, constructor references
        for (CtClass c : JarClassIterator.classes(jar)) {
            for (CtBehavior behavior : c.getDeclaredBehaviors()) {
                indexBehaviorReferences(behavior);
            }
        }

        if (buildInnerClasses) {

            // step 5: index inner classes and anonymous classes
            for (CtClass c : JarClassIterator.classes(jar)) {
                ClassEntry innerClassEntry = EntryFactory.getClassEntry(c);
                ClassEntry outerClassEntry = findOuterClass(c);
                if (outerClassEntry != null) {
                    this.innerClassesByOuter.put(outerClassEntry, innerClassEntry);
                    boolean innerWasAdded = this.outerClassesByInner.put(innerClassEntry, outerClassEntry) == null;
                    assert (innerWasAdded);

                    BehaviorEntry enclosingBehavior = isAnonymousClass(c, outerClassEntry);
                    if (enclosingBehavior != null) {
                        this.anonymousClasses.put(innerClassEntry, enclosingBehavior);

                        // DEBUG
                        //System.out.println("ANONYMOUS: " + outerClassEntry.getName() + "$" + innerClassEntry.getSimpleName());
                    }/* else {
                        // DEBUG
                        //System.out.println("INNER: " + outerClassEntry.getName() + "$" + innerClassEntry.getSimpleName());
                    }*/
                }
            }

            // step 6: update other indices with inner class info
            Map<String, String> renames = Maps.newHashMap();
            for (ClassEntry innerClassEntry : this.innerClassesByOuter.values()) {
                String newName = innerClassEntry.buildClassEntry(getObfClassChain(innerClassEntry)).getName();
                if (!innerClassEntry.getName().equals(newName)) {
                    // DEBUG
                    //System.out.println("REPLACE: " + innerClassEntry.getName() + " WITH " + newName);
                    renames.put(innerClassEntry.getName(), newName);
                }
            }
            EntryRenamer.renameClassesInSet(renames, this.obfClassEntries);
            this.translationIndex.renameClasses(renames);
            EntryRenamer.renameClassesInMultimap(renames, this.methodImplementations);
            EntryRenamer.renameClassesInMultimap(renames, this.behaviorReferences);
            EntryRenamer.renameClassesInMultimap(renames, this.fieldReferences);
            EntryRenamer.renameClassesInMap(renames, this.access);
        }
    }

    private void indexBehavior(CtBehavior behavior) {
        // get the behavior entry
        final BehaviorEntry behaviorEntry = EntryFactory.getBehaviorEntry(behavior);
        if (behaviorEntry instanceof MethodEntry) {
            MethodEntry methodEntry = (MethodEntry) behaviorEntry;

            // is synthetic
            if ((behavior.getModifiers() & AccessFlag.SYNTHETIC) != 0) {
                syntheticMethods.add(methodEntry);
            }

            // index implementation
            this.methodImplementations.put(behaviorEntry.getClassName(), methodEntry);

            // look for bridge and bridged methods
            CtMethod bridgedMethod = getBridgedMethod((CtMethod) behavior);
            if (bridgedMethod != null) {
                this.bridgedMethods.put(methodEntry, EntryFactory.getMethodEntry(bridgedMethod));
            }
        }
        // looks like we don't care about constructors here
    }

    private void indexBehaviorReferences(CtBehavior behavior) {
        // index method calls
        final BehaviorEntry behaviorEntry = EntryFactory.getBehaviorEntry(behavior);
        try {
            behavior.instrument(new ExprEditor() {
                @Override
                public void edit(MethodCall call) {
                    MethodEntry calledMethodEntry = EntryFactory.getMethodEntry(call);
                    ClassEntry resolvedClassEntry = translationIndex.resolveEntryClass(calledMethodEntry);
                    if (resolvedClassEntry != null && !resolvedClassEntry.equals(calledMethodEntry.getClassEntry())) {
                        calledMethodEntry = new MethodEntry(
                                resolvedClassEntry,
                                calledMethodEntry.getName(),
                                calledMethodEntry.getSignature()
                        );
                    }
                    EntryReference<BehaviorEntry, BehaviorEntry> reference = new EntryReference<>(
                            calledMethodEntry,
                            call.getMethodName(),
                            behaviorEntry
                    );
                    behaviorReferences.put(calledMethodEntry, reference);
                }

                @Override
                public void edit(FieldAccess call) {
                    FieldEntry calledFieldEntry = EntryFactory.getFieldEntry(call);
                    ClassEntry resolvedClassEntry = translationIndex.resolveEntryClass(calledFieldEntry);
                    if (resolvedClassEntry != null && !resolvedClassEntry.equals(calledFieldEntry.getClassEntry())) {
                        calledFieldEntry = new FieldEntry(calledFieldEntry, resolvedClassEntry);
                    }
                    EntryReference<FieldEntry, BehaviorEntry> reference = new EntryReference<>(
                            calledFieldEntry,
                            call.getFieldName(),
                            behaviorEntry
                    );
                    fieldReferences.put(calledFieldEntry, reference);
                }

                @Override
                public void edit(ConstructorCall call) {
                    ConstructorEntry calledConstructorEntry = EntryFactory.getConstructorEntry(call);
                    EntryReference<BehaviorEntry, BehaviorEntry> reference = new EntryReference<>(
                            calledConstructorEntry,
                            call.getMethodName(),
                            behaviorEntry
                    );
                    behaviorReferences.put(calledConstructorEntry, reference);
                }

                @Override
                public void edit(NewExpr call) {
                    ConstructorEntry calledConstructorEntry = EntryFactory.getConstructorEntry(call);
                    EntryReference<BehaviorEntry, BehaviorEntry> reference = new EntryReference<>(
                            calledConstructorEntry,
                            call.getClassName(),
                            behaviorEntry
                    );
                    behaviorReferences.put(calledConstructorEntry, reference);
                }
            });
        } catch (CannotCompileException ex) {
            throw new Error(ex);
        }
    }

    private CtMethod getBridgedMethod(CtMethod method) {

        // bridge methods just call another method, cast it to the return type, and return the result
        // let's see if we can detect this scenario

        // skip non-synthetic methods
        if ((method.getModifiers() & AccessFlag.SYNTHETIC) == 0) {
            return null;
        }

        // get all the called methods
        final List<MethodCall> methodCalls = Lists.newArrayList();
        try {
            method.instrument(new ExprEditor() {
                @Override
                public void edit(MethodCall call) {
                    methodCalls.add(call);
                }
            });
        } catch (CannotCompileException ex) {
            // this is stupid... we're not even compiling anything
            throw new Error(ex);
        }

        // is there just one?
        if (methodCalls.size() != 1) {
            return null;
        }
        MethodCall call = methodCalls.get(0);

        try {
            // we have a bridge method!
            return call.getMethod();
        } catch (NotFoundException ex) {
            // can't find the type? not a bridge method
            return null;
        }
    }

    private ClassEntry findOuterClass(CtClass c) {

        ClassEntry classEntry = EntryFactory.getClassEntry(c);

        // does this class already have an outer class?
        if (classEntry.isInnerClass()) {
            return classEntry.getOuterClassEntry();
        }

        // inner classes:
        // have constructors that can (illegally) set synthetic fields
        // the outer class is the only class that calls constructors

        // use the synthetic fields to find the synthetic constructors
        for (CtConstructor constructor : c.getDeclaredConstructors()) {
            Set<String> syntheticFieldTypes = Sets.newHashSet();
            if (!isIllegalConstructor(syntheticFieldTypes, constructor)) {
                continue;
            }

            ConstructorEntry constructorEntry = EntryFactory.getConstructorEntry(constructor);

            // gather the classes from the illegally-set synthetic fields
            Set<ClassEntry> illegallySetClasses = Sets.newHashSet();
            for (String type : syntheticFieldTypes) {
                if (type.startsWith("L")) {
                    ClassEntry outerClassEntry = new ClassEntry(type.substring(1, type.length() - 1));
                    if (isSaneOuterClass(outerClassEntry, classEntry)) {
                        illegallySetClasses.add(outerClassEntry);
                    }
                }
            }

            // who calls this constructor?
            Set<ClassEntry> callerClasses = Sets.newHashSet();
            for (EntryReference<BehaviorEntry, BehaviorEntry> reference : getBehaviorReferences(constructorEntry)) {

                // make sure it's not a call to super
                if (reference.entry instanceof ConstructorEntry && reference.context instanceof ConstructorEntry) {

                    // is the entry a superclass of the context?
                    ClassEntry calledClassEntry = reference.entry.getClassEntry();
                    ClassEntry superclassEntry = this.translationIndex.getSuperclass(reference.context.getClassEntry());
                    if (superclassEntry != null && superclassEntry.equals(calledClassEntry)) {
                        // it's a super call, skip
                        continue;
                    }
                }

                if (isSaneOuterClass(reference.context.getClassEntry(), classEntry)) {
                    callerClasses.add(reference.context.getClassEntry());
                }
            }

            // do we have an answer yet?
            if (callerClasses.isEmpty()) {
                if (illegallySetClasses.size() == 1) {
                    return illegallySetClasses.iterator().next();
                } else {
                    System.out.println(String.format("WARNING: Unable to find outer class for %s. No caller and no illegally set field classes.", classEntry));
                }
            } else {
                if (callerClasses.size() == 1) {
                    return callerClasses.iterator().next();
                } else {
                    // multiple callers, do the illegally set classes narrow it down?
                    Set<ClassEntry> intersection = Sets.newHashSet(callerClasses);
                    intersection.retainAll(illegallySetClasses);
                    if (intersection.size() == 1) {
                        return intersection.iterator().next();
                    } else {
                        System.out.println(String.format("WARNING: Unable to choose outer class for %s among options: %s", classEntry, callerClasses));
                    }
                }
            }
        }

        return null;
    }

    private boolean isSaneOuterClass(ClassEntry outerClassEntry, ClassEntry innerClassEntry) {

        // clearly this would be silly
        if (outerClassEntry.equals(innerClassEntry)) {
            return false;
        }

        // is the outer class in the jar?
        return this.obfClassEntries.contains(outerClassEntry);

    }

    @SuppressWarnings("unchecked")
    private boolean isIllegalConstructor(Set<String> syntheticFieldTypes, CtConstructor constructor) {

        // illegal constructors only set synthetic member fields, then call super()
        String className = constructor.getDeclaringClass().getName();

        // collect all the field accesses, constructor calls, and method calls
        final List<FieldAccess> illegalFieldWrites = Lists.newArrayList();
        final List<ConstructorCall> constructorCalls = Lists.newArrayList();
        try {
            constructor.instrument(new ExprEditor() {
                @Override
                public void edit(FieldAccess fieldAccess) {
                    if (fieldAccess.isWriter() && constructorCalls.isEmpty()) {
                        illegalFieldWrites.add(fieldAccess);
                    }
                }

                @Override
                public void edit(ConstructorCall constructorCall) {
                    constructorCalls.add(constructorCall);
                }
            });
        } catch (CannotCompileException ex) {
            // we're not compiling anything... this is stupid
            throw new Error(ex);
        }

        // are there any illegal field writes?
        if (illegalFieldWrites.isEmpty()) {
            return false;
        }

        // are all the writes to synthetic fields?
        for (FieldAccess fieldWrite : illegalFieldWrites) {

            // all illegal writes have to be to the local class
            if (!fieldWrite.getClassName().equals(className)) {
                System.err.println(String.format("WARNING: illegal write to non-member field %s.%s", fieldWrite.getClassName(), fieldWrite.getFieldName()));
                return false;
            }

            // find the field
            FieldInfo fieldInfo = null;
            for (FieldInfo info : (List<FieldInfo>) constructor.getDeclaringClass().getClassFile().getFields()) {
                if (info.getName().equals(fieldWrite.getFieldName()) && info.getDescriptor().equals(fieldWrite.getSignature())) {
                    fieldInfo = info;
                    break;
                }
            }
            if (fieldInfo == null) {
                // field is in a superclass or something, can't be a local synthetic member
                return false;
            }

            // is this field synthetic?
            boolean isSynthetic = (fieldInfo.getAccessFlags() & AccessFlag.SYNTHETIC) != 0;
            if (isSynthetic) {
                syntheticFieldTypes.add(fieldInfo.getDescriptor());
            } else {
                System.err.println(String.format("WARNING: illegal write to non synthetic field %s %s.%s", fieldInfo.getDescriptor(), className, fieldInfo.getName()));
                return false;
            }
        }

        // we passed all the tests!
        return true;
    }

    private BehaviorEntry isAnonymousClass(CtClass c, ClassEntry outerClassEntry) {

        // is this class already marked anonymous?
        EnclosingMethodAttribute enclosingMethodAttribute = (EnclosingMethodAttribute) c.getClassFile().getAttribute(EnclosingMethodAttribute.tag);
        if (enclosingMethodAttribute != null) {
            if (enclosingMethodAttribute.methodIndex() > 0) {
                return EntryFactory.getBehaviorEntry(
                        Descriptor.toJvmName(enclosingMethodAttribute.className()),
                        enclosingMethodAttribute.methodName(),
                        enclosingMethodAttribute.methodDescriptor()
                );
            } else {
                // an attribute but no method? assume not anonymous
                return null;
            }
        }

        // if there's an inner class attribute, but not an enclosing method attribute, then it's not anonymous
        InnerClassesAttribute innerClassesAttribute = (InnerClassesAttribute) c.getClassFile().getAttribute(InnerClassesAttribute.tag);
        if (innerClassesAttribute != null) {
            return null;
        }

        ClassEntry innerClassEntry = new ClassEntry(Descriptor.toJvmName(c.getName()));

        // anonymous classes:
        // can't be abstract
        // have only one constructor
        // it's called exactly once by the outer class
        // the type the instance is assigned to can't be this type

        // is abstract?
        if (Modifier.isAbstract(c.getModifiers())) {
            return null;
        }

        // is there exactly one constructor?
        if (c.getDeclaredConstructors().length != 1) {
            return null;
        }
        CtConstructor constructor = c.getDeclaredConstructors()[0];

        // is this constructor called exactly once?
        ConstructorEntry constructorEntry = EntryFactory.getConstructorEntry(constructor);
        Collection<EntryReference<BehaviorEntry, BehaviorEntry>> references = getBehaviorReferences(constructorEntry);
        if (references.size() != 1) {
            return null;
        }

        // does the caller use this type?
        BehaviorEntry caller = references.iterator().next().context;
        for (FieldEntry fieldEntry : getReferencedFields(caller)) {
            if (fieldEntry.getType().hasClass() && fieldEntry.getType().getClassEntry().equals(innerClassEntry)) {
                // caller references this type, so it can't be anonymous
                return null;
            }
        }
        for (BehaviorEntry behaviorEntry : getReferencedBehaviors(caller)) {
            if (behaviorEntry.getSignature().hasClass(innerClassEntry)) {
                return null;
            }
        }

        return caller;
    }

    public Set<ClassEntry> getObfClassEntries() {
        return this.obfClassEntries;
    }

    public Collection<FieldEntry> getObfFieldEntries() {
        return this.fields.values();
    }

    public Collection<FieldEntry> getObfFieldEntries(ClassEntry classEntry) {
        return this.fields.get(classEntry);
    }

    public Collection<BehaviorEntry> getObfBehaviorEntries() {
        return this.behaviors.values();
    }

    public Collection<BehaviorEntry> getObfBehaviorEntries(ClassEntry classEntry) {
        return this.behaviors.get(classEntry);
    }

    public TranslationIndex getTranslationIndex() {
        return this.translationIndex;
    }

    public Access getAccess(Entry entry) {
        return this.access.get(entry);
    }

    public ClassInheritanceTreeNode getClassInheritance(Translator deobfuscatingTranslator, ClassEntry obfClassEntry) {

        // get the root node
        List<String> ancestry = Lists.newArrayList();
        ancestry.add(obfClassEntry.getName());
        for (ClassEntry classEntry : this.translationIndex.getAncestry(obfClassEntry)) {
            if (containsObfClass(classEntry)) {
                ancestry.add(classEntry.getName());
            }
        }
        ClassInheritanceTreeNode rootNode = new ClassInheritanceTreeNode(
                deobfuscatingTranslator,
                ancestry.get(ancestry.size() - 1)
        );

        // expand all children recursively
        rootNode.load(this.translationIndex, true);

        return rootNode;
    }

    public ClassImplementationsTreeNode getClassImplementations(Translator deobfuscatingTranslator, ClassEntry obfClassEntry) {

        // is this even an interface?
        if (isInterface(obfClassEntry.getClassName())) {
            ClassImplementationsTreeNode node = new ClassImplementationsTreeNode(deobfuscatingTranslator, obfClassEntry);
            node.load(this);
            return node;
        }
        return null;
    }

    public MethodInheritanceTreeNode getMethodInheritance(Translator deobfuscatingTranslator, MethodEntry obfMethodEntry) {

        // travel to the ancestor implementation
        ClassEntry baseImplementationClassEntry = obfMethodEntry.getClassEntry();
        for (ClassEntry ancestorClassEntry : this.translationIndex.getAncestry(obfMethodEntry.getClassEntry())) {
            MethodEntry ancestorMethodEntry = new MethodEntry(
                    new ClassEntry(ancestorClassEntry),
                    obfMethodEntry.getName(),
                    obfMethodEntry.getSignature()
            );
            if (containsObfBehavior(ancestorMethodEntry)) {
                baseImplementationClassEntry = ancestorClassEntry;
            }
        }

        // make a root node at the base
        MethodEntry methodEntry = new MethodEntry(
                baseImplementationClassEntry,
                obfMethodEntry.getName(),
                obfMethodEntry.getSignature()
        );
        MethodInheritanceTreeNode rootNode = new MethodInheritanceTreeNode(
                deobfuscatingTranslator,
                methodEntry,
                containsObfBehavior(methodEntry)
        );

        // expand the full tree
        rootNode.load(this, true);

        return rootNode;
    }

    public List<MethodImplementationsTreeNode> getMethodImplementations(Translator deobfuscatingTranslator, MethodEntry obfMethodEntry) {

        List<MethodEntry> interfaceMethodEntries = Lists.newArrayList();

        // is this method on an interface?
        if (isInterface(obfMethodEntry.getClassName())) {
            interfaceMethodEntries.add(obfMethodEntry);
        } else {
            // get the interface class
            for (ClassEntry interfaceEntry : getInterfaces(obfMethodEntry.getClassName())) {

                // is this method defined in this interface?
                MethodEntry methodInterface = new MethodEntry(
                        interfaceEntry,
                        obfMethodEntry.getName(),
                        obfMethodEntry.getSignature()
                );
                if (containsObfBehavior(methodInterface)) {
                    interfaceMethodEntries.add(methodInterface);
                }
            }
        }

        List<MethodImplementationsTreeNode> nodes = Lists.newArrayList();
        if (!interfaceMethodEntries.isEmpty()) {
            for (MethodEntry interfaceMethodEntry : interfaceMethodEntries) {
                MethodImplementationsTreeNode node = new MethodImplementationsTreeNode(deobfuscatingTranslator, interfaceMethodEntry);
                node.load(this);
                nodes.add(node);
            }
        }
        return nodes;
    }

    public Set<MethodEntry> getRelatedMethodImplementations(MethodEntry obfMethodEntry) {
        Set<MethodEntry> methodEntries = Sets.newHashSet();
        getRelatedMethodImplementations(methodEntries, getMethodInheritance(new Translator(), obfMethodEntry));
        return methodEntries;
    }

    private void getRelatedMethodImplementations(Set<MethodEntry> methodEntries, MethodInheritanceTreeNode node) {
        MethodEntry methodEntry = node.getMethodEntry();

        if (containsObfBehavior(methodEntry)) {
            // collect the entry
            methodEntries.add(methodEntry);
        }

        // look at bridged methods!
        MethodEntry bridgedEntry = getBridgedMethod(methodEntry);
        while (bridgedEntry != null) {
            methodEntries.addAll(getRelatedMethodImplementations(bridgedEntry));
            bridgedEntry = getBridgedMethod(bridgedEntry);
        }

        // look at interface methods too
        for (MethodImplementationsTreeNode implementationsNode : getMethodImplementations(new Translator(), methodEntry)) {
            getRelatedMethodImplementations(methodEntries, implementationsNode);
        }

        // recurse
        for (int i = 0; i < node.getChildCount(); i++) {
            getRelatedMethodImplementations(methodEntries, (MethodInheritanceTreeNode) node.getChildAt(i));
        }
    }

    private void getRelatedMethodImplementations(Set<MethodEntry> methodEntries, MethodImplementationsTreeNode node) {
        MethodEntry methodEntry = node.getMethodEntry();
        if (containsObfBehavior(methodEntry)) {
            // collect the entry
            methodEntries.add(methodEntry);
        }

        // look at bridged methods!
        MethodEntry bridgedEntry = getBridgedMethod(methodEntry);
        while (bridgedEntry != null) {
            methodEntries.addAll(getRelatedMethodImplementations(bridgedEntry));
            bridgedEntry = getBridgedMethod(bridgedEntry);
        }

        // recurse
        for (int i = 0; i < node.getChildCount(); i++) {
            getRelatedMethodImplementations(methodEntries, (MethodImplementationsTreeNode) node.getChildAt(i));
        }
    }

    public Collection<EntryReference<FieldEntry, BehaviorEntry>> getFieldReferences(FieldEntry fieldEntry) {
        return this.fieldReferences.get(fieldEntry);
    }

    public Collection<FieldEntry> getReferencedFields(BehaviorEntry behaviorEntry) {
        // linear search is fast enough for now
        Set<FieldEntry> fieldEntries = Sets.newHashSet();
        for (EntryReference<FieldEntry, BehaviorEntry> reference : this.fieldReferences.values()) {
            if (reference.context == behaviorEntry) {
                fieldEntries.add(reference.entry);
            }
        }
        return fieldEntries;
    }

    public Collection<EntryReference<BehaviorEntry, BehaviorEntry>> getBehaviorReferences(BehaviorEntry behaviorEntry) {
        return this.behaviorReferences.get(behaviorEntry);
    }

    public Collection<BehaviorEntry> getReferencedBehaviors(BehaviorEntry behaviorEntry) {
        // linear search is fast enough for now
        Set<BehaviorEntry> behaviorEntries = Sets.newHashSet();
        for (EntryReference<BehaviorEntry, BehaviorEntry> reference : this.behaviorReferences.values()) {
            if (reference.context == behaviorEntry) {
                behaviorEntries.add(reference.entry);
            }
        }
        return behaviorEntries;
    }

    public Collection<ClassEntry> getInnerClasses(ClassEntry obfOuterClassEntry) {
        return this.innerClassesByOuter.get(obfOuterClassEntry);
    }

    public ClassEntry getOuterClass(ClassEntry obfInnerClassEntry) {
        return this.outerClassesByInner.get(obfInnerClassEntry);
    }

    public boolean isAnonymousClass(ClassEntry obfInnerClassEntry) {
        return this.anonymousClasses.containsKey(obfInnerClassEntry);
    }

    public boolean isSyntheticMethod(MethodEntry methodEntry) {
        return this.syntheticMethods.contains(methodEntry);
    }

    public BehaviorEntry getAnonymousClassCaller(ClassEntry obfInnerClassName) {
        return this.anonymousClasses.get(obfInnerClassName);
    }

    public Set<ClassEntry> getInterfaces(String className) {
        ClassEntry classEntry = new ClassEntry(className);
        Set<ClassEntry> interfaces = new HashSet<>();
        interfaces.addAll(this.translationIndex.getInterfaces(classEntry));
        for (ClassEntry ancestor : this.translationIndex.getAncestry(classEntry)) {
            interfaces.addAll(this.translationIndex.getInterfaces(ancestor));
        }
        return interfaces;
    }

    public Set<String> getImplementingClasses(String targetInterfaceName) {

        // linear search is fast enough for now
        Set<String> classNames = Sets.newHashSet();
        for (Map.Entry<ClassEntry, ClassEntry> entry : this.translationIndex.getClassInterfaces()) {
            ClassEntry classEntry = entry.getKey();
            ClassEntry interfaceEntry = entry.getValue();
            if (interfaceEntry.getName().equals(targetInterfaceName)) {
                String className = classEntry.getClassName();
                classNames.add(className);
                if (isInterface(className)) {
                    classNames.addAll(getImplementingClasses(className));
                }

                this.translationIndex.getSubclassNamesRecursively(classNames, classEntry);
            }
        }
        return classNames;
    }

    public boolean isInterface(String className) {
        return this.translationIndex.isInterface(new ClassEntry(className));
    }

    public boolean containsObfClass(ClassEntry obfClassEntry) {
        return this.obfClassEntries.contains(obfClassEntry);
    }

    public boolean containsObfField(FieldEntry obfFieldEntry) {
        return this.access.containsKey(obfFieldEntry);
    }

    public boolean containsObfBehavior(BehaviorEntry obfBehaviorEntry) {
        return this.access.containsKey(obfBehaviorEntry);
    }

    public boolean containsEntryWithSameName(Entry entry)
    {
        for (Entry target : this.access.keySet())
            if (target.getName().equals(entry.getName()) && entry.getClass().isInstance(target.getClass()))
                return true;
        return false;
    }

    public boolean containsObfArgument(ArgumentEntry obfArgumentEntry) {
        // check the behavior
        if (!containsObfBehavior(obfArgumentEntry.getBehaviorEntry())) {
            return false;
        }

        // check the argument
        return obfArgumentEntry.getIndex() < obfArgumentEntry.getBehaviorEntry().getSignature().getArgumentTypes().size();

    }

    public boolean containsObfEntry(Entry obfEntry) {
        if (obfEntry instanceof ClassEntry) {
            return containsObfClass((ClassEntry) obfEntry);
        } else if (obfEntry instanceof FieldEntry) {
            return containsObfField((FieldEntry) obfEntry);
        } else if (obfEntry instanceof BehaviorEntry) {
            return containsObfBehavior((BehaviorEntry) obfEntry);
        } else if (obfEntry instanceof ArgumentEntry) {
            return containsObfArgument((ArgumentEntry) obfEntry);
        } else if (obfEntry instanceof LocalVariableEntry) {
            // TODO: Implement it
            return false;
        } else {
            throw new Error("Entry type not supported: " + obfEntry.getClass().getName());
        }
    }

    public MethodEntry getBridgedMethod(MethodEntry bridgeMethodEntry) {
        return this.bridgedMethods.get(bridgeMethodEntry);
    }

    public List<ClassEntry> getObfClassChain(ClassEntry obfClassEntry) {

        // build class chain in inner-to-outer order
        List<ClassEntry> obfClassChain = Lists.newArrayList(obfClassEntry);
        ClassEntry checkClassEntry = obfClassEntry;
        while (true) {
            ClassEntry obfOuterClassEntry = getOuterClass(checkClassEntry);
            if (obfOuterClassEntry != null) {
                obfClassChain.add(obfOuterClassEntry);
                checkClassEntry = obfOuterClassEntry;
            } else {
                break;
            }
        }

        // switch to outer-to-inner order
        Collections.reverse(obfClassChain);

        return obfClassChain;
    }
}
