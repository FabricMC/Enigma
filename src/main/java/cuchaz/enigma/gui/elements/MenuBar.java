package cuchaz.enigma.gui.elements;

import cuchaz.enigma.config.Config;
import cuchaz.enigma.config.Themes;
import cuchaz.enigma.gui.Gui;
import cuchaz.enigma.gui.dialog.AboutDialog;
import cuchaz.enigma.gui.dialog.SearchDialog;
import cuchaz.enigma.translation.mapping.serde.MappingFormat;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.jar.JarFile;

public class MenuBar extends JMenuBar {

	public final JMenuItem closeJarMenu;
	public final JMenuItem openEnigmaMappingsMenu;
	public final JMenuItem openTinyMappingsMenu;
	public final JMenuItem saveMappingsMenu;
	public final JMenuItem saveMappingEnigmaFileMenu;
	public final JMenuItem saveMappingEnigmaDirectoryMenu;
	public final JMenuItem saveMappingsSrgMenu;
	public final JMenuItem closeMappingsMenu;
	public final JMenuItem exportSourceMenu;
	public final JMenuItem exportJarMenu;
	private final Gui gui;

	public MenuBar(Gui gui) {
		this.gui = gui;

		{
			JMenu menu = new JMenu("File");
			this.add(menu);
			{
				JMenuItem item = new JMenuItem("Open Jar...");
				menu.add(item);
				item.addActionListener(event -> {
					this.gui.jarFileChooser.setVisible(true);
					File file = new File(this.gui.jarFileChooser.getDirectory() + File.separator + this.gui.jarFileChooser.getFile());
					if (file.exists()) {
						// load the jar in a separate thread
						new Thread(() ->
						{
							try {
								gui.getController().openJar(new JarFile(file));
							} catch (IOException ex) {
								throw new Error(ex);
							}
						}).start();
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
						File selectedFile = this.gui.enigmaMappingsFileChooser.getSelectedFile();
						MappingFormat format = selectedFile.isDirectory() ? MappingFormat.ENIGMA_DIRECTORY : MappingFormat.ENIGMA_FILE;
						this.gui.getController().openMappings(format, selectedFile.toPath());
					}
				});
				this.openEnigmaMappingsMenu = item;

				item = new JMenuItem("Tiny");
				openMenu.add(item);
				item.addActionListener(event -> {
					this.gui.tinyMappingsFileChooser.setVisible(true);
					File file = new File(this.gui.tinyMappingsFileChooser.getDirectory() + File.separator + this.gui.tinyMappingsFileChooser.getFile());
					if (file.exists()) {
						this.gui.getController().openMappings(MappingFormat.TINY_FILE, file.toPath());
					}
				});
				this.openTinyMappingsMenu = item;
			}
			{
				JMenuItem item = new JMenuItem("Save Mappings");
				menu.add(item);
				item.addActionListener(event -> {
					this.gui.getController().saveMappings(this.gui.enigmaMappingsFileChooser.getSelectedFile().toPath());
				});
				item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
				this.saveMappingsMenu = item;
			}
			JMenu saveMenu = new JMenu("Save Mappings As...");
			menu.add(saveMenu);
			{
				JMenuItem item = new JMenuItem("Enigma (single file)");
				configureSaveMenuItem(saveMenu, item, MappingFormat.ENIGMA_FILE);
				this.saveMappingEnigmaFileMenu = item;
			}
			{
				JMenuItem item = new JMenuItem("Enigma (directory)");
				configureSaveMenuItem(saveMenu, item, MappingFormat.ENIGMA_DIRECTORY);
				item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));
				this.saveMappingEnigmaDirectoryMenu = item;
			}
			{
				JMenuItem item = new JMenuItem("SRG (single file)");
				configureSaveMenuItem(saveMenu, item, MappingFormat.SRG_FILE);
				this.saveMappingsSrgMenu = item;
			}
			{
				JMenuItem item = new JMenuItem("Close Mappings");
				menu.add(item);
				item.addActionListener(event -> {
					if (this.gui.getController().isDirty()) {
						this.gui.showDiscardDiag((response -> {
							if (response == JOptionPane.YES_OPTION) {
								gui.saveMapping();
								this.gui.getController().closeMappings();
							} else if (response == JOptionPane.NO_OPTION)
								this.gui.getController().closeMappings();
							return null;
						}), "Save and close", "Discard changes", "Cancel");
					} else
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
					this.gui.exportJarFileChooser.setVisible(true);
					if (this.gui.exportJarFileChooser.getFile() != null) {
						File file = new File(this.gui.exportJarFileChooser.getDirectory() + File.separator + this.gui.exportJarFileChooser.getFile());
						this.gui.getController().exportJar(file);
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
			JMenu menu = new JMenu("View");
			this.add(menu);
			{
				JMenu themes = new JMenu("Themes");
				menu.add(themes);
				for (Config.LookAndFeel lookAndFeel : Config.LookAndFeel.values()) {
					JMenuItem theme = new JMenuItem(lookAndFeel.getName());
					themes.add(theme);
					theme.addActionListener(event -> Themes.setLookAndFeel(gui, lookAndFeel));
				}

				JMenuItem search = new JMenuItem("Search");
				search.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, InputEvent.SHIFT_MASK));
				menu.add(search);
				search.addActionListener(event -> {
					if (this.gui.getController().getDeobfuscator() != null) {
						new SearchDialog(this.gui).show();
					}
				});

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
			{
				JMenuItem item = new JMenuItem("GitHub Page");
				menu.add(item);
				item.addActionListener(event -> {
					try {
						Desktop.getDesktop().browse(new URL("https://github.com/FabricMC/Enigma").toURI());
					} catch (URISyntaxException | IOException ignored) {
					}
				});
			}
		}
	}

	private void configureSaveMenuItem(JMenu menu, JMenuItem item, MappingFormat saveFormat) {
		menu.add(item);
		item.addActionListener(event -> {
			// TODO: Use a specific file chooser for it
			if (this.gui.enigmaMappingsFileChooser.showSaveDialog(this.gui.getFrame()) == JFileChooser.APPROVE_OPTION) {
				this.gui.getController().saveMappings(saveFormat, this.gui.enigmaMappingsFileChooser.getSelectedFile().toPath());
				this.saveMappingsMenu.setEnabled(true);
			}
		});
	}
}
