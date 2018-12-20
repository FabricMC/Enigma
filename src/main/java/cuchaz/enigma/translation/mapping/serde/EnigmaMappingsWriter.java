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

import com.google.common.collect.Lists;
import cuchaz.enigma.translation.MappingTranslator;
import cuchaz.enigma.translation.Translator;
import cuchaz.enigma.translation.mapping.AccessModifier;
import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.mapping.MappingDelta;
import cuchaz.enigma.translation.mapping.tree.MappingNode;
import cuchaz.enigma.translation.mapping.tree.MappingTree;
import cuchaz.enigma.translation.representation.entry.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

public enum EnigmaMappingsWriter implements MappingsWriter {
	FILE {
		@Override
		public void write(MappingTree<EntryMapping> mappings, MappingDelta delta, Path path) throws IOException {
			try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(path))) {
				for (MappingNode<EntryMapping> node : mappings) {
					if (node.getEntry() instanceof ClassEntry) {
						ClassEntry classEntry = (ClassEntry) node.getEntry();
						writeRoot(writer, mappings, classEntry);
					}
				}
			}
		}
	},
	DIRECTORY {
		@Override
		public void write(MappingTree<EntryMapping> mappings, MappingDelta delta, Path path) throws IOException {
			applyDeletions(delta.getDeletions(), path);

			Translator translator = new MappingTranslator(mappings);
			for (MappingNode<?> node : delta.getAdditions()) {
				Entry<?> entry = node.getEntry();
				if (entry instanceof ClassEntry) {
					ClassEntry classEntry = (ClassEntry) entry;

					Path classPath = resolve(path, translator.translate(classEntry));
					Files.deleteIfExists(classPath);
					Files.createDirectories(classPath.getParent());

					try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(classPath))) {
						writeRoot(writer, mappings, classEntry);
					}
				}
			}
		}

		private void applyDeletions(MappingTree<?> deletions, Path root) throws IOException {
			Collection<ClassEntry> deletedClasses = deletions.getRootEntries().stream()
					.filter(e -> e instanceof ClassEntry)
					.map(e -> (ClassEntry) e)
					.collect(Collectors.toList());

			for (ClassEntry classEntry : deletedClasses) {
				Files.deleteIfExists(resolve(root, classEntry));
			}

			for (ClassEntry classEntry : deletedClasses) {
				String packageName = classEntry.getPackageName();
				if (packageName != null) {
					Path packagePath = Paths.get(packageName);
					deleteDeadPackages(root, packagePath);
				}
			}
		}

		private void deleteDeadPackages(Path root, Path packagePath) throws IOException {
			for (int i = packagePath.getNameCount() - 1; i >= 0; i--) {
				Path subPath = packagePath.subpath(0, i + 1);
				Path packagePart = root.resolve(subPath);
				if (isEmpty(packagePart)) {
					Files.deleteIfExists(packagePart);
				}
			}
		}

		private boolean isEmpty(Path path) {
			try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
				return !stream.iterator().hasNext();
			} catch (IOException e) {
				return false;
			}
		}

		private Path resolve(Path root, ClassEntry classEntry) {
			return root.resolve(classEntry.getFullName() + ".mapping");
		}
	};

	protected void writeRoot(PrintWriter writer, MappingTree<EntryMapping> mappings, ClassEntry classEntry) {
		Collection<Entry<?>> children = groupChildren(mappings.getChildren(classEntry));

		writer.println(writeClass(classEntry, mappings.getMapping(classEntry)));
		for (Entry<?> child : children) {
			writeEntry(writer, mappings, child, 1);
		}
	}

	protected void writeEntry(PrintWriter writer, MappingTree<EntryMapping> mappings, Entry<?> entry, int depth) {
		MappingNode<EntryMapping> node = mappings.findNode(entry);
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

		Collection<Entry<?>> children = groupChildren(node.getChildren());
		for (Entry<?> child : children) {
			writeEntry(writer, mappings, child, depth + 1);
		}
	}

	private Collection<Entry<?>> groupChildren(Collection<Entry<?>> children) {
		Collection<Entry<?>> result = new ArrayList<>(children.size());

		children.stream().filter(e -> e instanceof ClassEntry).forEach(result::add);
		children.stream().filter(e -> e instanceof FieldEntry).forEach(result::add);
		children.stream().filter(e -> e instanceof MethodEntry).forEach(result::add);
		children.stream().filter(e -> e instanceof LocalVariableEntry).forEach(result::add);

		return result;
	}

	protected String writeClass(ClassEntry entry, EntryMapping mapping) {
		StringBuilder builder = new StringBuilder("CLASS ");
		builder.append(entry.getFullName()).append(' ');
		writeMapping(builder, mapping);

		return builder.toString();
	}

	protected String writeMethod(MethodEntry entry, EntryMapping mapping) {
		StringBuilder builder = new StringBuilder("METHOD ");
		builder.append(entry.getName()).append(' ');
		writeMapping(builder, mapping);

		builder.append(entry.getDesc().toString()).append(' ');

		return builder.toString();
	}

	protected String writeField(FieldEntry entry, EntryMapping mapping) {
		StringBuilder builder = new StringBuilder("FIELD ");
		builder.append(entry.getName()).append(' ');
		writeMapping(builder, mapping);

		builder.append(entry.getDesc().toString()).append(' ');

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
		return builder.toString();
	}

	private Collection<Entry<?>> sorted(Iterable<Entry<?>> iterable) {
		if (iterable == null) {
			return Collections.emptyList();
		}
		List<Entry<?>> sorted = Lists.newArrayList(iterable);
		sorted.sort(Comparator.comparing(Entry::getName));
		return sorted;
	}
}
