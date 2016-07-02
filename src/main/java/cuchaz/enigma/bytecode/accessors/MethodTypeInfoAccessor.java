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

public class MethodTypeInfoAccessor {

    private static Class<?> clazz;
    private static Field descriptorIndex;

    private Object item;

    public MethodTypeInfoAccessor(Object item) {
        this.item = item;
    }

    public int getTypeIndex() {
        try {
            return (Integer) descriptorIndex.get(this.item);
        } catch (Exception ex) {
            throw new Error(ex);
        }
    }

    public void setTypeIndex(int val) {
        try {
            descriptorIndex.set(this.item, val);
        } catch (Exception ex) {
            throw new Error(ex);
        }
    }

    static {
        try {
            clazz = Class.forName("javassist.bytecode.MethodTypeInfo");
            descriptorIndex = clazz.getDeclaredField("descriptor");
            descriptorIndex.setAccessible(true);
        } catch (Exception ex) {
            throw new Error(ex);
        }
    }

}
