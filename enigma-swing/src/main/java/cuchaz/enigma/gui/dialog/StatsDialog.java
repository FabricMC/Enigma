package cuchaz.enigma.gui.dialog;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import cuchaz.enigma.gui.Gui;
import cuchaz.enigma.gui.stats.StatsGenerator;
import cuchaz.enigma.gui.stats.StatsMember;
import cuchaz.enigma.gui.stats.StatsResult;
import cuchaz.enigma.gui.util.ScaleUtil;
import cuchaz.enigma.utils.I18n;

public class StatsDialog {

	public static void show(Gui gui) {
		ProgressDialog.runOffThread(gui.getFrame(), listener -> {
			final StatsGenerator statsGenerator = new StatsGenerator(gui.getController().project);
			final Map<StatsMember, StatsResult> results = new HashMap<>();
			for (StatsMember member : StatsMember.values()) {
				results.put(member, statsGenerator.generate(listener, Collections.singleton(member), ""));
			}
			SwingUtilities.invokeLater(() -> show(gui, results));
		});
	}

	public static void show(Gui gui, Map<StatsMember, StatsResult> results) {
		// init frame
		JDialog dialog = new JDialog(gui.getFrame(), I18n.translate("menu.file.stats.title"), true);
		Container contentPane = dialog.getContentPane();
		contentPane.setLayout(new GridBagLayout());

		GridBagConstraints c = new GridBagConstraints();
		c.insets = ScaleUtil.getInsets(4, 4, 4, 4);
		c.gridy = 0;

		Map<StatsMember, JCheckBox> checkboxes = new HashMap<>();

		results.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e -> {
			StatsMember m = e.getKey();
			StatsResult result = e.getValue();

			c.gridx = 0;
			c.weightx = 1.0;
			c.anchor = GridBagConstraints.WEST;
			JCheckBox checkBox = new JCheckBox(I18n.translate("type." + m.name().toLowerCase(Locale.ROOT)));
			checkboxes.put(m, checkBox);
			contentPane.add(checkBox, c);

			c.gridx = 1;
			c.weightx = 0.0;
			c.anchor = GridBagConstraints.EAST;
			contentPane.add(new JLabel(Integer.toString(result.getMapped())), c);

			c.gridx = 2;
			contentPane.add(new JLabel("/"), c);

			c.gridx = 3;
			contentPane.add(new JLabel(Integer.toString(result.getTotal())), c);

			c.gridx = 4;
			contentPane.add(new JLabel(String.format("%.2f%%", result.getPercentage())), c);

			c.gridy += 1;
		});

		c.gridx = 0;
		c.gridwidth = 5;
		c.weightx = 1.0;
		c.anchor = GridBagConstraints.WEST;

		// show top-level package option
		JLabel topLevelPackageOption = new JLabel(I18n.translate("menu.file.stats.top_level_package"));
		contentPane.add(topLevelPackageOption, c);

		c.gridy += 1;
		c.weightx = 1.0;
		c.fill = GridBagConstraints.HORIZONTAL;
		JTextField topLevelPackage = new JTextField();
		contentPane.add(topLevelPackage, c);

		c.gridy += 1;
		c.weighty = 1.0;
		c.fill = GridBagConstraints.NONE;
		c.anchor = GridBagConstraints.SOUTHEAST;

		// show generate button
		JButton button = new JButton(I18n.translate("menu.file.stats.generate"));
		button.setEnabled(false);
		button.addActionListener(action -> {
			dialog.dispose();
			generateStats(gui, checkboxes, topLevelPackage.getText());
		});

		contentPane.add(button, c);

		// add action listener to each checkbox
		checkboxes.forEach((key, value) -> value.addActionListener(action -> {
			if (!button.isEnabled()) {
				button.setEnabled(true);
			} else if (checkboxes.entrySet().stream().noneMatch(entry -> entry.getValue().isSelected())) {
				button.setEnabled(false);
			}
		}));

		// show the frame
		dialog.pack();
		Dimension size = dialog.getSize();
		dialog.setMinimumSize(size);
		size.width = ScaleUtil.scale(350);
		dialog.setSize(size);
		dialog.setLocationRelativeTo(gui.getFrame());
		dialog.setVisible(true);
	}

	private static void generateStats(Gui gui, Map<StatsMember, JCheckBox> checkboxes, String topLevelPackage) {
		// get members from selected checkboxes
		Set<StatsMember> includedMembers = checkboxes
				.entrySet()
				.stream()
				.filter(entry -> entry.getValue().isSelected())
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());

		// checks if a project is open
		if (gui.getController().project != null) {
			gui.getController().openStats(includedMembers, topLevelPackage);
		}
	}
}
