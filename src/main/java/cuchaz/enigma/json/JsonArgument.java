package cuchaz.enigma.json;

public class JsonArgument {

    private int index;
    private String name;

    public JsonArgument(int index, String name) {
        this.index = index;
        this.name = name;
    }

    public int getIndex() {
        return index;
    }

    public String getName() {
        return name;
    }
}
