package cuchaz.enigma.json;

import java.util.ArrayList;
import java.util.List;

public class JsonClass {
    private String obf;
    private String name;
    private List<JsonField> field = new ArrayList<>();
    private List<JsonConstructor> constructors = new ArrayList<>();
    private List<JsonMethod> method = new ArrayList<>();
    private List<JsonClass> innerClass = new ArrayList<>();

    public JsonClass(String obf, String name) {
        this.obf = obf;
        this.name = name;
    }

    public void addField(JsonField jsonField) {
        this.field.add(jsonField);
    }

    public void addConstructor(JsonConstructor jsonConstructor) {
        this.constructors.add(jsonConstructor);
    }

    public void addMethod(JsonMethod jsonMethod) {
        this.method.add(jsonMethod);
    }

    public void addInnerClass(JsonClass jsonInnerClass) {
        this.innerClass.add(jsonInnerClass);
    }

    public String getObf() {
        return obf;
    }

    public String getName() {
        return name;
    }

    public List<JsonField> getField() {
        return field;
    }

    public List<JsonConstructor> getConstructors() {
        return constructors;
    }

    public List<JsonMethod> getMethod() {
        return method;
    }

    public List<JsonClass> getInnerClass() {
        return innerClass;
    }
}
