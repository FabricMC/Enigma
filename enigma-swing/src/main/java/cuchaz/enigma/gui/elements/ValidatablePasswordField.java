package cuchaz.enigma.gui.elements;

import java.awt.Color;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JPasswordField;
import javax.swing.JToolTip;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;

import cuchaz.enigma.gui.util.ScaleUtil;
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
		List<String> strings = new ArrayList<>();
		if (tooltipText != null) {
			strings.add(tooltipText);
		}
		if (!messages.isEmpty()) {
			strings.add("Error(s): ");

			messages.forEach(msg -> {
				strings.add(String.format(" - %s", msg.getText()));
				String longDesc = msg.getLongText();
				if (!longDesc.isEmpty()) {
					Arrays.stream(longDesc.split("\n")).map(s -> String.format("   %s", s)).forEach(strings::add);
				}
			});
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
	public void addMessage(ParameterizedMessage message) {
		messages.add(message);
		setToolTipText0();
		repaint();
	}

	@Override
	public void paint(Graphics g) {
		super.paint(g);
		if (!messages.isEmpty()) {
			g.setColor(Color.RED);
			int x1 = getWidth() - ScaleUtil.scale(8) - 1;
			int x2 = getWidth() - ScaleUtil.scale(1) - 1;
			int y1 = ScaleUtil.scale(1);
			int y2 = ScaleUtil.scale(8);
			g.fillPolygon(new int[]{x1, x2, x2}, new int[]{y1, y1, y2}, 3);
		}
	}

}
