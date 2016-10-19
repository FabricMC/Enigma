package cuchaz.enigma.mapping;

import cuchaz.enigma.utils.Utils;

/**
 * Desc...
 * Created by Thog
 * 19/10/2016
 */
public class LocalVariableEntry implements Entry
{

    protected final BehaviorEntry behaviorEntry;
    protected final String name;
    protected final Type type;
    protected final int index;

    public LocalVariableEntry(BehaviorEntry behaviorEntry, int index, String name, Type type) {
        if (behaviorEntry == null) {
            throw new IllegalArgumentException("Behavior cannot be null!");
        }
        if (index < 0) {
            throw new IllegalArgumentException("Index must be non-negative!");
        }
        if (name == null) {
            throw new IllegalArgumentException("Variable name cannot be null!");
        }
        if (type == null) {
            throw new IllegalArgumentException("Variable type cannot be null!");
        }

        this.behaviorEntry = behaviorEntry;
        this.name = name;
        this.type = type;
        this.index = index;
    }


    public LocalVariableEntry(LocalVariableEntry other, ClassEntry newClassEntry) {
        this.behaviorEntry = (BehaviorEntry) other.behaviorEntry.cloneToNewClass(newClassEntry);
        this.name = other.name;
        this.type = other.type;
        this.index = other.index;
    }

    public BehaviorEntry getBehaviorEntry() {
        return this.behaviorEntry;
    }

    public Type getType() {
        return type;
    }

    public int getIndex() {
        return index;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public ClassEntry getClassEntry() {
        return this.behaviorEntry.getClassEntry();
    }

    @Override
    public String getClassName() {
        return this.behaviorEntry.getClassName();
    }

    @Override
    public LocalVariableEntry cloneToNewClass(ClassEntry classEntry) {
        return new LocalVariableEntry(this, classEntry);
    }

    public String getMethodName() {
        return this.behaviorEntry.getName();
    }

    public Signature getMethodSignature() {
        return this.behaviorEntry.getSignature();
    }

    @Override
    public int hashCode() {
        return Utils.combineHashesOrdered(this.behaviorEntry, this.type.hashCode(), this.name.hashCode(), Integer.hashCode(this.index));
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof LocalVariableEntry && equals((LocalVariableEntry) other);
    }

    public boolean equals(LocalVariableEntry other) {
        return this.behaviorEntry.equals(other.behaviorEntry) && this.type.equals(other.type) && this.name.equals(other.name) && this.index == other.index;
    }

    @Override
    public String toString() {
        return this.behaviorEntry.toString() + "(" + this.index + ":" + this.name + ":" + this.type + ")";
    }
}
