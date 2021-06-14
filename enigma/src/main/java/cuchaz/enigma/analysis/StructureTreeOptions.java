package cuchaz.enigma.analysis;

public record StructureTreeOptions(
        ObfuscationVisibility obfuscationVisibility,
        DocumentationVisibility documentationVisibility,
        SortingOrder sortingOrder) {

    public enum ObfuscationVisibility implements Option {
        ALL("structure.options.obfuscation.all"),
        OBFUSCATED("structure.options.obfuscation.obfuscated"),
        DEOBFUSCATED("structure.options.obfuscation.deobfuscated");

        private final String translationKey;

        ObfuscationVisibility(String translationKey) {
            this.translationKey = translationKey;
        }

        public String getTranslationKey() {
            return this.translationKey;
        }
    }

    public enum DocumentationVisibility implements Option {
        ALL("structure.options.documentation.all"),
        DOCUMENTED("structure.options.documentation.documented"),
        NON_DOCUMENTED("structure.options.documentation.non_documented");

        private final String translationKey;

        DocumentationVisibility(String translationKey) {
            this.translationKey = translationKey;
        }

        public String getTranslationKey() {
            return this.translationKey;
        }
    }

    public enum SortingOrder implements Option {
        DEFAULT("structure.options.sorting.default"),
        A_Z("structure.options.sorting.a_z"),
        Z_A("structure.options.sorting.z_a");

        private final String translationKey;

        SortingOrder(String translationKey) {
            this.translationKey = translationKey;
        }

        public String getTranslationKey() {
            return this.translationKey;
        }
    }

    public interface Option {
        String getTranslationKey();
    }
}
