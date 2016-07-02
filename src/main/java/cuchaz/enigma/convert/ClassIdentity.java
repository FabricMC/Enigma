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
package cuchaz.enigma.convert;

import com.google.common.collect.*;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cuchaz.enigma.Constants;
import cuchaz.enigma.Util;
import cuchaz.enigma.analysis.ClassImplementationsTreeNode;
import cuchaz.enigma.analysis.EntryReference;
import cuchaz.enigma.analysis.JarIndex;
import cuchaz.enigma.bytecode.ConstPoolEditor;
import cuchaz.enigma.bytecode.InfoType;
import cuchaz.enigma.bytecode.accessors.ConstInfoAccessor;
import cuchaz.enigma.convert.ClassNamer.SidedClassNamer;
import cuchaz.enigma.mapping.*;
import javassist.*;
import javassist.bytecode.*;
import javassist.expr.*;

public class ClassIdentity {

    private ClassEntry classEntry;
    private SidedClassNamer namer;
    private Multiset<String> fields;
    private Multiset<String> methods;
    private Multiset<String> constructors;
    private String staticInitializer;
    private String extendz;
    private Multiset<String> implementz;
    private Set<String> stringLiterals;
    private Multiset<String> implementations;
    private Multiset<String> references;
    private String outer;

    private final ClassNameReplacer m_classNameReplacer = new ClassNameReplacer() {

        private Map<String, String> m_classNames = Maps.newHashMap();

        @Override
        public String replace(String className) {

            // classes not in the none package can be passed through
            ClassEntry classEntry = new ClassEntry(className);
            if (!classEntry.getPackageName().equals(Constants.NonePackage)) {
                return className;
            }

            // is this class ourself?
            if (className.equals(classEntry.getName())) {
                return "CSelf";
            }

            // try the namer
            if (namer != null) {
                String newName = namer.getName(className);
                if (newName != null) {
                    return newName;
                }
            }

            // otherwise, use local naming
            if (!m_classNames.containsKey(className)) {
                m_classNames.put(className, getNewClassName());
            }
            return m_classNames.get(className);
        }

        private String getNewClassName() {
            return String.format("C%03d", m_classNames.size());
        }
    };

    public ClassIdentity(CtClass c, SidedClassNamer namer, JarIndex index, boolean useReferences) {
        this.namer = namer;

        // stuff from the bytecode

        this.classEntry = EntryFactory.getClassEntry(c);
        this.fields = HashMultiset.create();
        for (CtField field : c.getDeclaredFields()) {
            this.fields.add(scrubType(field.getSignature()));
        }
        this.methods = HashMultiset.create();
        for (CtMethod method : c.getDeclaredMethods()) {
            this.methods.add(scrubSignature(method.getSignature()) + "0x" + getBehaviorSignature(method));
        }
        this.constructors = HashMultiset.create();
        for (CtConstructor constructor : c.getDeclaredConstructors()) {
            this.constructors.add(scrubSignature(constructor.getSignature()) + "0x" + getBehaviorSignature(constructor));
        }
        this.staticInitializer = "";
        if (c.getClassInitializer() != null) {
            this.staticInitializer = getBehaviorSignature(c.getClassInitializer());
        }
        this.extendz = "";
        if (c.getClassFile().getSuperclass() != null) {
            this.extendz = scrubClassName(Descriptor.toJvmName(c.getClassFile().getSuperclass()));
        }
        this.implementz = HashMultiset.create();
        for (String interfaceName : c.getClassFile().getInterfaces()) {
            this.implementz.add(scrubClassName(Descriptor.toJvmName(interfaceName)));
        }

        this.stringLiterals = Sets.newHashSet();
        ConstPool constants = c.getClassFile().getConstPool();
        for (int i = 1; i < constants.getSize(); i++) {
            if (constants.getTag(i) == ConstPool.CONST_String) {
                this.stringLiterals.add(constants.getStringInfo(i));
            }
        }

        // stuff from the jar index

        this.implementations = HashMultiset.create();
        ClassImplementationsTreeNode implementationsNode = index.getClassImplementations(null, this.classEntry);
        if (implementationsNode != null) {
            @SuppressWarnings("unchecked")
            Enumeration<ClassImplementationsTreeNode> implementations = implementationsNode.children();
            while (implementations.hasMoreElements()) {
                ClassImplementationsTreeNode node = implementations.nextElement();
                this.implementations.add(scrubClassName(node.getClassEntry().getName()));
            }
        }

        this.references = HashMultiset.create();
        if (useReferences) {
            for (CtField field : c.getDeclaredFields()) {
                FieldEntry fieldEntry = EntryFactory.getFieldEntry(field);
                index.getFieldReferences(fieldEntry).forEach(this::addReference);
            }
            for (CtBehavior behavior : c.getDeclaredBehaviors()) {
                BehaviorEntry behaviorEntry = EntryFactory.getBehaviorEntry(behavior);
                index.getBehaviorReferences(behaviorEntry).forEach(this::addReference);
            }
        }

        this.outer = null;
        if (this.classEntry.isInnerClass()) {
            this.outer = this.classEntry.getOuterClassName();
        }
    }

    private void addReference(EntryReference<? extends Entry, BehaviorEntry> reference) {
        if (reference.context.getSignature() != null) {
            this.references.add(String.format("%s_%s",
                    scrubClassName(reference.context.getClassName()),
                    scrubSignature(reference.context.getSignature())
            ));
        } else {
            this.references.add(String.format("%s_<clinit>",
                    scrubClassName(reference.context.getClassName())
            ));
        }
    }

    public ClassEntry getClassEntry() {
        return this.classEntry;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("class: ");
        buf.append(this.classEntry.getName());
        buf.append(" ");
        buf.append(hashCode());
        buf.append("\n");
        for (String field : this.fields) {
            buf.append("\tfield ");
            buf.append(field);
            buf.append("\n");
        }
        for (String method : this.methods) {
            buf.append("\tmethod ");
            buf.append(method);
            buf.append("\n");
        }
        for (String constructor : this.constructors) {
            buf.append("\tconstructor ");
            buf.append(constructor);
            buf.append("\n");
        }
        if (this.staticInitializer.length() > 0) {
            buf.append("\tinitializer ");
            buf.append(this.staticInitializer);
            buf.append("\n");
        }
        if (this.extendz.length() > 0) {
            buf.append("\textends ");
            buf.append(this.extendz);
            buf.append("\n");
        }
        for (String interfaceName : this.implementz) {
            buf.append("\timplements ");
            buf.append(interfaceName);
            buf.append("\n");
        }
        for (String implementation : this.implementations) {
            buf.append("\timplemented by ");
            buf.append(implementation);
            buf.append("\n");
        }
        for (String reference : this.references) {
            buf.append("\treference ");
            buf.append(reference);
            buf.append("\n");
        }
        buf.append("\touter ");
        buf.append(this.outer);
        buf.append("\n");
        return buf.toString();
    }

    private String scrubClassName(String className) {
        return m_classNameReplacer.replace(className);
    }

    private String scrubType(String typeName) {
        return scrubType(new Type(typeName)).toString();
    }

    private Type scrubType(Type type) {
        if (type.hasClass()) {
            return new Type(type, m_classNameReplacer);
        } else {
            return type;
        }
    }

    private String scrubSignature(String signature) {
        return scrubSignature(new Signature(signature)).toString();
    }

    private Signature scrubSignature(Signature signature) {
        return new Signature(signature, m_classNameReplacer);
    }

    private boolean isClassMatchedUniquely(String className) {
        return this.namer != null && this.namer.getName(Descriptor.toJvmName(className)) != null;
    }

    private String getBehaviorSignature(CtBehavior behavior) {
        try {
            // does this method have an implementation?
            if (behavior.getMethodInfo().getCodeAttribute() == null) {
                return "(none)";
            }

            // compute the hash from the opcodes
            ConstPool constants = behavior.getMethodInfo().getConstPool();
            final MessageDigest digest = MessageDigest.getInstance("MD5");
            CodeIterator iter = behavior.getMethodInfo().getCodeAttribute().iterator();
            while (iter.hasNext()) {
                int pos = iter.next();

                // update the hash with the opcode
                int opcode = iter.byteAt(pos);
                digest.update((byte) opcode);

                switch (opcode) {
                    case Opcode.LDC: {
                        int constIndex = iter.byteAt(pos + 1);
                        updateHashWithConstant(digest, constants, constIndex);
                    }
                    break;

                    case Opcode.LDC_W:
                    case Opcode.LDC2_W: {
                        int constIndex = (iter.byteAt(pos + 1) << 8) | iter.byteAt(pos + 2);
                        updateHashWithConstant(digest, constants, constIndex);
                    }
                    break;
                }
            }

            // update hash with method and field accesses
            behavior.instrument(new ExprEditor() {
                @Override
                public void edit(MethodCall call) {
                    updateHashWithString(digest, scrubClassName(Descriptor.toJvmName(call.getClassName())));
                    updateHashWithString(digest, scrubSignature(call.getSignature()));
                    if (isClassMatchedUniquely(call.getClassName())) {
                        updateHashWithString(digest, call.getMethodName());
                    }
                }

                @Override
                public void edit(FieldAccess access) {
                    updateHashWithString(digest, scrubClassName(Descriptor.toJvmName(access.getClassName())));
                    updateHashWithString(digest, scrubType(access.getSignature()));
                    if (isClassMatchedUniquely(access.getClassName())) {
                        updateHashWithString(digest, access.getFieldName());
                    }
                }

                @Override
                public void edit(ConstructorCall call) {
                    updateHashWithString(digest, scrubClassName(Descriptor.toJvmName(call.getClassName())));
                    updateHashWithString(digest, scrubSignature(call.getSignature()));
                }

                @Override
                public void edit(NewExpr expr) {
                    updateHashWithString(digest, scrubClassName(Descriptor.toJvmName(expr.getClassName())));
                }
            });

            // convert the hash to a hex string
            return toHex(digest.digest());
        } catch (BadBytecode | NoSuchAlgorithmException | CannotCompileException ex) {
            throw new Error(ex);
        }
    }

    private void updateHashWithConstant(MessageDigest digest, ConstPool constants, int index) {
        ConstPoolEditor editor = new ConstPoolEditor(constants);
        ConstInfoAccessor item = editor.getItem(index);
        if (item.getType() == InfoType.StringInfo) {
            updateHashWithString(digest, constants.getStringInfo(index));
        }
        // TODO: other constants
    }

    private void updateHashWithString(MessageDigest digest, String val) {
        try {
            digest.update(val.getBytes("UTF8"));
        } catch (UnsupportedEncodingException ex) {
            throw new Error(ex);
        }
    }

    private String toHex(byte[] bytes) {
        // function taken from:
        // http://stackoverflow.com/questions/9655181/convert-from-byte-array-to-hex-string-in-java
        final char[] hexArray = "0123456789ABCDEF".toCharArray();
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ClassIdentity && equals((ClassIdentity) other);
    }

    public boolean equals(ClassIdentity other) {
        return this.fields.equals(other.fields)
                && this.methods.equals(other.methods)
                && this.constructors.equals(other.constructors)
                && this.staticInitializer.equals(other.staticInitializer)
                && this.extendz.equals(other.extendz)
                && this.implementz.equals(other.implementz)
                && this.implementations.equals(other.implementations)
                && this.references.equals(other.references);
    }

    @Override
    public int hashCode() {
        List<Object> objs = Lists.newArrayList();
        objs.addAll(this.fields);
        objs.addAll(this.methods);
        objs.addAll(this.constructors);
        objs.add(this.staticInitializer);
        objs.add(this.extendz);
        objs.addAll(this.implementz);
        objs.addAll(this.implementations);
        objs.addAll(this.references);
        return Util.combineHashesOrdered(objs);
    }

    public int getMatchScore(ClassIdentity other) {
        return 2 * getNumMatches(this.extendz, other.extendz)
                + 2 * getNumMatches(this.outer, other.outer)
                + 2 * getNumMatches(this.implementz, other.implementz)
                + getNumMatches(this.stringLiterals, other.stringLiterals)
                + getNumMatches(this.fields, other.fields)
                + getNumMatches(this.methods, other.methods)
                + getNumMatches(this.constructors, other.constructors);
    }

    public int getMaxMatchScore() {
        return 2 + 2 + 2 * this.implementz.size() + this.stringLiterals.size() + this.fields.size() + this.methods.size() + this.constructors.size();
    }

    public boolean matches(CtClass c) {
        // just compare declaration counts
        return this.fields.size() == c.getDeclaredFields().length
                && this.methods.size() == c.getDeclaredMethods().length
                && this.constructors.size() == c.getDeclaredConstructors().length;
    }

    private int getNumMatches(Set<String> a, Set<String> b) {
        int numMatches = 0;
        for (String val : a) {
            if (b.contains(val)) {
                numMatches++;
            }
        }
        return numMatches;
    }

    private int getNumMatches(Multiset<String> a, Multiset<String> b) {
        int numMatches = 0;
        for (String val : a) {
            if (b.contains(val)) {
                numMatches++;
            }
        }
        return numMatches;
    }

    private int getNumMatches(String a, String b) {
        if (a == null && b == null) {
            return 1;
        } else if (a != null && b != null && a.equals(b)) {
            return 1;
        }
        return 0;
    }
}
