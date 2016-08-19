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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.strobel.assembler.metadata.Buffer;
import com.strobel.assembler.metadata.ClasspathTypeLoader;
import com.strobel.assembler.metadata.ITypeLoader;
import cuchaz.enigma.analysis.BridgeMarker;
import cuchaz.enigma.analysis.JarIndex;
import cuchaz.enigma.bytecode.*;
import cuchaz.enigma.mapping.ClassEntry;
import cuchaz.enigma.mapping.Translator;
import cuchaz.enigma.mapping.javadoc.JavaDocMapping;
import javassist.*;
import javassist.bytecode.Descriptor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class TranslatingTypeLoader implements ITypeLoader {

    private JarFile jar;
    private JarIndex jarIndex;
    private Translator obfuscatingTranslator;
    private Translator deobfuscatingTranslator;
    private Map<String, byte[]> cache;
    private ClasspathTypeLoader defaultTypeLoader;
    private JavaDocMapping docMapping;

    public TranslatingTypeLoader(JarFile jar, JarIndex jarIndex, Translator obfuscatingTranslator,
            Translator deobfuscatingTranslator, JavaDocMapping docMapping) {
        this.jar = jar;
        this.jarIndex = jarIndex;
        this.obfuscatingTranslator = obfuscatingTranslator;
        this.deobfuscatingTranslator = deobfuscatingTranslator;
        this.cache = Maps.newHashMap();
        this.defaultTypeLoader = new ClasspathTypeLoader();
        this.docMapping = docMapping;
        this.docMapping.cleanBehaviors();
    }

    public void clearCache() {
        this.cache.clear();
    }

    @Override
    public boolean tryLoadType(String className, Buffer out) {

        // check the cache
        byte[] data;
        if (this.cache.containsKey(className)) {
            data = this.cache.get(className);
        } else {
            data = loadType(className);
            this.cache.put(className, data);
        }

        if (data == null) {
            // chain to default type loader
            return this.defaultTypeLoader.tryLoadType(className, out);
        }

        // send the class to the decompiler
        out.reset(data.length);
        System.arraycopy(data, 0, out.array(), out.position(), data.length);
        out.position(0);
        return true;
    }

    public CtClass loadClass(String deobfClassName) {

        byte[] data = loadType(deobfClassName);
        if (data == null) {
            return null;
        }

        // return a javassist handle for the class
        String javaClassFileName = Descriptor.toJavaName(deobfClassName);
        ClassPool classPool = new ClassPool();
        classPool.insertClassPath(new ByteArrayClassPath(javaClassFileName, data));
        try {
            return classPool.get(javaClassFileName);
        } catch (NotFoundException ex) {
            throw new Error(ex);
        }
    }

    private byte[] loadType(String className) {

        // NOTE: don't know if class name is obf or deobf
        ClassEntry classEntry = new ClassEntry(className);
        ClassEntry obfClassEntry = this.obfuscatingTranslator.translateEntry(classEntry);

        // is this an inner class referenced directly? (ie trying to load b instead of a$b)
        if (!obfClassEntry.isInnerClass()) {
            List<ClassEntry> classChain = this.jarIndex.getObfClassChain(obfClassEntry);
            if (classChain.size() > 1) {
                System.err.println(String.format("WARNING: no class %s after inner class reconstruction. Try %s",
                        className, obfClassEntry.buildClassEntry(classChain)
                ));
                return null;
            }
        }

        // is this a class we should even know about?
        if (!this.jarIndex.containsObfClass(obfClassEntry)) {
            return null;
        }

        // DEBUG
        //System.out.println(String.format("Looking for %s (obf: %s)", classEntry.getName(), obfClassEntry.getName()));

        // find the class in the jar
        String classInJarName = findClassInJar(obfClassEntry);
        if (classInJarName == null) {
            // couldn't find it
            return null;
        }

        try {
            // read the class file into a buffer
            ByteArrayOutputStream data = new ByteArrayOutputStream();
            byte[] buf = new byte[1024 * 1024]; // 1 KiB
            InputStream in = this.jar.getInputStream(this.jar.getJarEntry(classInJarName + ".class"));
            while (true) {
                int bytesRead = in.read(buf);
                if (bytesRead <= 0) {
                    break;
                }
                data.write(buf, 0, bytesRead);
            }
            data.close();
            in.close();
            buf = data.toByteArray();

            // load the javassist handle to the raw class
            ClassPool classPool = new ClassPool();
            String classInJarJavaName = Descriptor.toJavaName(classInJarName);
            classPool.insertClassPath(new ByteArrayClassPath(classInJarJavaName, buf));
            CtClass c = classPool.get(classInJarJavaName);

            c = transformClass(c);

            // sanity checking
            assertClassName(c, classEntry);

            // DEBUG
            //Util.writeClass( c );

            // we have a transformed class!
            return c.toBytecode();
        } catch (IOException | NotFoundException | CannotCompileException ex) {
            throw new Error(ex);
        }
    }

    private String findClassInJar(ClassEntry obfClassEntry) {

        // try to find the class in the jar
        for (String className : getClassNamesToTry(obfClassEntry)) {
            JarEntry jarEntry = this.jar.getJarEntry(className + ".class");
            if (jarEntry != null) {
                return className;
            }
        }

        // didn't find it  ;_;
        return null;
    }

    public List<String> getClassNamesToTry(String className) {
        return getClassNamesToTry(this.obfuscatingTranslator.translateEntry(new ClassEntry(className)));
    }

    public List<String> getClassNamesToTry(ClassEntry obfClassEntry) {
        List<String> classNamesToTry = Lists.newArrayList();
        classNamesToTry.add(obfClassEntry.getName());
        if (obfClassEntry.isInnerClass()) {
            // try just the inner class name
            classNamesToTry.add(obfClassEntry.getInnermostClassName());
        }
        return classNamesToTry;
    }

    public CtClass transformClass(CtClass c)
            throws IOException, NotFoundException, CannotCompileException {

        // reconstruct inner classes
        new InnerClassWriter(this.jarIndex).write(c);

        // re-get the javassist handle since we changed class names
        ClassEntry obfClassEntry = new ClassEntry(Descriptor.toJvmName(c.getName()));
        String javaClassReconstructedName = Descriptor.toJavaName(obfClassEntry.getName());
        ClassPool classPool = new ClassPool();
        classPool.insertClassPath(new ByteArrayClassPath(javaClassReconstructedName, c.toBytecode()));
        c = classPool.get(javaClassReconstructedName);

        // check that the file is correct after inner class reconstruction (ie cause Javassist to fail fast if something is wrong)
        assertClassName(c, obfClassEntry);

        // do all kinds of deobfuscating transformations on the class
        new BridgeMarker(this.jarIndex).markBridges(c);
        new MethodParameterWriter(this.deobfuscatingTranslator).writeMethodArguments(c);
        new LocalVariableRenamer(this.deobfuscatingTranslator).rename(c);
        new ClassTranslator(this.deobfuscatingTranslator, this.docMapping).translate(c);
        return c;
    }

    private void assertClassName(CtClass c, ClassEntry obfClassEntry) {
        String name1 = Descriptor.toJvmName(c.getName());
        assert (name1.equals(obfClassEntry.getName())) : String.format("Looking for %s, instead found %s", obfClassEntry.getName(), name1);

        String name2 = Descriptor.toJvmName(c.getClassFile().getName());
        assert (name2.equals(obfClassEntry.getName())) : String.format("Looking for %s, instead found %s", obfClassEntry.getName(), name2);
    }
}
