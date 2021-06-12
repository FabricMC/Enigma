package cuchaz.enigma.analysis;

import cuchaz.enigma.utils.I18n;

public record StructureTreeOptions(
        ObfuscationVisibility obfuscationVisibility,
        DocumentationVisibility documentationVisibility,
        SortingOrder sortingOrder) {

    public enum SortingOrder {
        DEFAULT,
        A_Z,
        Z_A;

        @Override
        public String toString() {
            return I18n.translate("structure.options.sorting." + this.name().toLowerCase());
        }
    }

    public enum ObfuscationVisibility {
        ALL,
        OBFUSCATED,
        DEOBFUSCATED;

        @Override
        public String toString() {
            return I18n.translate("structure.options.obfuscation." + this.name().toLowerCase());
        }
    }

    public enum DocumentationVisibility {
        ALL,
        DOCUMENTED,
        NON_DOCUMENTED;

        @Override
        public String toString() {
            return I18n.translate("structure.options.documentation." + this.name().toLowerCase());
        }
    }
}
