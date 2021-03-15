package cuchaz.enigma.gui.config;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.plaf.metal.MetalLookAndFeel;

import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatLightLaf;

public enum LookAndFeel {
	DEFAULT("Default"),
	DARCULA("Darcula"),
	METAL("Metal"),
	SYSTEM("System"),
	NONE("None (JVM default)");

	// the "JVM default" look and feel, get it at the beginning and store it so we can set it later
	private static final javax.swing.LookAndFeel NONE_LAF = UIManager.getLookAndFeel();
	private final String name;

	LookAndFeel(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setGlobalLAF() {
		try {
			switch (this) {
				case NONE:
					UIManager.setLookAndFeel(NONE_LAF);
					break;
				case DEFAULT:
					UIManager.setLookAndFeel(new FlatLightLaf());
					break;
				case METAL:
					UIManager.setLookAndFeel(new MetalLookAndFeel());
					break;
				case DARCULA:
					UIManager.setLookAndFeel(new FlatDarculaLaf());
					break;
				case SYSTEM:
					UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			}
		} catch (Exception e) {
			throw new Error("Failed to set global look and feel", e);
		}
	}

	public static boolean isDarkLaf() {
		// a bit of a hack because swing doesn't give any API for that, and we need colors that aren't defined in look and feel
		JPanel panel = new JPanel();
		panel.setSize(new Dimension(10, 10));
		panel.doLayout();

		BufferedImage image = new BufferedImage(panel.getSize().width, panel.getSize().height, BufferedImage.TYPE_INT_RGB);
		panel.printAll(image.getGraphics());

		Color c = new Color(image.getRGB(0, 0));

		// convert the color we got to grayscale
		int b = (int) (0.3 * c.getRed() + 0.59 * c.getGreen() + 0.11 * c.getBlue());
		return b < 85;
	}

}
