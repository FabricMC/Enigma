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
package cuchaz.enigma.bytecode;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;

import cuchaz.enigma.bytecode.accessors.ClassInfoAccessor;
import cuchaz.enigma.bytecode.accessors.ConstInfoAccessor;
import cuchaz.enigma.bytecode.accessors.MemberRefInfoAccessor;
import javassist.bytecode.ConstPool;
import javassist.bytecode.Descriptor;

public class ConstPoolEditor {

    private static Method getItem;
    private static Method addItem;
    private static Method addItem0;
    private static Field items;
    private static Field cache;
    private static Field numItems;
    private static Field objects;
    private static Field elements;
    private static Method methodWritePool;
    private static Constructor<ConstPool> constructorPool;

    static {
        try {
            getItem = ConstPool.class.getDeclaredMethod("getItem", int.class);
            getItem.setAccessible(true);

            addItem = ConstPool.class.getDeclaredMethod("addItem", Class.forName("javassist.bytecode.ConstInfo"));
            addItem.setAccessible(true);

            addItem0 = ConstPool.class.getDeclaredMethod("addItem0", Class.forName("javassist.bytecode.ConstInfo"));
            addItem0.setAccessible(true);

            items = ConstPool.class.getDeclaredField("items");
            items.setAccessible(true);

            cache = ConstPool.class.getDeclaredField("itemsCache");
            cache.setAccessible(true);

            numItems = ConstPool.class.getDeclaredField("numOfItems");
            numItems.setAccessible(true);

            objects = Class.forName("javassist.bytecode.LongVector").getDeclaredField("objects");
            objects.setAccessible(true);

            elements = Class.forName("javassist.bytecode.LongVector").getDeclaredField("elements");
            elements.setAccessible(true);

            methodWritePool = ConstPool.class.getDeclaredMethod("write", DataOutputStream.class);
            methodWritePool.setAccessible(true);

            constructorPool = ConstPool.class.getDeclaredConstructor(DataInputStream.class);
            constructorPool.setAccessible(true);
        } catch (Exception ex) {
            throw new Error(ex);
        }
    }

    private ConstPool pool;

    public ConstPoolEditor(ConstPool pool) {
        this.pool = pool;
    }

    public void writePool(DataOutputStream out) {
        try {
            methodWritePool.invoke(this.pool, out);
        } catch (Exception ex) {
            throw new Error(ex);
        }
    }

    public static ConstPool readPool(DataInputStream in) {
        try {
            return constructorPool.newInstance(in);
        } catch (Exception ex) {
            throw new Error(ex);
        }
    }

    public String getMemberrefClassname(int memberrefIndex) {
        return Descriptor.toJvmName(this.pool.getClassInfo(this.pool.getMemberClass(memberrefIndex)));
    }

    public String getMemberrefName(int memberrefIndex) {
        return this.pool.getUtf8Info(this.pool.getNameAndTypeName(this.pool.getMemberNameAndType(memberrefIndex)));
    }

    public String getMemberrefType(int memberrefIndex) {
        return this.pool.getUtf8Info(this.pool.getNameAndTypeDescriptor(this.pool.getMemberNameAndType(memberrefIndex)));
    }

    public ConstInfoAccessor getItem(int index) {
        try {
            Object entry = getItem.invoke(this.pool, index);
            if (entry == null) {
                return null;
            }
            return new ConstInfoAccessor(entry);
        } catch (Exception ex) {
            throw new Error(ex);
        }
    }

    public int addItem(Object item) {
        try {
            return (Integer) addItem.invoke(this.pool, item);
        } catch (Exception ex) {
            throw new Error(ex);
        }
    }

    public int addItemForceNew(Object item) {
        try {
            return (Integer) addItem0.invoke(this.pool, item);
        } catch (Exception ex) {
            throw new Error(ex);
        }
    }

    @SuppressWarnings("rawtypes")
    public void removeLastItem() {
        try {
            // remove the item from the cache
            HashMap cache = getCache();
            if (cache != null) {
                Object item = getItem(this.pool.getSize() - 1);
                cache.remove(item);
            }

            // remove the actual item
            // based off of LongVector.addElement()
            Object item = items.get(this.pool);
            Object[][] object = (Object[][]) objects.get(items);
            int numElements = (Integer) elements.get(items) - 1;
            int nth = numElements >> 7;
            int offset = numElements & (128 - 1);
            object[nth][offset] = null;

            // decrement the number of items
            elements.set(item, numElements);
            numItems.set(this.pool, (Integer) numItems.get(this.pool) - 1);
        } catch (Exception ex) {
            throw new Error(ex);
        }
    }

    @SuppressWarnings("rawtypes")
    public HashMap getCache() {
        try {
            return (HashMap) cache.get(this.pool);
        } catch (Exception ex) {
            throw new Error(ex);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public void changeMemberrefNameAndType(int memberrefIndex, String newName, String newType) {
        // NOTE: when changing values, we always need to copy-on-write
        try {
            // get the memberref item
            Object item = getItem(memberrefIndex).getItem();

            // update the cache
            HashMap cache = getCache();
            if (cache != null) {
                cache.remove(item);
            }

            new MemberRefInfoAccessor(item).setNameAndTypeIndex(this.pool.addNameAndTypeInfo(newName, newType));

            // update the cache
            if (cache != null) {
                cache.put(item, item);
            }
        } catch (Exception ex) {
            throw new Error(ex);
        }

        // make sure the change worked
        assert (newName.equals(getMemberrefName(memberrefIndex)));
        assert (newType.equals(getMemberrefType(memberrefIndex)));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public void changeClassName(int classNameIndex, String newName) {
        // NOTE: when changing values, we always need to copy-on-write
        try {
            // get the class item
            Object item = getItem(classNameIndex).getItem();

            // update the cache
            HashMap cache = getCache();
            if (cache != null) {
                cache.remove(item);
            }

            // add the new name and repoint the name-and-type to it
            new ClassInfoAccessor(item).setNameIndex(this.pool.addUtf8Info(newName));

            // update the cache
            if (cache != null) {
                cache.put(item, item);
            }
        } catch (Exception ex) {
            throw new Error(ex);
        }
    }

    public static ConstPool newConstPool() {
        // const pool expects the name of a class to initialize itself
        // but we want an empty pool
        // so give it a bogus name, and then clear the entries afterwards
        ConstPool pool = new ConstPool("a");

        ConstPoolEditor editor = new ConstPoolEditor(pool);
        int size = pool.getSize();
        for (int i = 0; i < size - 1; i++) {
            editor.removeLastItem();
        }

        // make sure the pool is actually empty
        // although, in this case "empty" means one thing in it
        // the JVM spec says index 0 should be reserved
        assert (pool.getSize() == 1);
        assert (editor.getItem(0) == null);
        assert (editor.getItem(1) == null);
        assert (editor.getItem(2) == null);
        assert (editor.getItem(3) == null);

        // also, clear the cache
        editor.getCache().clear();

        return pool;
    }

    public String dump() {
        StringBuilder buf = new StringBuilder();
        for (int i = 1; i < this.pool.getSize(); i++) {
            buf.append(String.format("%4d", i));
            buf.append("   ");
            buf.append(getItem(i).toString());
            buf.append("\n");
        }
        return buf.toString();
    }
}
