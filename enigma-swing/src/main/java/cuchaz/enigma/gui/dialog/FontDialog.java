package cuchaz.enigma.gui.dialog;

import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JList;

import org.drjekyll.fontchooser.FontChooser;

import cuchaz.enigma.gui.config.UiConfig;
import cuchaz.enigma.gui.util.GridBagConstraintsBuilder;
import cuchaz.enigma.gui.util.ScaleUtil;
import cuchaz.enigma.utils.I18n;

public class FontDialog extends JDialog {
	private static final List<String> CATEGORIES = List.of("Default", "Default 2", "Small", "Editor");

	private static final List<String> CATEGORY_TEXTS = List.of("fonts.cat.default", "fonts.cat.default2", "fonts.cat.small", "fonts.cat.editor");

	private final JList<String> entries = new JList<>(CATEGORY_TEXTS.stream().map(I18n::translate).toArray(String[]::new));
	private final FontChooser chooser = new FontChooser(Font.decode(Font.DIALOG));
	private final JCheckBox customCheckBox = new JCheckBox(I18n.translate("fonts.use_custom"));
	private final JButton okButton = new JButton(I18n.translate("prompt.ok"));
	private final JButton cancelButton = new JButton(I18n.translate("prompt.cancel"));

	private final Font[] fonts = CATEGORIES.stream().map(name -> UiConfig.getFont(name).orElseGet(() -> ScaleUtil.scaleFont(Font.decode(Font.DIALOG)))).toArray(Font[]::new);

	public FontDialog(Frame owner) {
		super(owner, "Fonts", true);

		this.customCheckBox.setSelected(UiConfig.useCustomFonts());

		this.entries.setPreferredSize(ScaleUtil.getDimension(100, 0));

		this.entries.addListSelectionListener(_e -> this.categoryChanged());
		this.chooser.addChangeListener(_e -> this.selectedFontChanged());
		this.customCheckBox.addActionListener(_e -> this.customFontsClicked());
		this.okButton.addActionListener(_e -> this.apply());
		this.cancelButton.addActionListener(_e -> this.cancel());

		Container contentPane = this.getContentPane();
		contentPane.setLayout(new GridBagLayout());

		GridBagConstraintsBuilder cb = GridBagConstraintsBuilder.create().insets(2);

		contentPane.add(this.entries, cb.pos(0, 0).weight(0.0, 1.0).fill(GridBagConstraints.BOTH).build());
		contentPane.add(this.chooser, cb.pos(1, 0).weight(1.0, 1.0).fill(GridBagConstraints.BOTH).size(2, 1).build());
		contentPane.add(this.customCheckBox, cb.pos(0, 1).anchor(GridBagConstraints.WEST).size(2, 1).build());
		contentPane.add(this.okButton, cb.pos(1, 1).anchor(GridBagConstraints.EAST).weight(1.0, 0.0).build());
		contentPane.add(this.cancelButton, cb.pos(2, 1).anchor(GridBagConstraints.EAST).weight(0.0, 0.0).build());

		this.updateUiState();

		this.setSize(ScaleUtil.getDimension(640, 360));
		this.setLocationRelativeTo(owner);
	}

	private void customFontsClicked() {
		this.updateUiState();
	}

	private void categoryChanged() {
		this.updateUiState();
		int selectedIndex = this.entries.getSelectedIndex();

		if (selectedIndex != -1) {
			this.chooser.setSelectedFont(this.fonts[selectedIndex]);
		}
	}

	private void selectedFontChanged() {
		int selectedIndex = this.entries.getSelectedIndex();

		if (selectedIndex != -1) {
			this.fonts[selectedIndex] = this.chooser.getSelectedFont();
		}
	}

	private void updateUiState() {
		recursiveSetEnabled(this.chooser, this.entries.getSelectedIndex() != -1 && this.customCheckBox.isSelected());
		this.entries.setEnabled(this.customCheckBox.isSelected());
	}

	private void apply() {
		for (int i = 0; i < CATEGORIES.size(); i++) {
			UiConfig.setFont(CATEGORIES.get(i), this.fonts[i]);
		}

		UiConfig.setUseCustomFonts(this.customCheckBox.isSelected());
		UiConfig.save();
		ChangeDialog.show(this);
		this.dispose();
	}

	private void cancel() {
		this.dispose();
	}

	public static void display(Frame parent) {
		FontDialog d = new FontDialog(parent);
		d.setVisible(true);
	}

	private static void recursiveSetEnabled(Component self, boolean enabled) {
		if (self instanceof Container) {
			for (Component component : ((Container) self).getComponents()) {
				recursiveSetEnabled(component, enabled);
			}

			self.setEnabled(enabled);
		}
	}
}
