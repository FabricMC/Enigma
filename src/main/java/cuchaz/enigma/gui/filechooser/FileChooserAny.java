package cuchaz.enigma.gui.filechooser;

import javax.swing.*;

public class FileChooserAny extends JFileChooser
{
    public FileChooserAny() {
        this.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        this.setAcceptAllFileFilterUsed(false);
    }
}