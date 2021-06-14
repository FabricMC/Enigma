package cuchaz.enigma.analysis;

public record StructureTreeOptions(
        ObfuscationVisibility obfuscationVisibility,
        DocumentationVisibility documentationVisibility,
        SortingOrder sortingOrder) {

    public enum SortingOrder {
        DEFAULT("default"),
        A_Z("a_z"),
        Z_A("z_a");

        private final String key;

        SortingOrder(String key) {
            this.key = key;
        }

        public String getKey() {
            return this.key;
        }
    }

    public enum ObfuscationVisibility {
        ALL("all"),
        OBFUSCATED("obfuscated"),
        DEOBFUSCATED("deobfuscated");

        private final String key;

        ObfuscationVisibility(String key) {
            this.key = key;
        }

        public String getKey() {
            return this.key;
        }
    }

    public enum DocumentationVisibility {
        ALL("all"),
        DOCUMENTED("documented"),
        NON_DOCUMENTED("non_documented");

        private final String key;

        DocumentationVisibility(String key) {
            this.key = key;
        }

        public String getKey() {
            return this.key;
        }
    }
}
