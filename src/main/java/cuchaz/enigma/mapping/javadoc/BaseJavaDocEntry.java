package cuchaz.enigma.mapping.javadoc;

import cuchaz.enigma.mapping.Entry;
import cuchaz.enigma.mapping.Type;

/**
 * Base of any JavaDoc entry
 * Created by Thog
 * 19/08/2016
 */
public abstract class BaseJavaDocEntry
{
    private String identifier;
    private String comment;

    public BaseJavaDocEntry(String identifier, String comment)
    {
        this.identifier = identifier;
        this.comment = comment;
    }

    public void setIdentifier(String identifier)
    {
        this.identifier = identifier;
    }

    public String getIdentifier()
    {
        return identifier;
    }

    public void setComment(String comment)
    {
        this.comment = comment;
    }

    public String getComment()
    {
        return comment;
    }

    public Type getType()
    {
        return new Type(getIdentifier());
    }

    public abstract JavaDocClass getClassDocEntry();

    public abstract Entry getEntry();
}
