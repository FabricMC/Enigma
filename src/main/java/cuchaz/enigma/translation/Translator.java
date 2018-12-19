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

package cuchaz.enigma.translation;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Type;

public interface Translator {
	<T extends Translatable> T translate(T translatable);

	// TODO: These can be static helpers? They are all specific to ASM
	Type translateType(Type type);

	Handle translateHandle(Handle handle);

	Object translateValue(Object value);
}
