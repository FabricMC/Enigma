package cuchaz.enigma.gui.panels;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.accessibility.AccessibleContext;
import org.jetbrains.annotations.Nullable;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeListener;

public class ClosableTabTitlePane {

	private final JPanel ui;
	private final JButton closeButton;
	private final JLabel label;

	private ChangeListener cachedChangeListener;
	private JTabbedPane parent;

	public ClosableTabTitlePane(String text, Runnable onClose) {
		this.ui = new JPanel(new FlowLayout(FlowLayout.CENTER, 2, 2));
		this.ui.setOpaque(false);
		this.label = new JLabel(text);
		this.ui.add(this.label);

		// Adapted from javax.swing.plaf.metal.MetalTitlePane
		this.closeButton = new JButton();
		this.closeButton.setFocusPainted(false);
		this.closeButton.setFocusable(false);
		this.closeButton.setOpaque(true);
		this.closeButton.setIcon(UIManager.getIcon("InternalFrame.closeIcon"));
		this.closeButton.putClientProperty("paintActive", Boolean.TRUE);
		this.closeButton.setBorder(new EmptyBorder(0, 0, 0, 0));
		this.closeButton.putClientProperty(AccessibleContext.ACCESSIBLE_NAME_PROPERTY, "Close");
		this.closeButton.setMaximumSize(new Dimension(this.closeButton.getIcon().getIconWidth(), this.closeButton.getIcon().getIconHeight()));
		this.ui.add(this.closeButton);

		// Use mouse listener here so that it also works for disabled buttons
		closeButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (SwingUtilities.isLeftMouseButton(e) || SwingUtilities.isMiddleMouseButton(e)) {
					onClose.run();
				}
			}
		});

		this.ui.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (SwingUtilities.isMiddleMouseButton(e)) {
					onClose.run();
				}
			}

			@Override
			public void mousePressed(MouseEvent e) {
				// for some reason registering a mouse listener on this makes
				// events never go to the tabbed pane, so we have to redirect
				// the event for tab selection and context menu to work
				if (parent != null) {
					Point pt = new Point(e.getXOnScreen(), e.getYOnScreen());
					SwingUtilities.convertPointFromScreen(pt, parent);
					MouseEvent e1 = new MouseEvent(
							parent,
							e.getID(),
							e.getWhen(),
							e.getModifiersEx(),
							(int) pt.getX(),
							(int) pt.getY(),
							e.getXOnScreen(),
							e.getYOnScreen(),
							e.getClickCount(),
							e.isPopupTrigger(),
							e.getButton()
					);
					parent.dispatchEvent(e1);
				}
			}
		});

		this.ui.putClientProperty(ClosableTabTitlePane.class, this);
	}

	public void setTabbedPane(JTabbedPane pane) {
		if (this.parent != null) {
			pane.removeChangeListener(cachedChangeListener);
		}
		if (pane != null) {
			updateState(pane);
			cachedChangeListener = e -> updateState(pane);
			pane.addChangeListener(cachedChangeListener);
		}
		this.parent = pane;
	}

	public void setText(String text) {
		this.label.setText(text);
	}

	public String getText() {
		return this.label.getText();
	}

	private void updateState(JTabbedPane pane) {
		int selectedIndex = pane.getSelectedIndex();
		boolean isActive = selectedIndex != -1 && pane.getTabComponentAt(selectedIndex) == this.ui;
		this.closeButton.setEnabled(isActive);
		this.closeButton.putClientProperty("paintActive", isActive);
		this.ui.repaint();
	}

	public JPanel getUi() {
		return ui;
	}

	@Nullable
	public static ClosableTabTitlePane byUi(Component c) {
		if (c instanceof JComponent) {
			Object prop = ((JComponent) c).getClientProperty(ClosableTabTitlePane.class);
			if (prop instanceof ClosableTabTitlePane) {
				return (ClosableTabTitlePane) prop;
			}
		}
		return null;
	}

}
