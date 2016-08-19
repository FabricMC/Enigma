package cuchaz.enigma.mapping.javadoc;

import cuchaz.enigma.mapping.ClassEntry;
import cuchaz.enigma.mapping.Entry;

/**
 * JavaDoc representation of a class
 * Created by Thog
 * 19/08/2016
 */
public class JavaDocClass extends BaseJavaDocEntry
{

    public JavaDocClass(String identifier, String comment)
    {
        super(identifier, comment);
    }

    @Override public JavaDocClass getClassDocEntry()
    {
        return this;
    }

    @Override
    public Entry getEntry()
    {
        return new ClassEntry(getIdentifier());
    }
}
