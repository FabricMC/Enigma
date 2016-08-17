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
package cuchaz.enigma.mapping;

public class ArgumentMapping implements Comparable<ArgumentMapping> {

    private int index;
    private String name;

    // NOTE: this argument order is important for the MethodReader/MethodWriter
    public ArgumentMapping(int index, String name) {
        this.index = index;
        this.name = NameValidator.validateArgumentName(name);
    }

    public ArgumentMapping(ArgumentMapping other) {
        this.index = other.index;
        this.name = other.name;
    }

    public int getIndex() {
        return this.index;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String val) {
        this.name = NameValidator.validateArgumentName(val);
    }

    @Override
    public int compareTo(ArgumentMapping other) {
        return Integer.compare(this.index, other.index);
    }
}
