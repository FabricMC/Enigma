package cuchaz.enigma.gui.elements;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.jar.JarFile;

import javax.swing.*;

import cuchaz.enigma.gui.Gui;
import cuchaz.enigma.gui.dialog.AboutDialog;
import cuchaz.enigma.throwables.MappingParseException;

public class MenuBar extends JMenuBar {

    private final Gui gui;

    public final JMenuItem closeJarMenu;

    public final JMenuItem openEnigmaMappingsMenu;

    public final JMenuItem saveMappingsMenu;
    public final JMenuItem saveMappingEnigmaFileMenu;
    public final JMenuItem saveMappingEnigmaDirectoryMenu;
    public final JMenuItem saveMappingsSrgMenu;
    public final JMenuItem closeMappingsMenu;


    public final JMenuItem exportSourceMenu;
    public final JMenuItem exportJarMenu;

    public MenuBar(Gui gui) {
        this.gui = gui;

        {
            JMenu menu = new JMenu("File");
            this.add(menu);
            {
                JMenuItem item = new JMenuItem("Open Jar...");
                menu.add(item);
                item.addActionListener(event -> {
                    if (this.gui.jarFileChooser.showOpenDialog(this.gui.getFrame()) == JFileChooser.APPROVE_OPTION) {
                        // load the jar in a separate thread
                        new Thread() {
                            @Override
                            public void run() {
                                try {
                                    gui.getController().openJar(new JarFile(gui.jarFileChooser.getSelectedFile()));
                                } catch (IOException ex) {
                                    throw new Error(ex);
                                }
                            }
                        }.start();
                    }
                });
            }
            {
                JMenuItem item = new JMenuItem("Close Jar");
                menu.add(item);
                item.addActionListener(event -> this.gui.getController().closeJar());
                this.closeJarMenu = item;
            }
            menu.addSeparator();
            JMenu openMenu = new JMenu("Open Mappings...");
            menu.add(openMenu);
            {
                JMenuItem item = new JMenuItem("Enigma");
                openMenu.add(item);
                item.addActionListener(event -> {
                    if (this.gui.enigmaMappingsFileChooser.showOpenDialog(this.gui.getFrame()) == JFileChooser.APPROVE_OPTION) {
                        try {
                            this.gui.getController().openEnigmaMappings(this.gui.enigmaMappingsFileChooser.getSelectedFile());
                        } catch (IOException ex) {
                            throw new Error(ex);
                        } catch (MappingParseException ex) {
                            JOptionPane.showMessageDialog(this.gui.getFrame(), ex.getMessage());
                        }
                    }
                });
                this.openEnigmaMappingsMenu = item;
            }
            {
                JMenuItem item = new JMenuItem("Save Mappings");
                menu.add(item);
                item.addActionListener(event -> {
                    try {
                        this.gui.getController().saveMappings(this.gui.enigmaMappingsFileChooser.getSelectedFile());
                    } catch (IOException ex) {
                        throw new Error(ex);
                    }
                });
                item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
                this.saveMappingsMenu = item;
            }
            JMenu saveMenu = new JMenu("Save Mappings As...");
            menu.add(saveMenu);
            {
                JMenuItem item = new JMenuItem("Enigma (single file)");
                saveMenu.add(item);
                item.addActionListener(event -> {
                    // TODO: Use a specific file chooser for it
                    if (this.gui.enigmaMappingsFileChooser.showSaveDialog(this.gui.getFrame()) == JFileChooser.APPROVE_OPTION) {
                        try {
                            this.gui.getController().saveEnigmaMappings(this.gui.enigmaMappingsFileChooser.getSelectedFile(), false);
                            this.saveMappingsMenu.setEnabled(true);
                        } catch (IOException ex) {
                            throw new Error(ex);
                        }
                    }
                });
                this.saveMappingEnigmaFileMenu = item;
            }
            {
                JMenuItem item = new JMenuItem("Enigma (directory)");
                saveMenu.add(item);
                item.addActionListener(event -> {
                    // TODO: Use a specific file chooser for it
                    if (this.gui.enigmaMappingsFileChooser.showSaveDialog(this.gui.getFrame()) == JFileChooser.APPROVE_OPTION) {
                        try {
                            this.gui.getController().saveEnigmaMappings(this.gui.enigmaMappingsFileChooser.getSelectedFile(), true);
                            this.saveMappingsMenu.setEnabled(true);
                        } catch (IOException ex) {
                            throw new Error(ex);
                        }
                    }
                });
                item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));
                this.saveMappingEnigmaDirectoryMenu = item;
            }
            {
                JMenuItem item = new JMenuItem("SRG (single file)");
                saveMenu.add(item);
                item.addActionListener(event -> {
                    // TODO: Use a specific file chooser for it
                    if (this.gui.enigmaMappingsFileChooser.showSaveDialog(this.gui.getFrame()) == JFileChooser.APPROVE_OPTION) {
                        try {
                            this.gui.getController().saveSRGMappings(this.gui.enigmaMappingsFileChooser.getSelectedFile());
                            this.saveMappingsMenu.setEnabled(true);
                        } catch (IOException ex) {
                            throw new Error(ex);
                        }
                    }
                });
                this.saveMappingsSrgMenu = item;
            }
            {
                JMenuItem item = new JMenuItem("Close Mappings");
                menu.add(item);
                item.addActionListener(event -> {
                    if (this.gui.getController().isDirty())
                    {
                        this.gui.showDiscardDiag((response -> {
                            if (response == JOptionPane.YES_OPTION)
                            {
                                try
                                {
                                    gui.saveMapping();
                                    this.gui.getController().closeMappings();
                                } catch (IOException e)
                                {
                                    throw new Error(e);
                                }
                            }
                            else if (response == JOptionPane.NO_OPTION)
                                this.gui.getController().closeMappings();
                            return null;
                        }), "Save and close", "Discard changes", "Cancel");
                    }
                    else
                        this.gui.getController().closeMappings();

                });
                this.closeMappingsMenu = item;
            }
            menu.addSeparator();
            {
                JMenuItem item = new JMenuItem("Export Source...");
                menu.add(item);
                item.addActionListener(event -> {
                    if (this.gui.exportSourceFileChooser.showSaveDialog(this.gui.getFrame()) == JFileChooser.APPROVE_OPTION) {
                        this.gui.getController().exportSource(this.gui.exportSourceFileChooser.getSelectedFile());
                    }
                });
                this.exportSourceMenu = item;
            }
            {
                JMenuItem item = new JMenuItem("Export Jar...");
                menu.add(item);
                item.addActionListener(event -> {
                    if (this.gui.exportJarFileChooser.showSaveDialog(this.gui.getFrame()) == JFileChooser.APPROVE_OPTION) {
                        this.gui.getController().exportJar(this.gui.exportJarFileChooser.getSelectedFile());
                    }
                });
                this.exportJarMenu = item;
            }
            menu.addSeparator();
            {
                JMenuItem item = new JMenuItem("Exit");
                menu.add(item);
                item.addActionListener(event -> this.gui.close());
            }
        }
        {
            JMenu menu = new JMenu("Help");
            this.add(menu);
            {
                JMenuItem item = new JMenuItem("About");
                menu.add(item);
                item.addActionListener(event -> AboutDialog.show(this.gui.getFrame()));
            }
        }
    }
}
