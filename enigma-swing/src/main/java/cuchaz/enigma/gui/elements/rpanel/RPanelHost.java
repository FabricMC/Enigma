package cuchaz.enigma.gui.elements.rpanel;

import java.awt.Rectangle;

public interface RPanelHost {

	void attach(RPanel panel);

	void detach(RPanel panel);

	boolean owns(RPanel panel);

	void titleChanged(RPanel panel);

	Rectangle getPanelLocation(RPanel panel);

	void tryMoveTo(RPanel panel, Rectangle rect);

	void activate(RPanel panel);

	void hide(RPanel panel);

	boolean isDedicatedHost();

}
