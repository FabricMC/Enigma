package cuchaz.enigma.gui.elements;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Nullable;

import cuchaz.enigma.gui.util.ScaleUtil;
import cuchaz.enigma.utils.validation.ParameterizedMessage;

public final class ValidatableUi {
	private ValidatableUi() {
	}

	public static String getTooltipText(String tooltipText, List<ParameterizedMessage> messages) {
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
			return null;
		} else {
			return String.join("\n", strings);
		}
	}

	public static String formatMessages(List<ParameterizedMessage> messages) {
		List<String> strings = new ArrayList<>();

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
			return null;
		} else {
			return String.join("\n", strings);
		}
	}

	public static void drawMarker(Component self, Graphics g, List<ParameterizedMessage> messages) {
		Color color = ValidatableUi.getMarkerColor(messages);

		if (color != null) {
			g.setColor(color);
			int x1 = self.getWidth() - ScaleUtil.scale(8) - 1;
			int x2 = self.getWidth() - ScaleUtil.scale(1) - 1;
			int y1 = ScaleUtil.scale(1);
			int y2 = ScaleUtil.scale(8);
			g.fillPolygon(new int[]{x1, x2, x2}, new int[]{y1, y1, y2}, 3);
		}
	}

	@Nullable
	public static Color getMarkerColor(List<ParameterizedMessage> messages) {
		int level = messages.stream().mapToInt(ValidatableUi::getMessageLevel).max().orElse(0);

		switch (level) {
		case 0:
			return null;
		case 1:
			return Color.BLUE;
		case 2:
			return Color.ORANGE;
		case 3:
			return Color.RED;
		}

		throw new IllegalStateException("unreachable");
	}

	private static int getMessageLevel(ParameterizedMessage message) {
		switch (message.message.type) {
		case INFO:
			return 1;
		case WARNING:
			return 2;
		case ERROR:
			return 3;
		}

		throw new IllegalStateException("unreachable");
	}
}
