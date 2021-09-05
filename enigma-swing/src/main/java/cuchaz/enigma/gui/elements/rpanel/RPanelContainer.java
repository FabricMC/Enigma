package cuchaz.enigma.gui.elements.rpanel;

import java.awt.BorderLayout;
import java.awt.Rectangle;
import java.awt.Window;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;
import javax.swing.JPanel;
import javax.swing.JRootPane;

public class RPanelContainer implements RPanelHost {

	private final JPanel ui = new JPanel(new BorderLayout());
	private final String id;

	private final Map<RPanel, StandaloneRootPane> panels = new HashMap<>();
	private RPanel openPanel = null;

	private final List<RPanelListener> listeners = new ArrayList<>();

	public RPanelContainer() {
		this(null);
	}

	public RPanelContainer(String id) {
		this.id = id;
	}

	public JPanel getUi() {
		return ui;
	}

	@Override
	public void attach(RPanel panel) {
		if (owns(panel)) return;

		JPanel dummy = new JPanel();
		StandaloneRootPane rp = new StandaloneRootPane();
		rp.addCloseListener(() -> detach(panel));
		rp.setTitle(panel.getTitle());
		rp.setContentPane(panel.getContentPane());
		dummy.add(rp);
		rp.setWindowDecorationStyle(JRootPane.PLAIN_DIALOG);
		RootPaneUtil.registerDragListener(rp, new WindowDragListener() {
			@Override
			public void onDragMove(Window w) {
				panel.windowize();
			}
		});
		dummy.remove(rp);

		panels.put(panel, rp);
		panel.setOwner(this);

		listeners.forEach(l -> l.onAttach(this, panel));

		if (openPanel == null) {
			activate(panel);
		}
	}

	@Override
	public void detach(RPanel panel) {
		if (!owns(panel)) return;

		minimize(panel);

		StandaloneRootPane rp = panels.remove(panel);
		panel.setOwner(null);

		rp.setContentPane(new JPanel());

		listeners.forEach(l -> l.onDetach(this, panel));

		this.ui.validate();
	}

	@Override
	public boolean owns(RPanel panel) {
		return panel != null && panels.containsKey(panel);
	}

	@Nullable
	@Override
	public RPanel getActivePanel() {
		return openPanel;
	}

	@Override
	public void titleChanged(RPanel panel) {
		StandaloneRootPane pane = this.panels.get(panel);

		if (pane != null) {
			pane.setTitle(panel.getTitle());
			listeners.forEach(l -> l.onTitleChange(this, panel));
		}
	}

	@Override
	public int getPanelCount() {
		return panels.size();
	}

	@Override
	public int getVisiblePanelCount() {
		return (int) panels.keySet().stream().filter(RPanel::isVisible).count();
	}

	@Override
	public void activate(RPanel panel) {
		if (!owns(panel)) return;
		if (!panel.isVisible()) return;

		minimize(openPanel);
		openPanel = panel;

		StandaloneRootPane rp = panels.get(panel);
		this.ui.add(rp, BorderLayout.CENTER);

		listeners.forEach(l -> l.onActivate(this, panel));

		this.ui.validate();
		this.ui.repaint();
	}

	@Override
	public void minimize(RPanel panel) {
		if (!owns(panel)) return;
		if (openPanel != panel) return;

		this.ui.remove(panels.get(openPanel));
		openPanel = null;

		listeners.forEach(l -> l.onMinimize(this, panel));

		this.ui.repaint();
	}

	@Override
	public void addRPanelListener(RPanelListener listener) {
		listeners.add(listener);
	}

	@Override
	public void removeRPanelListener(RPanelListener listener) {
		listeners.remove(listener);
	}

	@Override
	public void updateVisibleState(RPanel panel) {
		if (!owns(panel)) return;

		if (!panel.isVisible() && openPanel == panel) {
			minimize(panel);
		} else if (panel.isVisible() && openPanel == null) {
			activate(panel);
		}

		listeners.forEach(l -> l.onVisibleStateChange(this, panel));
	}

	@Override
	public Rectangle getPanelLocation(RPanel panel) {
		if (!owns(panel)) return null;

		Rectangle bounds = this.ui.getBounds();

		if (this.ui.isShowing()) {
			bounds.setLocation(this.ui.getLocationOnScreen());
		} else {
			// TODO fix panel location calculation
		}

		return bounds;
	}

	@Override
	public void tryMoveTo(RPanel panel, Rectangle rect) {
		// no
	}

	@Override
	public Rectangle getDragInsertBounds() {
		Rectangle bounds = this.ui.getBounds();
		bounds.setLocation(this.ui.getLocationOnScreen());
		return bounds;
	}

	@Override
	public boolean onDragOver(RPanel panel) {
		// TODO draw
		return true;
	}

	@Override
	public boolean isDedicatedHost() {
		return false;
	}

	@Nullable
	@Override
	public String getId() {
		return this.id;
	}
}
