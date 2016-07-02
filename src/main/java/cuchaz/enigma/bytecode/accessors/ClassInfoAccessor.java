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
package cuchaz.enigma.bytecode.accessors;

import java.lang.reflect.Field;

public class ClassInfoAccessor {

    private Object item;

    private static Class<?> clazz;
    private static Field nameIndex;

    public ClassInfoAccessor(Object item) {
        this.item = item;
    }

    public int getNameIndex() {
        try {
            return (Integer) nameIndex.get(this.item);
        } catch (Exception ex) {
            throw new Error(ex);
        }
    }

    public void setNameIndex(int val) {
        try {
            nameIndex.set(this.item, val);
        } catch (Exception ex) {
            throw new Error(ex);
        }
    }

    static {
        try {
            clazz = Class.forName("javassist.bytecode.ClassInfo");
            nameIndex = clazz.getDeclaredField("name");
            nameIndex.setAccessible(true);
        } catch (Exception ex) {
            throw new Error(ex);
        }
    }
}
