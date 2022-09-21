package cuchaz.enigma.gui.dialog.keybind;

import cuchaz.enigma.gui.config.keybind.KeyBind;
import cuchaz.enigma.gui.util.ScaleUtil;
import cuchaz.enigma.utils.I18n;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.util.List;

public class ConfigureCategoryKeyBindsDialog extends JDialog {
    public ConfigureCategoryKeyBindsDialog(Frame owner, String category, List<KeyBind> keyBinds) {
        super(owner, I18n.translateFormatted("menu.file.configure_keybinds.category_title", I18n.translate("keybind.category." + category)), true);

        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        // Add keybinds
        JPanel keyBindsPanel = new JPanel(new GridLayout(0, 1, 5, 5));
        keyBindsPanel.setBorder(new EmptyBorder(ScaleUtil.scale(10), ScaleUtil.scale(10), ScaleUtil.scale(10), ScaleUtil.scale(10)));
        for (KeyBind keyBind : keyBinds) {
            JPanel panel = new JPanel(new BorderLayout());

            JLabel label = new JLabel(I18n.translate("keybind." + category + "." + keyBind.name()));
            panel.add(label, BorderLayout.WEST);

            JButton button = new JButton(I18n.translate("menu.file.configure_keybinds.edit_keybind"));
            button.addActionListener(e -> {
                EditKeyBindDialog dialog = new EditKeyBindDialog(owner, keyBind);
                dialog.setVisible(true);
            });
            panel.add(button, BorderLayout.EAST);

            keyBindsPanel.add(panel);
        }
        contentPane.add(keyBindsPanel, BorderLayout.CENTER);

        // Add buttons
        Container buttonContainer = new JPanel(new FlowLayout(FlowLayout.RIGHT, ScaleUtil.scale(4), ScaleUtil.scale(4)));
        JButton okButton = new JButton(I18n.translate("prompt.ok"));
        okButton.addActionListener(event -> close());
        buttonContainer.add(okButton);
        contentPane.add(buttonContainer, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(owner);
    }

    private void close() {
        setVisible(false);
        dispose();
    }
}
