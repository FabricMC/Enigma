package cuchaz.enigma.gui.elements;

import java.awt.Color;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JTextField;
import javax.swing.JToolTip;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;

import cuchaz.enigma.utils.validation.Message;
import cuchaz.enigma.utils.validation.Validatable;

public class ValidatableTextField extends JTextField implements Validatable {

	private List<String> messages = new ArrayList<>();
	private String tooltipText = null;

	public ValidatableTextField() {
	}

	public ValidatableTextField(String text) {
		super(text);
	}

	public ValidatableTextField(int columns) {
		super(columns);
	}

	public ValidatableTextField(String text, int columns) {
		super(text, columns);
	}

	public ValidatableTextField(Document doc, String text, int columns) {
		super(doc, text, columns);
	}

	{
		getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void insertUpdate(DocumentEvent e) {
				clearMessages();
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				clearMessages();
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				clearMessages();
			}
		});
	}

	@Override
	public JToolTip createToolTip() {
		JMultiLineToolTip tooltip = new JMultiLineToolTip();
		tooltip.setComponent(this);
		return tooltip;
	}

	@Override
	public void setToolTipText(String text) {
		tooltipText = text;
		setToolTipText0();
	}

	private void setToolTipText0() {
		List<String> strings = new ArrayList<>();
		if (tooltipText != null) {
			strings.add(tooltipText);
		}
		if (!messages.isEmpty()) {
			strings.add("Error(s): ");
			messages.forEach(s -> strings.add(String.format(" - %s", s)));
		}
		if (strings.isEmpty()) {
			super.setToolTipText(null);
		} else {
			super.setToolTipText(String.join("\n", strings));
		}
	}

	@Override
	public void clearMessages() {
		messages.clear();
		setToolTipText0();
		repaint();
	}

	@Override
	public void addMessage(Message message, Object[] args) {
		messages.add(message.format(args));
		setToolTipText0();
		repaint();
	}

	@Override
	public void paint(Graphics g) {
		super.paint(g);
		if (!messages.isEmpty()) {
			g.setColor(Color.RED);
			int x1 = getWidth() - 9;
			int x2 = getWidth() - 2;
			int y1 = 1;
			int y2 = 8;
			g.fillPolygon(new int[]{x1, x2, x2}, new int[]{y1, y1, y2}, 3);
		}
	}

}
