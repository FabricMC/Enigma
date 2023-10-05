package cuchaz.enigma.gui.util;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.StringJoiner;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import cuchaz.enigma.translation.mapping.serde.MappingFormat;
import cuchaz.enigma.utils.I18n;

public final class ExtensionFileFilter extends FileFilter {
	private final String formatName;
	private final List<String> extensions;

	/**
	 * Constructs an {@code ExtensionFileFilter}.
	 *
	 * @param formatName the human-readable name of the file format
	 * @param extensions the file extensions with their leading dots (e.g. {@code .txt})
	 */
	public ExtensionFileFilter(String formatName, String... extensions) {
		this.formatName = formatName;
		this.extensions = List.of(extensions);
	}

	public List<String> getExtensions() {
		return extensions;
	}

	@Override
	public boolean accept(File f) {
		// Always accept directories so the user can see them.
		if (f.isDirectory()) {
			return true;
		}

		for (String extension : extensions) {
			if (f.getName().endsWith(extension)) {
				return true;
			}
		}

		return false;
	}

	@Override
	public String getDescription() {
		var joiner = new StringJoiner(", ");

		for (String extension : extensions) {
			joiner.add("*" + extension);
		}

		return I18n.translateFormatted("menu.file.mappings.file_filter", formatName, joiner.toString());
	}

	/**
	 * Sets up a file chooser with a mapping format. This method resets the choosable filters,
	 * and adds and selects a new filter based on the provided mapping format.
	 *
	 * @param fileChooser the mapping format
	 */
	public static void setupFileChooser(JFileChooser fileChooser, MappingFormat format) {
		// Remove previous custom filters.
		fileChooser.resetChoosableFileFilters();

		if (format.getFileType().isDirectory()) {
			fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		} else {
			fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
			String formatName = I18n.translate("mapping_format." + format.name().toLowerCase(Locale.ROOT));
			var filter = new ExtensionFileFilter(formatName, format.getFileType().extension());
			// Add our new filter to the list...
			fileChooser.addChoosableFileFilter(filter);
			// ...and choose it as the default.
			fileChooser.setFileFilter(filter);
		}
	}

	/**
	 * Fixes a missing file extension in a save file path when the selected filter
	 * is an {@code ExtensionFileFilter}.
	 *
	 * @param fileChooser the file chooser to check
	 * @return the fixed path
	 */
	public static Path getSavePath(JFileChooser fileChooser) {
		Path savePath = fileChooser.getSelectedFile().toPath();

		if (fileChooser.getFileFilter() instanceof ExtensionFileFilter extensionFilter) {
			// Check that the file name ends with the extension.
			String fileName = savePath.getFileName().toString();
			boolean hasExtension = false;

			for (String extension : extensionFilter.getExtensions()) {
				if (fileName.endsWith(extension)) {
					hasExtension = true;
					break;
				}
			}

			if (!hasExtension) {
				String defaultExtension = extensionFilter.getExtensions().get(0);
				// If not, add the extension.
				savePath = savePath.resolveSibling(fileName + defaultExtension);
				// Store the adjusted file, so that it shows up properly
				// the next time this dialog is used.
				fileChooser.setSelectedFile(savePath.toFile());
			}
		}

		return savePath;
	}
}
