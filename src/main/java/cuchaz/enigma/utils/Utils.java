/*******************************************************************************
 * Copyright (c) 2015 Jeff Martin.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public
 * License v3.0 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 * <p>
 * Contributors:
 * Jeff Martin - initial API and implementation
 ******************************************************************************/

package cuchaz.enigma.utils;

import com.google.common.io.CharStreams;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

public class Utils {

	public static int combineHashesOrdered(Object... objs) {
		return combineHashesOrdered(Arrays.asList(objs));
	}

	public static int combineHashesOrdered(List<Object> objs) {
		final int prime = 67;
		int result = 1;
		for (Object obj : objs) {
			result *= prime;
			if (obj != null) {
				result += obj.hashCode();
			}
		}
		return result;
	}

	public static String readStreamToString(InputStream in) throws IOException {
		return CharStreams.toString(new InputStreamReader(in, "UTF-8"));
	}

	public static String readResourceToString(String path) throws IOException {
		InputStream in = Utils.class.getResourceAsStream(path);
		if (in == null) {
			throw new IllegalArgumentException("Resource not found! " + path);
		}
		return readStreamToString(in);
	}

	public static void openUrl(String url) {
		if (Desktop.isDesktopSupported()) {
			Desktop desktop = Desktop.getDesktop();
			try {
				desktop.browse(new URI(url));
			} catch (IOException ex) {
				throw new Error(ex);
			} catch (URISyntaxException ex) {
				throw new IllegalArgumentException(ex);
			}
		}
	}

	public static JLabel unboldLabel(JLabel label) {
		Font font = label.getFont();
		label.setFont(font.deriveFont(font.getStyle() & ~Font.BOLD));
		return label;
	}

	public static void showToolTipNow(JComponent component) {
		// HACKHACK: trick the tooltip manager into showing the tooltip right now
		ToolTipManager manager = ToolTipManager.sharedInstance();
		int oldDelay = manager.getInitialDelay();
		manager.setInitialDelay(0);
		manager.mouseMoved(new MouseEvent(component, MouseEvent.MOUSE_MOVED, System.currentTimeMillis(), 0, 0, 0, 0, false));
		manager.setInitialDelay(oldDelay);
	}

	public static boolean getSystemPropertyAsBoolean(String property, boolean defValue) {
		String value = System.getProperty(property);
		return value == null ? defValue : Boolean.parseBoolean(value);
	}
}
