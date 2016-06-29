package cuchaz.enigma.json;

import java.util.List;

public class JsonMethod {
    private String obf;
    private String name;
    private String signature;
    private List<JsonArgument> args;

    public JsonMethod(String obf, String name, String signature, List<JsonArgument> args) {
        this.obf = obf;
        this.name = name;
        this.signature = signature;
        this.args = args;
    }

    public String getObf() {
        return obf;
    }

    public String getName() {
        return name;
    }

    public String getSignature() {
        return signature;
    }

    public List<JsonArgument> getArgs() {
        return args;
    }
}
