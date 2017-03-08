package cuchaz.enigma.throwables;

public class MappingConflict extends Exception {
	public MappingConflict(String clazz, String name, String nameExisting) {
		super(String.format("Conflicting mappings found for %s. The mapping file is %s and the second is %s", clazz, name, nameExisting));
	}
}
