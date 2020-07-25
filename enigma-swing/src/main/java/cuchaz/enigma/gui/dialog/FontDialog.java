package cuchaz.enigma.gui.dialog;

import java.awt.*;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import javax.swing.*;

import org.drjekyll.fontchooser.FontChooser;

import cuchaz.enigma.gui.util.GridBagConstraintsBuilder;
import cuchaz.enigma.gui.util.ScaleUtil;
import cuchaz.enigma.utils.I18n;

public class FontDialog extends JDialog {

	private static final List<String> CATEGORIES = Arrays.asList(
			"Default",
			"Default 2",
			"Small",
			"Editor"
	);

	private final JList<String> entries = new JList<>(new Vector<>(CATEGORIES));
	private final FontChooser chooser = new FontChooser(Font.decode(Font.DIALOG));
	private final JCheckBox customCheckBox = new JCheckBox(I18n.translate("Use Custom Fonts"));
	private final JButton okButton = new JButton(I18n.translate("OK"));
	private final JButton cancelButton = new JButton(I18n.translate("Cancel"));

	// private final Font[] fonts = CATEGORIES.stream().map(name -> UiConfig.getFont(name, ));

	public FontDialog(Frame owner) {
		super(owner, "Fonts", true);

		this.entries.setPreferredSize(ScaleUtil.getDimension(100, 0));
		this.customCheckBox.addActionListener(_e -> this.customFontsClicked());
		this.chooser.addChangeListener(_e -> this.selectionChanged());
		this.okButton.addActionListener(_e -> this.apply());
		this.cancelButton.addActionListener(_e -> this.cancel());

		Container contentPane = this.getContentPane();
		contentPane.setLayout(new GridBagLayout());

		GridBagConstraintsBuilder cb = GridBagConstraintsBuilder.create()
				.insets(2);

		contentPane.add(this.entries, cb.pos(0, 0).weight(0.0, 1.0).fill(GridBagConstraints.BOTH).build());
		contentPane.add(this.chooser, cb.pos(1, 0).weight(1.0, 1.0).fill(GridBagConstraints.BOTH).build());

		JPanel bottomPanel = new JPanel(new GridBagLayout());
		bottomPanel.add(this.customCheckBox, cb.pos(0, 0).build());
		bottomPanel.add(this.okButton, cb.pos(1, 0).weight(1.0, 0.0).anchor(GridBagConstraints.EAST).build());
		bottomPanel.add(this.cancelButton, cb.pos(2, 0).anchor(GridBagConstraints.EAST).build());
		contentPane.add(bottomPanel, cb.pos(0, 1).size(2, 1).weight(1.0, 0.0).fill(GridBagConstraints.BOTH).build());

		this.setSize(ScaleUtil.getDimension(640, 360));
		this.setLocationRelativeTo(owner);
	}

	private void customFontsClicked() {
		recursiveSetEnabled(this.chooser, this.customCheckBox.isSelected());
		this.entries.setEnabled(this.customCheckBox.isSelected());
	}

	private static void recursiveSetEnabled(Component self, boolean enabled) {
		if (self instanceof Container) {
			for (Component component : ((Container) self).getComponents()) {
				recursiveSetEnabled(component, enabled);
			}
			self.setEnabled(enabled);
		}
	}

	private void selectionChanged() {

	}

	private void apply() {
		this.dispose();
	}

	private void cancel() {
		this.dispose();
	}

	public static void display(Frame parent) {
		FontDialog d = new FontDialog(parent);
		d.setVisible(true);
	}

}
