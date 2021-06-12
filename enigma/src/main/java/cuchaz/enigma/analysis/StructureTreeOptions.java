package cuchaz.enigma.analysis;

import cuchaz.enigma.utils.I18n;

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

        @Override
        public String toString() {
            return I18n.translate("structure.options.sorting." + this.key);
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

        @Override
        public String toString() {
            return I18n.translate("structure.options.obfuscation." + this.key);
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

        @Override
        public String toString() {
            return I18n.translate("structure.options.documentation." + this.key);
        }
    }
}
