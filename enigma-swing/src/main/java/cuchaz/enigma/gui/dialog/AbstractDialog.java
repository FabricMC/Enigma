package cuchaz.enigma.gui.dialog;

import java.awt.*;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

import cuchaz.enigma.gui.util.ScaleUtil;
import cuchaz.enigma.utils.I18n;
import cuchaz.enigma.utils.Pair;
import cuchaz.enigma.utils.validation.ValidationContext;

public abstract class AbstractDialog extends JDialog {

	protected final ValidationContext vc = new ValidationContext();

	private boolean actionConfirm = false;

	public AbstractDialog(Frame owner, String title, String confirmAction, String cancelAction) {
		super(owner, I18n.translate(title), true);

		Container contentPane = getContentPane();
		contentPane.setLayout(new BorderLayout());
		Container inputContainer = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();

		List<Pair<String, Component>> components = createComponents();

		for (int i = 0; i < components.size(); i += 1) {
			Pair<String, Component> entry = components.get(i);
			JLabel label = new JLabel(I18n.translate(entry.a));
			Component component = entry.b;

			c.gridy = i;
			c.insets = ScaleUtil.getInsets(4, 4, 4, 4);

			c.gridx = 0;
			c.weightx = 0.0;
			c.anchor = GridBagConstraints.LINE_END;
			c.fill = GridBagConstraints.NONE;
			inputContainer.add(label, c);

			c.gridx = 1;
			c.weightx = 1.0;
			c.anchor = GridBagConstraints.LINE_START;
			c.fill = GridBagConstraints.HORIZONTAL;
			inputContainer.add(component, c);
		}
		contentPane.add(inputContainer, BorderLayout.CENTER);
		Container buttonContainer = new JPanel(new FlowLayout(FlowLayout.RIGHT, ScaleUtil.scale(4), ScaleUtil.scale(4)));
		JButton connectButton = new JButton(I18n.translate(confirmAction));
		connectButton.addActionListener(event -> confirm());
		buttonContainer.add(connectButton);
		JButton abortButton = new JButton(I18n.translate(cancelAction));
		abortButton.addActionListener(event -> cancel());
		buttonContainer.add(abortButton);
		contentPane.add(buttonContainer, BorderLayout.SOUTH);

		pack();
		setLocationRelativeTo(owner);
	}

	protected abstract List<Pair<String, Component>> createComponents();

	protected void confirm() {
		vc.reset();
		validateInputs();
		if (vc.canProceed()) {
			actionConfirm = true;
			setVisible(false);
		}
	}

	protected void cancel() {
		actionConfirm = false;
		setVisible(false);
	}

	public boolean isActionConfirm() {
		return actionConfirm;
	}

	public void validateInputs() {
	}

}
