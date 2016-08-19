package cuchaz.enigma.mapping.javadoc;

import cuchaz.enigma.mapping.ClassEntry;
import cuchaz.enigma.mapping.Entry;
import cuchaz.enigma.mapping.MethodEntry;
import cuchaz.enigma.mapping.Signature;

import java.util.List;

/**
 * Javadoc of a Method
 * TODO: @return
 * Created by Thog
 * 19/08/2016
 */
public class JavaDocMethod extends BaseJavaDocEntry
{
    private final JavaDocClass classDocEntry;
    private final String       name;
    private final List<String> argsComments;

    public JavaDocMethod(JavaDocClass classDocEntry, String name, String identifier, String comment, List<String> argsComments)
    {
        super(identifier, comment);
        this.name = name;
        this.classDocEntry = classDocEntry;
        this.argsComments = argsComments;
    }

    @Override public JavaDocClass getClassDocEntry()
    {
        return classDocEntry;
    }

    @Override public Entry getEntry()
    {
        return new MethodEntry((ClassEntry) classDocEntry.getEntry(), name, new Signature(getIdentifier()));
    }

    public String[] getArgsComments()
    {
        return argsComments.toArray(new String[argsComments.size()]);
    }

    public String getArgsComment(int id)
    {
        return argsComments.size() > id ? argsComments.get(id) : null;
    }
}
