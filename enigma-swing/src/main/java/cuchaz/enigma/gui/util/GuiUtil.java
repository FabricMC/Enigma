package cuchaz.enigma.gui.util;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.TextAttribute;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.ToolTipManager;

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

    public static JLabel createLink(String text, Runnable action) {
        JLabel link = new JLabel(text);
        link.setForeground(Color.BLUE.darker());
        link.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        @SuppressWarnings("unchecked")
        Map<TextAttribute, Object> attributes = (Map<TextAttribute, Object>) link.getFont().getAttributes();
        attributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
        link.setFont(link.getFont().deriveFont(attributes));
        link.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                action.run();
            }
        });
        return link;
    }

}
