package cuchaz.enigma.translation.mapping.serde;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.Set;

import net.fabricmc.mappingio.MappedElementKind;
import net.fabricmc.mappingio.MappingVisitor;
import net.fabricmc.mappingio.adapter.ForwardingMappingVisitor;

import cuchaz.enigma.ProgressListener;

final class ProgressTrackingMappingVisitor extends ForwardingMappingVisitor {
	private final Set<String> classNames;
	private final ProgressListener progressListener;
	private int progress = 0;

	ProgressTrackingMappingVisitor(MappingVisitor next, Set<String> classNames, ProgressListener progressListener) {
		super(next);
		this.classNames = classNames;
		this.progressListener = progressListener;
	}

	@Override
	public void visitDstName(MappedElementKind targetKind, int namespace, String name) throws IOException {
		if (targetKind == MappedElementKind.CLASS && classNames.contains(name)) {
			progressListener.step(++progress, name);
		}

		super.visitDstName(targetKind, namespace, name);
	}

	static void trackLoadingProgress(MappingVisitor next, Path path, MappingFormat format, ProgressListener progressListener, VisitorWithProgressConsumer consumer) throws IOException {
		if (format != MappingFormat.ENIGMA_DIRECTORY) {
			consumer.accept(next, 1);
			return;
		}

		Set<String> classNames = collectClassNames(path);
		consumer.accept(new ProgressTrackingMappingVisitor(next, classNames, progressListener), classNames.size());
	}

	private static Set<String> collectClassNames(Path dir) throws IOException {
		Set<String> names = new HashSet<>();

		Files.walkFileTree(dir, new SimpleFileVisitor<>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
				// Matches the logic for finding files in mapping-io's EnigmaDirReader.
				String extension = "." + net.fabricmc.mappingio.format.MappingFormat.ENIGMA_FILE.fileExt;

				if (file.getFileName().toString().endsWith(extension)) {
					String filePath = dir.relativize(file).toString().replace(dir.getFileSystem().getSeparator(), "/");
					String className = filePath.substring(0, filePath.length() - extension.length());
					names.add(className);
				}

				return FileVisitResult.CONTINUE;
			}
		});

		return names;
	}

	@FunctionalInterface
	interface VisitorWithProgressConsumer {
		void accept(MappingVisitor visitor, int totalWork) throws IOException;
	}
}
