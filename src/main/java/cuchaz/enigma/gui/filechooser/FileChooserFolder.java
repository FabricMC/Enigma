package cuchaz.enigma.gui.filechooser;

import javax.swing.JFileChooser;

public class FileChooserFolder extends JFileChooser {

    public FileChooserFolder() {
        this.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        this.setAcceptAllFileFilterUsed(false);
    }
}
