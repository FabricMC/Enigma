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
package cuchaz.enigma.throwables;

public class MappingParseException extends Exception {

    private int m_line;
    private String m_message;

    public MappingParseException(int line, String message) {
        m_line = line;
        m_message = message;
    }

    @Override
    public String getMessage() {
        return "Line " + m_line + ": " + m_message;
    }
}
