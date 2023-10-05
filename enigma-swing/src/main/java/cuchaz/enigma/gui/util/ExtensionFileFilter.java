package cuchaz.enigma.gui.util;

import java.io.File;
import java.util.List;
import java.util.StringJoiner;

import javax.swing.filechooser.FileFilter;

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
}
