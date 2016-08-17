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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import com.google.common.base.Charsets;
import cuchaz.enigma.bytecode.InfoType;

public class ConstInfoAccessor {

    private static Class<?> clazz;
    private static Field index;
    private static Method getTag;

    private Object item;

    public ConstInfoAccessor(Object item) {
        if (item == null) {
            throw new IllegalArgumentException("item cannot be null!");
        }
        this.item = item;
    }

    public Object getItem() {
        return this.item;
    }

    public int getIndex() {
        try {
            return (Integer) index.get(this.item);
        } catch (Exception ex) {
            throw new Error(ex);
        }
    }

    public int getTag() {
        try {
            return (Integer) getTag.invoke(this.item);
        } catch (Exception ex) {
            throw new Error(ex);
        }
    }

    public ConstInfoAccessor copy() {
        return new ConstInfoAccessor(copyItem());
    }

    public Object copyItem() {
        // I don't know of a simpler way to copy one of these silly things...
        try {
            // serialize the item
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(buf);
            write(out);

            // deserialize the item
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(buf.toByteArray()));
            Object item = new ConstInfoAccessor(in).getItem();
            in.close();

            return item;
        } catch (Exception ex) {
            throw new Error(ex);
        }
    }

    public void write(DataOutputStream out) throws IOException {
        try {
            out.writeUTF(this.item.getClass().getName());
            out.writeInt(getIndex());

            Method method = this.item.getClass().getMethod("write", DataOutputStream.class);
            method.setAccessible(true);
            method.invoke(this.item, out);
        } catch (IOException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new Error(ex);
        }
    }

    @Override
    public String toString() {
        try {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            PrintWriter out = new PrintWriter(new OutputStreamWriter(buf, Charsets.UTF_8));
            Method print = this.item.getClass().getMethod("print", PrintWriter.class);
            print.setAccessible(true);
            print.invoke(this.item, out);
            out.close();
            return buf.toString("UTF-8").replace("\n", "");
        } catch (Exception ex) {
            throw new Error(ex);
        }
    }

    public InfoType getType() {
        return InfoType.getByTag(getTag());
    }

    static {
        try {
            clazz = Class.forName("javassist.bytecode.ConstInfo");
            index = clazz.getDeclaredField("index");
            index.setAccessible(true);
            getTag = clazz.getMethod("getTag");
            getTag.setAccessible(true);
        } catch (Exception ex) {
            throw new Error(ex);
        }
    }
}
