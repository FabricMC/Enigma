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

public class InvokeDynamicInfoAccessor {

	private static Class<?> clazz;
	private static Field bootstrapIndex;
	private static Field nameAndTypeIndex;

	static {
		try {
			clazz = Class.forName("javassist.bytecode.InvokeDynamicInfo");
			bootstrapIndex = clazz.getDeclaredField("bootstrap");
			bootstrapIndex.setAccessible(true);
			nameAndTypeIndex = clazz.getDeclaredField("nameAndType");
			nameAndTypeIndex.setAccessible(true);
		} catch (Exception ex) {
			throw new Error(ex);
		}
	}

	private Object item;

	public InvokeDynamicInfoAccessor(Object item) {
		this.item = item;
	}

	public static boolean isType(ConstInfoAccessor accessor) {
		return clazz.isAssignableFrom(accessor.getItem().getClass());
	}

	public int getBootstrapIndex() {
		try {
			return (Integer) bootstrapIndex.get(this.item);
		} catch (Exception ex) {
			throw new Error(ex);
		}
	}

	public void setBootstrapIndex(int val) {
		try {
			bootstrapIndex.set(this.item, val);
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
}
