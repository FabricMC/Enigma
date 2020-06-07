package cuchaz.enigma.gui.dialog;

import java.awt.*;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.*;

import cuchaz.enigma.gui.Gui;
import cuchaz.enigma.gui.stats.StatsMember;
import cuchaz.enigma.gui.util.ScaleUtil;
import cuchaz.enigma.utils.I18n;

public class StatsDialog {

	public static void show(Gui gui) {
		// init frame
		JFrame frame = new JFrame(I18n.translate("menu.file.stats.title"));
		JPanel checkboxesPanel = new JPanel();
		JPanel excludePackagePanel = new JPanel();
		JPanel buttonPanel = new JPanel();
		frame.setLayout(new GridLayout(3, 0));
		frame.add(checkboxesPanel);
		frame.add(excludePackagePanel);
		frame.add(buttonPanel);

		// show checkboxes
		Map<StatsMember, JCheckBox> checkboxes = Arrays
				.stream(StatsMember.values())
				.collect(Collectors.toMap(m -> m, m -> {
					JCheckBox checkbox = new JCheckBox(I18n.translate("type." + m.name().toLowerCase(Locale.ROOT)));
					checkboxesPanel.add(checkbox);
					return checkbox;
				}));

		// show top-level package option
		JLabel topLevelPackageOption = new JLabel(I18n.translate("menu.file.stats.top_level_package"));
		JTextField topLevelPackage = new JTextField();
		topLevelPackage.setPreferredSize(ScaleUtil.getDimension(200, 25));
		excludePackagePanel.add(topLevelPackageOption);
		excludePackagePanel.add(topLevelPackage);

		// show generate button
		JButton button = new JButton(I18n.translate("menu.file.stats.generate"));
		buttonPanel.add(button);
		button.setEnabled(false);
		button.addActionListener(action -> {
			frame.dispose();
			generateStats(gui, checkboxes, topLevelPackage.getText());
		});

		// add action listener to each checkbox
		checkboxes.entrySet().forEach(checkbox -> {
			checkbox.getValue().addActionListener(action -> {
				if (!button.isEnabled()) {
					button.setEnabled(true);
				} else if (checkboxes.entrySet().stream().allMatch(entry -> !entry.getValue().isSelected())) {
					button.setEnabled(false);
				}
			});
		});

		// show the frame
		frame.pack();
		frame.setVisible(true);
		frame.setSize(ScaleUtil.getDimension(500, 150));
		frame.setResizable(false);
		frame.setLocationRelativeTo(gui.getFrame());
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
