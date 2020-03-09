package cuchaz.enigma.source.cfr;

import cuchaz.enigma.analysis.Token;
import cuchaz.enigma.source.SourceIndex;
import cuchaz.enigma.translation.representation.MethodDescriptor;
import cuchaz.enigma.translation.representation.TypeDescriptor;
import cuchaz.enigma.translation.representation.entry.*;
import org.benf.cfr.reader.bytecode.analysis.types.*;
import org.benf.cfr.reader.bytecode.analysis.variables.NamedVariable;
import org.benf.cfr.reader.entities.Field;
import org.benf.cfr.reader.entities.Method;
import org.benf.cfr.reader.mapping.NullMapping;
import org.benf.cfr.reader.mapping.ObfuscationMapping;
import org.benf.cfr.reader.state.TypeUsageInformation;
import org.benf.cfr.reader.util.collections.SetFactory;
import org.benf.cfr.reader.util.output.DelegatingDumper;
import org.benf.cfr.reader.util.output.Dumpable;
import org.benf.cfr.reader.util.output.Dumper;
import org.benf.cfr.reader.util.output.TypeContext;

import java.util.Set;
import java.util.stream.Collectors;

public class EnigmaDumper implements Dumper {
    private int outputCount = 0;
    private int indent;
    private boolean atStart = true;
    private boolean pendingCR = false;
    private final StringBuilder sb = new StringBuilder();
    private final TypeUsageInformation typeUsageInformation;
    private final Set<JavaTypeInstance> emitted = SetFactory.newSet();
    private final SourceIndex index = new SourceIndex();
    private int position;


    public EnigmaDumper(TypeUsageInformation typeUsageInformation) {
        this.typeUsageInformation = typeUsageInformation;
    }

    private void append(String s) {
        sb.append(s);
        position += s.length();
    }

    private String getDesc(JavaTypeInstance type) {
        type = type.getDeGenerifiedType();

        if (type instanceof JavaRefTypeInstance) {
            return "L" + type.getRawName().replace('.', '/') + ";";
        }

        if (type instanceof JavaArrayTypeInstance) {
            return "[" + getDesc(((JavaArrayTypeInstance) type).removeAnArrayIndirection());
        }

        if (type instanceof RawJavaType) {
            switch ((RawJavaType) type) {
                case BOOLEAN:
                    return "Z";
                case BYTE:
                    return "B";
                case CHAR:
                    return "C";
                case SHORT:
                    return "S";
                case INT:
                    return "I";
                case LONG:
                    return "J";
                case FLOAT:
                    return "F";
                case DOUBLE:
                    return "D";
                case VOID:
                    return "V";
                default:
                    throw new AssertionError();
            }
        }

        throw new AssertionError();
    }

    private MethodEntry getMethodEntry(MethodPrototype method) {
        if (method.getClassType() == null) {
            return null;
        }

        MethodDescriptor desc = new MethodDescriptor(
                method.getArgs().stream().map(type -> new TypeDescriptor(getDesc(type))).collect(Collectors.toList()),
                new TypeDescriptor(method.getName().equals("<init>") || method.getName().equals("<clinit>") ? "V" : getDesc(method.getReturnType()))
        );

        return new MethodEntry(getClassEntry(method.getClassType()), method.getName(), desc);
    }

    private LocalVariableEntry getParameterEntry(MethodPrototype method, int parameterIndex, String name) {
        int variableIndex = method.isInstanceMethod() ? 1 : 0;
        for (int i = 0; i < parameterIndex; i++) {
            variableIndex += method.getArgs().get(parameterIndex).getStackType().getComputationCategory();
        }

        return new LocalVariableEntry(getMethodEntry(method), variableIndex, name, true, null);
    }

    private FieldEntry getFieldEntry(JavaTypeInstance owner, String name, JavaTypeInstance type) {
        return new FieldEntry(getClassEntry(owner), name, new TypeDescriptor(getDesc(type)));
    }

    private ClassEntry getClassEntry(JavaTypeInstance type) {
        return new ClassEntry(type.getRawName().replace('.', '/'));
    }

    @Override
    public Dumper beginBlockComment(boolean inline) {
        print("/*").newln();
        return this;
    }

    @Override
    public Dumper endBlockComment() {
        print(" */").newln();
        return this;
    }

    @Override
    public Dumper label(String s, boolean inline) {
        processPendingCR();
        append(s);
        append(":");
        return this;
    }

    @Override
    public Dumper comment(String s) {
        append("// ");
        append(s);
        append("\n");
        return this;
    }

    @Override
    public void enqueuePendingCarriageReturn() {
        pendingCR = true;
    }

    @Override
    public Dumper removePendingCarriageReturn() {
        pendingCR = false;
        return this;
    }

    private void processPendingCR() {
        if (pendingCR) {
            append("\n");
            atStart = true;
            pendingCR = false;
        }
    }

    @Override
    public Dumper identifier(String s, Object ref, boolean defines) {
        return print(s);
    }

    @Override
    public Dumper methodName(String name, MethodPrototype method, boolean special, boolean defines) {
        doIndent();
        Token token = new Token(position, position + name.length(), name);
        Entry<?> entry = getMethodEntry(method);

        if (entry != null) {
            if (defines) {
                index.addDeclaration(token, entry);
            } else {
                index.addReference(token, entry, null);
            }
        }

        return identifier(name, null, defines);
    }

    @Override
    public Dumper parameterName(String name, MethodPrototype method, int index, boolean defines) {
        doIndent();
        Token token = new Token(position, position + name.length(), name);
        Entry<?> entry = getParameterEntry(method, index, name);

        if (entry != null) {
            if (defines) {
                this.index.addDeclaration(token, entry);
            } else {
                this.index.addReference(token, entry, null);
            }
        }

        return identifier(name, null, defines);
    }

    @Override
    public Dumper variableName(String name, NamedVariable variable, boolean defines) {
        return identifier(name, null, defines);
    }

    @Override
    public Dumper packageName(JavaRefTypeInstance t) {
        String s = t.getPackageName();

        if (!s.isEmpty()) {
            keyword("package ").print(s).endCodeln().newln();
        }

        return this;
    }

    @Override
    public Dumper fieldName(String name, Field field, JavaTypeInstance owner, boolean hiddenDeclaration, boolean defines) {
        doIndent();
        Token token = new Token(position, position + name.length(), name);
        Entry<?> entry = field == null ? null : getFieldEntry(owner, name, field.getJavaTypeInstance());

        if (entry != null) {
            if (defines) {
                index.addDeclaration(token, entry);
            } else {
                index.addReference(token, entry, null);
            }
        }

        identifier(name, null, defines);
        return this;
    }

    @Override
    public Dumper print(String s) {
        processPendingCR();
        doIndent();
        append(s);
        atStart = s.endsWith("\n");
        outputCount++;
        return this;
    }

    @Override
    public Dumper print(char c) {
        return print(String.valueOf(c));
    }

    @Override
    public Dumper newln() {
        append("\n");
        atStart = true;
        outputCount++;
        return this;
    }

    @Override
    public Dumper endCodeln() {
        append(";\n");
        atStart = true;
        outputCount++;
        return this;
    }

    @Override
    public Dumper keyword(String s) {
        print(s);
        return this;
    }

    @Override
    public Dumper operator(String s) {
        print(s);
        return this;
    }

    @Override
    public Dumper separator(String s) {
        print(s);
        return this;
    }

    @Override
    public Dumper literal(String s, Object o) {
        print(s);
        return this;
    }

    private void doIndent() {
        if (!atStart) return;
        String indents = "    ";

        for (int x = 0; x < indent; ++x) {
            append(indents);
        }

        atStart = false;
    }

    @Override
    public void indent(int diff) {
        indent += diff;
    }

    @Override
    public Dumper dump(Dumpable d) {
        if (d == null) {
            keyword("null");
            return this;
        }

        d.dump(this);
        return this;
    }

    @Override
    public TypeUsageInformation getTypeUsageInformation() {
        return typeUsageInformation;
    }

    @Override
    public ObfuscationMapping getObfuscationMapping() {
        return NullMapping.INSTANCE;
    }

    @Override
    public String toString() {
        return sb.toString();
    }

    @Override
    public void addSummaryError(Method method, String s) {}

    @Override
    public void close() {
    }

    @Override
    public boolean canEmitClass(JavaTypeInstance type) {
        return emitted.add(type);
    }

    @Override
    public int getOutputCount() {
        return outputCount;
    }

    @Override
    public Dumper dump(JavaTypeInstance type) {
        return dump(type, TypeContext.None, false);
    }

    @Override
    public Dumper dump(JavaTypeInstance type, boolean defines) {
        return dump(type, TypeContext.None, false);
    }

    @Override
    public Dumper dump(JavaTypeInstance type, TypeContext context) {
        return dump(type, context, false);
    }

    private Dumper dump(JavaTypeInstance type, TypeContext context, boolean defines) {
        doIndent();
        if (type instanceof JavaRefTypeInstance) {
            int start = position;
            type.dumpInto(this, typeUsageInformation, TypeContext.None);
            int end = position;
            Token token = new Token(start, end, sb.toString().substring(start, end));

            if (defines) {
                index.addDeclaration(token, getClassEntry(type));
            } else {
                index.addReference(token, getClassEntry(type), null);
            }

            return this;
        }

        type.dumpInto(this, typeUsageInformation, context);
        return this;
    }

    @Override
    public Dumper withTypeUsageInformation(TypeUsageInformation innerclassTypeUsageInformation) {
        return new WithTypeUsageInformationDumper(this, innerclassTypeUsageInformation);
    }

    public SourceIndex getIndex() {
        index.setSource(getString());
        return index;
    }

    public String getString() {
        return sb.toString();
    }

    public static class WithTypeUsageInformationDumper extends DelegatingDumper {
        private final TypeUsageInformation typeUsageInformation;

        WithTypeUsageInformationDumper(Dumper delegate, TypeUsageInformation typeUsageInformation) {
            super(delegate);
            this.typeUsageInformation = typeUsageInformation;
        }

        @Override
        public TypeUsageInformation getTypeUsageInformation() {
            return typeUsageInformation;
        }

        @Override
        public Dumper dump(JavaTypeInstance javaTypeInstance) {
            return dump(javaTypeInstance, TypeContext.None);
        }

        @Override
        public Dumper dump(JavaTypeInstance javaTypeInstance, TypeContext typeContext) {
            javaTypeInstance.dumpInto(this, typeUsageInformation, typeContext);
            return this;
        }

        @Override
        public Dumper withTypeUsageInformation(TypeUsageInformation innerclassTypeUsageInformation) {
            return new WithTypeUsageInformationDumper(delegate, innerclassTypeUsageInformation);
        }
    }
}
