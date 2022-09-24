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
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import cuchaz.enigma.gui.Gui;
import cuchaz.enigma.gui.config.UiConfig;
import cuchaz.enigma.gui.stats.StatsGenerator;
import cuchaz.enigma.gui.stats.StatsMember;
import cuchaz.enigma.gui.stats.StatsResult;
import cuchaz.enigma.gui.util.GridBagConstraintsBuilder;
import cuchaz.enigma.gui.util.ScaleUtil;
import cuchaz.enigma.utils.I18n;

public class StatsDialog {
	public static void show(Gui gui) {
		ProgressDialog.runOffThread(gui.getFrame(), listener -> {
			final StatsGenerator statsGenerator = new StatsGenerator(gui.getController().project);
			final Map<StatsMember, StatsResult> results = new HashMap<>();

			for (StatsMember member : StatsMember.values()) {
				results.put(member, statsGenerator.generate(listener, Collections.singleton(member), "", false));
			}

			SwingUtilities.invokeLater(() -> show(gui, results, ""));
		});
	}

	public static void show(Gui gui, Map<StatsMember, StatsResult> results, String packageName) {
		// init frame
		JDialog dialog = new JDialog(gui.getFrame(), packageName.isEmpty() ? I18n.translate("menu.file.stats.title") : I18n.translateFormatted("menu.file.stats.title_filtered", packageName), true);
		Container contentPane = dialog.getContentPane();
		contentPane.setLayout(new GridBagLayout());

		GridBagConstraintsBuilder cb = GridBagConstraintsBuilder.create().insets(2);

		Map<StatsMember, JCheckBox> checkboxes = new HashMap<>();

		int[] i = {0};
		results.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e -> {
			StatsMember m = e.getKey();
			StatsResult result = e.getValue();

			JCheckBox checkBox = new JCheckBox(I18n.translate("type." + m.name().toLowerCase(Locale.ROOT)));
			checkboxes.put(m, checkBox);
			contentPane.add(checkBox, cb.pos(0, i[0]).weightX(1.0).anchor(GridBagConstraints.WEST).build());

			GridBagConstraintsBuilder labels = cb.anchor(GridBagConstraints.EAST);

			contentPane.add(new JLabel(Integer.toString(result.getMapped())), labels.pos(1, i[0]).build());
			contentPane.add(new JLabel("/"), labels.pos(2, i[0]).build());
			contentPane.add(new JLabel(Integer.toString(result.getTotal())), labels.pos(3, i[0]).build());
			contentPane.add(new JLabel(String.format("%.2f%%", result.getPercentage())), labels.pos(4, i[0]).build());

			i[0]++;
		});

		GridBagConstraintsBuilder cb1 = cb.pos(0, 0).width(5).weightX(1.0).anchor(GridBagConstraints.WEST);

		// show top-level package option
		JLabel topLevelPackageOption = new JLabel(I18n.translate("menu.file.stats.top_level_package"));
		contentPane.add(topLevelPackageOption, cb1.pos(0, results.size() + 1).build());

		JTextField topLevelPackage = new JTextField();
		topLevelPackage.setText(UiConfig.getLastTopLevelPackage());
		contentPane.add(topLevelPackage, cb1.pos(0, results.size() + 2).fill(GridBagConstraints.HORIZONTAL).build());

		// Show filter button
		JButton filterButton = new JButton(I18n.translate("menu.file.stats.filter"));
		filterButton.addActionListener(action -> {
			dialog.dispose();
			ProgressDialog.runOffThread(gui.getFrame(), listener -> {
				UiConfig.setLastTopLevelPackage(topLevelPackage.getText());
				UiConfig.save();

				final StatsGenerator statsGenerator = new StatsGenerator(gui.getController().project);
				final Map<StatsMember, StatsResult> filteredResults = new HashMap<>();

				for (StatsMember member : StatsMember.values()) {
					filteredResults.put(member, statsGenerator.generate(listener, Collections.singleton(member), UiConfig.getLastTopLevelPackage(), false));
				}

				SwingUtilities.invokeLater(() -> show(gui, filteredResults, UiConfig.getLastTopLevelPackage()));
			});
		});
		contentPane.add(filterButton, cb1.pos(0, results.size() + 3).anchor(GridBagConstraints.EAST).build());

		// show synthetic members option
		JCheckBox syntheticParametersOption = new JCheckBox(I18n.translate("menu.file.stats.synthetic_parameters"));
		syntheticParametersOption.setSelected(UiConfig.shouldIncludeSyntheticParameters());
		contentPane.add(syntheticParametersOption, cb1.pos(0, results.size() + 4).build());

		// show generate button
		JButton button = new JButton(I18n.translate("menu.file.stats.generate"));
		button.setEnabled(false);
		button.addActionListener(action -> {
			dialog.dispose();

			UiConfig.setLastTopLevelPackage(topLevelPackage.getText());
			UiConfig.setIncludeSyntheticParameters(syntheticParametersOption.isSelected());
			UiConfig.save();

			generateStats(gui, checkboxes, topLevelPackage.getText(), syntheticParametersOption.isSelected());
		});

		contentPane.add(button, cb1.pos(0, results.size() + 5).weightY(1.0).anchor(GridBagConstraints.SOUTHWEST).build());

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

	private static void generateStats(Gui gui, Map<StatsMember, JCheckBox> checkboxes, String topLevelPackage, boolean includeSynthetic) {
		// get members from selected checkboxes
		Set<StatsMember> includedMembers = checkboxes.entrySet().stream().filter(entry -> entry.getValue().isSelected()).map(Map.Entry::getKey).collect(Collectors.toSet());

		// checks if a project is open
		if (gui.getController().project != null) {
			gui.getController().openStats(includedMembers, topLevelPackage, includeSynthetic);
		}
	}
}
