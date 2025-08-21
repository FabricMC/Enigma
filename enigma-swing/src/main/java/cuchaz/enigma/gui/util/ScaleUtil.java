package cuchaz.enigma.gui.util;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

import javax.swing.BorderFactory;
import javax.swing.UIManager;
import javax.swing.border.Border;

import com.github.swingdpi.UiDefaultsScaler;
import com.github.swingdpi.plaf.BasicTweaker;
import com.github.swingdpi.plaf.MetalTweaker;
import com.github.swingdpi.plaf.NimbusTweaker;
import com.github.swingdpi.plaf.WindowsTweaker;

import cuchaz.enigma.gui.config.UiConfig;

public class ScaleUtil {
	private static List<ScaleChangeListener> listeners = new ArrayList<>();

	public static void setScaleFactor(float scaleFactor) {
		float oldScale = UiConfig.getScaleFactor();
		float clamped = Math.min(Math.max(0.25f, scaleFactor), 10.0f);
		UiConfig.setScaleFactor(clamped);
		rescaleFontInConfig("Default", oldScale);
		rescaleFontInConfig("Default 2", oldScale);
		rescaleFontInConfig("Small", oldScale);
		rescaleFontInConfig("Editor", oldScale);
		UiConfig.save();
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

	public static Insets getInsets(int top, int left, int bottom, int right) {
		return new Insets(scale(top), scale(left), scale(bottom), scale(right));
	}

	public static Font getFont(String fontName, int style, int fontSize) {
		return scaleFont(new Font(fontName, style, fontSize));
	}

	public static Font scaleFont(Font font) {
		return createTweakerForCurrentLook(UiConfig.getActiveScaleFactor()).modifyFont("", font);
	}

	private static void rescaleFontInConfig(String name, float oldScale) {
		UiConfig.getFont(name).ifPresent(font -> UiConfig.setFont(name, rescaleFont(font, oldScale)));
	}

	// This does not use the font that's currently active in the UI!
	private static Font rescaleFont(Font font, float oldScale) {
		float newSize = Math.round(font.getSize() / oldScale * UiConfig.getScaleFactor());
		return font.deriveFont(newSize);
	}

	public static float scale(float f) {
		return f * UiConfig.getActiveScaleFactor();
	}

	public static float invert(float f) {
		return f / UiConfig.getActiveScaleFactor();
	}

	public static int scale(int i) {
		return (int) (i * UiConfig.getActiveScaleFactor());
	}

	public static Border createEmptyBorder(int top, int left, int bottom, int right) {
		return BorderFactory.createEmptyBorder(scale(top), scale(left), scale(bottom), scale(right));
	}

	public static int invert(int i) {
		return (int) (i / UiConfig.getActiveScaleFactor());
	}

	public static void applyScaling() {
		float scale = UiConfig.getActiveScaleFactor();

		if (UiConfig.getActiveLookAndFeel().needsScaling()) {
			UiDefaultsScaler.updateAndApplyGlobalScaling((int) (100 * scale), true);
		}
	}

	@SuppressWarnings("null")
	private static BasicTweaker createTweakerForCurrentLook(float dpiScaling) {
		String testString = UIManager.getLookAndFeel().getName().toLowerCase(Locale.ROOT);

		if (testString.contains("windows")) {
			return new WindowsTweaker(dpiScaling, testString.contains("classic")) {
				@Override
				public Font modifyFont(Object key, Font original) {
					return ScaleUtil.fallbackModifyFont(key, original, super.modifyFont(key, original), scaleFactor, BasicTweaker::isUnscaled);
				}
			};
		}

		if (testString.contains("metal")) {
			return new MetalTweaker(dpiScaling) {
				@Override
				public Font modifyFont(Object key, Font original) {
					return ScaleUtil.fallbackModifyFont(key, original, super.modifyFont(key, original), scaleFactor, BasicTweaker::isUnscaled);
				}
			};
		}

		if (testString.contains("nimbus")) {
			return new NimbusTweaker(dpiScaling) {
				@Override
				public Font modifyFont(Object key, Font original) {
					return ScaleUtil.fallbackModifyFont(key, original, super.modifyFont(key, original), scaleFactor, BasicTweaker::isUnscaled);
				}
			};
		}

		return new BasicTweaker(dpiScaling) {
			@Override
			public Font modifyFont(Object key, Font original) {
				return ScaleUtil.fallbackModifyFont(key, original, super.modifyFont(key, original), scaleFactor, BasicTweaker::isUnscaled);
			}
		};
	}

	private static Font fallbackModifyFont(Object key, Font original, Font modified, float scaleFactor, Predicate<Float> unscaledCheck) {
		if (modified == original && !unscaledCheck.test(scaleFactor)) {
			return original.deriveFont(original.getSize() * scaleFactor);
		}

		return modified;
	}
}
