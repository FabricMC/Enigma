package cuchaz.enigma.mapping.javadoc;

import com.strobel.decompiler.languages.java.ast.*;
import cuchaz.enigma.mapping.*;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.CtField;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ConstPool;
import javassist.bytecode.annotation.*;
import javassist.bytecode.annotation.Annotation;

import java.io.File;
import java.util.*;

/**
 * Representation of a javadoc mapping
 * Created by Thog
 * 19/08/2016
 */
public class JavaDocMapping
{
    private final Map<ClassEntry, JavaDocClass>   javaDocClassByID;
    private final Map<FieldEntry, JavaDocField>   javaDocFieldByID;
    private final Map<BehaviorEntry, JavaDocMethod> javaDocMethodByID;
    private final List<BehaviorEntry> behaviorByID;
    private Translator translator;

    public JavaDocMapping()
    {
        this.javaDocClassByID = new HashMap<>();
        this.javaDocFieldByID = new HashMap<>();
        this.javaDocMethodByID = new HashMap<>();
        this.behaviorByID = new ArrayList<>();

        // TODO: File format
        addField("a", "none/akw", "Lnone/kp;", "Hello from Enigma");
        addMethod("<init>", "none/akw", "(Lnone/ayo;)V", "You know what? I love constructors!", "The material of the block!");
        addClass("none/akw", "HEY I'M A BLOCK YOU KNOW THAT?!");
    }

    private void addClass(String className, String comment)
    {
        this.javaDocClassByID.put(new ClassEntry(className), new JavaDocClass(className, comment));
    }

    public void addField(String fieldName, String className, String type, String comment)
    {
        this.javaDocFieldByID.put(new FieldEntry(new ClassEntry(className), fieldName, new Type(type)), new JavaDocField(new JavaDocClass(className, "LOL"), fieldName, type, comment));
    }

    public void addMethod(String methodName, String className, String signature, String comment, String... argComment)
    {
        BehaviorEntry behaviorEntry;
        if (methodName.equals("<init>"))
            behaviorEntry = new ConstructorEntry(new ClassEntry(className), new Signature(signature));
        else
            behaviorEntry = new MethodEntry(new ClassEntry(className), methodName, new Signature(signature));

        this.javaDocMethodByID.put(behaviorEntry, new JavaDocMethod(new JavaDocClass(className, "LOL"), methodName, signature, comment,
                Arrays.asList(argComment)));
    }

    public void cleanBehaviors()
    {
        behaviorByID.clear();
    }

    public void loadMapping(File mappingDirectory)
    {
        // NOP
    }

    public void closeMapping()
    {
        behaviorByID.clear();
        javaDocClassByID.clear();
        javaDocFieldByID.clear();
        javaDocMethodByID.clear();
    }

    public CtClass tryToAddJavaDoc(CtClass ctClass, ClassEntry entry)
    {
        if (javaDocClassByID.containsKey(entry))
        {
            JavaDocClass javaDocClass = javaDocClassByID.get(entry);

            // Get the constant pool
            ConstPool constpool = ctClass.getClassFile().getConstPool();

            // Prepare the annotation
            AnnotationsAttribute attr = new AnnotationsAttribute(constpool, AnnotationsAttribute.visibleTag);
            Annotation annot = new Annotation("enigma.remapper.ClassDoc", constpool);
            annot.addMemberValue("comment", new StringMemberValue(javaDocClass.getComment(), constpool));
            attr.addAnnotation(annot);
            ctClass.getClassFile().addAttribute(attr);
        }
        return ctClass;
    }

    public CtBehavior tryToAddJavaDoc(CtBehavior ctBehavior, BehaviorEntry obEntry, BehaviorEntry deobEntry)
    {
        if (javaDocMethodByID.containsKey(obEntry))
        {
            int id = getEntryID(obEntry);
            if (id != -1)
                behaviorByID.set(id, deobEntry);
        }
        return tryToAddJavaDoc(ctBehavior, deobEntry);
    }

    public CtBehavior tryToAddJavaDoc(CtBehavior ctBehavior, BehaviorEntry entry)
    {
        if (javaDocMethodByID.containsKey(entry))
        {
            JavaDocMethod javaDocMethod = javaDocMethodByID.get(entry);

            // Get the constant pool
            ConstPool constpool = ctBehavior.getDeclaringClass().getClassFile().getConstPool();

            // Prepare the annotation
            AnnotationsAttribute attr = new AnnotationsAttribute(constpool, AnnotationsAttribute.visibleTag);
            Annotation annot = new Annotation("enigma.remapper.MethodDoc", constpool);
            annot.addMemberValue("comment", new StringMemberValue(javaDocMethod.getComment(), constpool));
            behaviorByID.add(entry);
            annot.addMemberValue("behavior", new IntegerMemberValue(constpool, behaviorByID.size() - 1));
            annot.addMemberValue("args", translateStringArray(constpool, javaDocMethod.getArgsComments()));
            attr.addAnnotation(annot);
            ctBehavior.getMethodInfo().addAttribute(attr);
        }
        return ctBehavior;
    }

    public CtField tryToAddJavaDoc(CtField ctField, FieldEntry entry)
    {
        if (javaDocFieldByID.containsKey(entry))
        {
            JavaDocField javaDocField = javaDocFieldByID.get(entry);

            // Get the constant pool
            ConstPool constpool = ctField.getDeclaringClass().getClassFile().getConstPool();

            // Prepare the annotation
            AnnotationsAttribute attr = new AnnotationsAttribute(constpool, AnnotationsAttribute.visibleTag);
            Annotation annot = new Annotation("enigma.remapper.FieldDoc", constpool);
            annot.addMemberValue("comment", new StringMemberValue(javaDocField.getComment(), constpool));
            attr.addAnnotation(annot);
            ctField.getFieldInfo().addAttribute(attr);
        }
        return ctField;
    }

    private MemberValue[] toMemberValue(ConstPool pool, String[] args)
    {
        MemberValue[] result = new MemberValue[args.length];
        for (int i = 0; i < result.length; i++)
            result[i] = new StringMemberValue(args[i], pool);

        return result;
    }

    private ArrayMemberValue translateStringArray(ConstPool pool, String[] args)
    {
        ArrayMemberValue res = new ArrayMemberValue(new StringMemberValue(pool), pool);
        res.setValue(toMemberValue(pool, args));
        return res;
    }

    public BehaviorEntry getEntry(int id)
    {
        if (id == -1)
            return null;
        return this.behaviorByID.get(id);
    }

    public int getEntryID(BehaviorEntry taget)
    {
        for (int i = 0; i < behaviorByID.size(); i++)
            if (behaviorByID.get(i).equals(taget))
                return i;
        return -1;
    }

    /**
     *
     * @param annotation
     * @param spacesCount
     * @return
     */
    public String convertAnnotationToJavaDoc(com.strobel.decompiler.languages.java.ast.Annotation annotation, int spacesCount)
    {
        int behaviorID = -1;
        StringBuilder builder = new StringBuilder();
        String spaces = buildLineSpace(spacesCount);
        builder.append("/**\n");
        AstNodeCollection<Expression> annotationArgs = annotation.getArguments();
        for (Expression expression : annotationArgs)
        {
            String id = expression.getFirstChild().toString();
            for (AstNode child : expression.getChildren())
            {
                if (child instanceof PrimitiveExpression)
                {
                    PrimitiveExpression data = (PrimitiveExpression) child;
                    if (id.equals("comment"))
                    {
                        builder.append(spaces);
                        builder.append(" * ");
                        builder.append(data.getValue());
                        builder.append("\n");
                    }
                    else if (id.equals("behavior"))
                        behaviorID = (Integer) data.getValue();
                }
                else if (child instanceof ArrayInitializerExpression)
                {
                    ArrayInitializerExpression data = (ArrayInitializerExpression) child;
                    if (id.equals("args"))
                    {
                        BehaviorEntry entry = getEntry(behaviorID);
                        if (entry == null)
                            System.err.println("(SEVERE): CANNOT FIND BEHAVIOR ENTRY FOR ID " + behaviorID);

                        int i = 0;
                        for (Expression expArg : data.getElements())
                        {
                            if (expArg instanceof PrimitiveExpression)
                            {
                                PrimitiveExpression primitiveExpression = (PrimitiveExpression) expArg;
                                String argName = entry == null ? null : this.translator.translate(new ArgumentEntry(entry, i, ""));
                                if (argName == null)
                                    argName = "a" + (i + 1);
                                builder.append(spaces);
                                builder.append(" * @param ");
                                builder.append(argName);
                                builder.append(" ");
                                builder.append(primitiveExpression.getValue());
                                builder.append("\n");
                            }
                            i++;
                        }
                    }
                }
            }
        }
        builder.append(spaces);
        builder.append(" */");
        return builder.toString();
    }

    private String buildLineSpace(int spacesCount)
    {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i< spacesCount; i++)
            builder.append(" ");
        return builder.toString();
    }

    public void setTranslator(Translator translator)
    {
        this.translator = translator;
    }
}
