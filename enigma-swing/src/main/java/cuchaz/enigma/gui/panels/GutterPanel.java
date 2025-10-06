package cuchaz.enigma.gui.panels;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.LayoutManager2;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;

import de.sciss.syntaxpane.actions.ActionUtils;

import cuchaz.enigma.api.service.GuiService;
import cuchaz.enigma.gui.config.UiConfig;

public class GutterPanel extends JPanel {
	private final JPanel markerPanel;

	public GutterPanel(JEditorPane editor, JComponent lineNumbers) {
		markerPanel = new MarkerPanel(editor);

		setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
		add(lineNumbers);
		add(markerPanel);

		setBackground(UiConfig.getLineNumbersBackgroundColor());
		setForeground(UiConfig.getLineNumbersForegroundColor());
		markerPanel.setBackground(UiConfig.getLineNumbersBackgroundColor());
		markerPanel.setForeground(UiConfig.getLineNumbersForegroundColor());
		setBorder(lineNumbers.getBorder());
		lineNumbers.setBorder(null);
	}

	public void clearMarkers() {
		markerPanel.removeAll();
	}

	public void addMarker(int line, GuiService.GutterMarkerAlignment alignment, Component marker) {
		markerPanel.add(marker, new MarkerLayout.Constraint(line, alignment));
	}

	private static class MarkerPanel extends JPanel implements CaretListener, DocumentListener, PropertyChangeListener {
		private final JEditorPane editor;
		private final Color currentLineColor = UiConfig.getLineNumbersSelectedColor();

		private MarkerPanel(JEditorPane editor) {
			this.editor = editor;
			setLayout(new MarkerLayout());

			Insets editorInsets = editor.getInsets();

			if (editorInsets.top != 0 || editorInsets.bottom != 0) {
				setBorder(BorderFactory.createEmptyBorder(editorInsets.top, 0, editorInsets.bottom, 0));
			}

			setFont(editor.getFont());

			editor.addCaretListener(this);
			editor.getDocument().addDocumentListener(this);
			editor.addPropertyChangeListener(this);
		}

		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);

			int currentLine;

			try {
				currentLine = ActionUtils.getLineNumber(editor, editor.getCaretPosition());
			} catch (BadLocationException ex) {
				return; // no valid caret -> nothing to draw
			}

			FontMetrics fm = getFontMetrics(getFont());
			int lh = fm.getHeight();
			Insets insets = getInsets();

			int y = currentLine * lh;

			g.setColor(currentLineColor);
			g.fillRect(0, y, getWidth(), lh);
		}

		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			if (evt.getPropertyName().equals("document")) {
				repaint();
			}
		}

		@Override
		public void caretUpdate(CaretEvent e) {
			repaint();
		}

		@Override
		public void insertUpdate(DocumentEvent e) {
			documentChanged();
		}

		@Override
		public void removeUpdate(DocumentEvent e) {
			documentChanged();
		}

		@Override
		public void changedUpdate(DocumentEvent e) {
			documentChanged();
		}

		private void documentChanged() {
			SwingUtilities.invokeLater(this::repaint);
		}
	}

	private static class MarkerLayout implements LayoutManager2 {
		private static final int GAP = 2;
		private static final int HUGE_HEIGHT = 0x100000; // taken from LineNumbersRuler

		private final Map<Component, Constraint> constraints = new HashMap<>();

		@Override
		public void addLayoutComponent(Component comp, Object constraints) {
			this.constraints.put(comp, (Constraint) constraints);
		}

		@Override
		public Dimension maximumLayoutSize(Container target) {
			return preferredLayoutSize(target);
		}

		@Override
		public float getLayoutAlignmentX(Container target) {
			return 0.5f;
		}

		@Override
		public float getLayoutAlignmentY(Container target) {
			return 0.5f;
		}

		@Override
		public void invalidateLayout(Container target) {
		}

		@Override
		public void addLayoutComponent(String name, Component comp) {
		}

		@Override
		public void removeLayoutComponent(Component comp) {
			constraints.remove(comp);
		}

		@Override
		public Dimension preferredLayoutSize(Container parent) {
			synchronized (parent.getTreeLock()) {
				Insets insets = parent.getInsets();

				if (constraints.isEmpty()) {
					return new Dimension(insets.left + insets.right, HUGE_HEIGHT);
				}

				Map<Integer, Integer> leftCount = new HashMap<>();
				Map<Integer, Integer> rightCount = new HashMap<>();

				for (Constraint constraint : constraints.values()) {
					switch (constraint.alignment) {
					case LEFT -> leftCount.merge(constraint.line, 1, Integer::sum);
					case RIGHT -> rightCount.merge(constraint.line, 1, Integer::sum);
					}
				}

				int maxLeft = leftCount.values().stream().mapToInt(Integer::intValue).max().orElse(0);
				int maxRight = rightCount.values().stream().mapToInt(Integer::intValue).max().orElse(0);

				int lineHeight = parent.getFontMetrics(parent.getFont()).getHeight();
				return new Dimension(
						GAP + insets.left + insets.right + (maxLeft + maxRight) * lineHeight,
						HUGE_HEIGHT
				);
			}
		}

		@Override
		public Dimension minimumLayoutSize(Container parent) {
			return preferredLayoutSize(parent);
		}

		@Override
		public void layoutContainer(Container parent) {
			synchronized (parent.getTreeLock()) {
				Map<Integer, Integer> leftCount = new HashMap<>();
				Map<Integer, Integer> rightCount = new HashMap<>();

				int lineHeight = parent.getFontMetrics(parent.getFont()).getHeight();

				int numComponents = parent.getComponentCount();

				for (int i = 0; i < numComponents; i++) {
					Component comp = parent.getComponent(i);
					Constraint constraint = constraints.get(comp);

					if (constraint == null) {
						continue;
					}

					int left = switch (constraint.alignment) {
					case LEFT -> GAP + (leftCount.merge(constraint.line, 1, Integer::sum) - 1) * lineHeight + GAP / 2;
					case RIGHT -> parent.getWidth() - rightCount.merge(constraint.line, 1, Integer::sum) * lineHeight + GAP / 2;
					};
					comp.setBounds(left, constraint.line * lineHeight + GAP / 2, lineHeight - GAP, lineHeight - GAP);
				}
			}
		}

		private record Constraint(int line, GuiService.GutterMarkerAlignment alignment) {
		}
	}
}
