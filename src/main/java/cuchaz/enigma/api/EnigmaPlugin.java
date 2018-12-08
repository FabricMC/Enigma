package cuchaz.enigma.api;

import org.objectweb.asm.tree.ClassNode;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.function.Function;

public abstract class EnigmaPlugin {
    public void onClassesLoaded(Map<String, byte[]> classData, Function<String, ClassNode> classNodeGetter) {

    }

    @Nullable
    public String proposeFieldName(String owner, String name, String desc) {
        return null;
    }
}
