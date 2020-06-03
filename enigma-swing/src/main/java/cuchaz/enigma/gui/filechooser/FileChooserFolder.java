package cuchaz.enigma.gui.filechooser;

import javax.swing.*;

public class FileChooserFolder extends JFileChooser {

	public FileChooserFolder() {
		this.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		this.setAcceptAllFileFilterUsed(false);
	}
}
