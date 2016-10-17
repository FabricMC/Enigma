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
package cuchaz.enigma;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.strobel.assembler.metadata.MetadataSystem;
import com.strobel.assembler.metadata.TypeDefinition;
import com.strobel.assembler.metadata.TypeReference;
import com.strobel.decompiler.DecompilerContext;
import com.strobel.decompiler.DecompilerSettings;
import com.strobel.decompiler.PlainTextOutput;
import com.strobel.decompiler.languages.java.JavaOutputVisitor;
import com.strobel.decompiler.languages.java.ast.AstBuilder;
import com.strobel.decompiler.languages.java.ast.CompilationUnit;
import com.strobel.decompiler.languages.java.ast.InsertParenthesesVisitor;
import cuchaz.enigma.analysis.*;
import cuchaz.enigma.bytecode.ClassProtectifier;
import cuchaz.enigma.bytecode.ClassPublifier;
import cuchaz.enigma.mapping.*;
import cuchaz.enigma.utils.Utils;
import javassist.CtClass;
import javassist.bytecode.Descriptor;

import java.io.*;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

public class Deobfuscator {

    public interface ProgressListener {
        void init(int totalWork, String title);

        void onProgress(int numDone, String message);
    }

    private final JarFile jar;
    private final DecompilerSettings settings;
    private final JarIndex jarIndex;
    private final MappingsRenamer renamer;
    private final Map<TranslationDirection, Translator> translatorCache;
    private Mappings mappings;

    public Deobfuscator(JarFile jar) {
        this.jar = jar;

        // build the jar index
        this.jarIndex = new JarIndex();
        this.jarIndex.indexJar(this.jar, true);

        // config the decompiler
        this.settings = DecompilerSettings.javaDefaults();
        this.settings.setMergeVariables(Utils.getSystemPropertyAsBoolean("enigma.mergeVariables", true));
        this.settings.setForceExplicitImports(Utils.getSystemPropertyAsBoolean("enigma.forceExplicitImports", true));
        this.settings.setForceExplicitTypeArguments(
                Utils.getSystemPropertyAsBoolean("enigma.forceExplicitTypeArguments", true));
        // DEBUG
        this.settings.setShowDebugLineNumbers(Utils.getSystemPropertyAsBoolean("enigma.showDebugLineNumbers", false));
        this.settings.setShowSyntheticMembers(Utils.getSystemPropertyAsBoolean("enigma.showSyntheticMembers", false));

        // init defaults
        this.translatorCache = Maps.newTreeMap();
        this.renamer = new MappingsRenamer(this.jarIndex, null);
        // init mappings
        setMappings(new Mappings());
    }

    public JarFile getJar() {
        return this.jar;
    }

    public String getJarName() {
        return this.jar.getName();
    }

    public JarIndex getJarIndex() {
        return this.jarIndex;
    }

    public Mappings getMappings() {
        return this.mappings;
    }

    public void setMappings(Mappings val) {
        setMappings(val, true);
    }

    public void setMappings(Mappings val, boolean warnAboutDrops) {
        if (val == null) {
            val = new Mappings();
        }

        // drop mappings that don't match the jar
        MappingsChecker checker = new MappingsChecker(this.jarIndex);
        checker.dropBrokenMappings(val);
        if (warnAboutDrops) {
            for (java.util.Map.Entry<ClassEntry, ClassMapping> mapping : checker.getDroppedClassMappings().entrySet()) {
                System.out.println("WARNING: Couldn't find class entry " + mapping.getKey() + " (" + mapping.getValue().getDeobfName() + ") in jar. Mapping was dropped.");
            }
            for (java.util.Map.Entry<ClassEntry, ClassMapping> mapping : checker.getDroppedInnerClassMappings().entrySet()) {
                System.out.println("WARNING: Couldn't find inner class entry " + mapping.getKey() + " (" + mapping.getValue().getDeobfName() + ") in jar. Mapping was dropped.");
            }
            for (java.util.Map.Entry<FieldEntry, FieldMapping> mapping : checker.getDroppedFieldMappings().entrySet()) {
                System.out.println("WARNING: Couldn't find field entry " + mapping.getKey() + " (" + mapping.getValue().getDeobfName() + ") in jar. Mapping was dropped.");
            }
            for (java.util.Map.Entry<BehaviorEntry, MethodMapping> mapping : checker.getDroppedMethodMappings().entrySet()) {
                System.out.println("WARNING: Couldn't find behavior entry " + mapping.getKey() + " (" + mapping.getValue().getDeobfName() + ") in jar. Mapping was dropped.");
            }
        }

        this.mappings = val;
        this.renamer.setMappings(mappings);
        this.translatorCache.clear();
    }

    public Translator getTranslator(TranslationDirection direction) {
        Translator translator = this.translatorCache.get(direction);
        if (translator == null) {
            translator = this.mappings.getTranslator(direction, this.jarIndex.getTranslationIndex());
            this.translatorCache.put(direction, translator);
        }
        return translator;
    }

    public void getSeparatedClasses(List<ClassEntry> obfClasses, List<ClassEntry> deobfClasses) {
        for (ClassEntry obfClassEntry : this.jarIndex.getObfClassEntries()) {
            // skip inner classes
            if (obfClassEntry.isInnerClass()) {
                continue;
            }

            // separate the classes
            ClassEntry deobfClassEntry = deobfuscateEntry(obfClassEntry);
            if (!deobfClassEntry.equals(obfClassEntry)) {
                // if the class has a mapping, clearly it's deobfuscated
                deobfClasses.add(deobfClassEntry);
            } else if (!obfClassEntry.getPackageName().equals(Constants.NONE_PACKAGE)) {
                // also call it deobufscated if it's not in the none package
                deobfClasses.add(obfClassEntry);
            } else {
                // otherwise, assume it's still obfuscated
                obfClasses.add(obfClassEntry);
            }
        }
    }

    public TranslatingTypeLoader createTypeLoader()
    {
        return new TranslatingTypeLoader(
                this.jar,
                this.jarIndex,
                getTranslator(TranslationDirection.Obfuscating),
                getTranslator(TranslationDirection.Deobfuscating)
        );
    }

    public CompilationUnit getSourceTree(String className) {

        // we don't know if this class name is obfuscated or deobfuscated
        // we need to tell the decompiler the deobfuscated name so it doesn't get freaked out
        // the decompiler only sees classes after deobfuscation, so we need to load it by the deobfuscated name if there is one

        // first, assume class name is deobf
        String deobfClassName = className;

        // if it wasn't actually deobf, then we can find a mapping for it and get the deobf name
        ClassMapping classMapping = this.mappings.getClassByObf(className);
        if (classMapping != null && classMapping.getDeobfName() != null) {
            deobfClassName = classMapping.getDeobfName();
        }

        // set the type loader
        TranslatingTypeLoader loader  = createTypeLoader();
        this.settings.setTypeLoader(loader);

        // see if procyon can find the type
        TypeReference type = new MetadataSystem(loader).lookupType(deobfClassName);
        if (type == null) {
            throw new Error(String.format("Unable to find type: %s (deobf: %s)\nTried class names: %s",
                    className, deobfClassName, loader.getClassNamesToTry(deobfClassName)
            ));
        }
        TypeDefinition resolvedType = type.resolve();

        // decompile it!
        DecompilerContext context = new DecompilerContext();
        context.setCurrentType(resolvedType);
        context.setSettings(this.settings);
        AstBuilder builder = new AstBuilder(context);
        builder.addType(resolvedType);
        builder.runTransformations(null);
        return builder.getCompilationUnit();
    }

    public SourceIndex getSourceIndex(CompilationUnit sourceTree, String source) {
        return getSourceIndex(sourceTree, source, null);
    }

    public SourceIndex getSourceIndex(CompilationUnit sourceTree, String source, Boolean ignoreBadTokens) {

        // build the source index
        SourceIndex index;
        if (ignoreBadTokens != null) {
            index = new SourceIndex(source, ignoreBadTokens);
        } else {
            index = new SourceIndex(source);
        }
        sourceTree.acceptVisitor(new SourceIndexVisitor(), index);

        // DEBUG
        // sourceTree.acceptVisitor( new TreeDumpVisitor( new File( "tree.txt" ) ), null );

        // resolve all the classes in the source references
        for (Token token : index.referenceTokens()) {
            EntryReference<Entry, Entry> deobfReference = index.getDeobfReference(token);

            // get the obfuscated entry
            Entry obfEntry = obfuscateEntry(deobfReference.entry);

            // try to resolve the class
            ClassEntry resolvedObfClassEntry = this.jarIndex.getTranslationIndex().resolveEntryClass(obfEntry);
            if (resolvedObfClassEntry != null && !resolvedObfClassEntry.equals(obfEntry.getClassEntry())) {
                // change the class of the entry
                obfEntry = obfEntry.cloneToNewClass(resolvedObfClassEntry);

                // save the new deobfuscated reference
                deobfReference.entry = deobfuscateEntry(obfEntry);
                index.replaceDeobfReference(token, deobfReference);
            }

            // DEBUG
            // System.out.println( token + " -> " + reference + " -> " + index.getReferenceToken( reference ) );
        }

        return index;
    }

    public String getSource(CompilationUnit sourceTree) {
        // render the AST into source
        StringWriter buf = new StringWriter();
        sourceTree.acceptVisitor(new InsertParenthesesVisitor(), null);
        sourceTree.acceptVisitor(new JavaOutputVisitor(new PlainTextOutput(buf), this.settings), null);
        return buf.toString();
    }

    public void writeSources(File dirOut, ProgressListener progress) {
        // get the classes to decompile
        Set<ClassEntry> classEntries = Sets.newHashSet();
        for (ClassEntry obfClassEntry : this.jarIndex.getObfClassEntries()) {
            // skip inner classes
            if (obfClassEntry.isInnerClass()) {
                continue;
            }

            classEntries.add(obfClassEntry);
        }

        if (progress != null) {
            progress.init(classEntries.size(), "Decompiling classes...");
        }

        // DEOBFUSCATE ALL THE THINGS!! @_@
        int i = 0;
        for (ClassEntry obfClassEntry : classEntries) {
            ClassEntry deobfClassEntry = deobfuscateEntry(new ClassEntry(obfClassEntry));
            if (progress != null) {
                progress.onProgress(i++, deobfClassEntry.toString());
            }

            try {
                // get the source
                String source = getSource(getSourceTree(obfClassEntry.getName()));

                // write the file
                File file = new File(dirOut, deobfClassEntry.getName().replace('.', '/') + ".java");
                file.getParentFile().mkdirs();
                try (OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(file), Charsets.UTF_8)) {
                    out.write(source);
                }
            } catch (Throwable t) {
                // don't crash the whole world here, just log the error and keep going
                // TODO: set up logback via log4j
                System.err.println("Unable to deobfuscate class " + deobfClassEntry.toString() + " (" + obfClassEntry.toString() + ")");
                t.printStackTrace(System.err);
            }
        }
        if (progress != null) {
            progress.onProgress(i, "Done!");
        }
    }

    private void addAllPotentialAncestors(Set<ClassEntry> classEntries, ClassEntry classObfEntry) {
        for (ClassEntry interfaceEntry : jarIndex.getTranslationIndex().getInterfaces(classObfEntry)) {
            if (classEntries.add(interfaceEntry)) {
                addAllPotentialAncestors(classEntries, interfaceEntry);
            }
        }

        ClassEntry superClassEntry = jarIndex.getTranslationIndex().getSuperclass(classObfEntry);
        if (superClassEntry != null && classEntries.add(superClassEntry)) {
            addAllPotentialAncestors(classEntries, superClassEntry);
        }
    }

    private boolean isBehaviorProvider(ClassEntry classObfEntry, BehaviorEntry behaviorEntry) {
        if (behaviorEntry instanceof MethodEntry) {
            MethodEntry methodEntry = (MethodEntry) behaviorEntry;

            Set<ClassEntry> classEntries = new HashSet<>();
            addAllPotentialAncestors(classEntries, classObfEntry);

            for (ClassEntry parentEntry : classEntries) {
                MethodEntry ancestorMethodEntry = new MethodEntry(parentEntry, methodEntry.getName(), methodEntry.getSignature());
                if (jarIndex.containsObfBehavior(ancestorMethodEntry)) {
                    return false;
                }
            }
        }

        return true;
    }

    public void rebuildMethodNames(ProgressListener progress) {
        int i = 0;
        Map<ClassMapping, Map<Entry, String>> renameClassMap = new HashMap<>();

        progress.init(getMappings().classes().size() * 3, "Rebuilding method names");

        for (ClassMapping classMapping : Lists.newArrayList(getMappings().classes())) {
            Map<Entry, String> renameEntries = new HashMap<>();

            progress.onProgress(i++, classMapping.getDeobfName());

            for (MethodMapping methodMapping : Lists.newArrayList(classMapping.methods())) {
                ClassEntry classObfEntry = classMapping.getObfEntry();
                BehaviorEntry obfEntry = methodMapping.getObfEntry(classObfEntry);

                if (isBehaviorProvider(classObfEntry, obfEntry)) {
                    if (hasDeobfuscatedName(obfEntry) && !(obfEntry instanceof ConstructorEntry)
                            && !(methodMapping.getDeobfName().equals(methodMapping.getObfName()))) {
                        renameEntries.put(obfEntry, methodMapping.getDeobfName());
                    }

                    for (ArgumentMapping argumentMapping : Lists.newArrayList(methodMapping.arguments())) {
                        Entry argObfEntry = argumentMapping.getObfEntry(obfEntry);
                        if (hasDeobfuscatedName(argObfEntry)) {
                            renameEntries.put(argObfEntry, deobfuscateEntry(argObfEntry).getName());
                        }
                    }
                }
            }

            renameClassMap.put(classMapping, renameEntries);
        }

        for (Map.Entry<ClassMapping, Map<Entry, String>> renameClassMapEntry : renameClassMap.entrySet()) {
            progress.onProgress(i++, renameClassMapEntry.getKey().getDeobfName());

            for (Map.Entry<Entry, String> entry : renameClassMapEntry.getValue().entrySet()) {
                Entry obfEntry = entry.getKey();

                removeMapping(obfEntry);
            }
        }

        for (Map.Entry<ClassMapping, Map<Entry, String>> renameClassMapEntry : renameClassMap.entrySet()) {
            progress.onProgress(i++, renameClassMapEntry.getKey().getDeobfName());

            for (Map.Entry<Entry, String> entry : renameClassMapEntry.getValue().entrySet()) {
                Entry obfEntry = entry.getKey();
                String name = entry.getValue();

                rename(obfEntry, name);
            }
        }
    }

    public void writeJar(File out, ProgressListener progress) {
        transformJar(out, progress, createTypeLoader()::transformClass);
    }

    public void protectifyJar(File out, ProgressListener progress) {
        transformJar(out, progress, ClassProtectifier::protectify);
    }

    public void publifyJar(File out, ProgressListener progress) {
        transformJar(out, progress, ClassPublifier::publify);
    }

    public interface ClassTransformer {
        CtClass transform(CtClass c) throws Exception;
    }

    public void transformJar(File out, ProgressListener progress, ClassTransformer transformer) {
        try (JarOutputStream outJar = new JarOutputStream(new FileOutputStream(out))) {
            if (progress != null) {
                progress.init(JarClassIterator.getClassEntries(this.jar).size(), "Transforming classes...");
            }

            int i = 0;
            for (CtClass c : JarClassIterator.classes(this.jar)) {
                if (progress != null) {
                    progress.onProgress(i++, c.getName());
                }

                try {
                    c = transformer.transform(c);
                    outJar.putNextEntry(new JarEntry(c.getName().replace('.', '/') + ".class"));
                    outJar.write(c.toBytecode());
                    outJar.closeEntry();
                } catch (Throwable t) {
                    throw new Error("Unable to transform class " + c.getName(), t);
                }
            }
            if (progress != null) {
                progress.onProgress(i, "Done!");
            }

            outJar.close();
        } catch (IOException ex) {
            throw new Error("Unable to write to Jar file!");
        }
    }

    public <T extends Entry> T obfuscateEntry(T deobfEntry) {
        if (deobfEntry == null) {
            return null;
        }
        return getTranslator(TranslationDirection.Obfuscating).translateEntry(deobfEntry);
    }

    public <T extends Entry> T deobfuscateEntry(T obfEntry) {
        if (obfEntry == null) {
            return null;
        }
        return getTranslator(TranslationDirection.Deobfuscating).translateEntry(obfEntry);
    }

    public <E extends Entry, C extends Entry> EntryReference<E, C> obfuscateReference(EntryReference<E, C> deobfReference) {
        if (deobfReference == null) {
            return null;
        }
        return new EntryReference<>(obfuscateEntry(deobfReference.entry), obfuscateEntry(deobfReference.context), deobfReference);
    }

    public <E extends Entry, C extends Entry> EntryReference<E, C> deobfuscateReference(EntryReference<E, C> obfReference) {
        if (obfReference == null) {
            return null;
        }
        return new EntryReference<>(deobfuscateEntry(obfReference.entry), deobfuscateEntry(obfReference.context), obfReference);
    }

    public boolean isObfuscatedIdentifier(Entry obfEntry) {
        return isObfuscatedIdentifier(obfEntry, false);
    }

    public boolean isObfuscatedIdentifier(Entry obfEntry, boolean hack) {

        if (obfEntry instanceof MethodEntry) {

            // HACKHACK: Object methods are not obfuscated identifiers
            MethodEntry obfMethodEntry = (MethodEntry) obfEntry;
            String name = obfMethodEntry.getName();
            String sig = obfMethodEntry.getSignature().toString();
            if (name.equals("clone") && sig.equals("()Ljava/lang/Object;")) {
                return false;
            } else if (name.equals("equals") && sig.equals("(Ljava/lang/Object;)Z")) {
                return false;
            } else if (name.equals("finalize") && sig.equals("()V")) {
                return false;
            } else if (name.equals("getClass") && sig.equals("()Ljava/lang/Class;")) {
                return false;
            } else if (name.equals("hashCode") && sig.equals("()I")) {
                return false;
            } else if (name.equals("notify") && sig.equals("()V")) {
                return false;
            } else if (name.equals("notifyAll") && sig.equals("()V")) {
                return false;
            } else if (name.equals("toString") && sig.equals("()Ljava/lang/String;")) {
                return false;
            } else if (name.equals("wait") && sig.equals("()V")) {
                return false;
            } else if (name.equals("wait") && sig.equals("(J)V")) {
                return false;
            } else if (name.equals("wait") && sig.equals("(JI)V")) {
                return false;
            }

            // FIXME: HACK EVEN MORE HACK!
            if (hack && this.jarIndex.containsObfEntry(obfEntry.getClassEntry()))
                return true;
        }

        return this.jarIndex.containsObfEntry(obfEntry);
    }

    public boolean isRenameable(EntryReference<Entry, Entry> obfReference, boolean activeHack) {
        return obfReference.isNamed() && isObfuscatedIdentifier(obfReference.getNameableEntry(), activeHack);
    }

    public boolean isRenameable(EntryReference<Entry, Entry> obfReference) {
        return isRenameable(obfReference, false);
    }

    // NOTE: these methods are a bit messy... oh well

    public boolean hasDeobfuscatedName(Entry obfEntry) {
        Translator translator = getTranslator(TranslationDirection.Deobfuscating);
        if (obfEntry instanceof ClassEntry) {
            ClassEntry obfClass = (ClassEntry) obfEntry;
            List<ClassMapping> mappingChain = this.mappings.getClassMappingChain(obfClass);
            ClassMapping classMapping = mappingChain.get(mappingChain.size() - 1);
            return classMapping != null && classMapping.getDeobfName() != null;
        } else if (obfEntry instanceof FieldEntry) {
            return translator.translate((FieldEntry) obfEntry) != null;
        } else if (obfEntry instanceof MethodEntry) {
            return translator.translate((MethodEntry) obfEntry) != null;
        } else if (obfEntry instanceof ConstructorEntry) {
            // constructors have no names
            return false;
        } else if (obfEntry instanceof ArgumentEntry) {
            return translator.translate((ArgumentEntry) obfEntry) != null;
        } else {
            throw new Error("Unknown entry type: " + obfEntry.getClass().getName());
        }
    }

    public void rename(Entry obfEntry, String newName) {
        if (obfEntry instanceof ClassEntry) {
            this.renamer.setClassName((ClassEntry) obfEntry, Descriptor.toJvmName(newName));
        } else if (obfEntry instanceof FieldEntry) {
            this.renamer.setFieldName((FieldEntry) obfEntry, newName);
        } else if (obfEntry instanceof MethodEntry) {
            this.renamer.setMethodTreeName((MethodEntry) obfEntry, newName);
        } else if (obfEntry instanceof ConstructorEntry) {
            throw new IllegalArgumentException("Cannot rename constructors");
        } else if (obfEntry instanceof ArgumentEntry) {
            this.renamer.setArgumentTreeName((ArgumentEntry) obfEntry, newName);
        } else {
            throw new Error("Unknown entry type: " + obfEntry.getClass().getName());
        }

        // clear caches
        this.translatorCache.clear();
    }

    public void removeMapping(Entry obfEntry) {
        if (obfEntry instanceof ClassEntry) {
            this.renamer.removeClassMapping((ClassEntry) obfEntry);
        } else if (obfEntry instanceof FieldEntry) {
            this.renamer.removeFieldMapping((FieldEntry) obfEntry);
        } else if (obfEntry instanceof MethodEntry) {
            this.renamer.removeMethodTreeMapping((MethodEntry) obfEntry);
        } else if (obfEntry instanceof ConstructorEntry) {
            throw new IllegalArgumentException("Cannot rename constructors");
        } else if (obfEntry instanceof ArgumentEntry) {
            this.renamer.removeArgumentMapping((ArgumentEntry) obfEntry);
        } else {
            throw new Error("Unknown entry type: " + obfEntry);
        }

        // clear caches
        this.translatorCache.clear();
    }

    public void markAsDeobfuscated(Entry obfEntry) {
        if (obfEntry instanceof ClassEntry) {
            this.renamer.markClassAsDeobfuscated((ClassEntry) obfEntry);
        } else if (obfEntry instanceof FieldEntry) {
            this.renamer.markFieldAsDeobfuscated((FieldEntry) obfEntry);
        } else if (obfEntry instanceof MethodEntry) {
            this.renamer.markMethodTreeAsDeobfuscated((MethodEntry) obfEntry);
        } else if (obfEntry instanceof ConstructorEntry) {
            throw new IllegalArgumentException("Cannot rename constructors");
        } else if (obfEntry instanceof ArgumentEntry) {
            this.renamer.markArgumentAsDeobfuscated((ArgumentEntry) obfEntry);
        } else {
            throw new Error("Unknown entry type: " + obfEntry);
        }

        // clear caches
        this.translatorCache.clear();
    }
}
