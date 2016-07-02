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

public class MemberRefInfoAccessor {

    private static Class<?> clazz;
    private static Field classIndex;
    private static Field nameAndTypeIndex;

    private Object item;

    public MemberRefInfoAccessor(Object item) {
        this.item = item;
    }

    public int getClassIndex() {
        try {
            return (Integer) classIndex.get(this.item);
        } catch (Exception ex) {
            throw new Error(ex);
        }
    }

    public void setClassIndex(int val) {
        try {
            classIndex.set(this.item, val);
        } catch (Exception ex) {
            throw new Error(ex);
        }
    }

    public int getNameAndTypeIndex() {
        try {
            return (Integer) nameAndTypeIndex.get(this.item);
        } catch (Exception ex) {
            throw new Error(ex);
        }
    }

    public void setNameAndTypeIndex(int val) {
        try {
            nameAndTypeIndex.set(this.item, val);
        } catch (Exception ex) {
            throw new Error(ex);
        }
    }

    static {
        try {
            clazz = Class.forName("javassist.bytecode.MemberrefInfo");
            classIndex = clazz.getDeclaredField("classIndex");
            classIndex.setAccessible(true);
            nameAndTypeIndex = clazz.getDeclaredField("nameAndTypeIndex");
            nameAndTypeIndex.setAccessible(true);
        } catch (Exception ex) {
            throw new Error(ex);
        }
    }
}
