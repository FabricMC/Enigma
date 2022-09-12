package cuchaz.enigma.gui.elements;

import java.awt.Graphics;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JTextArea;
import javax.swing.JToolTip;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;

import cuchaz.enigma.utils.validation.ParameterizedMessage;
import cuchaz.enigma.utils.validation.Validatable;

public class ValidatableTextArea extends JTextArea implements Validatable {
	private List<ParameterizedMessage> messages = new ArrayList<>();
	private String tooltipText = null;

	public ValidatableTextArea() {
	}

	public ValidatableTextArea(String text) {
		super(text);
	}

	public ValidatableTextArea(int rows, int columns) {
		super(rows, columns);
	}

	public ValidatableTextArea(String text, int rows, int columns) {
		super(text, rows, columns);
	}

	public ValidatableTextArea(Document doc) {
		super(doc);
	}

	public ValidatableTextArea(Document doc, String text, int rows, int columns) {
		super(doc, text, rows, columns);
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
		super.setToolTipText(ValidatableUi.getTooltipText(tooltipText, messages));
	}

	@Override
	public void clearMessages() {
		messages.clear();
		setToolTipText0();
		repaint();
	}

	@Override
	public void addMessage(ParameterizedMessage message) {
		messages.add(message);
		setToolTipText0();
		repaint();
	}

	@Override
	public void paint(Graphics g) {
		super.paint(g);
		ValidatableUi.drawMarker(this, g, messages);
	}
}
