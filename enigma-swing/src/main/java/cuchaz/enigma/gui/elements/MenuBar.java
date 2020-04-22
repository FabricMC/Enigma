package cuchaz.enigma.gui.elements;

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
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.swing.*;

import cuchaz.enigma.gui.ConnectionState;
import cuchaz.enigma.gui.Gui;
import cuchaz.enigma.gui.config.Config;
import cuchaz.enigma.gui.config.Themes;
import cuchaz.enigma.gui.dialog.*;
import cuchaz.enigma.gui.util.ScaleUtil;
import cuchaz.enigma.translation.mapping.serde.MappingFormat;
import cuchaz.enigma.utils.I18n;
import cuchaz.enigma.utils.Pair;

public class MenuBar {

	private final JMenuBar ui = new JMenuBar();

	private final JMenu fileMenu = new JMenu(I18n.translate("menu.file"));
	private final JMenuItem jarOpenItem = new JMenuItem(I18n.translate("menu.file.jar.open"));
	private final JMenuItem jarCloseItem = new JMenuItem(I18n.translate("menu.file.jar.close"));
	private final JMenu openMenu = new JMenu(I18n.translate("menu.file.mappings.open"));
	private final JMenuItem saveMappingsItem = new JMenuItem(I18n.translate("menu.file.mappings.save"));
	private final JMenu saveMappingsAsMenu = new JMenu(I18n.translate("menu.file.mappings.save_as"));
	private final JMenuItem closeMappingsItem = new JMenuItem(I18n.translate("menu.file.mappings.close"));
	private final JMenuItem dropMappingsItem = new JMenuItem(I18n.translate("menu.file.mappings.drop"));
	private final JMenuItem exportSourceItem = new JMenuItem(I18n.translate("menu.file.export.source"));
	private final JMenuItem exportJarItem = new JMenuItem(I18n.translate("menu.file.export.jar"));
	private final JMenuItem statsItem = new JMenuItem(I18n.translate("menu.file.stats"));
	private final JMenuItem exitItem = new JMenuItem(I18n.translate("menu.file.exit"));

	private final JMenu decompilerMenu = new JMenu(I18n.translate("menu.decompiler"));

	private final JMenu viewMenu = new JMenu(I18n.translate("menu.view"));
	private final JMenu themesMenu = new JMenu(I18n.translate("menu.view.themes"));
	private final JMenu languagesMenu = new JMenu(I18n.translate("menu.view.languages"));
	private final JMenu scaleMenu = new JMenu(I18n.translate("menu.view.scale"));
	private final JMenuItem customScaleItem = new JMenuItem(I18n.translate("menu.view.scale.custom"));
	private final JMenuItem searchItem = new JMenuItem(I18n.translate("menu.view.search"));

	private final JMenu collabMenu = new JMenu(I18n.translate("menu.collab"));
	private final JMenuItem connectItem = new JMenuItem(I18n.translate("menu.collab.connect"));
	private final JMenuItem startServerItem = new JMenuItem(I18n.translate("menu.collab.server.start"));

	private final JMenu helpMenu = new JMenu(I18n.translate("menu.help"));
	private final JMenuItem aboutItem = new JMenuItem(I18n.translate("menu.help.about"));
	private final JMenuItem githubItem = new JMenuItem(I18n.translate("menu.help.github"));

	private final Gui gui;

	public MenuBar(Gui gui) {
		this.gui = gui;

		prepareOpenMenu(this.openMenu, gui);
		prepareSaveMappingsAsMenu(this.saveMappingsAsMenu, this.saveMappingsItem, gui);
		prepareDecompilerMenu(this.decompilerMenu, gui);
		prepareThemesMenu(this.themesMenu, gui);
		prepareLanguagesMenu(this.languagesMenu, gui);
		prepareScaleMenu(this.scaleMenu, gui);

		this.fileMenu.add(this.jarOpenItem);
		this.fileMenu.add(this.jarCloseItem);
		this.fileMenu.addSeparator();
		this.fileMenu.add(this.openMenu);
		this.fileMenu.add(this.saveMappingsItem);
		this.fileMenu.add(this.saveMappingsAsMenu);
		this.fileMenu.add(this.closeMappingsItem);
		this.fileMenu.add(this.dropMappingsItem);
		this.fileMenu.addSeparator();
		this.fileMenu.add(this.exportSourceItem);
		this.fileMenu.add(this.exportJarItem);
		this.fileMenu.addSeparator();
		this.fileMenu.add(this.statsItem);
		this.fileMenu.addSeparator();
		this.fileMenu.add(this.exitItem);
		this.ui.add(this.fileMenu);

		this.ui.add(this.decompilerMenu);

		this.viewMenu.add(this.themesMenu);
		this.viewMenu.add(this.languagesMenu);
		this.scaleMenu.add(this.customScaleItem);
		this.viewMenu.add(this.scaleMenu);
		this.viewMenu.addSeparator();
		this.viewMenu.add(this.searchItem);
		this.ui.add(this.viewMenu);

		this.collabMenu.add(this.connectItem);
		this.collabMenu.add(this.startServerItem);
		this.ui.add(this.collabMenu);

		this.helpMenu.add(this.aboutItem);
		this.helpMenu.add(this.githubItem);
		this.ui.add(this.helpMenu);

		this.saveMappingsItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
		this.searchItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, InputEvent.SHIFT_DOWN_MASK));

		this.jarOpenItem.addActionListener(_e -> this.onOpenJarClicked());
		this.jarCloseItem.addActionListener(_e -> this.gui.getController().closeJar());
		this.saveMappingsItem.addActionListener(_e -> this.onSaveMappingsClicked());
		this.closeMappingsItem.addActionListener(_e -> this.onCloseMappingsClicked());
		this.dropMappingsItem.addActionListener(_e -> this.gui.getController().dropMappings());
		this.exportSourceItem.addActionListener(_e -> this.onExportSourceClicked());
		this.exportJarItem.addActionListener(_e -> this.onExportJarClicked());
		this.statsItem.addActionListener(_e -> StatsDialog.show(this.gui));
		this.exitItem.addActionListener(_e -> this.gui.close());
		this.customScaleItem.addActionListener(_e -> this.onCustomScaleClicked());
		this.searchItem.addActionListener(_e -> this.onSearchClicked());
		this.connectItem.addActionListener(_e -> this.onConnectClicked());
		this.startServerItem.addActionListener(_e -> this.onStartServerClicked());
		this.aboutItem.addActionListener(_e -> AboutDialog.show(this.gui.getFrame()));
		this.githubItem.addActionListener(_e -> this.onGithubClicked());
	}

	public void updateUiState() {
		boolean jarOpen = this.gui.isJarOpen();
		ConnectionState connectionState = this.gui.getConnectionState();

		this.connectItem.setEnabled(jarOpen && connectionState != ConnectionState.HOSTING);
		this.connectItem.setText(I18n.translate(connectionState != ConnectionState.CONNECTED ? "menu.collab.connect" : "menu.collab.disconnect"));
		this.startServerItem.setEnabled(jarOpen && connectionState != ConnectionState.CONNECTED);
		this.startServerItem.setText(I18n.translate(connectionState != ConnectionState.HOSTING ? "menu.collab.server.start" : "menu.collab.server.stop"));

		this.jarCloseItem.setEnabled(jarOpen);
		this.openMenu.setEnabled(jarOpen);
		this.saveMappingsItem.setEnabled(jarOpen && this.gui.enigmaMappingsFileChooser.getSelectedFile() != null && connectionState != ConnectionState.CONNECTED);
		this.saveMappingsAsMenu.setEnabled(jarOpen);
		this.closeMappingsItem.setEnabled(jarOpen);
		this.exportSourceItem.setEnabled(jarOpen);
		this.exportJarItem.setEnabled(jarOpen);
	}

	public JMenuBar getUi() {
		return this.ui;
	}

	private void onOpenJarClicked() {
		this.gui.jarFileChooser.setVisible(true);
		String file = this.gui.jarFileChooser.getFile();
		// checks if the file name is not empty
		if (file != null) {
			Path path = Paths.get(this.gui.jarFileChooser.getDirectory()).resolve(file);
			// checks if the file name corresponds to an existing file
			if (Files.exists(path)) {
				this.gui.getController().openJar(path);
			}
		}
	}

	private void onSaveMappingsClicked() {
		this.gui.getController().saveMappings(this.gui.enigmaMappingsFileChooser.getSelectedFile().toPath());
	}

	private void onCloseMappingsClicked() {
		if (this.gui.getController().isDirty()) {
			this.gui.showDiscardDiag((response -> {
				if (response == JOptionPane.YES_OPTION) {
					this.gui.saveMapping();
					this.gui.getController().closeMappings();
				} else if (response == JOptionPane.NO_OPTION)
					this.gui.getController().closeMappings();
				return null;
			}), I18n.translate("prompt.close.save"), I18n.translate("prompt.close.discard"), I18n.translate("prompt.close.cancel"));
		} else {
			this.gui.getController().closeMappings();
		}
	}

	private void onExportSourceClicked() {
		if (this.gui.exportSourceFileChooser.showSaveDialog(this.gui.getFrame()) == JFileChooser.APPROVE_OPTION) {
			this.gui.getController().exportSource(this.gui.exportSourceFileChooser.getSelectedFile().toPath());
		}
	}

	private void onExportJarClicked() {
		this.gui.exportJarFileChooser.setVisible(true);
		if (this.gui.exportJarFileChooser.getFile() != null) {
			Path path = Paths.get(this.gui.exportJarFileChooser.getDirectory(), this.gui.exportJarFileChooser.getFile());
			this.gui.getController().exportJar(path);
		}
	}

	private void onCustomScaleClicked() {
		String answer = (String) JOptionPane.showInputDialog(this.gui.getFrame(), I18n.translate("menu.view.scale.custom.title"), I18n.translate("menu.view.scale.custom.title"),
				JOptionPane.QUESTION_MESSAGE, null, null, Float.toString(ScaleUtil.getScaleFactor() * 100));
		if (answer == null) return;
		float newScale = 1.0f;
		try {
			newScale = Float.parseFloat(answer) / 100f;
		} catch (NumberFormatException ignored) {
		}
		ScaleUtil.setScaleFactor(newScale);
		ChangeDialog.show(this.gui);
	}

	private void onSearchClicked() {
		if (this.gui.getController().project != null) {
			this.gui.getSearchDialog().show();
		}
	}

	private void onConnectClicked() {
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
			this.gui.getController().createClient(result.getUsername(), result.getAddress().address, result.getAddress().port, result.getPassword());
		} catch (IOException e) {
			JOptionPane.showMessageDialog(this.gui.getFrame(), e.toString(), I18n.translate("menu.collab.connect.error"), JOptionPane.ERROR_MESSAGE);
			this.gui.getController().disconnectIfConnected(null);
		}
		Arrays.fill(result.getPassword(), (char) 0);
	}

	private void onStartServerClicked() {
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
	}

	private void onGithubClicked() {
		try {
			Desktop.getDesktop().browse(new URL("https://github.com/FabricMC/Enigma").toURI());
		} catch (URISyntaxException | IOException ignored) {
		}
	}

	private static void prepareOpenMenu(JMenu openMenu, Gui gui) {
		for (MappingFormat format : MappingFormat.values()) {
			if (format.getReader() != null) {
				JMenuItem item = new JMenuItem(I18n.translate("mapping_format." + format.name().toLowerCase(Locale.ROOT)));
				item.addActionListener(event -> {
					if (gui.enigmaMappingsFileChooser.showOpenDialog(gui.getFrame()) == JFileChooser.APPROVE_OPTION) {
						File selectedFile = gui.enigmaMappingsFileChooser.getSelectedFile();
						gui.getController().openMappings(format, selectedFile.toPath());
					}
				});
				openMenu.add(item);
			}
		}
	}

	private static void prepareSaveMappingsAsMenu(JMenu saveMappingsAsMenu, JMenuItem saveMappingsItem, Gui gui) {
		for (MappingFormat format : MappingFormat.values()) {
			if (format.getWriter() != null) {
				JMenuItem item = new JMenuItem(I18n.translate("mapping_format." + format.name().toLowerCase(Locale.ROOT)));
				item.addActionListener(event -> {
					// TODO: Use a specific file chooser for it
					if (gui.enigmaMappingsFileChooser.showSaveDialog(gui.getFrame()) == JFileChooser.APPROVE_OPTION) {
						gui.getController().saveMappings(gui.enigmaMappingsFileChooser.getSelectedFile().toPath(), format);
						saveMappingsItem.setEnabled(true);
					}
				});
				saveMappingsAsMenu.add(item);
			}
		}
	}

	private static void prepareDecompilerMenu(JMenu decompilerMenu, Gui gui) {
		ButtonGroup decompilerGroup = new ButtonGroup();

		for (Config.Decompiler decompiler : Config.Decompiler.values()) {
			JRadioButtonMenuItem decompilerButton = new JRadioButtonMenuItem(decompiler.name);
			decompilerGroup.add(decompilerButton);
			if (decompiler.equals(Config.getInstance().decompiler)) {
				decompilerButton.setSelected(true);
			}
			decompilerButton.addActionListener(event -> {
				gui.getController().setDecompiler(decompiler.service);

				try {
					Config.getInstance().decompiler = decompiler;
					Config.getInstance().saveConfig();
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			});
			decompilerMenu.add(decompilerButton);
		}
	}

	private static void prepareThemesMenu(JMenu themesMenu, Gui gui) {
		ButtonGroup themeGroup = new ButtonGroup();
		for (Config.LookAndFeel lookAndFeel : Config.LookAndFeel.values()) {
			JRadioButtonMenuItem themeButton = new JRadioButtonMenuItem(I18n.translate("menu.view.themes." + lookAndFeel.name().toLowerCase(Locale.ROOT)));
			themeGroup.add(themeButton);
			if (lookAndFeel.equals(Config.getInstance().lookAndFeel)) {
				themeButton.setSelected(true);
			}
			themeButton.addActionListener(_e -> Themes.setLookAndFeel(lookAndFeel));
			themesMenu.add(themeButton);
		}
	}

	private static void prepareLanguagesMenu(JMenu languagesMenu, Gui gui) {
		ButtonGroup languageGroup = new ButtonGroup();
		for (String lang : I18n.getAvailableLanguages()) {
			JRadioButtonMenuItem languageButton = new JRadioButtonMenuItem(I18n.getLanguageName(lang));
			languageGroup.add(languageButton);
			if (lang.equals(Config.getInstance().language)) {
				languageButton.setSelected(true);
			}
			languageButton.addActionListener(event -> {
				Config.getInstance().language = lang;
						try {
							Config.getInstance().saveConfig();
						} catch (IOException e) {
							e.printStackTrace();
						}
				ChangeDialog.show(gui);
			});
			languagesMenu.add(languageButton);
		}
	}

	private static void prepareScaleMenu(JMenu scaleMenu, Gui gui) {
		ButtonGroup scaleGroup = new ButtonGroup();
		Map<Float, JRadioButtonMenuItem> scaleButtons = IntStream.of(100, 125, 150, 175, 200)
				.mapToObj(scaleFactor -> {
					float realScaleFactor = scaleFactor / 100f;
					JRadioButtonMenuItem menuItem = new JRadioButtonMenuItem(String.format("%d%%", scaleFactor));
					menuItem.addActionListener(event -> ScaleUtil.setScaleFactor(realScaleFactor));
					menuItem.addActionListener(event -> ChangeDialog.show(gui));
					scaleGroup.add(menuItem);
					scaleMenu.add(menuItem);
					return new Pair<>(realScaleFactor, menuItem);
				})
				.collect(Collectors.toMap(x -> x.a, x -> x.b));

		JRadioButtonMenuItem currentScaleButton = scaleButtons.get(ScaleUtil.getScaleFactor());
		if (currentScaleButton != null) {
			currentScaleButton.setSelected(true);
		}

		ScaleUtil.addListener((newScale, _oldScale) -> {
			JRadioButtonMenuItem mi = scaleButtons.get(newScale);
			if (mi != null) {
				mi.setSelected(true);
			} else {
				scaleGroup.clearSelection();
			}
		});
	}

}
