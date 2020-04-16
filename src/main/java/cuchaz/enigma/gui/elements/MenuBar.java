package cuchaz.enigma.gui.elements;

import cuchaz.enigma.config.Config;
import cuchaz.enigma.config.Themes;
import cuchaz.enigma.gui.Gui;
import cuchaz.enigma.gui.dialog.AboutDialog;
import cuchaz.enigma.gui.dialog.ConnectToServerDialog;
import cuchaz.enigma.gui.dialog.SearchDialog;
import cuchaz.enigma.gui.stats.StatsMember;
import cuchaz.enigma.network.EnigmaServer;
import cuchaz.enigma.translation.mapping.serde.MappingFormat;
import cuchaz.enigma.utils.I18n;

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
	public final JMenuItem connectToServerMenu;
	public final JMenuItem startServerMenu;
	private final Gui gui;

	public MenuBar(Gui gui) {
		this.gui = gui;

		{
			JMenu menu = new JMenu(I18n.translate("menu.file"));
			this.add(menu);
			{
				JMenuItem item = new JMenuItem(I18n.translate("menu.file.jar.open"));
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
				JMenuItem item = new JMenuItem(I18n.translate("menu.file.jar.close"));
				menu.add(item);
				item.addActionListener(event -> this.gui.getController().closeJar());
				this.closeJarMenu = item;
			}
			menu.addSeparator();
			JMenu openMenu = new JMenu(I18n.translate("menu.file.mappings.open"));
			menu.add(openMenu);
			{
				openMappingsMenus = new ArrayList<>();
				for (MappingFormat format : MappingFormat.values()) {
					if (format.getReader() != null) {
						JMenuItem item = new JMenuItem(I18n.translate("mapping_format." + format.name().toLowerCase(Locale.ROOT)));
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
				JMenuItem item = new JMenuItem(I18n.translate("menu.file.mappings.save"));
				menu.add(item);
				item.addActionListener(event -> {
					this.gui.getController().saveMappings(this.gui.enigmaMappingsFileChooser.getSelectedFile().toPath());
				});
				item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
				this.saveMappingsMenu = item;
			}
			JMenu saveMenu = new JMenu(I18n.translate("menu.file.mappings.save_as"));
			menu.add(saveMenu);
			{
				saveMappingsMenus = new ArrayList<>();
				for (MappingFormat format : MappingFormat.values()) {
					if (format.getWriter() != null) {
						JMenuItem item = new JMenuItem(I18n.translate("mapping_format." + format.name().toLowerCase(Locale.ROOT)));
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
				JMenuItem item = new JMenuItem(I18n.translate("menu.file.mappings.close"));
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
						}), I18n.translate("prompt.close.save"), I18n.translate("prompt.close.discard"), I18n.translate("prompt.close.cancel"));
					} else
						this.gui.getController().closeMappings();

				});
				this.closeMappingsMenu = item;
			}
			{
				JMenuItem item = new JMenuItem(I18n.translate("menu.file.mappings.drop"));
				menu.add(item);
				item.addActionListener(event -> this.gui.getController().dropMappings());
				this.dropMappingsMenu = item;
			}
			menu.addSeparator();
			{
				JMenuItem item = new JMenuItem(I18n.translate("menu.file.export.source"));
				menu.add(item);
				item.addActionListener(event -> {
					if (this.gui.exportSourceFileChooser.showSaveDialog(this.gui.getFrame()) == JFileChooser.APPROVE_OPTION) {
						this.gui.getController().exportSource(this.gui.exportSourceFileChooser.getSelectedFile().toPath());
					}
				});
				this.exportSourceMenu = item;
			}
			{
				JMenuItem item = new JMenuItem(I18n.translate("menu.file.export.jar"));
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
				JMenuItem stats = new JMenuItem(I18n.translate("menu.file.stats"));

				stats.addActionListener(event -> {
					JFrame frame = new JFrame(I18n.translate("menu.file.stats.title"));
					Container pane = frame.getContentPane();
					pane.setLayout(new FlowLayout());

					Map<StatsMember, JCheckBox> checkboxes = Arrays
							.stream(StatsMember.values())
							.collect(Collectors.toMap(m -> m, m -> {
								JCheckBox checkbox = new JCheckBox(I18n.translate("type." + m.name().toLowerCase(Locale.ROOT)));
								pane.add(checkbox);
								return checkbox;
							}));

					JButton button = new JButton(I18n.translate("menu.file.stats.generate"));

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
				JMenuItem item = new JMenuItem(I18n.translate("menu.file.exit"));
				menu.add(item);
				item.addActionListener(event -> this.gui.close());
			}
		}

		{
			JMenu menu = new JMenu(I18n.translate("menu.decompiler"));
			add(menu);

			for (Config.Decompiler decompiler : Config.Decompiler.values()) {
				JMenuItem label = new JMenuItem(decompiler.name);
				menu.add(label);
				label.addActionListener(event -> {
					gui.getController().setDecompiler(decompiler.service);

					try {
						Config.getInstance().decompiler = decompiler;
						Config.getInstance().saveConfig();
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				});
			}
		}

		{
			JMenu menu = new JMenu(I18n.translate("menu.view"));
			this.add(menu);
			{
				JMenu themes = new JMenu(I18n.translate("menu.view.themes"));
				menu.add(themes);
				for (Config.LookAndFeel lookAndFeel : Config.LookAndFeel.values()) {
					JMenuItem theme = new JMenuItem(I18n.translate("menu.view.themes." + lookAndFeel.name().toLowerCase(Locale.ROOT)));
					themes.add(theme);
					theme.addActionListener(event -> Themes.setLookAndFeel(gui, lookAndFeel));
				}
				
				JMenu languages = new JMenu(I18n.translate("menu.view.languages"));
				menu.add(languages);
				for (String lang : I18n.getAvailableLanguages()) {
					JMenuItem language = new JMenuItem(I18n.getLanguageName(lang));
					languages.add(language);
					language.addActionListener(event -> I18n.setLanguage(lang));
					language.addActionListener(event -> {
						JFrame frame = new JFrame(I18n.translate("menu.view.languages.title"));
						Container pane = frame.getContentPane();
						pane.setLayout(new FlowLayout());
						
						JLabel text = new JLabel((I18n.translate("menu.view.languages.summary")));
						text.setHorizontalAlignment(JLabel.CENTER);
						pane.add(text);
						
						JButton okButton = new JButton(I18n.translate("menu.view.languages.ok"));
						okButton.setAlignmentX(JButton.CENTER_ALIGNMENT);
						okButton.setHorizontalAlignment(JButton.CENTER);
						pane.add(okButton);
						okButton.addActionListener(arg0 -> frame.dispose());
						
						frame.setSize(350, 110);
						frame.setResizable(false);
						frame.setLocationRelativeTo(this.gui.getFrame());
						frame.setVisible(true);
						frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
					});
				}

				JMenuItem search = new JMenuItem(I18n.translate("menu.view.search"));
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
			JMenu menu = new JMenu(I18n.translate("menu.colab"));
			this.add(menu);
			{
				JMenuItem item = new JMenuItem(I18n.translate("menu.colab.connect"));
				menu.add(item);
				item.addActionListener(event -> {
					ConnectToServerDialog.Result result = ConnectToServerDialog.show(this.gui.getFrame());
					if (result == null) {
						return;
					}
					this.gui.getController().disconnectIfConnected(null);
					try {
						this.gui.getController().createClient(result.getUsername(), result.getIp(), result.getPort());
					} catch (IOException e) {
						JOptionPane.showMessageDialog(this.gui.getFrame(), e.toString(), I18n.translate("menu.colab.connect.error"), JOptionPane.ERROR_MESSAGE);
						this.gui.getController().disconnectIfConnected(null);
					}
				});
				this.connectToServerMenu = item;
			}
			{
				JMenuItem item = new JMenuItem(I18n.translate("menu.colab.server.start"));
				menu.add(item);
				item.addActionListener(event -> {
					if (this.gui.getController().getServer() != null) {
						this.gui.getController().disconnectIfConnected(null);
						return;
					}
					String result = (String) JOptionPane.showInputDialog(
							this.gui.getFrame(),
							I18n.translate("prompt.port"),
							I18n.translate("prompt.port"),
							JOptionPane.QUESTION_MESSAGE,
							null,
							null,
							String.valueOf(EnigmaServer.DEFAULT_PORT)
					);
					if (result == null) {
						return;
					}
					int port;
					try {
						port = Integer.parseInt(result);
					} catch (NumberFormatException e) {
						JOptionPane.showMessageDialog(this.gui.getFrame(), I18n.translate("prompt.port.nan"), I18n.translate("prompt.port"), JOptionPane.ERROR_MESSAGE);
						return;
					}
					if (port < 1024 || port >= 65536) {
						JOptionPane.showMessageDialog(this.gui.getFrame(), I18n.translate("prompt.port.invalid"), I18n.translate("prompt.port"), JOptionPane.ERROR_MESSAGE);
						return;
					}
					this.gui.getController().disconnectIfConnected(null);
					try {
						this.gui.getController().createServer(port);
					} catch (IOException e) {
						JOptionPane.showMessageDialog(this.gui.getFrame(), e.toString(), I18n.translate("menu.colab.server.start.error"), JOptionPane.ERROR_MESSAGE);
						this.gui.getController().disconnectIfConnected(null);
					}
				});
				this.startServerMenu = item;
			}
		}
		{
			JMenu menu = new JMenu(I18n.translate("menu.help"));
			this.add(menu);
			{
				JMenuItem item = new JMenuItem(I18n.translate("menu.help.about"));
				menu.add(item);
				item.addActionListener(event -> AboutDialog.show(this.gui.getFrame()));
			}
			{
				JMenuItem item = new JMenuItem(I18n.translate("menu.help.github"));
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
