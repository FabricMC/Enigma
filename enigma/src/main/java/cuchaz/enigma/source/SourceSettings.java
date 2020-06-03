package cuchaz.enigma.source;

public class SourceSettings {
    public final boolean removeImports;
    public final boolean removeVariableFinal;

    public SourceSettings(boolean removeImports, boolean removeVariableFinal) {
        this.removeImports = removeImports;
        this.removeVariableFinal = removeVariableFinal;
    }
}
