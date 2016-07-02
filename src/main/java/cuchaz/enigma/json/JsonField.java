package cuchaz.enigma.json;

public class JsonField {
    private String obf;
    private String name;
    private String type;

    public JsonField(String obf, String name, String type) {
        this.obf = obf;
        this.name = name;
        this.type = type;
    }

    public String getObf() {
        return this.obf;
    }

    public String getName() {
        return this.name;
    }

    public String getType() {
        return this.type;
    }
}
