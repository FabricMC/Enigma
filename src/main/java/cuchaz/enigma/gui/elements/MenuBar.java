package cuchaz.enigma.gui.elements;

import cuchaz.enigma.config.Config;
import cuchaz.enigma.config.Themes;
import cuchaz.enigma.gui.Gui;
import cuchaz.enigma.gui.dialog.AboutDialog;
import cuchaz.enigma.gui.dialog.SearchDialog;
import cuchaz.enigma.gui.stats.StatsMember;
import cuchaz.enigma.translation.mapping.serde.MappingFormat;
import cuchaz.enigma.utils.LangUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

public class MenuBar extends JMenuBar {

	public final JMenuItem closeJarMenu;
	public final List<JMenuItem> openMappingsMenus;
	public final JMenuItem saveMappingsMenu;
	public final List<JMenuItem> saveMappingsMenus;
	public final JMenuItem closeMappingsMenu;
	public final JMenuItem dropMappingsMenu;
	public final JMenuItem exportSourceMenu;
	public final JMenuItem exportJarMenu;
	private final Gui gui;

	public MenuBar(Gui gui) {
		this.gui = gui;

		{
			JMenu menu = new JMenu(LangUtils.translate("menu.file"));
			this.add(menu);
			{
				JMenuItem item = new JMenuItem(LangUtils.translate("menu.file.jar.open"));
				menu.add(item);
				item.addActionListener(event -> {
					this.gui.jarFileChooser.setVisible(true);
					Path path = Paths.get(this.gui.jarFileChooser.getDirectory()).resolve(this.gui.jarFileChooser.getFile());
					if (Files.exists(path)) {
						gui.getController().openJar(path);
					}
				});
			}
			{
				JMenuItem item = new JMenuItem(LangUtils.translate("menu.file.jar.close"));
				menu.add(item);
				item.addActionListener(event -> this.gui.getController().closeJar());
				this.closeJarMenu = item;
			}
			menu.addSeparator();
			JMenu openMenu = new JMenu(LangUtils.translate("menu.file.mappings.open"));
			menu.add(openMenu);
			{
				openMappingsMenus = new ArrayList<>();
				for (MappingFormat format : MappingFormat.values()) {
					if (format.getReader() != null) {
						JMenuItem item = new JMenuItem(LangUtils.translate("mapping_format." + format.name().toLowerCase()));
						openMenu.add(item);
						item.addActionListener(event -> {
							if (this.gui.enigmaMappingsFileChooser.showOpenDialog(this.gui.getFrame()) == JFileChooser.APPROVE_OPTION) {
								File selectedFile = this.gui.enigmaMappingsFileChooser.getSelectedFile();
								this.gui.getController().openMappings(format, selectedFile.toPath());
							}
						});
						openMappingsMenus.add(item);
					}
				}
			}
			{
				JMenuItem item = new JMenuItem(LangUtils.translate("menu.file.mappings.save"));
				menu.add(item);
				item.addActionListener(event -> {
					this.gui.getController().saveMappings(this.gui.enigmaMappingsFileChooser.getSelectedFile().toPath());
				});
				item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
				this.saveMappingsMenu = item;
			}
			JMenu saveMenu = new JMenu(LangUtils.translate("menu.file.mappings.save_as"));
			menu.add(saveMenu);
			{
				saveMappingsMenus = new ArrayList<>();
				for (MappingFormat format : MappingFormat.values()) {
					if (format.getWriter() != null) {
						JMenuItem item = new JMenuItem(LangUtils.translate("mapping_format." + format.name().toLowerCase()));
						saveMenu.add(item);
						item.addActionListener(event -> {
							// TODO: Use a specific file chooser for it
							if (this.gui.enigmaMappingsFileChooser.showSaveDialog(this.gui.getFrame()) == JFileChooser.APPROVE_OPTION) {
								this.gui.getController().saveMappings(this.gui.enigmaMappingsFileChooser.getSelectedFile().toPath(), format);
								this.saveMappingsMenu.setEnabled(true);
							}
						});
						saveMappingsMenus.add(item);
					}
				}
			}
			{
				JMenuItem item = new JMenuItem(LangUtils.translate("menu.file.mappings.close"));
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
						}), LangUtils.translate("prompt.close.save"), LangUtils.translate("prompt.close.discard"), LangUtils.translate("prompt.close.cancel"));
					} else
						this.gui.getController().closeMappings();

				});
				this.closeMappingsMenu = item;
			}
			{
				JMenuItem item = new JMenuItem(LangUtils.translate("menu.file.mappings.drop"));
				menu.add(item);
				item.addActionListener(event -> this.gui.getController().dropMappings());
				this.dropMappingsMenu = item;
			}
			menu.addSeparator();
			{
				JMenuItem item = new JMenuItem(LangUtils.translate("menu.file.export.source"));
				menu.add(item);
				item.addActionListener(event -> {
					if (this.gui.exportSourceFileChooser.showSaveDialog(this.gui.getFrame()) == JFileChooser.APPROVE_OPTION) {
						this.gui.getController().exportSource(this.gui.exportSourceFileChooser.getSelectedFile().toPath());
					}
				});
				this.exportSourceMenu = item;
			}
			{
				JMenuItem item = new JMenuItem(LangUtils.translate("menu.file.export.jar"));
				menu.add(item);
				item.addActionListener(event -> {
					this.gui.exportJarFileChooser.setVisible(true);
					if (this.gui.exportJarFileChooser.getFile() != null) {
						Path path = Paths.get(this.gui.exportJarFileChooser.getDirectory(), this.gui.exportJarFileChooser.getFile());
						this.gui.getController().exportJar(path);
					}
				});
				this.exportJarMenu = item;
			}
			menu.addSeparator();
			{
				JMenuItem stats = new JMenuItem(LangUtils.translate("menu.file.stats"));

				stats.addActionListener(event -> {
                    JFrame frame = new JFrame(LangUtils.translate("menu.file.stats.title"));
					Container pane = frame.getContentPane();
					pane.setLayout(new FlowLayout());

					Map<StatsMember, JCheckBox> checkboxes = Arrays
							.stream(StatsMember.values())
							.collect(Collectors.toMap(m -> m, m -> {
								JCheckBox checkbox = new JCheckBox(LangUtils.translate("type." + m.name().toLowerCase()));
								pane.add(checkbox);
								return checkbox;
							}));

					JButton button = new JButton(LangUtils.translate("menu.file.stats.generate"));

					button.addActionListener(e -> {
						Set<StatsMember> includedMembers = checkboxes
								.entrySet()
								.stream()
								.filter(entry -> entry.getValue().isSelected())
								.map(Map.Entry::getKey)
								.collect(Collectors.toSet());

						frame.setVisible(false);
						frame.dispose();
						gui.getController().openStats(includedMembers);
					});

					pane.add(button);
                    frame.pack();
                    frame.setVisible(true);
                });

				menu.add(stats);
			}
			menu.addSeparator();
			{
				JMenuItem item = new JMenuItem(LangUtils.translate("menu.file.exit"));
				menu.add(item);
				item.addActionListener(event -> this.gui.close());
			}
		}
		{
			JMenu menu = new JMenu(LangUtils.translate("menu.view"));
			this.add(menu);
			{
				JMenu themes = new JMenu(LangUtils.translate("menu.view.themes"));
				menu.add(themes);
				for (Config.LookAndFeel lookAndFeel : Config.LookAndFeel.values()) {
					JMenuItem theme = new JMenuItem(LangUtils.translate("menu.view.themes." + lookAndFeel.name().toLowerCase()));
					themes.add(theme);
					theme.addActionListener(event -> Themes.setLookAndFeel(gui, lookAndFeel));
				}
				
				JMenu languages = new JMenu(LangUtils.translate("menu.view.languages"));
				menu.add(languages);
				for (String lang : LangUtils.getAvailableLanguages()) {
					JMenuItem language = new JMenuItem(LangUtils.getLanguageName(lang));
					languages.add(language);
					language.addActionListener(event -> LangUtils.setLanguage(lang));
				}

				JMenuItem search = new JMenuItem(LangUtils.translate("menu.view.search"));
				search.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, InputEvent.SHIFT_MASK));
				menu.add(search);
				search.addActionListener(event -> {
					if (this.gui.getController().project != null) {
						new SearchDialog(this.gui).show();
					}
				});

			}
		}
		{
			JMenu menu = new JMenu(LangUtils.translate("menu.help"));
			this.add(menu);
			{
				JMenuItem item = new JMenuItem(LangUtils.translate("menu.help.about"));
				menu.add(item);
				item.addActionListener(event -> AboutDialog.show(this.gui.getFrame()));
			}
			{
				JMenuItem item = new JMenuItem(LangUtils.translate("menu.help.github"));
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
}
