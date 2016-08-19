package cuchaz.enigma.mapping.javadoc;

import cuchaz.enigma.mapping.ClassEntry;
import cuchaz.enigma.mapping.Entry;
import cuchaz.enigma.mapping.FieldEntry;

/**
 * JavaDoc of a Field
 * Created by Thog
 * 19/08/2016
 */
public class JavaDocField extends BaseJavaDocEntry
{
    private final JavaDocClass classDocEntry;
    private final String       name;
    public JavaDocField(JavaDocClass classDocEntry, String name, String identifier, String comment)
    {
        super(identifier, comment);
        this.name = name;
        this.classDocEntry = classDocEntry;
    }

    @Override public JavaDocClass getClassDocEntry()
    {
        return classDocEntry;
    }

    @Override public Entry getEntry()
    {
        return new FieldEntry((ClassEntry) classDocEntry.getEntry(), name, getType());
    }
}
