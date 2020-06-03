package cuchaz.enigma.gui.elements;

import cuchaz.enigma.gui.config.Config;
import cuchaz.enigma.gui.config.Themes;
import cuchaz.enigma.gui.Gui;
import cuchaz.enigma.gui.dialog.AboutDialog;
import cuchaz.enigma.gui.dialog.ChangeDialog;
import cuchaz.enigma.gui.dialog.ConnectToServerDialog;
import cuchaz.enigma.gui.dialog.CreateServerDialog;
import cuchaz.enigma.gui.dialog.StatsDialog;
import cuchaz.enigma.gui.util.ScaleUtil;
import cuchaz.enigma.translation.mapping.serde.MappingFormat;
import cuchaz.enigma.utils.I18n;
import cuchaz.enigma.utils.Pair;

import java.awt.Desktop;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.swing.*;

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

		/*
		 * File menu
		 */
		{
			JMenu menu = new JMenu(I18n.translate("menu.file"));
			this.add(menu);
			{
				JMenuItem item = new JMenuItem(I18n.translate("menu.file.jar.open"));
				menu.add(item);
				item.addActionListener(event -> {
					this.gui.jarFileChooser.setVisible(true);
					String file = this.gui.jarFileChooser.getFile();
					// checks if the file name is not empty
					if (file != null) {
						Path path = Paths.get(this.gui.jarFileChooser.getDirectory()).resolve(file);
						// checks if the file name corresponds to an existing file
						if (Files.exists(path)) {
							gui.getController().openJar(path);
						}
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
				menu.add(stats);
				stats.addActionListener(event -> StatsDialog.show(this.gui));
			}
			menu.addSeparator();
			{
				JMenuItem item = new JMenuItem(I18n.translate("menu.file.exit"));
				menu.add(item);
				item.addActionListener(event -> this.gui.close());
			}
		}

		/*
		 * Decompiler menu
		 */
		{
			JMenu menu = new JMenu(I18n.translate("menu.decompiler"));
			this.add(menu);

			ButtonGroup decompilerGroup = new ButtonGroup();

			for (Config.Decompiler decompiler : Config.Decompiler.values()) {
				JRadioButtonMenuItem decompilerButton = new JRadioButtonMenuItem(decompiler.name);
				decompilerGroup.add(decompilerButton);
				if (decompiler.equals(Config.getInstance().decompiler)) {
					decompilerButton.setSelected(true);
				}
				menu.add(decompilerButton);
				decompilerButton.addActionListener(event -> {
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

		/*
		 * View menu
		 */
		{
			JMenu menu = new JMenu(I18n.translate("menu.view"));
			this.add(menu);
			{
				JMenu themes = new JMenu(I18n.translate("menu.view.themes"));
				menu.add(themes);
				ButtonGroup themeGroup = new ButtonGroup();
				for (Config.LookAndFeel lookAndFeel : Config.LookAndFeel.values()) {
					JRadioButtonMenuItem themeButton = new JRadioButtonMenuItem(I18n.translate("menu.view.themes." + lookAndFeel.name().toLowerCase(Locale.ROOT)));
					themeGroup.add(themeButton);
					if (lookAndFeel.equals(Config.getInstance().lookAndFeel)) {
						themeButton.setSelected(true);
					}
					themes.add(themeButton);
					themeButton.addActionListener(event -> Themes.setLookAndFeel(gui, lookAndFeel));
				}
			}
			{
				JMenu languages = new JMenu(I18n.translate("menu.view.languages"));
				menu.add(languages);
				ButtonGroup languageGroup = new ButtonGroup();
				for (String lang : I18n.getAvailableLanguages()) {
					JRadioButtonMenuItem languageButton = new JRadioButtonMenuItem(I18n.getLanguageName(lang));
					languageGroup.add(languageButton);
					if (lang.equals(Config.getInstance().language)) {
						languageButton.setSelected(true);
					}
					languages.add(languageButton);
					languageButton.addActionListener(event -> {
						I18n.setLanguage(lang);
						ChangeDialog.show(this.gui);
					});
				}
			}
			{
				JMenu scale = new JMenu(I18n.translate("menu.view.scale"));
				{
					ButtonGroup scaleGroup = new ButtonGroup();
					Map<Float, JRadioButtonMenuItem> map = IntStream.of(100, 125, 150, 175, 200)
							.mapToObj(scaleFactor -> {
								float realScaleFactor = scaleFactor / 100f;
								JRadioButtonMenuItem menuItem = new JRadioButtonMenuItem(String.format("%d%%", scaleFactor));
								menuItem.addActionListener(event -> ScaleUtil.setScaleFactor(realScaleFactor));
								menuItem.addActionListener(event -> ChangeDialog.show(this.gui));
								scaleGroup.add(menuItem);
								scale.add(menuItem);
								return new Pair<>(realScaleFactor, menuItem);
							})
							.collect(Collectors.toMap(x -> x.a, x -> x.b));

					JMenuItem customScale = new JMenuItem(I18n.translate("menu.view.scale.custom"));
					customScale.addActionListener(event -> {
						String answer = (String) JOptionPane.showInputDialog(gui.getFrame(), I18n.translate("menu.view.scale.custom.title"), I18n.translate("menu.view.scale.custom.title"),
								JOptionPane.QUESTION_MESSAGE, null, null, Float.toString(ScaleUtil.getScaleFactor() * 100));
						if (answer == null) return;
						float newScale = 1.0f;
						try {
							newScale = Float.parseFloat(answer) / 100f;
						} catch (NumberFormatException ignored) {
						}
						ScaleUtil.setScaleFactor(newScale);
						ChangeDialog.show(this.gui);
					});
					scale.add(customScale);
					ScaleUtil.addListener((newScale, _oldScale) -> {
						JRadioButtonMenuItem mi = map.get(newScale);
						if (mi != null) {
							mi.setSelected(true);
						} else {
							scaleGroup.clearSelection();
						}
					});
					JRadioButtonMenuItem mi = map.get(ScaleUtil.getScaleFactor());
					if (mi != null) {
						mi.setSelected(true);
					}
				}
				menu.add(scale);
			}
			menu.addSeparator();
			{
				JMenuItem search = new JMenuItem(I18n.translate("menu.view.search"));
				search.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, InputEvent.SHIFT_MASK));
				menu.add(search);
				search.addActionListener(event -> {
					if (this.gui.getController().project != null) {
						this.gui.getSearchDialog().show();
					}
				});
			}
		}

		/*
		 * Collab menu
		 */
		{
			JMenu menu = new JMenu(I18n.translate("menu.collab"));
			this.add(menu);
			{
				JMenuItem item = new JMenuItem(I18n.translate("menu.collab.connect"));
				menu.add(item);
				item.addActionListener(event -> {
					if (this.gui.getController().getClient() != null) {
						this.gui.getController().disconnectIfConnected(null);
						return;
					}
					ConnectToServerDialog.Result result = ConnectToServerDialog.show(this.gui.getFrame());
					if (result == null) {
						return;
					}
					this.gui.getController().disconnectIfConnected(null);
					try {
						this.gui.getController().createClient(result.getUsername(), result.getIp(), result.getPort(), result.getPassword());
					} catch (IOException e) {
						JOptionPane.showMessageDialog(this.gui.getFrame(), e.toString(), I18n.translate("menu.collab.connect.error"), JOptionPane.ERROR_MESSAGE);
						this.gui.getController().disconnectIfConnected(null);
					}
					Arrays.fill(result.getPassword(), (char)0);
				});
				this.connectToServerMenu = item;
			}
			{
				JMenuItem item = new JMenuItem(I18n.translate("menu.collab.server.start"));
				menu.add(item);
				item.addActionListener(event -> {
					if (this.gui.getController().getServer() != null) {
						this.gui.getController().disconnectIfConnected(null);
						return;
					}
					CreateServerDialog.Result result = CreateServerDialog.show(this.gui.getFrame());
					if (result == null) {
						return;
					}
					this.gui.getController().disconnectIfConnected(null);
					try {
						this.gui.getController().createServer(result.getPort(), result.getPassword());
					} catch (IOException e) {
						JOptionPane.showMessageDialog(this.gui.getFrame(), e.toString(), I18n.translate("menu.collab.server.start.error"), JOptionPane.ERROR_MESSAGE);
						this.gui.getController().disconnectIfConnected(null);
					}
				});
				this.startServerMenu = item;
			}
		}

		/*
		 * Help menu
		 */
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
