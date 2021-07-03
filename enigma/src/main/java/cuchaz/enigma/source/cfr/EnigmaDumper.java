package cuchaz.enigma.source.cfr;

import cuchaz.enigma.source.SourceIndex;
import cuchaz.enigma.source.SourceSettings;
import cuchaz.enigma.source.Token;
import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.mapping.EntryRemapper;
import cuchaz.enigma.translation.representation.MethodDescriptor;
import cuchaz.enigma.translation.representation.TypeDescriptor;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.Entry;
import cuchaz.enigma.translation.representation.entry.FieldEntry;
import cuchaz.enigma.translation.representation.entry.LocalVariableEntry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;
import org.benf.cfr.reader.bytecode.analysis.loc.HasByteCodeLoc;
import org.benf.cfr.reader.bytecode.analysis.types.JavaRefTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.JavaTypeInstance;
import org.benf.cfr.reader.bytecode.analysis.types.MethodPrototype;
import org.benf.cfr.reader.bytecode.analysis.variables.NamedVariable;
import org.benf.cfr.reader.entities.AccessFlag;
import org.benf.cfr.reader.entities.ClassFile;
import org.benf.cfr.reader.entities.ClassFileField;
import org.benf.cfr.reader.entities.Field;
import org.benf.cfr.reader.entities.Method;
import org.benf.cfr.reader.state.TypeUsageInformation;
import org.benf.cfr.reader.util.getopt.Options;
import org.benf.cfr.reader.util.output.Dumper;
import org.benf.cfr.reader.util.output.IllegalIdentifierDump;
import org.benf.cfr.reader.util.output.MovableDumperContext;
import org.benf.cfr.reader.util.output.StringStreamDumper;
import org.benf.cfr.reader.util.output.TypeContext;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class EnigmaDumper extends StringStreamDumper {
    private final StringBuilder sb;
    private final SourceSettings sourceSettings;
    private final SourceIndex index;
    private final @Nullable EntryRemapper mapper;
    private final Map<Object, Entry<?>> refs = new HashMap<>();
    private final TypeUsageInformation typeUsage;
    private final MovableDumperContext dumperContext;
    private boolean muteLine = false;
    private MethodEntry contextMethod = null;

    public EnigmaDumper(StringBuilder sb, SourceSettings sourceSettings, TypeUsageInformation typeUsage, Options options,
            @Nullable EntryRemapper mapper) {
        this(sb, sourceSettings, typeUsage, options, mapper, new SourceIndex(), new MovableDumperContext());
    }

    protected EnigmaDumper(StringBuilder sb, SourceSettings sourceSettings, TypeUsageInformation typeUsage, Options options,
            @Nullable EntryRemapper mapper, SourceIndex index, MovableDumperContext context) {
        super((m, e) -> {
        }, sb, typeUsage, options, IllegalIdentifierDump.Nop.getInstance(), context);
        this.sb = sb;
        this.sourceSettings = sourceSettings;
        this.typeUsage = typeUsage;
        this.mapper = mapper;
        this.dumperContext = context;
        this.index = index;
    }

    private MethodEntry getMethodEntry(MethodPrototype method) {
        if (method == null || method.getOwner() == null) {
            return null;
        }

        MethodDescriptor desc = new MethodDescriptor(method.getOriginalDescriptor());

        return new MethodEntry(getClassEntry(method.getOwner()), method.getName(), desc);
    }

    private LocalVariableEntry getParameterEntry(MethodPrototype method, int parameterIndex, String name) {
        MethodEntry owner = getMethodEntry(method);
        // params may be not computed if cfr creates a lambda expression fallback, e.g. in PointOfInterestSet
        if (owner == null || !method.parametersComputed()) {
            return null;
        }

        int variableIndex = method.getParameterLValues().get(parameterIndex).localVariable.getIdx();

        return new LocalVariableEntry(owner, variableIndex, name, true, null);
    }

    private FieldEntry getFieldEntry(JavaTypeInstance owner, String name, String desc) {
        return new FieldEntry(getClassEntry(owner), name, new TypeDescriptor(desc));
    }

    private ClassEntry getClassEntry(JavaTypeInstance type) {
        return new ClassEntry(type.getRawName().replace('.', '/'));
    }

    @Override
    public Dumper packageName(JavaRefTypeInstance t) {
        if (sourceSettings.removeImports) {
            return this;
        }
        return super.packageName(t);
    }

    @Override
    public Dumper keyword(String s) {
        if (sourceSettings.removeImports && s.startsWith("import")) {
            muteLine = true;
            return this;
        }
        return super.keyword(s);
    }

    @Override
    public Dumper endCodeln() {
        if (muteLine) {
            muteLine = false;
            return this;
        }
        return super.endCodeln();
    }

    @Override
    public Dumper print(String s) {
        if (muteLine) {
            return this;
        }
        return super.print(s);
    }

    @Override
    public Dumper dumpClassDoc(JavaTypeInstance owner) {
        if (mapper != null) {
            List<String> recordComponentDocs = new LinkedList<>();

            if (isRecord(owner)) {
                ClassFile classFile = ((JavaRefTypeInstance) owner).getClassFile();
                for (ClassFileField field : classFile.getFields()) {
                    if (field.getField().testAccessFlag(AccessFlag.ACC_STATIC)) {
                        continue;
                    }

                    EntryMapping mapping = mapper.getDeobfMapping(getFieldEntry(owner, field.getFieldName(), field.getField().getDescriptor()));
                    if (mapping == null) {
                        continue;
                    }

                    String javaDoc = mapping.getJavadoc();
                    if (javaDoc != null) {
                        recordComponentDocs.add(String.format("@param %s %s", field.getFieldName(), javaDoc));
                    }
                }
            }

            EntryMapping mapping = mapper.getDeobfMapping(getClassEntry(owner));

            String javadoc = null;
            if (mapping != null) {
                javadoc = mapping.getJavadoc();
            }

            if (javadoc != null || !recordComponentDocs.isEmpty()) {
                print("/**").newln();
                if (javadoc != null) {
                    for (String line : javadoc.split("\\R")) {
                        print(" * ").print(line).newln();
                    }

                    if (!recordComponentDocs.isEmpty()) {
                        print(" * ").newln();
                    }
                }

                for (String componentDoc : recordComponentDocs) {
                    print(" * ").print(componentDoc).newln();
                }

                print(" */").newln();
            }
        }
        return this;
    }

    @Override
    public Dumper dumpMethodDoc(MethodPrototype method) {
        if (mapper != null) {
            List<String> lines = new ArrayList<>();
            MethodEntry methodEntry = getMethodEntry(method);
            EntryMapping mapping = mapper.getDeobfMapping(methodEntry);
            if (mapping != null) {
                String javadoc = mapping.getJavadoc();
                if (javadoc != null) {
                    lines.addAll(Arrays.asList(javadoc.split("\\R")));
                }
            }

            Collection<Entry<?>> children = mapper.getObfChildren(methodEntry);

            if (children != null && !children.isEmpty()) {
                for (Entry<?> each : children) {
                    if (each instanceof LocalVariableEntry) {
                        EntryMapping paramMapping = mapper.getDeobfMapping(each);
                        if (paramMapping != null) {
                            String javadoc = paramMapping.getJavadoc();
                            if (javadoc != null) {
                                lines.addAll(Arrays.asList(("@param " + paramMapping.getTargetName() + " " + javadoc).split("\\R")));
                            }
                        }
                    }
                }
            }

            if (!lines.isEmpty()) {
                print("/**").newln();
                for (String line : lines) {
                    print(" * ").print(line).newln();
                }
                print(" */").newln();
            }
        }
        return this;
    }

    @Override
    public Dumper dumpFieldDoc(Field field, JavaTypeInstance owner) {
        boolean recordComponent = isRecord(owner) && !field.testAccessFlag(AccessFlag.ACC_STATIC);
        if (mapper != null && !recordComponent) {
            EntryMapping mapping = mapper.getDeobfMapping(getFieldEntry(owner, field.getFieldName(), field.getDescriptor()));
            if (mapping != null) {
                String javadoc = mapping.getJavadoc();
                if (javadoc != null) {
                    print("/**").newln();
                    for (String line : javadoc.split("\\R")) {
                        print(" * ").print(line).newln();
                    }
                    print(" */").newln();
                }
            }
        }
        return this;
    }

    @Override
    public Dumper methodName(String name, MethodPrototype method, boolean special, boolean defines) {
        Entry<?> entry = getMethodEntry(method);
        super.methodName(name, method, special, defines);
        int now = sb.length();
        Token token = new Token(now - name.length(), now, name);

        if (entry != null) {
            if (defines) {
                index.addDeclaration(token, entry); // override as cfr reuses local vars
            } else {
                index.addReference(token, entry, contextMethod);
            }
        }

        return this;
    }

    @Override
    public Dumper parameterName(String name, Object ref, MethodPrototype method, int index, boolean defines) {
        super.parameterName(name, ref, method, index, defines);
        int now = sb.length();
        Token token = new Token(now - name.length(), now, name);
        Entry<?> entry;
        if (defines) {
            refs.put(ref, entry = getParameterEntry(method, index, name));
        } else {
            entry = refs.get(ref);
        }

        if (entry != null) {
            if (defines) {
                this.index.addDeclaration(token, entry);
            } else {
                this.index.addReference(token, entry, contextMethod);
            }
        }

        return this;
    }

    @Override
    public Dumper variableName(String name, NamedVariable variable, boolean defines) {
        // todo catch var declarations in the future
        return super.variableName(name, variable, defines);
    }

    @Override
    public Dumper identifier(String name, Object ref, boolean defines) {
        super.identifier(name, ref, defines);
        Entry<?> entry;
        if (defines) {
            refs.remove(ref);
            return this;
        }
        if ((entry = refs.get(ref)) == null) {
            return this;
        }
        int now = sb.length();
        Token token = new Token(now - name.length(), now, name);
        index.addReference(token, entry, contextMethod);
        return this;
    }

    @Override
    public Dumper fieldName(String name, String descriptor, JavaTypeInstance owner, boolean hiddenDeclaration, boolean isStatic, boolean defines) {
        super.fieldName(name, descriptor, owner, hiddenDeclaration, isStatic, defines);
        int now = sb.length();
        Token token = new Token(now - name.length(), now, name);
        if (descriptor != null) {
            Entry<?> entry = getFieldEntry(owner, name, descriptor);

            if (defines) {
                index.addDeclaration(token, entry);
            } else {
                index.addReference(token, entry, contextMethod);
            }
        }

        return this;
    }

    @Override
    public Dumper dump(JavaTypeInstance type) {
        dumpClass(TypeContext.None, type, false);
        return this;
    }

    @Override
    public Dumper dump(JavaTypeInstance type, boolean defines) {
        dumpClass(TypeContext.None, type, defines);
        return this;
    }

    @Override
    public Dumper dump(JavaTypeInstance type, TypeContext context) {
        dumpClass(context, type, false);
        return this;
    }

    private void dumpClass(TypeContext context, JavaTypeInstance type, boolean defines) {
        if (type instanceof JavaRefTypeInstance) {
            type.dumpInto(this, typeUsage, context);
            String name = typeUsage.getName(type, context); // the actually used name, dump will indent
            int now = sb.length();
            Token token = new Token(now - name.length(), now, name);

            if (defines) {
                index.addDeclaration(token, getClassEntry(type));
            } else {
                index.addReference(token, getClassEntry(type), contextMethod);
            }
            return;
        }

        type.dumpInto(this, typeUsage, context);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Otherwise the type usage override dumper will not go through the type instance dump
     * we have here.
     */
    @Override
    public Dumper withTypeUsageInformation(TypeUsageInformation innerclassTypeUsageInformation) {
        return new EnigmaDumper(this.sb, sourceSettings, innerclassTypeUsageInformation, options, mapper, index, dumperContext);
    }

    @Override
    public void informBytecodeLoc(HasByteCodeLoc loc) {
        Collection<Method> methods = loc.getLoc().getMethods();
        if (!methods.isEmpty()) {
            this.contextMethod = getMethodEntry(methods.iterator().next().getMethodPrototype());
        }
    }

    public SourceIndex getIndex() {
        index.setSource(getString());
        return index;
    }

    public String getString() {
        return sb.toString();
    }

    private boolean isRecord(JavaTypeInstance javaTypeInstance) {
        if (javaTypeInstance instanceof JavaRefTypeInstance) {
            ClassFile classFile = ((JavaRefTypeInstance) javaTypeInstance).getClassFile();
            return classFile.getClassSignature().getSuperClass().getRawName().equals("java.lang.Record");
        }

        return false;
    }

}
