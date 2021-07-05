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

package cuchaz.enigma.translation.mapping.serde.enigma;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import cuchaz.enigma.ProgressListener;
import cuchaz.enigma.translation.MappingTranslator;
import cuchaz.enigma.translation.Translator;
import cuchaz.enigma.translation.mapping.AccessModifier;
import cuchaz.enigma.translation.mapping.EntryMapping;
import cuchaz.enigma.translation.mapping.MappingDelta;
import cuchaz.enigma.translation.mapping.VoidEntryResolver;
import cuchaz.enigma.translation.mapping.serde.*;
import cuchaz.enigma.translation.mapping.tree.EntryTree;
import cuchaz.enigma.translation.mapping.tree.EntryTreeNode;
import cuchaz.enigma.translation.representation.entry.*;
import cuchaz.enigma.utils.I18n;

public enum EnigmaMappingsWriter implements MappingsWriter {
	FILE {
		@Override
		public void write(EntryTree<EntryMapping> mappings, MappingDelta<EntryMapping> delta, Path path, ProgressListener progress, MappingSaveParameters saveParameters) {
			Collection<ClassEntry> classes = mappings.getRootNodes()
					.filter(entry -> entry.getEntry() instanceof ClassEntry)
					.map(entry -> (ClassEntry) entry.getEntry())
					.toList();

			progress.init(classes.size(), I18n.translate("progress.mappings.enigma_file.writing"));

			int steps = 0;
			try (PrintWriter writer = new LfPrintWriter(Files.newBufferedWriter(path))) {
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
					.toList();

			applyDeletions(path, changedClasses, mappings, delta.getBaseMappings(), saveParameters.getFileNameFormat());

			progress.init(changedClasses.size(), I18n.translate("progress.mappings.enigma_directory.writing"));

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

					try (PrintWriter writer = new LfPrintWriter(Files.newBufferedWriter(classPath))) {
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

			Collection<ClassEntry> deletedClasses = deletedClassStream.toList();

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
				Path packagePart = root.resolve(subPath.toString());
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
	},
	ZIP {
		@Override
		public void write(EntryTree<EntryMapping> mappings, MappingDelta<EntryMapping> delta, Path zip, ProgressListener progress, MappingSaveParameters saveParameters) {
			try (FileSystem fs = FileSystems.newFileSystem(new URI("jar:file", null, zip.toUri().getPath(), ""), Collections.singletonMap("create", "true"))) {
				DIRECTORY.write(mappings, delta, fs.getPath("/"), progress, saveParameters);
			} catch (IOException e) {
				e.printStackTrace();
			} catch (URISyntaxException e) {
				throw new RuntimeException("Unexpected error creating URI for " + zip, e);
			}
		}
	};

	protected void writeRoot(PrintWriter writer, EntryTree<EntryMapping> mappings, ClassEntry classEntry) {
		Collection<Entry<?>> children = groupChildren(mappings.getChildren(classEntry));

		EntryMapping classEntryMapping = mappings.get(classEntry);

		if (classEntryMapping == null) {
			classEntryMapping = EntryMapping.DEFAULT;
		}

		writer.println(writeClass(classEntry, classEntryMapping).trim());
		if (classEntryMapping.javadoc() != null) {
			writeDocs(writer, classEntryMapping, 0);
		}

		for (Entry<?> child : children) {
			writeEntry(writer, mappings, child, 1);
		}
	}

	private void writeDocs(PrintWriter writer, EntryMapping mapping, int depth) {
		String jd = mapping.javadoc();
		if (jd != null) {
			for (String line : jd.split("\\R")) {
				writer.println(indent(EnigmaFormat.COMMENT + " " + MappingHelper.escape(line), depth + 1));
			}
		}
	}

	protected void writeEntry(PrintWriter writer, EntryTree<EntryMapping> mappings, Entry<?> entry, int depth) {
		EntryTreeNode<EntryMapping> node = mappings.findNode(entry);
		if (node == null) {
			return;
		}

		EntryMapping mapping = node.getValue();

		if (mapping == null) {
			mapping = EntryMapping.DEFAULT;
		}

		String line = null;
		if (entry instanceof ClassEntry classEntry) {
			line = writeClass(classEntry, mapping);
		} else if (entry instanceof MethodEntry methodEntry) {
			line = writeMethod(methodEntry, mapping);
		} else if (entry instanceof FieldEntry fieldEntry) {
			line = writeField(fieldEntry, mapping);
		} else if (entry instanceof LocalVariableEntry varEntry && mapping.targetName() != null) {
			line = writeArgument(varEntry, mapping);
		}

		if (line != null) {
			writer.println(indent(line, depth));
		}

		if (mapping.javadoc() != null) {
			writeDocs(writer, mapping, depth);
		}

		Collection<Entry<?>> children = groupChildren(node.getChildren());
		for (Entry<?> child : children) {
			writeEntry(writer, mappings, child, depth + 1);
		}
	}

	private Collection<Entry<?>> groupChildren(Collection<Entry<?>> children) {
		Collection<Entry<?>> result = new ArrayList<>(children.size());

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

		children.stream().filter(e -> e instanceof ClassEntry)
				.map(e -> (ClassEntry) e)
				.sorted()
				.forEach(result::add);

		return result;
	}

	protected String writeClass(ClassEntry entry, @Nonnull EntryMapping mapping) {
		StringBuilder builder = new StringBuilder(EnigmaFormat.CLASS + " ");
		builder.append(entry.getName()).append(' ');
		writeMapping(builder, mapping);

		return builder.toString();
	}

	protected String writeMethod(MethodEntry entry, @Nonnull EntryMapping mapping) {
		StringBuilder builder = new StringBuilder(EnigmaFormat.METHOD + " ");
		builder.append(entry.getName()).append(' ');
		writeMapping(builder, mapping);

		builder.append(entry.getDesc().toString());

		return builder.toString();
	}

	protected String writeField(FieldEntry entry, @Nonnull EntryMapping mapping) {
		StringBuilder builder = new StringBuilder(EnigmaFormat.FIELD + " ");
		builder.append(entry.getName()).append(' ');
		writeMapping(builder, mapping);

		builder.append(entry.getDesc().toString());

		return builder.toString();
	}

	protected String writeArgument(LocalVariableEntry entry, @Nonnull EntryMapping mapping) {
		return EnigmaFormat.PARAMETER + " " + entry.getIndex() + ' ' + mapping.targetName();
	}

	private void writeMapping(StringBuilder builder, EntryMapping mapping) {
		if (mapping.targetName() != null) {
			builder.append(mapping.targetName()).append(' ');
			if (mapping.accessModifier() != AccessModifier.UNCHANGED) {
				builder.append(mapping.accessModifier().getFormattedName()).append(' ');
			}
		} else if (mapping.accessModifier() != AccessModifier.UNCHANGED) {
			builder.append("- ").append(mapping.accessModifier().getFormattedName()).append(' ');
		}
	}

	private String indent(String line, int depth) {
		StringBuilder builder = new StringBuilder();
		builder.append("\t".repeat(Math.max(0, depth)));
		builder.append(line.trim());
		return builder.toString();
	}
}
