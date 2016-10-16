/*******************************************************************************
 * Copyright (c) 2015 Jeff Martin.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public
 * License v3.0 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * Contributors:
 * Jeff Martin - initial API and implementation
 ******************************************************************************/

package cuchaz.enigma.mapping;

import com.google.common.base.Charsets;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MappingsEnigmaWriter {

	public void write(File out, Mappings mappings, boolean isDirectoryFormat) throws IOException {
		if (!isDirectoryFormat) {
			PrintWriter outputWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(out), Charsets.UTF_8));
			write(outputWriter, mappings);
			outputWriter.close();
		} else
			writeAsDirectory(out, mappings);
	}

	private void deleteDir(File file) {
		File[] contents = file.listFiles();
		if (contents != null) {
			for (File f : contents) {
				deleteDir(f);
			}
		}
		file.delete();
	}

	public void writeAsDirectory(File target, Mappings mappings) throws IOException {
		//TODO: Know what have changes during write to not rewrite all the things
		if (!target.exists() && !target.mkdirs())
			throw new IOException("Cannot create mapping directory!");

		for (String removeObf : mappings.getRemoveList()) {
			File obFile = new File(target, removeObf + ".mapping");
			if (obFile.exists())
				obFile.delete();
		}
		mappings.getRemoveList().clear();
		for (ClassMapping classMapping : sorted(mappings.classes())) {
			File obFile = new File(target, classMapping.getObfFullName() + ".mapping");
			File result;
			if (classMapping.getDeobfName() == null)
				result = obFile;
			else {
				// Make sure that old version of the file doesn't exist
				if (obFile.exists())
					obFile.delete();
				result = new File(target, classMapping.getDeobfName() + ".mapping");
			}

			if (!result.getParentFile().exists())
				result.getParentFile().mkdirs();

			if (classMapping.isDirty || !result.exists()) {
				result.createNewFile();
				PrintWriter outputWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(result), Charsets.UTF_8));
				write(outputWriter, classMapping, 0);
				outputWriter.close();

				classMapping.isDirty = false;
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
			out.format("%sCLASS %s\n", getIndent(depth), classMapping.getObfFullName());
		} else {
			out.format("%sCLASS %s %s\n", getIndent(depth), classMapping.getObfFullName(), classMapping.getDeobfName());
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

	private void write(PrintWriter out, FieldMapping fieldMapping, int depth) throws IOException {
		out.format("%sFIELD %s %s %s\n", getIndent(depth), fieldMapping.getObfName(), fieldMapping.getDeobfName(), fieldMapping.getObfType().toString());
	}

	private void write(PrintWriter out, MethodMapping methodMapping, int depth) throws IOException {
		if (methodMapping.getDeobfName() == null) {
			out.format("%sMETHOD %s %s\n", getIndent(depth), methodMapping.getObfName(), methodMapping.getObfSignature());
		} else {
			out.format("%sMETHOD %s %s %s\n", getIndent(depth), methodMapping.getObfName(), methodMapping.getDeobfName(), methodMapping.getObfSignature());
		}

		for (ArgumentMapping argumentMapping : sorted(methodMapping.arguments())) {
			write(out, argumentMapping, depth + 1);
		}
	}

	private void write(PrintWriter out, ArgumentMapping argumentMapping, int depth) throws IOException {
		out.format("%sARG %d %s\n", getIndent(depth), argumentMapping.getIndex(), argumentMapping.getName());
	}

	private <T extends Comparable<T>> List<T> sorted(Iterable<T> classes) {
		List<T> out = new ArrayList<T>();
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
