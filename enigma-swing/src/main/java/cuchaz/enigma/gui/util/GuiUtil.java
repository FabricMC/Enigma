package cuchaz.enigma.gui.util;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.StringJoiner;

public class GuiUtil {
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

}
