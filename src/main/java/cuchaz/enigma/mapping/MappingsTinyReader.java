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
		System.out.println(file);
		List<String> lines = Files.readAllLines(file.toPath(), Charsets.UTF_8);
		Map<String, ClassMapping> classMappingMap = Maps.newHashMap();
		String header = lines.remove(0);
		for (int lineNumber = 0; lineNumber < lines.size(); lineNumber++) {
			String line = lines.get(lineNumber);
			String[] parts = line.split("\\s");
			try {
				String token = parts[0];
				ClassMapping classMapping;
				switch (token) {
					case "CLASS":
						if (classMappingMap.containsKey(parts[1])) {
							classMapping = classMappingMap.get(parts[1]);
							String deobName = parts[2].contains("$") ?
							                  parts[2].substring(parts[2].lastIndexOf('$') + 1) :
							                  parts[2];
							classMappingMap.put(parts[2], classMapping.setDeobInner(parts[2]));
							classMapping.setDeobfName(deobName);
							classMapping.resetDirty();
						} else
							classMapping = readClass(parts);
						classMappingMap.put(parts[1], classMapping);
						break;
					case "FIELD":
						classMapping = classMappingMap.computeIfAbsent(parts[1], k -> new ClassMapping(parts[1]));
						//throw new MappingParseException(file, lineNumber, "Cannot find class '" + parts[1] + "' declaration!");
						classMapping.addFieldMapping(readField(parts));
						break;
					case "METHOD":
						classMapping = classMappingMap.computeIfAbsent(parts[1], k -> new ClassMapping(parts[1]));
						//throw new MappingParseException(file, lineNumber, "Cannot find class '" + parts[1] + "' declaration!");
						classMapping.addMethodMapping(readMethod(parts));
						break;
					default:
						throw new MappingParseException(file, lineNumber, "Unknown token '" + token + "' !");
				}
			} catch (ArrayIndexOutOfBoundsException | IllegalArgumentException ex) {
				throw new MappingParseException(file, lineNumber, "Malformed line:\n" + line);
			}
		}

		List<ClassMapping> toRegister = new ArrayList<>(classMappingMap.values());
		for (ClassMapping classMapping : toRegister) {
			ClassEntry obEntry = classMapping.getObfEntry();
			ClassEntry deobEntry = classMapping.getDeObfEntry();
			try {
				if (obEntry.isInnerClass()) {
					ClassMapping parent = classMappingMap.get(obEntry.getOuterClassName());
					if (parent == null) {
						parent = new ClassMapping(obEntry.getOuterClassName());
						classMappingMap.put(obEntry.getOuterClassName(), parent);
						mappings.addClassMapping(parent);
					}
					parent.addInnerClassMapping(classMapping);
				} else if (deobEntry != null && deobEntry.isInnerClass())
					classMappingMap.get(deobEntry.getOuterClassName()).addInnerClassMapping(classMapping);
				else
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
