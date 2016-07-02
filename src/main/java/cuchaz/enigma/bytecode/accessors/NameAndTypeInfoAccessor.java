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

public class NameAndTypeInfoAccessor {

    private static Class<?> clazz;
    private static Field nameIndex;
    private static Field typeIndex;

    private Object item;

    public NameAndTypeInfoAccessor(Object item) {
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

    public int getTypeIndex() {
        try {
            return (Integer) typeIndex.get(this.item);
        } catch (Exception ex) {
            throw new Error(ex);
        }
    }

    public void setTypeIndex(int val) {
        try {
            typeIndex.set(this.item, val);
        } catch (Exception ex) {
            throw new Error(ex);
        }
    }

    public static boolean isType(ConstInfoAccessor accessor) {
        return clazz.isAssignableFrom(accessor.getItem().getClass());
    }

    static {
        try {
            clazz = Class.forName("javassist.bytecode.NameAndTypeInfo");
            nameIndex = clazz.getDeclaredField("memberName");
            nameIndex.setAccessible(true);
            typeIndex = clazz.getDeclaredField("typeDescriptor");
            typeIndex.setAccessible(true);
        } catch (Exception ex) {
            throw new Error(ex);
        }
    }
}
