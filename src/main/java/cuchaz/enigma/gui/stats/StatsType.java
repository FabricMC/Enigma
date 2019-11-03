package cuchaz.enigma.gui.stats;

public enum StatsType {
    METHODS(true, false),
    FIELDS(false, true),
    METHODS_AND_FIELDS(true, true);

    public final boolean includesMethods;
    public final boolean includesFields;

    StatsType(boolean includesMethods, boolean includesFields) {
        this.includesMethods = includesMethods;
        this.includesFields = includesFields;
    }
}
