package cuchaz.enigma.gui.elements;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

import javax.swing.JTextField;
import javax.swing.text.Document;

public class VerifiableTextField extends JTextField {

	private boolean hasError;

	public VerifiableTextField() {
	}

	public VerifiableTextField(String text) {
		super(text);
	}

	public VerifiableTextField(int columns) {
		super(columns);
	}

	public VerifiableTextField(String text, int columns) {
		super(text, columns);
	}

	public VerifiableTextField(Document doc, String text, int columns) {
		super(doc, text, columns);
	}

	{
		addFocusListener(new FocusAdapter() {
			@Override
			public void focusGained(FocusEvent e) {
				setErrorState(false);
			}
		});
	}

	public void setErrorState(boolean b) {
		this.hasError = b;
		repaint();
	}

	@Override
	public void paint(Graphics g) {
		super.paint(g);
		if (hasError) {
			g.setColor(Color.RED);
			int x1 = getWidth() - 9;
			int x2 = getWidth() - 2;
			int y1 = 1;
			int y2 = 8;
			g.fillPolygon(new int[]{x1, x2, x2}, new int[]{y1, y1, y2}, 3);
		}
	}

}
