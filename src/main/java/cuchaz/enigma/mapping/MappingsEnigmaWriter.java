/*******************************************************************************
 * Copyright (c) 2015 Jeff Martin.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public
 * License v3.0 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * Contributors:
 *     Jeff Martin - initial API and implementation
 ******************************************************************************/

package cuchaz.enigma.mapping;

import com.google.common.base.Charsets;

import java.io.*;
import java.util.*;

public class MappingsEnigmaWriter {

	public void write(File out, Mappings mappings, boolean isDirectoryFormat) throws IOException {
		if (!isDirectoryFormat) {
			PrintWriter outputWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(out), Charsets.UTF_8));
			write(outputWriter, mappings);
			outputWriter.close();
		} else
			writeAsDirectory(out, mappings);
	}

	public void writeAsDirectory(File target, Mappings mappings) throws IOException {
		if (!target.exists() && !target.mkdirs())
			throw new IOException("Cannot create mapping directory!");

		Mappings previousState = mappings.getPreviousState();
		for (ClassMapping classMapping : sorted(mappings.classes())) {
			if (!classMapping.isDirty()) {
				continue;
			}

			if (previousState != null) {
				ClassMapping previousClass = previousState.classesByObf.get(classMapping.getObfFullName());
				if (previousClass != null) {
					File previousFile = new File(target, previousClass.getSaveName() + ".mapping");
					if (previousFile.exists() && !previousFile.delete()) {
						System.err.println("Failed to delete old class mapping " + previousFile.getName());
					}
				}
			}

			File result = new File(target, classMapping.getSaveName() + ".mapping");

			File packageFile = result.getParentFile();
			if (!packageFile.exists()) {
				packageFile.mkdirs();
			}
			result.createNewFile();

			try (PrintWriter outputWriter = new PrintWriter(new BufferedWriter(new FileWriter(result)))) {
				write(outputWriter, classMapping, 0);
			}
		}

		// Remove dropped mappings
		if (previousState != null) {
			Set<ClassMapping> droppedClassMappings = new HashSet<>(previousState.classes());
			droppedClassMappings.removeAll(mappings.classes());
			for (ClassMapping droppedMapping : droppedClassMappings) {
				File result = new File(target, droppedMapping.getSaveName() + ".mapping");
				if (!result.exists()) {
					continue;
				}
				if (!result.delete()) {
					System.err.println("Failed to delete dropped class mapping " + result.getName());
				}
			}
		}
	}

	public void write(PrintWriter out, Mappings mappings) throws IOException {
		for (ClassMapping classMapping : sorted(mappings.classes())) {
			write(out, classMapping, 0);
		}
	}

	private void write(PrintWriter out, ClassMapping classMapping, int depth) throws IOException {
		if (classMapping.getDeobfName() == null) {
			out.format("%sCLASS %s%s\n", getIndent(depth), classMapping.getObfFullName(),
				classMapping.getModifier() == Mappings.EntryModifier.UNCHANGED ? "" : classMapping.getModifier().getFormattedName());
		} else {
			out.format("%sCLASS %s %s%s\n", getIndent(depth), classMapping.getObfFullName(), classMapping.getDeobfName(),
				classMapping.getModifier() == Mappings.EntryModifier.UNCHANGED ? "" : classMapping.getModifier().getFormattedName());
		}

		for (ClassMapping innerClassMapping : sorted(classMapping.innerClasses())) {
			write(out, innerClassMapping, depth + 1);
		}

		for (FieldMapping fieldMapping : sorted(classMapping.fields())) {
			write(out, fieldMapping, depth + 1);
		}

		for (MethodMapping methodMapping : sorted(classMapping.methods())) {
			write(out, methodMapping, depth + 1);
		}
	}

	private void write(PrintWriter out, FieldMapping fieldMapping, int depth) {
		if (fieldMapping.getDeobfName() == null)
			out.format("%sFIELD %s %s%s\n", getIndent(depth), fieldMapping.getObfName(), fieldMapping.getObfDesc().toString(),
				fieldMapping.getModifier() == Mappings.EntryModifier.UNCHANGED ? "" : fieldMapping.getModifier().getFormattedName());
		else
			out.format("%sFIELD %s %s %s%s\n", getIndent(depth), fieldMapping.getObfName(), fieldMapping.getDeobfName(), fieldMapping.getObfDesc().toString(),
				fieldMapping.getModifier() == Mappings.EntryModifier.UNCHANGED ? "" : fieldMapping.getModifier().getFormattedName());
	}

	private void write(PrintWriter out, MethodMapping methodMapping, int depth) throws IOException {
		if (methodMapping.isObfuscated()) {
			out.format("%sMETHOD %s %s%s\n", getIndent(depth), methodMapping.getObfName(), methodMapping.getObfDesc(),
				methodMapping.getModifier() == Mappings.EntryModifier.UNCHANGED ? "" : methodMapping.getModifier().getFormattedName());
		} else {
			out.format("%sMETHOD %s %s %s%s\n", getIndent(depth), methodMapping.getObfName(), methodMapping.getDeobfName(), methodMapping.getObfDesc(),
				methodMapping.getModifier() == Mappings.EntryModifier.UNCHANGED ? "" : methodMapping.getModifier().getFormattedName());
		}

		for (LocalVariableMapping localVariableMapping : sorted(methodMapping.arguments())) {
			write(out, localVariableMapping, depth + 1);
		}
	}

	private void write(PrintWriter out, LocalVariableMapping localVariableMapping, int depth) {
		out.format("%sARG %d %s\n", getIndent(depth), localVariableMapping.getIndex(), localVariableMapping.getName());
	}

	private <T extends Comparable<T>> List<T> sorted(Iterable<T> classes) {
		List<T> out = new ArrayList<>();
		for (T t : classes) {
			out.add(t);
		}
		Collections.sort(out);
		return out;
	}

	private String getIndent(int depth) {
		StringBuilder buf = new StringBuilder();
		for (int i = 0; i < depth; i++) {
			buf.append("\t");
		}
		return buf.toString();
	}
}
