package cuchaz.enigma.gui.util;

import cuchaz.enigma.utils.Os;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.TextAttribute;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

public class GuiUtil {
    public static final Icon CLASS_ICON = loadIcon("class");
    public static final Icon METHOD_ICON = loadIcon("method");
    public static final Icon FIELD_ICON = loadIcon("field");
    public static final Icon CONSTRUCTOR_ICON = loadIcon("constructor");

    public static void openUrl(String url) {
        try {
            switch (Os.getOs()) {
                case LINUX:
                    new ProcessBuilder("/usr/bin/env", "xdg-open", url).start();
                    break;
                default:
                    if (Desktop.isDesktopSupported()) {
                        Desktop desktop = Desktop.getDesktop();
                        desktop.browse(new URI(url));
                    }
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException(ex);
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

    public static Icon loadIcon(String name) {
        try {
            InputStream inputStream = GuiUtil.class.getResourceAsStream("/icons/" + name + ".png");
            Image image = ImageIO.read(inputStream).getScaledInstance(ScaleUtil.scale(16), ScaleUtil.scale(16), Image.SCALE_DEFAULT);
            return new ImageIcon(image);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }
}
