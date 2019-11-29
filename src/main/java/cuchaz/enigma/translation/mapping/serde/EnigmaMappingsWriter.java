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

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import cuchaz.enigma.ProgressListener;
import cuchaz.enigma.translation.MappingTranslator;
import cuchaz.enigma.translation.Translator;
import cuchaz.enigma.translation.mapping.AccessModifier;
import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.mapping.MappingDelta;
import cuchaz.enigma.translation.mapping.MappingFileNameFormat;
import cuchaz.enigma.translation.mapping.MappingSaveParameters;
import cuchaz.enigma.translation.mapping.VoidEntryResolver;
import cuchaz.enigma.translation.mapping.tree.EntryTree;
import cuchaz.enigma.translation.mapping.tree.EntryTreeNode;
import cuchaz.enigma.translation.representation.entry.ClassEntry;
import cuchaz.enigma.translation.representation.entry.Entry;
import cuchaz.enigma.translation.representation.entry.FieldEntry;
import cuchaz.enigma.translation.representation.entry.LocalVariableEntry;
import cuchaz.enigma.translation.representation.entry.MethodEntry;
import cuchaz.enigma.utils.LFPrintWriter;
import javafx.util.Pair;

public enum EnigmaMappingsWriter implements MappingsWriter {
	FILE {
		@Override
		public void write(EntryTree<EntryMapping> mappings, MappingDelta<EntryMapping> delta, Path path, ProgressListener progress, MappingSaveParameters saveParameters) {
			Collection<ClassEntry> classes = mappings.getRootNodes()
							.filter(entry -> entry instanceof ClassEntry)
							.map(entry -> (ClassEntry) entry)
							.collect(Collectors.toList());

			progress.init(classes.size(), "Writing classes");

			int steps = 0;
			try (PrintWriter writer = new LFPrintWriter(Files.newBufferedWriter(path))) {
				for (ClassEntry classEntry : classes) {
					progress.step(steps++, classEntry.getFullName());
					writeRoot(writer, mappings, classEntry);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	},
	DIRECTORY {
		@Override
		public void write(EntryTree<EntryMapping> mappings, MappingDelta<EntryMapping> delta, Path path, ProgressListener progress, MappingSaveParameters saveParameters) {
			Collection<ClassEntry> changedClasses = delta.getChangedRoots()
							.filter(entry -> entry instanceof ClassEntry)
							.map(entry -> (ClassEntry) entry)
							.collect(Collectors.toList());

			applyDeletions(path, changedClasses, mappings, delta.getBaseMappings(), saveParameters.getFileNameFormat());

			progress.init(changedClasses.size(), "Writing classes");

			AtomicInteger steps = new AtomicInteger();

			Translator translator = new MappingTranslator(mappings, VoidEntryResolver.INSTANCE);
			changedClasses.parallelStream().forEach(classEntry -> {
				progress.step(steps.getAndIncrement(), classEntry.getFullName());

				try {
					ClassEntry fileEntry = classEntry;
					if (saveParameters.getFileNameFormat() == MappingFileNameFormat.BY_DEOBF) {
						fileEntry = translator.translate(fileEntry);
					}

					Path classPath = resolve(path, fileEntry);
					Files.createDirectories(classPath.getParent());
					Files.deleteIfExists(classPath);

					try (PrintWriter writer = new LFPrintWriter(Files.newBufferedWriter(classPath))) {
						writeRoot(writer, mappings, classEntry);
					}
				} catch (Throwable t) {
					System.err.println("Failed to write class '" + classEntry.getFullName() + "'");
					t.printStackTrace();
				}
			});
		}

		private void applyDeletions(Path root, Collection<ClassEntry> changedClasses, EntryTree<EntryMapping> mappings, EntryTree<EntryMapping> oldMappings, MappingFileNameFormat fileNameFormat) {
			Translator oldMappingTranslator = new MappingTranslator(oldMappings, VoidEntryResolver.INSTANCE);

			Stream<ClassEntry> deletedClassStream = changedClasses.stream()
							.filter(e -> !Objects.equals(oldMappings.get(e), mappings.get(e)));

			if (fileNameFormat == MappingFileNameFormat.BY_DEOBF) {
				deletedClassStream = deletedClassStream.map(oldMappingTranslator::translate);
			}

			Collection<ClassEntry> deletedClasses = deletedClassStream.collect(Collectors.toList());

			for (ClassEntry classEntry : deletedClasses) {
				try {
					Files.deleteIfExists(resolve(root, classEntry));
				} catch (IOException e) {
					System.err.println("Failed to delete deleted class '" + classEntry + "'");
					e.printStackTrace();
				}
			}

			for (ClassEntry classEntry : deletedClasses) {
				String packageName = classEntry.getPackageName();
				if (packageName != null) {
					Path packagePath = Paths.get(packageName);
					try {
						deleteDeadPackages(root, packagePath);
					} catch (IOException e) {
						System.err.println("Failed to delete dead package '" + packageName + "'");
						e.printStackTrace();
					}
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

	protected void writeRoot(PrintWriter writer, EntryTree<EntryMapping> mappings, ClassEntry classEntry) {
		Collection<Entry<?>> children = groupChildren(mappings.getChildren(classEntry));

		EntryMapping classEntryMapping = mappings.get(classEntry);

		writer.println(writeClass(classEntry, classEntryMapping).trim());
		if (classEntryMapping != null && classEntryMapping.getJavadoc() != null) {
			writeDocs(writer, classEntryMapping, 0);
		}

		for (Entry<?> child : children) {
			writeEntry(writer, mappings, child, 1);
		}

	}

	private void writeDocs(PrintWriter writer, EntryMapping mapping, int depth) {
		String jd = mapping.getJavadoc();
		if (jd != null) {
			for (String line : jd.split("\\R")) {
				writer.println(indent(EnigmaFormat.COMMENT.toUpperCase() + " " + MappingHelper.escape(line), depth + 1));
			}
		}
	}

	protected void writeEntry(PrintWriter writer, EntryTree<EntryMapping> mappings, Entry<?> entry, int depth) {
		EntryTreeNode<EntryMapping> node = mappings.findNode(entry);
		if (node == null) {
			return;
		}

		EntryMapping mapping = node.getValue();

		if (entry instanceof ClassEntry) {
			String line = writeClass((ClassEntry) entry, mapping);
			writer.println(indent(line, depth));
		} else if (entry instanceof MethodEntry) {
			String line = writeMethod((MethodEntry) entry, mapping);
			writer.println(indent(line, depth));
		} else if (entry instanceof FieldEntry) {
			String line = writeField((FieldEntry) entry, mapping);
			writer.println(indent(line, depth));
		} else if (entry instanceof LocalVariableEntry && mapping != null) {
			String line = writeArgument((LocalVariableEntry) entry, mapping);
			writer.println(indent(line, depth));
		}
		if (mapping != null && mapping.getJavadoc() != null) {
			writeDocs(writer, mapping, depth);
		}

		Collection<Entry<?>> children = groupChildren(node.getChildren());
		for (Entry<?> child : children) {
			writeEntry(writer, mappings, child, depth + 1);
		}
	}

	private Collection<Entry<?>> groupChildren(Collection<Entry<?>> children) {
		Collection<Entry<?>> result = new ArrayList<>(children.size());

		children.stream().filter(e -> e instanceof ClassEntry)
						.map(e -> (ClassEntry) e)
						.sorted()
						.forEach(result::add);

		children.stream().filter(e -> e instanceof FieldEntry)
						.map(e -> (FieldEntry) e)
						.sorted()
						.forEach(result::add);

		children.stream().filter(e -> e instanceof MethodEntry)
						.map(e -> (MethodEntry) e)
						.sorted()
						.forEach(result::add);

		children.stream().filter(e -> e instanceof LocalVariableEntry)
						.map(e -> (LocalVariableEntry) e)
						.sorted()
						.forEach(result::add);

		return result;
	}

	protected String writeClass(ClassEntry entry, EntryMapping mapping) {
		StringBuilder builder = new StringBuilder("CLASS ");
		builder.append(entry.getName()).append(' ');
		writeMapping(builder, mapping);

		return builder.toString();
	}

	protected String writeMethod(MethodEntry entry, EntryMapping mapping) {
		StringBuilder builder = new StringBuilder("METHOD ");
		builder.append(entry.getName()).append(' ');
		writeMapping(builder, mapping);

		builder.append(entry.getDesc().toString());

		return builder.toString();
	}

	protected String writeField(FieldEntry entry, EntryMapping mapping) {
		StringBuilder builder = new StringBuilder("FIELD ");
		builder.append(entry.getName()).append(' ');
		writeMapping(builder, mapping);

		builder.append(entry.getDesc().toString());

		return builder.toString();
	}

	protected String writeArgument(LocalVariableEntry entry, EntryMapping mapping) {
		return "ARG " + entry.getIndex() + ' ' + mapping.getTargetName();
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
		builder.append(line.trim());
		return builder.toString();
	}
}
