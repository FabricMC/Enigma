package cuchaz.enigma.analysis;

import cuchaz.enigma.translation.representation.entry.ClassEntry;

public abstract class ReferenceTargetType {
    private static final None NONE = new None();
    private static final Uninitialized UNINITIALIZED = new Uninitialized();

    public abstract Kind getKind();

    public static None none() {
        return NONE;
    }

    public static Uninitialized uninitialized() {
        return UNINITIALIZED;
    }

    public static ClassType classType(ClassEntry name) {
        return new ClassType(name);
    }

    public enum Kind {
        NONE,
        UNINITIALIZED,
        CLASS_TYPE
    }

    public static class None extends ReferenceTargetType {
        @Override
        public Kind getKind() {
            return Kind.NONE;
        }

        @Override
        public String toString() {
            return "(none)";
        }
    }

    public static class Uninitialized extends ReferenceTargetType {
        @Override
        public Kind getKind() {
            return Kind.UNINITIALIZED;
        }

        @Override
        public String toString() {
            return "(uninitialized)";
        }
    }

    public static class ClassType extends ReferenceTargetType {
        private final ClassEntry entry;

        private ClassType(ClassEntry entry) {
            this.entry = entry;
        }

        public ClassEntry getEntry() {
            return entry;
        }

        @Override
        public Kind getKind() {
            return Kind.CLASS_TYPE;
        }

        @Override
        public String toString() {
            return entry.toString();
        }
    }
}
