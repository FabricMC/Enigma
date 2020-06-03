package cuchaz.enigma.gui.util;

import java.awt.Dimension;
import java.awt.Font;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.UIManager;
import javax.swing.border.Border;

import com.github.swingdpi.UiDefaultsScaler;
import com.github.swingdpi.plaf.BasicTweaker;
import com.github.swingdpi.plaf.MetalTweaker;
import com.github.swingdpi.plaf.NimbusTweaker;
import com.github.swingdpi.plaf.WindowsTweaker;
import cuchaz.enigma.gui.config.Config;
import de.sciss.syntaxpane.DefaultSyntaxKit;

public class ScaleUtil {

	private static List<ScaleChangeListener> listeners = new ArrayList<>();

	public static float getScaleFactor() {
		return Config.getInstance().scaleFactor;
	}

	public static void setScaleFactor(float scaleFactor) {
		float oldScale = getScaleFactor();
		float clamped = Math.min(Math.max(0.25f, scaleFactor), 10.0f);
		Config.getInstance().scaleFactor = clamped;
		try {
			Config.getInstance().saveConfig();
		} catch (IOException e) {
			e.printStackTrace();
		}
		listeners.forEach(l -> l.onScaleChanged(clamped, oldScale));
	}

	public static void addListener(ScaleChangeListener listener) {
		listeners.add(listener);
	}

	public static void removeListener(ScaleChangeListener listener) {
		listeners.remove(listener);
	}

	public static Dimension getDimension(int width, int height) {
		return new Dimension(scale(width), scale(height));
	}

	public static Font getFont(String fontName, int plain, int fontSize) {
		return scaleFont(new Font(fontName, plain, fontSize));
	}

	public static Font scaleFont(Font font) {
		return createTweakerForCurrentLook(getScaleFactor()).modifyFont("", font);
	}

	public static float scale(float f) {
		return f * getScaleFactor();
	}

	public static float invert(float f) {
		return f / getScaleFactor();
	}

	public static int scale(int i) {
		return (int) (i * getScaleFactor());
	}

	public static Border createEmptyBorder(int top, int left, int bottom, int right) {
		return BorderFactory.createEmptyBorder(scale(top), scale(left), scale(bottom), scale(right));
	}

	public static int invert(int i) {
		return (int) (i / getScaleFactor());
	}

	public static void applyScaling() {
		float scale = getScaleFactor();
		UiDefaultsScaler.updateAndApplyGlobalScaling((int) (100 * scale), true);
		try {
			Field defaultFontField = DefaultSyntaxKit.class.getDeclaredField("DEFAULT_FONT");
			defaultFontField.setAccessible(true);
			Font font = (Font) defaultFontField.get(null);
			font = font.deriveFont(12 * scale);
			defaultFontField.set(null, font);
		} catch (NoSuchFieldException | IllegalAccessException e) {
			e.printStackTrace();
		}
	}

	private static BasicTweaker createTweakerForCurrentLook(float dpiScaling) {
		String testString = UIManager.getLookAndFeel().getName().toLowerCase();
		if (testString.contains("windows")) {
			return new WindowsTweaker(dpiScaling, testString.contains("classic"));
		}
		if (testString.contains("metal")) {
			return new MetalTweaker(dpiScaling);
		}
		if (testString.contains("nimbus")) {
			return new NimbusTweaker(dpiScaling);
		}
		return new BasicTweaker(dpiScaling);
	}

}
