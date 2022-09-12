package cuchaz.enigma.translation;

import java.util.Collection;
import java.util.Locale;

import cuchaz.enigma.translation.mapping.IdentifierValidation;
import cuchaz.enigma.translation.representation.TypeDescriptor;

public class LocalNameGenerator {
	public static String generateArgumentName(int index, TypeDescriptor desc, Collection<TypeDescriptor> arguments) {
		boolean uniqueType = arguments.stream().filter(desc::equals).count() <= 1;
		String translatedName;
		int nameIndex = index + 1;
		StringBuilder nameBuilder = new StringBuilder(getTypeName(desc));

		if (!uniqueType || IdentifierValidation.isReservedMethodName(nameBuilder.toString())) {
			nameBuilder.append(nameIndex);
		}

		translatedName = nameBuilder.toString();
		return translatedName;
	}

	public static String generateLocalVariableName(int index, TypeDescriptor desc) {
		int nameIndex = index + 1;
		return getTypeName(desc) + nameIndex;
	}

	private static String getTypeName(TypeDescriptor desc) {
		// Unfortunately each of these have different name getters, so they have different code paths
		if (desc.isPrimitive()) {
			TypeDescriptor.Primitive argCls = desc.getPrimitive();
			return argCls.name().toLowerCase(Locale.ROOT);
		} else if (desc.isArray()) {
			// List types would require this whole block again, so just go with aListx
			return "arr";
		} else if (desc.isType()) {
			String typeName = desc.getTypeEntry().getSimpleName().replace("$", "");
			typeName = typeName.substring(0, 1).toLowerCase(Locale.ROOT) + typeName.substring(1);
			return typeName;
		} else {
			System.err.println("Encountered invalid argument type descriptor " + desc.toString());
			return "var";
		}
	}
}
