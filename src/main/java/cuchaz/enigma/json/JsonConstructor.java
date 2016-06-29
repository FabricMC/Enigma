package cuchaz.enigma.json;

import java.util.List;

public class JsonConstructor {
    private String signature;
    private List<JsonArgument> args;
    private boolean statics;

    public JsonConstructor(String signature, List<JsonArgument> args, boolean statics) {
        this.signature=signature;
        this.args = args;
        this.statics = statics;
    }
}
