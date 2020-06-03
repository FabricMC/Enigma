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

package cuchaz.enigma.translation.mapping.serde;

import java.io.File;
import java.util.function.Supplier;

public class MappingParseException extends Exception {

	private int line;
	private String message;
	private String filePath;

	public MappingParseException(File file, int line, String message) {
		this.line = line;
		this.message = message;
		filePath = file.getAbsolutePath();
	}

	public MappingParseException(Supplier<String> filenameProvider, int line, String message) {
		this.line = line;
		this.message = message;
		filePath = filenameProvider.get();
	}

	@Override
	public String getMessage() {
		return "Line " + line + ": " + message + " in file " + filePath;
	}
}
