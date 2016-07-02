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

public class MethodHandleInfoAccessor {

    private static Class<?> clazz;
    private static Field kindIndex;
    private static Field indexIndex;

    private Object item;

    public MethodHandleInfoAccessor(Object item) {
        this.item = item;
    }

    public int getTypeIndex() {
        try {
            return (Integer) kindIndex.get(this.item);
        } catch (Exception ex) {
            throw new Error(ex);
        }
    }

    public void setTypeIndex(int val) {
        try {
            kindIndex.set(this.item, val);
        } catch (Exception ex) {
            throw new Error(ex);
        }
    }

    public int getMethodRefIndex() {
        try {
            return (Integer) indexIndex.get(this.item);
        } catch (Exception ex) {
            throw new Error(ex);
        }
    }

    public void setMethodRefIndex(int val) {
        try {
            indexIndex.set(this.item, val);
        } catch (Exception ex) {
            throw new Error(ex);
        }
    }

    static {
        try {
            clazz = Class.forName("javassist.bytecode.MethodHandleInfo");
            kindIndex = clazz.getDeclaredField("refKind");
            kindIndex.setAccessible(true);
            indexIndex = clazz.getDeclaredField("refIndex");
            indexIndex.setAccessible(true);
        } catch (Exception ex) {
            throw new Error(ex);
        }
    }
}
