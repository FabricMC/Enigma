package cuchaz.enigma.gui.elements;

import java.awt.Graphics;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPasswordField;
import javax.swing.JToolTip;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;

import cuchaz.enigma.utils.validation.ParameterizedMessage;
import cuchaz.enigma.utils.validation.Validatable;

public class ValidatablePasswordField extends JPasswordField implements Validatable {
	private List<ParameterizedMessage> messages = new ArrayList<>();
	private String tooltipText = null;

	public ValidatablePasswordField() {
	}

	public ValidatablePasswordField(String text) {
		super(text);
	}

	public ValidatablePasswordField(int columns) {
		super(columns);
	}

	public ValidatablePasswordField(String text, int columns) {
		super(text, columns);
	}

	public ValidatablePasswordField(Document doc, String txt, int columns) {
		super(doc, txt, columns);
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
