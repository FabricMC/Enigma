package cuchaz.enigma.mapping;

import com.google.common.base.Charsets;
import com.google.common.collect.Maps;
import cuchaz.enigma.throwables.MappingConflict;
import cuchaz.enigma.throwables.MappingParseException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MappingsTinyReader {
	public ClassMapping readClass(String[] parts) {
		// Extract the inner naming of the deob form if it have one
		String deobName = parts[2].contains("$") ? parts[2].substring(parts[2].lastIndexOf('$') + 1) : parts[2];
		return new ClassMapping(parts[1], deobName).setDeobInner(parts[2]);
	}

	public FieldMapping readField(String[] parts) {
		return new FieldMapping(parts[3], new Type(parts[2]), parts[4], Mappings.EntryModifier.UNCHANGED);
	}

	public MethodMapping readMethod(String[] parts) {
		return new MethodMapping(parts[3], new Signature(parts[2]), parts[4]);
	}

	public Mappings read(File file) throws IOException, MappingParseException {
		Mappings mappings = new Mappings(Mappings.FormatType.TINY_FILE);
		List<String> lines = Files.readAllLines(file.toPath(), Charsets.UTF_8);
		Map<String, ClassMapping> classMappingMap = Maps.newHashMap();
		lines.remove(0); // TODO: use the header
		for (int lineNumber = 0; lineNumber < lines.size(); lineNumber++) {
			String line = lines.get(lineNumber);
			String[] parts = line.split("\t");
			try {
				String token = parts[0];
				ClassMapping classMapping;
				switch (token) {
					case "CLASS":

						// Check for orphan created by field or method entries. It shouldn't be possible but I prefer to handle this case
						if (classMappingMap.containsKey(parts[1])) {
							classMapping = classMappingMap.get(parts[1]);

							// We have the full deob name, Enigma only support simple class name so we extract it.
							String deobName = parts[2].contains("$") ?
							                  parts[2].substring(parts[2].lastIndexOf('$') + 1) :
							                  parts[2];

							// Add full deob name to the class mapping to handle inner class after this loop
							classMappingMap.put(parts[2], classMapping.setDeobInner(parts[2]));
							classMapping.setDeobfName(deobName);

							// Avoid to make the mapping dirty directly at the startup
							classMapping.resetDirty();
						} else
							classMapping = readClass(parts);
						classMappingMap.put(parts[1], classMapping);
						break;
					case "FIELD":
						// We can have missing classes mappings because they don't have a ob name, so we create it and use it
						classMapping = classMappingMap.computeIfAbsent(parts[1], k -> new ClassMapping(parts[1]));
						classMapping.addFieldMapping(readField(parts));
						break;
					case "METHOD":
						// We can have missing classes mappings because they don't have a ob name, so we create it and use it
						classMapping = classMappingMap.computeIfAbsent(parts[1], k -> new ClassMapping(parts[1]));
						classMapping.addMethodMapping(readMethod(parts));
						break;
					case "MTH-ARG":
						classMapping = classMappingMap.computeIfAbsent(parts[1], k -> new ClassMapping(parts[1]));
						classMapping.setArgumentName(parts[3], new Signature(parts[2]), Integer.parseInt(parts[4]), parts[5]);
						break;
					default:
						throw new MappingParseException(file, lineNumber, "Unknown token '" + token + "' !");
				}
			} catch (ArrayIndexOutOfBoundsException | IllegalArgumentException ex) {
				ex.printStackTrace();
				throw new MappingParseException(file, lineNumber, "Malformed line:\n" + line);
			}
		}

		List<ClassMapping> toRegister = new ArrayList<>(classMappingMap.values());

		// After having completely parsed the file, we need to register it to the real mapping
		for (ClassMapping classMapping : toRegister) {
			ClassEntry obEntry = classMapping.getObfEntry();
			ClassEntry deobEntry = classMapping.getDeObfEntry();
			try {
				if (obEntry.isInnerClass()) {
					ClassMapping parent = classMappingMap.get(obEntry.getOuterClassName());
					// Inner class can miss their parent... So we create it and add it to the mappings
					if (parent == null) {
						parent = new ClassMapping(obEntry.getOuterClassName()); // FIXME: WE ACTUALLY DON'T MANAGE INNER CLASS OF INNER CLASS
						classMappingMap.put(obEntry.getOuterClassName(), parent);
						mappings.addClassMapping(parent);
					}
					// Add the inner class to the parent
					parent.addInnerClassMapping(classMapping);
				}
				// obf class can become deobf inner classs, manage this case.
				else if (deobEntry != null && deobEntry.isInnerClass()) {
					String outerClassName = deobEntry.getOuterClassName();
					ClassMapping parent = classMappingMap.get(outerClassName);

					// Only the inner is deob??? Okay
					if (parent == null) {
						parent = classMappingMap.get(outerClassName);
						if (parent == null) {
							parent = new ClassMapping(outerClassName); // FIXME: WE ACTUALLY DON'T MANAGE INNER CLASS OF INNER CLASS
							classMappingMap.put(outerClassName, parent);
							mappings.addClassMapping(parent);
						}
					}
					parent.addInnerClassMapping(classMapping);
				} else
					mappings.addClassMapping(classMapping);
			} catch (MappingConflict e) {
				throw new MappingParseException(file, -1, e.getMessage());
			}
		}
		lines.clear();
		classMappingMap.clear();
		return mappings;
	}
}
