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
package cuchaz.enigma.gui;

import javax.swing.text.DefaultCaret;
import javax.swing.text.Highlighter;

public class BrowserCaret extends DefaultCaret {

    private static final long serialVersionUID = 1158977422507969940L;

    private static final Highlighter.HighlightPainter selectionPainter = (g, p0, p1, bounds, c) -> {
    };

    @Override
    public boolean isSelectionVisible() {
        return false;
    }

    @Override
    public boolean isVisible() {
        return true;
    }

    @Override
    public Highlighter.HighlightPainter getSelectionPainter() {
        return selectionPainter;
    }
}
