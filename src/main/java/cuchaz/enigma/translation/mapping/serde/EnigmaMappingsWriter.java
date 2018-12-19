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

package cuchaz.enigma.translation.mapping.serde;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import cuchaz.enigma.translation.mapping.AccessModifier;
import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.mapping.MappingDelta;
import cuchaz.enigma.translation.mapping.tree.MappingNode;
import cuchaz.enigma.translation.mapping.tree.MappingTree;
import cuchaz.enigma.translation.representation.entry.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;

// TODO: sorted mappings in all writers
public enum EnigmaMappingsWriter implements MappingsWriter {
	FILE {
		@Override
		public void write(MappingTree<EntryMapping> mappings, MappingDelta<EntryMapping> delta, Path path) throws IOException {
			Multimap<ClassEntry, ClassEntry> innerClasses = collectInnerClasses(mappings);
			try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(path))) {
				for (MappingNode<EntryMapping> node : mappings) {
					if (node.getEntry() instanceof ClassEntry) {
						ClassEntry classEntry = (ClassEntry) node.getEntry();
						writeRoot(writer, mappings, innerClasses, classEntry);
					}
				}
			}
		}
	},
	DIRECTORY {
		@Override
		public void write(MappingTree<EntryMapping> mappings, MappingDelta<EntryMapping> delta, Path path) throws IOException {
			Multimap<ClassEntry, ClassEntry> innerClasses = collectInnerClasses(mappings);

			applyDeletions(delta.getDeletions(), path);

			for (MappingNode<EntryMapping> node : delta.getAdditions()) {
				Entry entry = node.getEntry();
				if (entry instanceof ClassEntry && !((ClassEntry) entry).isInnerClass()) {
					ClassEntry classEntry = (ClassEntry) entry;

					Path classPath = resolve(path, classEntry);
					Files.deleteIfExists(classPath);
					Files.createDirectories(classPath.getParent());

					try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(classPath))) {
						writeRoot(writer, mappings, innerClasses, classEntry);
					}
				}
			}
		}

		private void applyDeletions(MappingTree<EntryMapping> deletions, Path path) throws IOException {
			for (MappingNode<EntryMapping> node : deletions) {
				Entry entry = node.getEntry();
				if (entry instanceof ClassEntry && !((ClassEntry) entry).isInnerClass()) {
					Path classPath = resolve(path, (ClassEntry) entry);
					Files.deleteIfExists(classPath);
				}
			}
		}

		private Path resolve(Path root, ClassEntry classEntry) {
			return root.resolve(classEntry.getFullName().replace('.', '/') + ".mapping");
		}
	};

	protected Multimap<ClassEntry, ClassEntry> collectInnerClasses(MappingTree<EntryMapping> mappings) {
		Multimap<ClassEntry, ClassEntry> innerClasses = HashMultimap.create();

		for (MappingNode<EntryMapping> node : mappings) {
			Entry entry = node.getEntry();
			if (entry instanceof ClassEntry) {
				ClassEntry classEntry = (ClassEntry) entry;
				if (classEntry.isInnerClass()) {
					innerClasses.put(classEntry.getOuterClass(), classEntry);
				}
			}
		}

		return innerClasses;
	}

	protected void writeRoot(PrintWriter writer, MappingTree<EntryMapping> mappings, Multimap<ClassEntry, ClassEntry> innerClasses, ClassEntry classEntry) {
		Collection<Entry> children = new ArrayList<>(mappings.getChildren(classEntry));
		children.addAll(innerClasses.get(classEntry));

		writer.println(writeClass(classEntry, mappings.getMapping(classEntry)));
		for (Entry child : children) {
			writeEntry(writer, mappings, child, 1);
		}
	}

	protected void writeEntry(PrintWriter writer, MappingTree<EntryMapping> mappings, Entry entry, int depth) {
		MappingNode<EntryMapping> node = mappings.leaf(entry);
		if (node == null) {
			return;
		}

		EntryMapping mapping = node.getMapping();
		if (entry instanceof ClassEntry) {
			String line = writeClass((ClassEntry) entry, mapping);
			writer.println(indent(line, depth));
		} else if (entry instanceof MethodEntry) {
			String line = writeMethod((MethodEntry) entry, mapping);
			writer.println(indent(line, depth));
		} else if (entry instanceof FieldEntry) {
			String line = writeField((FieldEntry) entry, mapping);
			writer.println(indent(line, depth));
		} else if (entry instanceof LocalVariableEntry) {
			String line = writeArgument((LocalVariableEntry) entry, mapping);
			writer.println(indent(line, depth));
		}

		for (Entry child : node.getChildren()) {
			writeEntry(writer, mappings, child, depth + 1);
		}
	}

	protected String writeClass(ClassEntry entry, EntryMapping mapping) {
		StringBuilder builder = new StringBuilder("CLASS ");
		builder.append(entry.getFullName()).append(' ');
		writeMapping(builder, mapping);

		return builder.toString();
	}

	protected String writeMethod(MethodEntry entry, EntryMapping mapping) {
		StringBuilder builder = new StringBuilder("FIELD ");
		builder.append(entry.getName()).append(' ');
		builder.append(entry.getDesc().toString()).append(' ');

		writeMapping(builder, mapping);

		return builder.toString();
	}

	protected String writeField(FieldEntry entry, EntryMapping mapping) {
		StringBuilder builder = new StringBuilder("FIELD ");
		builder.append(entry.getName()).append(' ');
		builder.append(entry.getDesc().toString()).append(' ');

		writeMapping(builder, mapping);

		return builder.toString();
	}

	protected String writeArgument(LocalVariableEntry entry, EntryMapping mapping) {
		StringBuilder builder = new StringBuilder("ARG ");
		builder.append(entry.getIndex()).append(' ');

		String mappedName = mapping != null ? mapping.getTargetName() : entry.getName();
		builder.append(mappedName).append(' ');

		return builder.toString();
	}

	private void writeMapping(StringBuilder builder, EntryMapping mapping) {
		if (mapping != null) {
			builder.append(mapping.getTargetName()).append(' ');
			if (mapping.getAccessModifier() != AccessModifier.UNCHANGED) {
				builder.append(mapping.getAccessModifier().getFormattedName()).append(' ');
			}
		}
	}

	private String indent(String line, int depth) {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < depth; i++) {
			builder.append("\t");
		}
		builder.append(line);
		return builder.toString().trim();
	}
}
